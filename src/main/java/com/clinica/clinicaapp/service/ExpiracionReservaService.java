package com.clinica.clinicaapp.service;
import com.clinica.clinicaapp.repository.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ExpiracionReservaService {
    private final CitaRepository citas;
    private final TurnoRepository turnos;
    private final Clock clock;
    public ExpiracionReservaService(CitaRepository citas,TurnoRepository turnos,Clock clock) {
        this.citas=citas;this.turnos=turnos;this.clock=clock;
    }
    @Transactional
    public void expirar(Long id) {
        var cita=citas.bloquear(id).orElse(null);
        if(cita==null || !"EN_PROCESO".equals(cita.getEstado()))return;
        var ahora=LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima")));
        if(cita.getFechaBloqueoExpiracion()!=null && cita.getFechaBloqueoExpiracion().isAfter(ahora))return;
        var turno=turnos.bloquear(cita.getTurno().getId()).orElseThrow();
        cita.setEstado("EXPIRADA");
        if("EN_PROCESO".equals(turno.getEstado()))turno.setEstado("DISPONIBLE");
    }
}
