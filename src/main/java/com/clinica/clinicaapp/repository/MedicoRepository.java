package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Medico;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
    @EntityGraph(attributePaths = "especialidad")
    List<Medico> findAllByOrderByApellidosAscNombresAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.id = :id")
    Optional<Medico> bloquear(@Param("id") Long id);
}
