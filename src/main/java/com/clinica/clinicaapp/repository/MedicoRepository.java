package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Medico;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.id=:id")
    Optional<Medico> bloquear(@Param("id") Long id);
}
