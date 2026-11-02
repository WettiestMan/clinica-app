package com.clinica.clinicaapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:roles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop", "clinica.jefe.correo=", "clinica.jefe.password="})
@AutoConfigureMockMvc
class AccesoRolesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonimoNoAccedeAJefe() throws Exception {
        mvc.perform(get("/jefe/horarios")).andExpect(status().is3xxRedirection());
    }

    @Test
    void pacienteNoAccedeAJefeNiMedico() throws Exception {
        mvc.perform(get("/jefe/horarios").with(user("p").roles("PACIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/medico/agenda").with(user("p").roles("PACIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void medicoNoAsignaTurnos() throws Exception {
        mvc.perform(post("/jefe/horarios").with(user("m").roles("MEDICO")).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void jefePuedeVerFormularioConTokenCsrf() throws Exception {
        mvc.perform(get("/jefe/horarios").with(user("j").roles("JEFE_MEDICO"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    }

    @Test
    void jefeSinCsrfNoPuedeAsignar() throws Exception {
        mvc.perform(post("/jefe/horarios").with(user("j").roles("JEFE_MEDICO"))).andExpect(status().isForbidden());
    }
    @Autowired
    com.clinica.clinicaapp.repository.UsuarioPersonalRepository usuarios;
    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void loginRealDeJefeDirigeASuPanel() throws Exception {
        var jefe = new com.clinica.clinicaapp.entity.UsuarioPersonal();
        jefe.setCorreo("jefe-login@example.test");
        jefe.setPassword(encoder.encode("ClaveSegura123!"));
        jefe.setRol(com.clinica.clinicaapp.entity.Rol.JEFE_MEDICO);
        usuarios.saveAndFlush(jefe);
        mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin("/login").user("correo", "jefe-login@example.test").password("ClaveSegura123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/jefe/horarios")).andExpect(request().sessionAttribute("rol", "JEFE_MEDICO"));
    }
}
