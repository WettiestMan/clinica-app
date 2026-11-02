package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.EventoCancelacion;
import java.util.*;

public interface EventoCancelacionRepository
    extends org.springframework.data.jpa.repository.JpaRepository<EventoCancelacion, Long> {
  List<EventoCancelacion> findTop20ByTipoAndCancelacionCitaTurnoMedicoIdOrderByIdDesc(
      String tipo, Long medico);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select e from EventoCancelacion e where e.estado='PENDIENTE' and"
          + " e.tipo<>'AVISO_CUPO_LIBERADO' and e.proximoIntento<=:ahora order by e.id")
  List<EventoCancelacion> pendientes(
      @org.springframework.data.repository.query.Param("ahora") java.time.Instant ahora,
      org.springframework.data.domain.Pageable pagina);
}
