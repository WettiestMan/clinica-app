package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.ComprobantePago;
import com.clinica.clinicaapp.repository.ComprobantePagoRepository;
import org.springframework.beans.factory.annotation.Value;
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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final java.time.Clock clock;
    private final com.clinica.clinicaapp.repository.PacienteRepository pacientes;
    private final com.clinica.clinicaapp.repository.MedicoRepository medicos;
    private final com.clinica.clinicaapp.repository.UsuarioPersonalRepository usuarios;
    private final TurnoRepository turnoRepository;
    private final CitaRepository citaRepository;

    private final ComprobantePagoRepository pagos;
    @Value("${clinica.pago.simulado:false}")
    private boolean pagoSimulado;

    public CitaService(TurnoRepository turnoRepository, CitaRepository citaRepository,
                       ComprobantePagoRepository pagos, java.time.Clock clock,
            com.clinica.clinicaapp.repository.PacienteRepository pacientes,
            com.clinica.clinicaapp.repository.MedicoRepository medicos,
            com.clinica.clinicaapp.repository.UsuarioPersonalRepository usuarios) {
        this.pacientes = pacientes; this.medicos = medicos; this.usuarios = usuarios;
        this.clock = clock;
        this.pagos = pagos;
        this.turnoRepository = turnoRepository;
        this.citaRepository = citaRepository;
    }

    /**
     * RF-02 / HU-01: Bloquea el turno en estado temporal 'EN_PROCESO' y fija el temporizador de 15 minutos.
     */
   @Transactional
    public Cita iniciarProcesoEmision(Long turnoId, Paciente paciente) {
        return iniciarProcesoEmision(turnoId, paciente, null);
    }

    @Transactional
    public Cita iniciarProcesoEmision(Long turnoId, Paciente paciente, String asunto) {
        String asuntoLimpio = asunto == null ? null : asunto.trim();
        if (asuntoLimpio != null && asuntoLimpio.length() > 500)
            throw new IllegalArgumentException("El asunto debe tener como máximo 500 caracteres.");
        Turno referencia = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("El turno no existe"));
        medicos.bloquear(referencia.getMedico().getId())
                .orElseThrow(() -> new IllegalArgumentException("El médico no existe"));
        if (usuarios.existsByMedicoIdAndActivoFalse(referencia.getMedico().getId()))
            throw new IllegalStateException("El médico no tiene el acceso activo. Selecciona otro horario.");
        paciente = pacientes.bloquear(paciente.getId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe"));
        if (!paciente.isActivo()) throw new IllegalStateException("La cuenta del paciente está de baja.");
        entityManager.refresh(referencia, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Turno turno = referencia;
        if (!turno.isActivo()) throw new IllegalStateException("La disponibilidad fue dada de baja. Selecciona otro turno.");
        if (!"DISPONIBLE".equals(turno.getEstado())) {
            throw new IllegalStateException("El turno ya no se encuentra disponible.");
        }

        if (!turno.getFecha().atTime(turno.getHoraInicio()).isAfter(ahora()))
            throw new IllegalStateException("Selecciona un turno futuro.");

        // Bloqueo temporal
        turno.setEstado("EN_PROCESO");
        turnoRepository.save(turno);

        BigDecimal costoConsulta = turno.getMedico().getEspecialidad().getPrecioConsulta();

        Cita cita = new Cita();
        cita.setAsunto(asuntoLimpio);
        cita.setPaciente(paciente);
        cita.setTurno(turno);
        cita.setEstado("EN_PROCESO");
        cita.setMonto(costoConsulta);
        cita.setFechaRegistro(ahora());
        LocalDateTime limite=ahora().plusMinutes(15);
        LocalDateTime inicio=turno.getFecha().atTime(turno.getHoraInicio());
        cita.setFechaBloqueoExpiracion(inicio.isBefore(limite) ? inicio : limite);

        return citaRepository.save(cita);
    }
    /**
     * RF-03, RF-05, RF-08 / HU-02, HU-03: Procesa el pago, emite código único y cierra el turno como OCUPADO.
     */
    @Transactional(noRollbackFor = ReservaExpiradaException.class)
    public Cita confirmarPagoYEmision(Long citaId, String numeroTarjeta, String metodoPago) {
        Cita cita = citaRepository.bloquear(citaId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        if (("CONFIRMADA".equals(cita.getEstado()) || "REPROGRAMADA".equals(cita.getEstado()))
                && pagos.findByCitaIdAndEstado(citaId,"APROBADO").isPresent()) return cita;
        if (!"EN_PROCESO".equals(cita.getEstado()))
            throw new IllegalStateException("La cita no está pendiente de emisión.");
        turnoRepository.bloquear(cita.getTurno().getId())
                .orElseThrow(() -> new IllegalStateException("Turno no encontrado"));
        if (!cita.getTurno().getFecha().atTime(cita.getTurno().getHoraInicio()).isAfter(ahora())
                || cita.getFechaBloqueoExpiracion() == null ||
                !ahora().isBefore(cita.getFechaBloqueoExpiracion())) {
            liberarTurnoExpirado(cita);
            throw new ReservaExpiradaException("El tiempo de espera de 15 minutos ha expirado. El turno fue liberado.");
        }

        if (!pagoSimulado) {
            throw new IllegalStateException("No hay una pasarela de pago integrada. Activa el modo de pruebas para simular un pago.");
        }
        if (numeroTarjeta == null || numeroTarjeta.isBlank() || metodoPago == null || metodoPago.isBlank()) {
            throw new IllegalStateException("Completa los datos de prueba del pago.");
        }
        // Pago simulado para la demostración académica; no realiza ningún cobro.
        // RF-05: Generar identificador alfanumérico único
        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String codigoCita = "CIT-" + ahora().getYear() + "-" + sufijo;

        cita.setCodigoCita(codigoCita);
        cita.setEstado("CONFIRMADA");
        cita.setMetodoPago(metodoPago);
        cita.setNumeroOperacion("SIM-" + UUID.randomUUID());
        cita.setCodigo(cita.getCodigoVisible());
        ComprobantePago pago = new ComprobantePago();
        pago.setCita(cita);
        pago.setNumero(cita.getNumeroOperacion());
        pago.setEstado("APROBADO");
        pago.setImporte(cita.getMonto());
        pago.setMoneda("PEN");
        pagos.save(pago);

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

        if (!"CONFIRMADA".equalsIgnoreCase(cita.getEstado())
                && !"REPROGRAMADA".equalsIgnoreCase(cita.getEstado())) {
            throw new IllegalStateException("Solo se pueden postergar citas en estado Confirmada.");
        }

        Turno turnoActual = cita.getTurno();
        LocalDateTime fechaHoraCita = LocalDateTime.of(turnoActual.getFecha(), turnoActual.getHoraInicio());

        // Regla reglamentaria: mínimo 24 horas antes
        if (ahora().plusHours(24).isAfter(fechaHoraCita)) {
            throw new IllegalStateException("No se puede reprogramar: la solicitud debe realizarse con un mínimo de 24 horas de anticipación.");
        }
    }

    /**
     * HU-06, HU-07 / RF-12, RF-13, RF-14, RF-15:
     * - Libera el turno original a 'DISPONIBLE'
     * - Asigna el nuevo turno a 'OCUPADO'
     * - Genera nuevo código de validación de reprogramación
     */
    @Transactional
    public Cita reprogramarCita(Long citaId, Long nuevoTurnoId, Paciente paciente) {
        Cita cita = citaRepository.bloquear(citaId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        // Validar plazo reglamentario
        validarPlazoPostergacion(cita, paciente);

        Long anteriorId = cita.getTurno().getId();
        if (anteriorId.equals(nuevoTurnoId)) {
            throw new IllegalStateException("Selecciona un horario diferente.");
        }
        turnoRepository.bloquear(Math.min(anteriorId, nuevoTurnoId));
        turnoRepository.bloquear(Math.max(anteriorId, nuevoTurnoId));
        Turno nuevoTurno = turnoRepository.findById(nuevoTurnoId)
                .orElseThrow(() -> new IllegalArgumentException("El turno seleccionado no existe"));

        if (!cita.getTurno().getMedico().getId().equals(nuevoTurno.getMedico().getId())) {
            throw new IllegalStateException("Selecciona un turno del mismo médico.");
        }
        if (!LocalDateTime.of(nuevoTurno.getFecha(), nuevoTurno.getHoraInicio())
                .isAfter(ahora())) {
            throw new IllegalStateException("Selecciona un horario futuro.");
        }
        entityManager.refresh(nuevoTurno);
        if (!nuevoTurno.isActivo()) throw new IllegalStateException("El turno seleccionado fue dado de baja.");
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
        cita.setEstado("REPROGRAMADA");

        return citaRepository.save(cita);
    }
    private LocalDateTime ahora() {
        return LocalDateTime.now(clock.withZone(java.time.ZoneId.of("America/Lima")));
    }

    private static class ReservaExpiradaException extends IllegalStateException {
        ReservaExpiradaException(String mensaje) { super(mensaje); }
    }
}
