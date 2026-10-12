package com.clinica.clinicaapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.clinica.clinicaapp.entity.Paciente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.sql.init.mode=never",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class CancelacionWebTest {
  @Autowired MockMvc mvc;
  @Autowired jakarta.persistence.EntityManager em;

  @Test
  @org.springframework.transaction.annotation.Transactional
  void flujoCompletoConConstancia() throws Exception {
    var p = new com.clinica.clinicaapp.entity.Paciente();
    p.setNombres("Ana");
    p.setApellidos("Prueba");
    p.setDni("88888888");
    p.setCorreo("web@example.test");
    p.setPassword("hash");
    em.persist(p);
    var esp = new com.clinica.clinicaapp.entity.Especialidad();
    esp.setNombre("Cardiología");
    em.persist(esp);
    var doctor = new com.clinica.clinicaapp.entity.Medico();
    doctor.setNombres("Dr.");
    doctor.setApellidos("Tello");
    doctor.setEspecialidad(esp);
    em.persist(doctor);
    var turno = new com.clinica.clinicaapp.entity.Turno();
    turno.setMedico(doctor);
    turno.setFecha(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(3));
    turno.setHoraInicio(java.time.LocalTime.of(12, 0));
    turno.setHoraFin(java.time.LocalTime.of(12, 30));
    turno.setDuracionMinutos(30);
    turno.setEstado("OCUPADO");
    em.persist(turno);
    var cita = new com.clinica.clinicaapp.entity.Cita();
    cita.setPaciente(p);
    cita.setTurno(turno);
    cita.setEstado("EMITIDA");
    cita.setCodigo("CIT-WEB");
    em.persist(cita);
    em.flush();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("usuarioLogueado", p);
    String html =
        mvc.perform(get("/citas/cancelar").param("id", cita.getId().toString()).session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    java.nio.file.Files.writeString(java.nio.file.Path.of("target/sp03-formulario.html"), html);
    mvc.perform(get("/citas/mis-citas").session(session)).andExpect(status().isOk());
    mvc.perform(
            post("/citas/cancelar/confirmar")
                .session(session)
                .param("id", cita.getId().toString())
                .param("motivo", "Imprevisto personal")
                .param("csrf", (String) session.getAttribute("sp03Csrf")))
        .andExpect(status().is3xxRedirection());
    var baja =
        em.createQuery(
                "select b from Cancelacion b", com.clinica.clinicaapp.entity.Cancelacion.class)
            .getSingleResult();
    mvc.perform(get("/citas/cancelaciones/" + baja.getId()).session(session))
        .andExpect(status().isOk());
    byte[] pdf =
        mvc.perform(
                get("/citas/cancelaciones/" + baja.getId() + "/constancia.pdf").session(session))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    java.nio.file.Files.write(java.nio.file.Path.of("target/sp03-constancia.pdf"), pdf);
    MockHttpSession medico = new MockHttpSession();
    medico.setAttribute("medicoLogueado", doctor);
    mvc.perform(
            get("/medico/agenda/datos").param("fecha", turno.getFecha().toString()).session(medico))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contador").value(0))
        .andExpect(jsonPath("$.avisos[0].texto").value("Cupo liberado por cancelación"));
  }

  @Test
  void pacienteSinSesionRedirigido() throws Exception {
    mvc.perform(get("/citas/cancelar"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void formularioSeRenderiza() throws Exception {
    Paciente p = new Paciente();
    p.setId(1L);
    MockHttpSession s = new MockHttpSession();
    s.setAttribute("usuarioLogueado", p);
    mvc.perform(get("/citas/cancelar").session(s))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Código único de cita")));
  }

  @Test
  void tokenFalsoBloqueado() throws Exception {
    Paciente p = new Paciente();
    p.setId(1L);
    MockHttpSession s = new MockHttpSession();
    s.setAttribute("usuarioLogueado", p);
    mvc.perform(
            post("/citas/cancelar/confirmar")
                .session(s)
                .param("id", "1")
                .param("motivo", "Error al agendar")
                .param("csrf", "falso"))
        .andExpect(status().isForbidden());
  }

  @Test
  void agendaNoExpuestaAPaciente() throws Exception {
    Paciente p = new Paciente();
    p.setId(1L);
    MockHttpSession s = new MockHttpSession();
    s.setAttribute("usuarioLogueado", p);
    mvc.perform(get("/medico/agenda/datos").session(s)).andExpect(status().isForbidden());
  }
}
