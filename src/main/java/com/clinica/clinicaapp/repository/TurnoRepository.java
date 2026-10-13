package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // RF-01 / HU-01: Búsqueda dinámica con filtro opcional de especialidad y rango de fechas
    @Query("SELECT t FROM Turno t WHERE (:especialidadId IS NULL OR t.medico.especialidad.id = :especialidadId) " +
           "AND t.fecha BETWEEN :fechaDesde AND :fechaHasta " +
           "AND t.estado = 'DISPONIBLE' " +
           "ORDER BY t.fecha ASC, t.horaInicio ASC")
    List<Turno> buscarTurnosDisponibles(@Param("especialidadId") Long especialidadId,
                                        @Param("fechaDesde") LocalDate fechaDesde,
                                        @Param("fechaHasta") LocalDate fechaHasta);

    // HU-06 / RF-11: Turnos del mismo médico para reprogramación
    @Query("SELECT t FROM Turno t WHERE t.medico.id = :medicoId " +
           "AND t.fecha >= :fechaHoy " +
           "ORDER BY t.fecha ASC, t.horaInicio ASC")
    List<Turno> findTurnosByMedicoId(@Param("medicoId") Long medicoId,
                                     @Param("fechaHoy") LocalDate fechaHoy);
}