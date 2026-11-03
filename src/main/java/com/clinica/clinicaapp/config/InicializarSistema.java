package com.clinica.clinicaapp.config;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Inicialización no destructiva posterior al esquema de Hibernate. */
@Component
@Order(0)
public class InicializarSistema implements ApplicationRunner {
    private final DataSource datasource;
    private final JdbcTemplate jdbc;
    private final ControlSistemaRepository control;
    private final EspecialidadRepository especialidades;

    public InicializarSistema(DataSource datasource, JdbcTemplate jdbc, ControlSistemaRepository control,
            EspecialidadRepository especialidades) {
        this.datasource=datasource; this.jdbc=jdbc; this.control=control; this.especialidades=especialidades;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        boolean postgres;
        try (Connection connection=datasource.getConnection()) {
            postgres=connection.getMetaData().getDatabaseProductName().equals("PostgreSQL");
        }
        if (postgres) {
            jdbc.execute("SELECT pg_advisory_xact_lock(73194021)");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT lower(correo) FROM pacientes GROUP BY lower(correo) HAVING COUNT(*) > 1) x",Long.class)>0)
                throw new IllegalStateException("Hay pacientes con correos duplicados al ignorar mayúsculas. Revisa el diagnóstico SQL.");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM pacientes p JOIN usuarios_personal u ON lower(p.correo)=lower(u.correo)",Long.class)>0)
                throw new IllegalStateException("Un correo está asignado a paciente y personal. Corrige esa coincidencia.");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT turno_id FROM citas WHERE UPPER(estado) IN ('EN_PROCESO','PENDIENTE_PAGO','CONFIRMADA','EMITIDA','REPROGRAMADA') GROUP BY turno_id HAVING COUNT(*)>1) x",Long.class)>0)
                throw new IllegalStateException("Hay varias citas activas en el mismo turno. Revisa el diagnóstico SQL; no se borraron citas.");
            jdbc.execute("""
                    DO $$ DECLARE restr RECORD; BEGIN
                      FOR restr IN SELECT c.conname FROM pg_constraint c
                      WHERE c.conrelid='public.citas'::regclass AND c.contype='u'
                      AND c.conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='public.citas'::regclass AND attname='turno_id')]::smallint[]
                      LOOP EXECUTE format('ALTER TABLE public.citas DROP CONSTRAINT %I',restr.conname); END LOOP;
                    END $$
                    """);
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_cita_activa_por_turno ON citas(turno_id) WHERE UPPER(estado) IN ('EN_PROCESO','PENDIENTE_PAGO','CONFIRMADA','EMITIDA','REPROGRAMADA')");
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_personal_correo_ci ON usuarios_personal(lower(correo))");
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_paciente_correo_ci ON pacientes(lower(correo))");
            jdbc.execute("CREATE INDEX IF NOT EXISTS ix_turnos_medico_fecha ON turnos(medico_id,fecha,hora_inicio)");
            jdbc.execute("""
                    DO $$ DECLARE tabla TEXT; secuencia TEXT; maximo BIGINT; BEGIN
                      FOREACH tabla IN ARRAY ARRAY['especialidades','medicos','turnos','pacientes','citas','usuarios_personal','notificaciones_medico'] LOOP
                        secuencia:=pg_get_serial_sequence('public.'||tabla,'id');
                        IF secuencia IS NOT NULL THEN
                          EXECUTE format('SELECT COALESCE(MAX(id),0) FROM public.%I',tabla) INTO maximo;
                          PERFORM setval(secuencia::regclass,GREATEST(maximo,nextval(secuencia::regclass)),true);
                        END IF;
                      END LOOP;
                    END $$
                    """);
            jdbc.update("UPDATE usuarios_personal SET nombre=CASE WHEN rol='JEFE_MEDICO' THEN 'Jefe Médico' ELSE 'Médico' END WHERE nombre IS NULL OR nombre=''");
        }
        if (!control.existsById(1L)) control.saveAndFlush(new ControlSistema(1L));
        if (especialidades.count()==0) {
            String[] nombres={"Cardiologia","Pediatria","Medicina General","Dermatologia","Cirugia General","Oftalmologia","Traumatologia","Ginecologia","Odontologia"};
            int[] duraciones={30,30,15,30,120,30,30,30,45};
            String[] precios={"80.00","60.00","40.00","75.00","100.00","70.00","85.00","75.00","50.00"};
            for(int i=0;i<nombres.length;i++) {
                Especialidad e=new Especialidad();e.setNombre(nombres[i]);e.setDuracionMinutosDefault(duraciones[i]);
                e.setPrecioConsulta(new BigDecimal(precios[i]));especialidades.save(e);
            }
        }
    }
}
