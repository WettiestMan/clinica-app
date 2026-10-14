package com.clinica.clinicaapp;

import static org.junit.jupiter.api.Assertions.*;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest(
    properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({
  CancelacionService.class,
  PoliticaCancelacion.class,
  ProcesadorEventosCancelacion.class,
  CancelacionServiceTest.Reloj.class
})
class CancelacionServiceTest {
  @TestConfiguration
  static class Reloj {
    @Bean
    IntegracionesCancelacion integraciones() {
      return new IntegracionesNoConfiguradas();
    }

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-11T17:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired ProcesadorEventosCancelacion procesador;
  @Autowired EntityManager em;
  @Autowired CancelacionService service;
  @Autowired CancelacionRepository bajas;
  @Autowired EventoCancelacionRepository eventos;
  Paciente paciente;
  Turno turno;
  Cita cita;

  @BeforeEach
  void crear() {
    paciente = new Paciente();
    paciente.setNombres("Ana");
    paciente.setApellidos("Prueba");
    paciente.setDni("12345678");
    paciente.setCorreo("prueba@example.test");
    paciente.setPassword("hash");
    em.persist(paciente);
    Especialidad esp = new Especialidad();
    esp.setNombre("Cardiología");
    em.persist(esp);
    Medico medico = new Medico();
    medico.setNombres("Dr.");
    medico.setApellidos("Tello");
    medico.setEspecialidad(esp);
    em.persist(medico);
    turno = new Turno();
    turno.setMedico(medico);
    turno.setFecha(LocalDate.of(2026, 10, 12));
    turno.setHoraInicio(LocalTime.of(12, 0));
    turno.setHoraFin(LocalTime.of(12, 30));
    turno.setDuracionMinutos(30);
    turno.setEstado("OCUPADO");
    em.persist(turno);
    cita = new Cita();
    cita.setCodigo("CIT-PRUEBA");
    cita.setPaciente(paciente);
    cita.setTurno(turno);
    cita.setEstado("EMITIDA");
    em.persist(cita);
    em.flush();
  }

  void pagar() {
    ComprobantePago pago = new ComprobantePago();
    pago.setCita(cita);
    pago.setNumero("B001-123");
    pago.setEstado("APROBADO");
    pago.setImporte(new BigDecimal("100.00"));
    em.persist(pago);
    em.flush();
  }

  @Test
  void confirmadaConPagoCancelable() {
    pagar();
    cita.setEstado("CONFIRMADA");
    assertNotNull(service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false));
  }

  @Test
  void sinPagoNoCancelaNiLibera() {
    cita.setEstado("CONFIRMADA");
    assertThrows(ResponseStatusException.class,
        () -> service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false));
    assertEquals("CONFIRMADA", cita.getEstado());
    assertEquals("OCUPADO", turno.getEstado());
    assertEquals(0, bajas.count());
    assertEquals(0, eventos.count());
  }

  @Test
  void buscaCodigoVisibleYDeEmision() {
    cita.setCodigoCita("CIT-2026-ABCDEF");
    assertEquals(cita.getId(), service.buscar(cita.getCodigoVisible(), paciente.getId()).getId());
    assertEquals(cita.getId(), service.buscar("cit-2026-abcdef", paciente.getId()).getId());
  }

  @Test
  void dentroPlazoConPago() {
    pagar();
    Cancelacion b =
        service.confirmar(cita.getId(), paciente.getId(), "Imprevisto personal", "", false);
    assertEquals("CANCELADA", cita.getEstado());
    assertEquals("DISPONIBLE", turno.getEstado());
    assertEquals(new BigDecimal("100.00"), b.getMontoReintegrable());
    assertEquals(3, eventos.count());
    assertEquals(paciente.getId(), b.getUsuarioId());
    assertNotNull(b.getFecha());
  }

  @Test
  void fueraPlazoRequiereReconfirmacion() {
    pagar();
    turno.setFecha(LocalDate.of(2026, 10, 11));
    turno.setHoraInicio(LocalTime.of(18, 0));
    assertThrows(
        ResponseStatusException.class,
        () -> service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false));
    assertEquals("EMITIDA", cita.getEstado());
    assertEquals(0, bajas.count());
  }

  @Test
  void fueraPlazoConfirmadaNoEmiteNota() {
    pagar();
    turno.setFecha(LocalDate.of(2026, 10, 11));
    turno.setHoraInicio(LocalTime.of(18, 0));
    Cancelacion b = service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", true);
    assertEquals(0, b.getMontoReintegrable().signum());
    assertEquals(2, eventos.count());
  }

  @Test
  void atendidaBloqueada() {
    cita.setEstado("ATENDIDA");
    assertThrows(
        ResponseStatusException.class, () -> service.evaluar(cita.getId(), paciente.getId()));
  }

  @Test
  void inexistenteBloqueada() {
    assertThrows(
        ResponseStatusException.class, () -> service.buscar("NO-EXISTE", paciente.getId()));
  }

  @Test
  void ajenaBloqueada() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.confirmar(cita.getId(), 999L, "Error al agendar", "", false));
  }

  @Test
  void motivoObligatorio() {
    pagar();
    assertThrows(
        ResponseStatusException.class,
        () -> service.confirmar(cita.getId(), paciente.getId(), "", "", false));
  }

  @Test
  void otroRequiereDetalle() {
    pagar();
    assertThrows(
        ResponseStatusException.class,
        () -> service.confirmar(cita.getId(), paciente.getId(), "Otro", "", false));
  }

  @Test
  void reprogramadaCancelable() {
    pagar();
    cita.setEstado("REPROGRAMADA");
    assertNotNull(service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false));
  }

  @Test
  void confirmacionIdempotente() {
    pagar();
    Cancelacion b =
        service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false);
    assertEquals(
        b.getId(),
        service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false).getId());
    assertEquals(1, bajas.count());
    assertEquals(3, eventos.count());
  }

  @Test
  void historialPermiteNuevaReserva() {
    pagar();
    service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false);
    Cita nueva = new Cita();
    nueva.setPaciente(paciente);
    nueva.setTurno(turno);
    nueva.setCodigo("CIT-NUEVA");
    em.persist(nueva);
    em.flush();
    assertNotNull(nueva.getId());
    assertEquals("CANCELADA", cita.getEstado());
  }

  @Test
  void proveedorCaidoMantieneBajaYCola() {
    pagar();
    service.confirmar(cita.getId(), paciente.getId(), "Error al agendar", "", false);
    procesador.procesar();
    em.flush();
    assertEquals("CANCELADA", cita.getEstado());
    assertEquals("DISPONIBLE", turno.getEstado());
    assertEquals(3, eventos.count());
    for (EventoCancelacion e : eventos.findAll()) {
      assertEquals("PENDIENTE", e.getEstado());
      if (!e.getTipo().equals("AVISO_CUPO_LIBERADO")) {
        assertEquals(1, e.getIntentos());
        assertTrue(e.getProximoIntento().isAfter(Instant.parse("2026-10-11T17:00:00Z")));
      }
    }
  }

  @Test
  void limiteExacto24HorasIncluido() {
    pagar();
    assertTrue(service.evaluar(cita.getId(), paciente.getId()).dentroPlazo());
    turno.setHoraInicio(LocalTime.of(11, 59, 59));
    assertFalse(service.evaluar(cita.getId(), paciente.getId()).dentroPlazo());
  }
}
