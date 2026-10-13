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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Turno t WHERE t.id = :id")
    Optional<Turno> bloquear(@Param("id") Long id);

    @Query("""
        SELECT t FROM Turno t
        JOIN FETCH t.medico m
        JOIN FETCH m.especialidad e
        WHERE (:especialidadId IS NULL OR e.id = :especialidadId)
          AND t.fecha BETWEEN :desde AND :hasta
          AND t.estado = 'DISPONIBLE'
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
          AND t.estado = 'DISPONIBLE'
        ORDER BY t.fecha ASC, t.horaInicio ASC
        """)
    List<Turno> findTurnosByMedicoId(
            @Param("medicoId") Long medicoId,
            @Param("fechaHoy") LocalDate fechaHoy);
}