package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisponibilidadService {
    private final MedicoRepository medicos;
    private final TurnoRepository turnos;
    private final UsuarioPersonalRepository usuarios;
    private final NotificacionMedicoRepository notificaciones;
    private final Clock clock;
    private final CitaRepository citas;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public DisponibilidadService(MedicoRepository medicos, TurnoRepository turnos,
            UsuarioPersonalRepository usuarios, NotificacionMedicoRepository notificaciones, Clock clock, CitaRepository citas) {
        this.medicos = medicos;
        this.turnos = turnos;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.clock = clock;
        this.citas = citas;
    }

    @Transactional
    public int asignar(Long medicoId, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos) {
        return asignarInterno(medicoId, fecha, inicio, fin, minutos, null);
    }

    @Transactional
    public int asignar(Long medicoId, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos, String consultorio) {
        return asignarInterno(medicoId, fecha, inicio, fin, minutos, validarConsultorio(consultorio));
    }

    private String validarConsultorio(String valor) {
        if (valor == null || valor.isBlank() || valor.trim().length() > 60)
            throw new IllegalArgumentException("Indica el consultorio (máximo 60 caracteres).");
        return valor.trim();
    }

    @Transactional
    public void actualizarConsultorio(Long turnoId, String consultorio) {
        String valor = validarConsultorio(consultorio);
        var referencia = turnos.findById(turnoId).orElseThrow(() -> new IllegalArgumentException("Turno inexistente."));
        medicos.bloquear(referencia.getMedico().getId()).orElseThrow();
        entityManager.refresh(referencia, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var turno = referencia;
        if (!turno.isActivo()) throw new IllegalArgumentException("El turno está de baja.");
        turno.setConsultorio(valor);
        var aviso = new NotificacionMedico();
        aviso.setMedico(turno.getMedico());
        aviso.setCreada(LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima"))));
        aviso.setMensaje("Consultorio actualizado: " + turno.getFecha() + " a las " + turno.getHoraInicio() + ", " + valor + ".");
        notificaciones.save(aviso);
    }

    private Turno bloquearGestion(Long id, long version) {
        var referencia = turnos.findById(id).orElseThrow(() -> new IllegalArgumentException("Turno inexistente."));
        medicos.bloquear(referencia.getMedico().getId()).orElseThrow();
        entityManager.refresh(referencia, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var turno = referencia;
        if (turno.getVersion() != version) throw new IllegalArgumentException("El turno cambió. Vuelve a abrir el formulario.");
        if (!turno.isActivo()) throw new IllegalArgumentException("El turno ya está de baja.");
        if (!turno.getFecha().atTime(turno.getHoraInicio()).isAfter(ahora()))
            throw new IllegalArgumentException("Solo puedes administrar turnos futuros.");
        if (!"DISPONIBLE".equals(turno.getEstado()) || citas.contarActivasEnTurno(id) > 0)
            throw new IllegalArgumentException("El turno tiene una cita activa o en proceso de pago. Resuélvela antes de modificarlo.");
        return turno;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima")));
    }

    private void notificar(Turno turno, String mensaje) {
        var aviso = new NotificacionMedico();
        aviso.setMedico(turno.getMedico()); aviso.setCreada(ahora()); aviso.setMensaje(mensaje);
        notificaciones.save(aviso);
    }

    @Transactional
    public void darDeBaja(Long id, long version, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > 300)
            throw new IllegalArgumentException("Indica el motivo de baja (máximo 300 caracteres).");
        var turno = bloquearGestion(id, version);
        turno.setActivo(false); turno.setMotivoBaja(motivo.trim());
        notificar(turno, "Disponibilidad dada de baja: " + turno.getFecha() + " de " + turno.getHoraInicio()
                + " a " + turno.getHoraFin() + ". Motivo: " + motivo.trim());
    }

    @Transactional
    public Long editar(Long id, long version, LocalDate fecha, LocalTime inicio, LocalTime fin, String consultorio) {
        String ubicacion = validarConsultorio(consultorio);
        if (fecha == null || inicio == null || fin == null || !fecha.atTime(inicio).isAfter(ahora()) || !fin.isAfter(inicio))
            throw new IllegalArgumentException("Indica una fecha futura y una hora final posterior al inicio.");
        long minutos = Duration.between(inicio, fin).toMinutes();
        if (minutos < 5 || minutos > 240 || inicio.getSecond() != 0 || fin.getSecond() != 0 || inicio.getNano() != 0 || fin.getNano() != 0)
            throw new IllegalArgumentException("Cada turno debe durar entre 5 y 240 minutos, sin segundos.");
        var anterior = bloquearGestion(id, version);
        if (!usuarios.existsByMedicoIdAndActivoTrue(anterior.getMedico().getId()))
            throw new IllegalArgumentException("El médico debe tener su cuenta activa.");
        if (turnos.contarCruces(anterior.getMedico().getId(), id, fecha, inicio, fin) > 0)
            throw new IllegalArgumentException("El horario se superpone con otro turno del médico.");
        if (fecha.equals(anterior.getFecha()) && inicio.equals(anterior.getHoraInicio()) && fin.equals(anterior.getHoraFin()) && ubicacion.equals(anterior.getConsultorio()))
            throw new IllegalArgumentException("No hay cambios para guardar.");
        // Conservar el registro original evita alterar fechas de citas canceladas o expiradas.
        anterior.setActivo(false); anterior.setMotivoBaja("Reemplazado mediante edición de disponibilidad.");
        var nuevo = new Turno(); nuevo.setMedico(anterior.getMedico()); nuevo.setFecha(fecha);
        nuevo.setHoraInicio(inicio); nuevo.setHoraFin(fin); nuevo.setDuracionMinutos((int) minutos);
        nuevo.setConsultorio(ubicacion); nuevo.setEstado("DISPONIBLE");
        turnos.saveAndFlush(nuevo);
        notificar(anterior, "Disponibilidad modificada: " + anterior.getFecha() + " " + anterior.getHoraInicio()
                + " → " + fecha + " de " + inicio + " a " + fin + ", " + ubicacion + ".");
        return nuevo.getId();
    }

    private int asignarInterno(Long medicoId, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos, String consultorio) {
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima")));
        if (medicoId == null || fecha == null || inicio == null || fin == null)
            throw new IllegalArgumentException("Completa el médico, fecha y horario.");
        if (!fecha.atTime(inicio).isAfter(ahora) || !fin.isAfter(inicio))
            throw new IllegalArgumentException("Selecciona un horario futuro y una hora final posterior al inicio.");
        if (minutos < 5 || minutos > 240 || inicio.getSecond() != 0 || fin.getSecond() != 0
                || inicio.getNano() != 0 || fin.getNano() != 0)
            throw new IllegalArgumentException("Usa horas sin segundos y turnos de 5 a 240 minutos.");
        long duracion = Duration.between(inicio, fin).toMinutes();
        if (duracion % minutos != 0)
            throw new IllegalArgumentException("El intervalo debe dividirse exactamente en turnos completos.");

        // El bloqueo del médico evita cruces incluso si todavía no tiene turnos.
        Medico medico = medicos.bloquear(medicoId)
                .orElseThrow(() -> new IllegalArgumentException("El médico no existe."));
        if (!usuarios.existsByMedicoIdAndActivoTrue(medicoId))
            throw new IllegalArgumentException("Crea primero la cuenta de acceso de este médico.");
        if (turnos.existsByMedicoIdAndActivoTrueAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(medicoId, fecha, fin, inicio))
            throw new IllegalArgumentException("El médico ya tiene turnos que se superponen con ese horario.");
        int cantidad = (int) (duracion / minutos);
        for (int i = 0; i < cantidad; i++) {
            Turno turno = new Turno();
            turno.setMedico(medico);
            turno.setConsultorio(consultorio);
            turno.setFecha(fecha);
            turno.setHoraInicio(inicio.plusMinutes((long) i * minutos));
            turno.setHoraFin(inicio.plusMinutes((long) (i + 1) * minutos));
            turno.setDuracionMinutos(minutos);
            turno.setEstado("DISPONIBLE");
            turnos.save(turno);
        }
        NotificacionMedico aviso = new NotificacionMedico();
        aviso.setMedico(medico);
        aviso.setCreada(ahora);
        aviso.setMensaje("Disponibilidad asignada: " + fecha + " de " + inicio + " a " + fin
                + " (" + cantidad + " turnos de " + minutos + " minutos).");
        notificaciones.save(aviso);
        turnos.flush();
        return cantidad;
    }
}
