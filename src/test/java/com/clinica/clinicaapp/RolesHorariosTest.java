package com.clinica.clinicaapp;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({DisponibilidadService.class, PersonalService.class, CitaService.class, ExpiracionReservaService.class, RolesHorariosTest.Config.class})
class RolesHorariosTest {

    @TestConfiguration
    static class Config {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-28T17:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        PasswordEncoder encoder() {
            return new BCryptPasswordEncoder();
        }
    }
    @Autowired
    EntityManager em;
    @Autowired
    PersonalService personal;
    @Autowired
    DisponibilidadService disponibilidad;
    @Autowired
    CitaService citasService;
    @Autowired
    CitaRepository citas;
    @Autowired
    TurnoRepository turnos;
    @Autowired
    NotificacionMedicoRepository avisos;
    @Autowired
    UsuarioPersonalRepository usuarios;
    @Autowired
    ExpiracionReservaService expiracion;
    Medico medico;
    Paciente paciente;

    @BeforeEach
    void preparar() {
        var e = new Especialidad();
        e.setNombre("Pruebas");
        em.persist(e);
        medico = new Medico();
        medico.setNombres("Ana");
        medico.setApellidos("Médica");
        medico.setEspecialidad(e);
        em.persist(medico);
        paciente = new Paciente();
        paciente.setNombres("Luis");
        paciente.setApellidos("Paciente");
        paciente.setCorreo("paciente@example.test");
        paciente.setDni("87654321");
        paciente.setPassword("hash");
        em.persist(paciente);
        em.flush();
        personal.crearMedico(medico.getId(), "", "", null, "medico@example.test", "UnaClaveLarga123!");
    }

    @Test
    void vinculaMedicoSinDuplicarloYGuardaHash() {
        var u = usuarios.findByCorreoIgnoreCase("MEDICO@example.test").orElseThrow();
        assertEquals(medico.getId(), u.getMedico().getId());
        assertEquals(Rol.MEDICO, u.getRol());
        assertNotEquals("UnaClaveLarga123!", u.getPassword());
    }

    @Test
    void generaCuatroTurnosConNotificacionYRechazaCruce() {
        assertEquals(4, disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 29), LocalTime.of(8, 0), LocalTime.of(10, 0), 30));
        em.flush();
        assertEquals(4, turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medico.getId(), LocalDate.of(2026, 10, 29)).size());
        assertEquals(1, avisos.findTop50ByMedicoIdOrderByIdDesc(medico.getId()).size());
        assertThrows(IllegalArgumentException.class, () -> disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 29), LocalTime.of(9, 30), LocalTime.of(10, 30), 30));
    }

    @Test
    void aceptaIntervaloAdyacente() {
        disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 29), LocalTime.of(8, 0), LocalTime.of(9, 0), 30);
        em.flush();
        assertEquals(2, disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 29), LocalTime.of(9, 0), LocalTime.of(10, 0), 30));
    }

    @Test
    void rechazaPasadoEIntervaloIncompleto() {
        assertThrows(IllegalArgumentException.class, () -> disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 27), LocalTime.of(8, 0), LocalTime.of(9, 0), 30));
        assertThrows(IllegalArgumentException.class, () -> disponibilidad.asignar(medico.getId(), LocalDate.of(2026, 10, 29), LocalTime.of(8, 0), LocalTime.of(9, 10), 30));
    }

    Turno turno(LocalDate fecha) {
        var t = new Turno();
        t.setMedico(medico);
        t.setFecha(fecha);
        t.setHoraInicio(LocalTime.of(9, 0));
        t.setHoraFin(LocalTime.of(9, 30));
        t.setDuracionMinutos(30);
        t.setEstado("DISPONIBLE");
        em.persist(t);
        em.flush();
        return t;
    }

    @Test
    void reservaPasadaRechazada() {
        var t = turno(LocalDate.of(2026, 10, 27));
        assertThrows(IllegalStateException.class, () -> citasService.iniciarProcesoEmision(t.getId(), paciente));
    }

    @Test
    void vencimientoLiberaSinNavegadorYEsIdempotente() {
        var t = turno(LocalDate.of(2026, 10, 30));
        var c = citasService.iniciarProcesoEmision(t.getId(), paciente);
        c.setFechaBloqueoExpiracion(LocalDateTime.of(2026, 10, 28, 11, 59));
        em.flush();
        expiracion.expirar(c.getId());
        expiracion.expirar(c.getId());
        assertEquals("EXPIRADA", c.getEstado());
        assertEquals("DISPONIBLE", t.getEstado());
    }

    @Test
    void proximaReprogramadaYHistoricaConfirmadaSeSeparan() {
        var vieja = citasService.iniciarProcesoEmision(turno(LocalDate.of(2026, 10, 30)).getId(), paciente);
        vieja.setEstado("CONFIRMADA");
        vieja.getTurno().setFecha(LocalDate.of(2026, 10, 27));
        var futura = citasService.iniciarProcesoEmision(turno(LocalDate.of(2026, 10, 31)).getId(), paciente);
        futura.setEstado("REPROGRAMADA");
        em.flush();
        var hoy = LocalDate.of(2026, 10, 28);
        var hora = LocalTime.NOON;
        assertEquals(java.util.List.of(futura.getId()), citas.findProximasEn(paciente.getId(), hoy, hora).stream().map(Cita::getId).toList());
        assertEquals(java.util.List.of(vieja.getId()), citas.findHistoricasEn(paciente.getId(), hoy, hora).stream().map(Cita::getId).toList());
    }

    @Test
    void reprogramacionConMenosDe24HorasRechazada() {
        var c = citasService.iniciarProcesoEmision(turno(LocalDate.of(2026, 10, 29)).getId(), paciente);
        c.setEstado("REPROGRAMADA");
        assertThrows(IllegalStateException.class, () -> citasService.validarPlazoPostergacion(c, paciente));
    }
}
