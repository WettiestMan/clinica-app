package com.clinica.clinicaapp;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Solo se ejecuta con una base PostgreSQL desechable indicada explícitamente. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="TEST_POSTGRES_URL",matches=".+")
class PersistenciaPostgresTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.config.import",()->"");
        r.add("spring.datasource.url",()->System.getenv("TEST_POSTGRES_URL"));
        r.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_POSTGRES_USER","clinica_test"));
        r.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_POSTGRES_PASSWORD",""));
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto",()->"update");
        r.add("clinica.reservas.limpieza-activa",()->"false");
        r.add("spring.devtools.restart.enabled",()->"false");
    }
    @Autowired PersonalService personal;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired UsuarioPersonalRepository usuarios;
    @Autowired EspecialidadRepository especialidades;
    @Autowired TurnoRepository turnos;
    @Autowired NotificacionMedicoRepository avisos;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    Long crearMedico() {
        String correo="pg-"+UUID.randomUUID()+"@example.test";
        personal.crearMedico(null,"Médico","Postgres",especialidades.findAll().getFirst().getId(),correo,"ClaveMedicoPostgres123!");
        return usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getMedico().getId();
    }

    @Test
    void dosAsignacionesSimultaneasNoCreanCruces()throws Exception {
        Long medico=crearMedico();LocalDate fecha=LocalDate.now(ZoneId.of("America/Lima")).plusDays(8);
        CountDownLatch salida=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> asignar=()->{
            salida.await();
            try { disponibilidad.asignar(medico,fecha,LocalTime.of(8,0),LocalTime.of(10,0),30);return true; }
            catch(IllegalArgumentException conflicto){return false;}
        };
        try {
            var primera=pool.submit(asignar);var segunda=pool.submit(asignar);salida.countDown();
            int exitos=(primera.get(30,TimeUnit.SECONDS)?1:0)+(segunda.get(30,TimeUnit.SECONDS)?1:0);
            assertEquals(1,exitos);
            assertEquals(4,turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medico,fecha).size());
            assertEquals(1,avisos.findTop50ByMedicoIdOrderByIdDesc(medico).size());
        } finally {pool.shutdownNow();}
    }

    @Test
    void losDatosSiguenAlAbrirUnaNuevaInstanciaDeLaAplicacion() {
        Long medico=crearMedico();LocalDate fecha=LocalDate.now(ZoneId.of("America/Lima")).plusDays(9);
        disponibilidad.asignar(medico,fecha,LocalTime.of(10,0),LocalTime.of(11,0),30);
        var aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(medico).getFirst();aviso.setLeida(true);avisos.saveAndFlush(aviso);
        try(var otra=new SpringApplicationBuilder(ClinicaappApplication.class).run(
                "--server.port=0","--spring.config.import=",
                "--spring.datasource.url="+System.getenv("TEST_POSTGRES_URL"),
                "--spring.datasource.username="+System.getenv().getOrDefault("TEST_POSTGRES_USER","clinica_test"),
                "--spring.datasource.password="+System.getenv().getOrDefault("TEST_POSTGRES_PASSWORD",""),
                "--spring.jpa.hibernate.ddl-auto=update","--clinica.reservas.limpieza-activa=false","--spring.devtools.restart.enabled=false")) {
            assertEquals(2,otra.getBean(TurnoRepository.class).findByMedicoIdAndFechaOrderByHoraInicioAsc(medico,fecha).size());
            assertTrue(otra.getBean(NotificacionMedicoRepository.class).findById(aviso.getId()).orElseThrow().isLeida());
        }
    }

    @Test
    void existeRestriccionDeUnaCitaActivaPorTurno() {
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM pg_indexes WHERE schemaname='public' AND indexname='ux_cita_activa_por_turno'",Long.class));
    }
}
