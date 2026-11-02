package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.ComprobantePago;
import java.util.*;

public interface ComprobantePagoRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ComprobantePago, Long> {
  Optional<ComprobantePago> findByCitaIdAndEstado(Long id, String estado);
}
