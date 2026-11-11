package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Cita;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("""
        select count(c) from Cita c where c.turno.id = :turnoId
        and upper(c.estado) in ('CONFIRMADA','REPROGRAMADA','EMITIDA','EN_PROCESO','PENDIENTE_PAGO')
        """)
    long contarActivasEnTurno(@Param("turnoId") Long turnoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.id = :id")
    Optional<Cita> bloquear(@Param("id") Long id);

    Optional<Cita> findByCodigo(String codigo);

    Optional<Cita> findByCodigoCitaIgnoreCase(String codigo);

    Optional<Cita> findByCodigoIgnoreCase(String codigo);

    @Query("""
        SELECT c FROM Cita c
        WHERE c.turno.medico.id = :medico
          AND c.turno.fecha = :fecha
          AND UPPER(c.estado) IN ('CONFIRMADA','REPROGRAMADA','EMITIDA')
        ORDER BY c.turno.horaInicio ASC
        """)
    List<Cita> agenda(
            @Param("medico") Long medico,
            @Param("fecha") LocalDate fecha);

    List<Cita> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);

    @Query("""
        SELECT c FROM Cita c
        WHERE c.paciente.id = :pacienteId
          AND UPPER(c.estado) IN (
              'CONFIRMADA',
              'EN_PROCESO',
              'PENDIENTE_PAGO',
              'EMITIDA',
              'REPROGRAMADA'
          )
          AND (c.turno.fecha > :hoy OR (c.turno.fecha = :hoy AND c.turno.horaInicio > :hora))
        ORDER BY c.turno.fecha ASC, c.turno.horaInicio ASC
        """)
    List<Cita> findProximasEn(@Param("pacienteId") Long pacienteId, @Param("hoy") LocalDate hoy, @Param("hora") java.time.LocalTime hora);

    default List<Cita> findProximas(Long pacienteId) {
        var ahora = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        return findProximasEn(pacienteId, ahora.toLocalDate(), ahora.toLocalTime());
    }

    @Query("""
        SELECT c FROM Cita c
        WHERE c.paciente.id = :pacienteId
          AND UPPER(c.estado) NOT IN ('EXPIRADA', 'EN_PROCESO', 'PENDIENTE_PAGO')
          AND (
              UPPER(c.estado) IN ('ATENDIDA', 'CANCELADA')
              OR c.turno.fecha < :hoy
              OR (c.turno.fecha = :hoy AND c.turno.horaInicio <= :hora)
          )
        ORDER BY c.turno.fecha DESC, c.turno.horaInicio DESC
        """)
    List<Cita> findHistoricasEn(@Param("pacienteId") Long pacienteId, @Param("hoy") LocalDate hoy, @Param("hora") java.time.LocalTime hora);

    default List<Cita> findHistoricas(Long pacienteId) {
        var ahora = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        return findHistoricasEn(pacienteId, ahora.toLocalDate(), ahora.toLocalTime());
    }

    @Query("select c.id from Cita c where c.estado='EN_PROCESO' and (c.fechaBloqueoExpiracion is null or c.fechaBloqueoExpiracion <= :ahora)")
    List<Long> vencidas(@Param("ahora") java.time.LocalDateTime ahora);

    @Query("""
        select count(c) from Cita c
        where c.paciente.id = :id
          and upper(c.estado) in ('CONFIRMADA','REPROGRAMADA','EMITIDA','EN_PROCESO','PENDIENTE_PAGO')
          and (c.turno.fecha > :hoy or (c.turno.fecha = :hoy and c.turno.horaInicio > :hora))
        """)
    long contarActivasFuturasPaciente(@Param("id") Long id, @Param("hoy") LocalDate hoy,
            @Param("hora") java.time.LocalTime hora);

    @Query("""
        select count(c) from Cita c
        where c.turno.medico.id = :id
          and upper(c.estado) in ('CONFIRMADA','REPROGRAMADA','EMITIDA','EN_PROCESO','PENDIENTE_PAGO')
          and (c.turno.fecha > :hoy or (c.turno.fecha = :hoy and c.turno.horaInicio > :hora))
        """)
    long contarActivasFuturasMedico(@Param("id") Long id, @Param("hoy") LocalDate hoy,
            @Param("hora") java.time.LocalTime hora);
}