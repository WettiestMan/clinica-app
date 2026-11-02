package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.ControlSistema;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ControlSistemaRepository extends JpaRepository<ControlSistema, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ControlSistema c where c.id = 1")
    Optional<ControlSistema> bloquear();
}
