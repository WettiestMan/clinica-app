package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Cita;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.id=:id")
    Optional<Cita> bloquear(@Param("id") Long id);

    Optional<Cita> findByCodigo(String codigo);

    @Query(
            "select c from Cita c where c.turno.medico.id=:medico and c.turno.fecha=:fecha and"
            + " upper(c.estado) <> 'CANCELADA'")
    List<Cita> agenda(@Param("medico") Long medico, @Param("fecha") java.time.LocalDate fecha);

    List<Cita> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);
}
