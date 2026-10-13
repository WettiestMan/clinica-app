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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.id = :id")
    Optional<Cita> bloquear(@Param("id") Long id);

    Optional<Cita> findByCodigo(String codigo);

    Optional<Cita> findByCodigoIgnoreCase(String codigo);

    @Query("""
        SELECT c FROM Cita c
        WHERE c.turno.medico.id = :medico
          AND c.turno.fecha = :fecha
          AND UPPER(c.estado) <> 'CANCELADA'
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
        ORDER BY c.turno.fecha ASC, c.turno.horaInicio ASC
        """)
    List<Cita> findProximas(@Param("pacienteId") Long pacienteId);

    @Query("""
        SELECT c FROM Cita c
        WHERE c.paciente.id = :pacienteId
          AND UPPER(c.estado) IN (
              'ATENDIDA',
              'CANCELADA',
              'EXPIRADA'
          )
        ORDER BY c.turno.fecha DESC, c.turno.horaInicio DESC
        """)
    List<Cita> findHistoricas(@Param("pacienteId") Long pacienteId);
}