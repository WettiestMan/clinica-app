package com.clinica.clinicaapp;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RolesPersistenciaWebTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioPersonalRepository usuarios;
    @Autowired MedicoRepository medicos;
    @Autowired EspecialidadRepository especialidades;
    @Autowired TurnoRepository turnos;
    @Autowired NotificacionMedicoRepository avisos;
    @Autowired PacienteRepository pacientes;
    @Autowired CitaRepository citas;
    @Autowired CitaService citaService;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired PersonalService personal;
    @Autowired ExpiracionReservaService expiracion;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;
    static Long medicoId;
    static LocalDate fecha=LocalDate.now(ZoneId.of("America/Lima")).plusDays(5);

    MockHttpSession login(String correo,String clave,String destino)throws Exception {
        return (MockHttpSession)mvc.perform(formLogin("/login").user("correo",correo).password(clave))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(destino))
                .andReturn().getRequest().getSession(false);
    }

    @Test @Order(1)
    void configuracionInicialPersisteJefeYNoSePuedeRepetir()throws Exception {
        mvc.perform(get("/configuracion-inicial")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        mvc.perform(post("/configuracion-inicial").with(csrf()).param("nombre","Jefa Prueba")
                .param("correo","jefa@example.test").param("password","ClaveJefaSegura123!")
                .param("confirmPassword","ClaveJefaSegura123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?jefeCreado"));
        assertTrue(usuarios.existsByRol(Rol.JEFE_MEDICO));
        assertNotEquals("ClaveJefaSegura123!",usuarios.findByCorreoIgnoreCase("jefa@example.test").orElseThrow().getPassword());
        mvc.perform(post("/configuracion-inicial").with(csrf()).param("nombre","Otro Jefe")
                .param("correo","otro@example.test").param("password","ClaveOtraSegura123!").param("confirmPassword","ClaveOtraSegura123!"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(2)
    void jefeCreaMedicoYTurnosQueSeRecuperanTrasCerrarSesion()throws Exception {
        var sesion=login("jefa@example.test","ClaveJefaSegura123!","/jefe/horarios");
        var esp=especialidades.findAll().getFirst();
        mvc.perform(post("/jefe/medicos").session(sesion).with(csrf())
                .param("nombres","Elena").param("apellidos","Ruiz").param("especialidadId",esp.getId().toString())
                .param("correo","elena@example.test").param("password","ClaveMedicaSegura123!"))
                .andExpect(status().is3xxRedirection());
        medicoId=usuarios.findByCorreoIgnoreCase("elena@example.test").orElseThrow().getMedico().getId();
        mvc.perform(post("/jefe/horarios").session(sesion).with(csrf())
                .param("medicoId",medicoId.toString()).param("fecha",fecha.toString()).param("inicio","08:00").param("fin","10:00").param("minutos","30").param("consultorio","203"))
                .andExpect(status().is3xxRedirection());
        assertEquals(4,turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoId,fecha).size());
        assertEquals(1,avisos.findTop50ByMedicoIdOrderByIdDesc(medicoId).size());
        mvc.perform(get("/jefe/horarios").session(sesion).param("medicoId",medicoId.toString()).param("fechaConsulta",fecha.toString()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("elena@example.test")));
        mvc.perform(post("/logout").session(sesion).with(csrf())).andExpect(status().is3xxRedirection());
        var nueva=login("jefa@example.test","ClaveJefaSegura123!","/jefe/horarios");
        mvc.perform(get("/jefe/horarios").session(nueva).param("medicoId",medicoId.toString()).param("fechaConsulta",fecha.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("turnosAsignados"));
    }

    @Test @Order(3)
    void pacienteReservaYPagaYMedicoVeLaCitaReal()throws Exception {
        mvc.perform(post("/registro").with(csrf()).param("id","1").param("nombres","Luis").param("apellidos","Paciente")
                .param("dni","77777777").param("correo","luis@example.test").param("password","ClavePaciente123!"))
                .andExpect(redirectedUrl("/login?registroExitoso"));
        var paciente=login("luis@example.test","ClavePaciente123!","/citas/agendar");
        mvc.perform(get("/citas/agendar").session(paciente).param("fechaDesde",fecha.toString()).param("fechaHasta",fecha.toString()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Elena")));
        Long turnoId=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoId,fecha).getFirst().getId();
        mvc.perform(post("/citas/confirmar").session(paciente).with(csrf()).param("turnoId",turnoId.toString()))
                .andExpect(status().isOk());
        mvc.perform(post("/citas/iniciar-pago").session(paciente).with(csrf()).param("turnoId",turnoId.toString()))
                .andExpect(status().is3xxRedirection());
        var cita=citas.findByPacienteIdOrderByFechaRegistroDesc(pacientes.findByCorreoIgnoreCase("luis@example.test").orElseThrow().getId()).getFirst();
        mvc.perform(get("/citas/pago/"+cita.getId()).session(paciente)).andExpect(status().isOk());
        mvc.perform(post("/citas/procesar-pago").session(paciente).with(csrf()).param("citaId",cita.getId().toString())
                .param("numeroTarjeta","DATOS-DE-PRUEBA").param("metodoPago","SIMULADO"))
                .andExpect(redirectedUrl("/citas/comprobante/"+cita.getId()));
        mvc.perform(get("/citas/comprobante/"+cita.getId()).session(paciente)).andExpect(status().isOk());
        mvc.perform(get("/citas/mis-citas").session(paciente)).andExpect(status().isOk());
        mvc.perform(get("/citas/detalle/"+cita.getId()).session(paciente)).andExpect(status().isOk());
        assertEquals("OCUPADO",turnos.findById(turnoId).orElseThrow().getEstado());
        var medico=login("elena@example.test","ClaveMedicaSegura123!","/medico/agenda");
        mvc.perform(get("/medico/agenda").session(medico)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sc-toast-region")));
        mvc.perform(get("/medico/agenda/datos").session(medico).param("fecha",fecha.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contador").value(1))
                .andExpect(jsonPath("$.citas[0].paciente").value("Luis Paciente"));
        mvc.perform(get("/medico/turnos").session(medico).param("fecha",fecha.toString())).andExpect(status().isOk());
        mvc.perform(get("/medico/notificaciones").session(medico)).andExpect(status().isOk());
        var aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(medicoId).getFirst();
        mvc.perform(post("/medico/notificaciones/"+aviso.getId()+"/leer").session(medico).with(csrf())).andExpect(status().is3xxRedirection());
        assertTrue(avisos.findById(aviso.getId()).orElseThrow().isLeida());
        assertEquals(4,turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoId,fecha).size());
    }

    @Test @Order(4)
    void rolesCsrfYMedicoAjenoQuedanProtegidos()throws Exception {
        mvc.perform(get("/jefe/horarios")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/jefe/horarios").with(user("p").roles("PACIENTE"))).andExpect(status().isForbidden());
        mvc.perform(post("/jefe/horarios").with(user("elena@example.test").roles("MEDICO")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/jefe/horarios").with(user("jefa@example.test").roles("JEFE_MEDICO"))).andExpect(status().isForbidden());
        personal.crearMedico(null,"Otro","Médico",especialidades.findAll().getFirst().getId(),"otro-medico@example.test","ClaveOtroMedico123!");
        var otro=login("otro-medico@example.test","ClaveOtroMedico123!","/medico/agenda");
        mvc.perform(get("/medico/agenda/datos").session(otro).param("fecha",fecha.toString())).andExpect(jsonPath("$.contador").value(0));
        Long aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(medicoId).getFirst().getId();
        mvc.perform(post("/medico/notificaciones/"+aviso+"/leer").session(otro).with(csrf())).andExpect(status().isNotFound());
    }

    @Test @Order(5)
    void rechazaCrucesPasadoYDuracionIncompletaSinCrearAvisos() {
        long antes=avisos.count();
        assertThrows(IllegalArgumentException.class,()->disponibilidad.asignar(medicoId,fecha,LocalTime.of(9,30),LocalTime.of(10,30),30));
        assertThrows(IllegalArgumentException.class,()->disponibilidad.asignar(medicoId,LocalDate.now().minusDays(1),LocalTime.of(8,0),LocalTime.of(9,0),30));
        assertThrows(IllegalArgumentException.class,()->disponibilidad.asignar(medicoId,fecha.plusDays(1),LocalTime.of(8,0),LocalTime.of(9,10),30));
        assertEquals(antes,avisos.count());
        assertEquals(2,disponibilidad.asignar(medicoId,fecha,LocalTime.of(10,0),LocalTime.of(11,0),30));
    }

    @Test @Order(6)
    void reservaVencidaLiberaTurnoYSeConservaElHistorial() {
        var turno=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoId,fecha).stream()
                .filter(t->t.getEstado().equals("DISPONIBLE")).findFirst().orElseThrow();
        var paciente=pacientes.findByCorreoIgnoreCase("luis@example.test").orElseThrow();
        Long id=tx.execute(status->{
            var cita=citaService.iniciarProcesoEmision(turno.getId(),paciente);
            cita.setFechaBloqueoExpiracion(LocalDateTime.now(ZoneId.of("America/Lima")).minusMinutes(1));
            return cita.getId();
        });
        expiracion.expirar(id); expiracion.expirar(id);
        assertEquals("EXPIRADA",citas.findById(id).orElseThrow().getEstado());
        assertEquals("DISPONIBLE",turnos.findById(turno.getId()).orElseThrow().getEstado());
        var nueva=citaService.iniciarProcesoEmision(turno.getId(),paciente);
        assertNotEquals(id,nueva.getId());
        assertTrue(citas.existsById(id));
    }
}
