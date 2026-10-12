package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CitaService {

    private final TurnoRepository turnoRepository;
    private final CitaRepository citaRepository;

    public CitaService(TurnoRepository turnoRepository, CitaRepository citaRepository) {
        this.turnoRepository = turnoRepository;
        this.citaRepository = citaRepository;
    }

    /**
     * RF-02 / HU-01: Bloquea el turno en estado temporal 'EN_PROCESO' y fija el temporizador de 15 minutos.
     */
    @Transactional
    public Cita iniciarProcesoEmision(Long turnoId, Paciente paciente) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("El turno no existe"));

        if (!"DISPONIBLE".equals(turno.getEstado())) {
            throw new IllegalStateException("El turno ya no se encuentra disponible.");
        }

        // Bloqueo temporal
        turno.setEstado("EN_PROCESO");
        turnoRepository.save(turno);

        // Definición de precio según duración o especialidad
        BigDecimal costoConsulta = (turno.getDuracionMinutos() <= 15) ? new BigDecimal("45.00") : new BigDecimal("70.00");

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setTurno(turno);
        cita.setEstado("EN_PROCESO");
        cita.setMonto(costoConsulta);
        cita.setFechaRegistro(LocalDateTime.now()); // <-- Asignar fecha_registro
        cita.setFechaBloqueoExpiracion(LocalDateTime.now().plusMinutes(15)); // RF-04: 15 minutos de temporizador

        return citaRepository.save(cita);
    }

    /**
     * RF-03, RF-05, RF-08 / HU-02, HU-03: Procesa el pago, emite código único y cierra el turno como OCUPADO.
     */
    @Transactional
    public Cita confirmarPagoYEmision(Long citaId, String numeroTarjeta, String metodoPago) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        // Validar expiración del temporizador (RF-04)
        if (LocalDateTime.now().isAfter(cita.getFechaBloqueoExpiracion()) || !"EN_PROCESO".equals(cita.getEstado())) {
            liberarTurnoExpirado(cita);
            throw new IllegalStateException("El tiempo de espera de 15 minutos ha expirado. El turno fue liberado.");
        }

        // RF-05: Generar identificador alfanumérico único
        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String codigoCita = "CIT-2026-" + sufijo;

        cita.setCodigoCita(codigoCita);
        cita.setEstado("CONFIRMADA");
        cita.setMetodoPago(metodoPago);
        cita.setNumeroOperacion("OP-" + System.currentTimeMillis() % 1000000);

        // RF-08: Transición definitiva del turno
        Turno turno = cita.getTurno();
        turno.setEstado("OCUPADO");
        turnoRepository.save(turno);

        return citaRepository.save(cita);
    }

    /**
     * RF-04: Libera el horario devolviéndolo a DISPONIBLE si vence el temporizador o el usuario cancela.
     */
    @Transactional
    public void liberarTurnoExpirado(Cita cita) {
        Turno turno = cita.getTurno();
        if (turno != null) {
            turno.setEstado("DISPONIBLE");
            turnoRepository.save(turno);
        }
        cita.setEstado("EXPIRADA");
        citaRepository.save(cita);
    }
}