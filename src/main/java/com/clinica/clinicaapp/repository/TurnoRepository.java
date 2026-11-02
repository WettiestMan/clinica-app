package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Turno;
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
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    boolean existsByMedicoId(Long medicoId);

    boolean existsByMedicoIdAndActivoTrueAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long medicoId, LocalDate fecha, java.time.LocalTime fin, java.time.LocalTime inicio);

    @Query("""
        select count(t) from Turno t where t.medico.id = :medicoId and t.activo = true
          and t.id <> :excluir and t.fecha = :fecha and t.horaInicio < :fin and t.horaFin > :inicio
        """)
    long contarCruces(@Param("medicoId") Long medicoId, @Param("excluir") Long excluir,
        @Param("fecha") LocalDate fecha, @Param("inicio") java.time.LocalTime inicio,
        @Param("fin") java.time.LocalTime fin);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"medico", "medico.especialidad"})
    List<Turno> findByMedicoIdAndFechaOrderByHoraInicioAsc(Long medicoId, LocalDate fecha);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Turno t WHERE t.id = :id")
    Optional<Turno> bloquear(@Param("id") Long id);

    @Query("""
    SELECT t FROM Turno t
    JOIN FETCH t.medico m
    JOIN FETCH m.especialidad e
    WHERE (:especialidadId IS NULL OR e.id = :especialidadId)
      AND t.activo = true
      AND t.fecha BETWEEN :desde AND :hasta
      AND NOT EXISTS (SELECT u.id FROM UsuarioPersonal u WHERE u.medico.id = m.id AND u.activo = false)
    ORDER BY t.fecha ASC, m.apellidos ASC, t.horaInicio ASC
    """)
    List<Turno> buscarTurnosDisponibles(
            @Param("especialidadId") Long especialidadId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
        SELECT t FROM Turno t
        WHERE t.medico.id = :medicoId
          AND t.fecha >= :fechaHoy
          AND t.activo = true
          AND t.estado = 'DISPONIBLE'
          AND NOT EXISTS (SELECT u.id FROM UsuarioPersonal u WHERE u.medico.id = :medicoId AND u.activo = false)
        ORDER BY t.fecha ASC, t.horaInicio ASC
        """)
    List<Turno> findTurnosByMedicoId(
            @Param("medicoId") Long medicoId,
            @Param("fechaHoy") LocalDate fechaHoy);
}
