package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.EventoCancelacionRepository;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesadorEventosCancelacion {
  private final EventoCancelacionRepository eventos;
  private final IntegracionesCancelacion integraciones;
  private final Clock clock;

  public ProcesadorEventosCancelacion(
      EventoCancelacionRepository e, IntegracionesCancelacion i, Clock c) {
    eventos = e;
    integraciones = i;
    clock = c;
  }

  @Scheduled(fixedDelayString = "${clinica.cancelacion.reintento-ms:10000}")
  @Transactional
  public void procesar() {
    for (EventoCancelacion e : eventos.pendientes(clock.instant(), PageRequest.of(0, 10))) {
      if ("AVISO_CUPO_LIBERADO".equals(e.getTipo()))
        continue; // Persistido para consumo autenticado por la futura agenda médica.
      try {
        String ref = integraciones.enviar(e, "SP03-" + e.getId());
        if (ref == null || ref.isBlank())
          throw new IllegalStateException("El proveedor no devolvió confirmación");
        e.setReferenciaExterna(ref);
        e.setEstado("ENVIADO");
        e.setUltimoError(null);
      } catch (Exception ex) {
        e.setIntentos(e.getIntentos() + 1);
        e.setUltimoError("Integración no disponible; se reintentará.");
        e.setProximoIntento(
            clock
                .instant()
                .plusSeconds(Math.min(3600, 30L * (1L << Math.min(e.getIntentos(), 6)))));
      }
    }
  }
}
