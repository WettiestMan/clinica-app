package com.clinica.clinicaapp;

import com.clinica.clinicaapp.controller.JefeMedicoController;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Regresión de la conversión localizada de LocalDate en RedirectAttributes. */
class JefeMedicoFechaRedireccionTest {
    private final DisponibilidadService disponibilidad = mock(DisponibilidadService.class);

    private MockMvc mvc() {
        var controller = new JefeMedicoController(mock(MedicoRepository.class),
                mock(EspecialidadRepository.class), mock(UsuarioPersonalRepository.class),
                mock(TurnoRepository.class), mock(PersonalService.class), disponibilidad);
        // Se prueba el binding y la redirección MVC, sin arrancar una base ni renderizar HTML.
        return MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers((nombre, locale) -> {
                    if (nombre.startsWith("redirect:"))
                        return new org.springframework.web.servlet.view.RedirectView(nombre.substring(9));
                    return new AbstractView() {
                    @Override
                    protected void renderMergedOutputModel(Map<String, Object> model,
                            HttpServletRequest request, HttpServletResponse response) {
                        response.setStatus(200);
                    }
                    };
                }).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"es-PE", "es-ES", "en-US"})
    void guardaYRedirigeConFechaIsoIndependienteDeLaRegion(String region) throws Exception {
        LocalDate fecha = LocalDate.of(2026, 11, 3);
        when(disponibilidad.asignar(7L, fecha, LocalTime.of(8, 0), LocalTime.of(10, 0), 30, "203")).thenReturn(4);
        MockMvc mvc = mvc();
        var respuesta = mvc.perform(post("/jefe/horarios").locale(Locale.forLanguageTag(region))
                .param("medicoId", "7").param("fecha", "2026-11-03")
                .param("inicio", "08:00").param("fin", "10:00").param("minutos", "30").param("consultorio", "203"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = respuesta.getResponse().getRedirectedUrl();
        assertEquals("/jefe/horarios?medicoId=7&fechaConsulta=2026-11-03", destino);
        mvc.perform(get(URI.create(destino)).locale(Locale.forLanguageTag(region)))
                .andExpect(status().isOk()).andExpect(model().attribute("fechaConsulta", fecha));
        verify(disponibilidad).asignar(7L, fecha, LocalTime.of(8, 0), LocalTime.of(10, 0), 30, "203");
    }

    @Test
    void tambienConservaFormatoIsoCuandoLaAsignacionEsRechazada() throws Exception {
        when(disponibilidad.asignar(anyLong(), any(), any(), any(), anyInt(), anyString()))
                .thenThrow(new IllegalArgumentException("Horario superpuesto."));
        mvc().perform(post("/jefe/horarios").locale(Locale.forLanguageTag("es-PE"))
                .param("medicoId", "7").param("fecha", "2026-11-03")
                .param("inicio", "08:00").param("fin", "10:00").param("minutos", "30").param("consultorio", "203"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jefe/horarios?medicoId=7&fechaConsulta=2026-11-03"))
                .andExpect(flash().attribute("error", "Horario superpuesto."));
    }
}
