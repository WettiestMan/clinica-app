package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Cancelacion;
import java.util.*;

public interface CancelacionRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Cancelacion, Long> {
  Optional<Cancelacion> findByCitaId(Long id);
}
