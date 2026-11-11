package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Turno;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    boolean existsByMedicoId(Long medicoId);

    boolean existsByMedicoIdAndActivoTrueAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long medicoId, LocalDate fecha, LocalTime fin, LocalTime inicio);

    @Query("""
        SELECT count(t) FROM Turno t 
        WHERE t.medico.id = :medicoId AND t.activo = true
          AND t.id <> :excluir AND t.fecha = :fecha 
          AND t.horaInicio < :fin AND t.horaFin > :inicio
        """)
    long contarCruces(@Param("medicoId") Long medicoId, 
                      @Param("excluir") Long excluir,
                      @Param("fecha") LocalDate fecha, 
                      @Param("inicio") LocalTime inicio,
                      @Param("fin") LocalTime fin);

    @EntityGraph(attributePaths = {"medico", "medico.especialidad"})

    List<Turno> findByMedicoIdAndFechaBetweenAndActivoTrueOrderByHoraInicioAsc(
            Long medicoId, LocalDate fechaInicio, LocalDate fechaFin);
            
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
          AND t.estado = 'DISPONIBLE'
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

    boolean existsByMedicoIdAndFechaAndHoraInicio(Long medicoId, LocalDate fecha, LocalTime horaInicio);

    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.activo = true
          AND LOWER(TRIM(t.consultorio)) = LOWER(TRIM(:consultorio))
          AND t.fecha = :fecha
          AND t.medico.id <> :medicoId
          AND t.horaInicio < :fin
          AND t.horaFin > :inicio
        """)
    boolean existeConflictoConsultorio(
            @Param("consultorio") String consultorio,
            @Param("fecha") LocalDate fecha,
            @Param("inicio") LocalTime inicio,
            @Param("fin") LocalTime fin,
            @Param("medicoId") Long medicoId);
}