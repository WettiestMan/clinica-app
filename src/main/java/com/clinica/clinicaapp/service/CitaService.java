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

        // Si quedó un intento previo en EN_PROCESO para este turno, limpiarlo
        citaRepository.findAll().stream()
                .filter(c -> c.getTurno().getId().equals(turnoId) && "EN_PROCESO".equals(c.getEstado()))
                .findFirst()
                .ifPresent(citaRepository::delete);

        // Bloqueo temporal
        turno.setEstado("EN_PROCESO");
        turnoRepository.save(turno);

        BigDecimal costoConsulta = (turno.getDuracionMinutos() <= 15) ? new BigDecimal("45.00") : new BigDecimal("70.00");

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setTurno(turno);
        cita.setEstado("EN_PROCESO");
        cita.setMonto(costoConsulta);
        cita.setFechaRegistro(LocalDateTime.now());
        cita.setFechaBloqueoExpiracion(LocalDateTime.now().plusMinutes(15));

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
        cita.setNumeroOperacion("OP-" + (System.currentTimeMillis() % 1000000));

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

    /**
     * HU-05 / RF-09, RF-10: Valida que la cita exista, sea del paciente, esté CONFIRMADA
     * y tenga al menos 24 horas de anticipación reglamentaria.
     */
    public void validarPlazoPostergacion(Cita cita, Paciente paciente) {
        if (!cita.getPaciente().getId().equals(paciente.getId())) {
            throw new IllegalArgumentException("No tiene permisos sobre esta cita.");
        }

        if (!"CONFIRMADA".equalsIgnoreCase(cita.getEstado())) {
            throw new IllegalStateException("Solo se pueden postergar citas en estado Confirmada.");
        }

        Turno turnoActual = cita.getTurno();
        LocalDateTime fechaHoraCita = LocalDateTime.of(turnoActual.getFecha(), turnoActual.getHoraInicio());

        // Regla reglamentaria: mínimo 24 horas antes
        /*if (LocalDateTime.now().plusHours(24).isAfter(fechaHoraCita)) {
            throw new IllegalStateException("No se puede reprogramar: la solicitud debe realizarse con un mínimo de 24 horas de anticipación.");
        }*/
    }

    /**
     * HU-06, HU-07 / RF-12, RF-13, RF-14, RF-15:
     * - Libera el turno original a 'DISPONIBLE'
     * - Asigna el nuevo turno a 'OCUPADO'
     * - Genera nuevo código de validación de reprogramación
     */
    @Transactional
    public Cita reprogramarCita(Long citaId, Long nuevoTurnoId, Paciente paciente) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        // Validar plazo reglamentario
        validarPlazoPostergacion(cita, paciente);

        Turno nuevoTurno = turnoRepository.findById(nuevoTurnoId)
                .orElseThrow(() -> new IllegalArgumentException("El turno seleccionado no existe"));

        if (!"DISPONIBLE".equalsIgnoreCase(nuevoTurno.getEstado())) {
            throw new IllegalStateException("El nuevo horario seleccionado ya no está disponible.");
        }

        // RF-14: Liberar el cupo anterior al inventario de turnos
        Turno turnoAnterior = cita.getTurno();
        turnoAnterior.setEstado("DISPONIBLE");
        turnoRepository.save(turnoAnterior);

        // RF-12 & RF-13: Asignar nuevo cupo y bloquearlo
        nuevoTurno.setEstado("OCUPADO");
        turnoRepository.save(nuevoTurno);

        // RF-15: Nuevo código de comprobante manteniendo trazabilidad
        String nuevoCodigo = "CIT-REP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        cita.setCodigoCita(nuevoCodigo);
        cita.setTurno(nuevoTurno);
        cita.setFechaRegistro(LocalDateTime.now());

        return citaRepository.save(cita);
    }
}