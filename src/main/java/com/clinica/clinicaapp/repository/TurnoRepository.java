package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Turno;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Turno t where t.id=:id")
    java.util.Optional<Turno> bloquear(
            @org.springframework.data.repository.query.Param("id") Long id);

    @Query(
            "SELECT t FROM Turno t JOIN FETCH t.medico m JOIN FETCH m.especialidad e "
            + "WHERE (:especialidadId IS NULL OR e.id = :especialidadId) "
            + "AND t.fecha BETWEEN :desde AND :hasta "
            + "ORDER BY m.apellidos, t.horaInicio")
    List<Turno> buscarTurnosDisponibles(
            @Param("especialidadId") Long especialidadId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
