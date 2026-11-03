package com.clinica.clinicaapp.service;
import com.clinica.clinicaapp.repository.CitaRepository;
import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="clinica.reservas.limpieza-activa",havingValue="true",matchIfMissing=true)
public class ExpiracionReservasJob {
    private final CitaRepository citas;
    private final ExpiracionReservaService servicio;
    private final Clock clock;
    public ExpiracionReservasJob(CitaRepository citas,ExpiracionReservaService servicio,Clock clock) {
        this.citas=citas;this.servicio=servicio;this.clock=clock;
    }
    @Scheduled(fixedDelayString="${clinica.reservas.revision-ms:30000}")
    public void ejecutar() {
        for(Long id:citas.vencidas(LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima")))))servicio.expirar(id);
    }
}
