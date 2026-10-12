package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CancelacionService {
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
  public static final List<String> MOTIVOS =
      List.of("Ya no lo necesito", "Error al agendar", "Imprevisto personal", "Otro");
  private final CitaRepository citas;
  private final CancelacionRepository bajas;
  private final ComprobantePagoRepository pagos;
  private final EventoCancelacionRepository eventos;
  private final PoliticaCancelacion politica;
  private final Clock clock;

  public CancelacionService(
      CitaRepository c,
      CancelacionRepository b,
      ComprobantePagoRepository p,
      EventoCancelacionRepository e,
      PoliticaCancelacion policy,
      Clock clock) {
    citas = c;
    bajas = b;
    pagos = p;
    eventos = e;
    politica = policy;
    this.clock = clock;
  }

  public record Evaluacion(Cita cita, boolean dentroPlazo, BigDecimal importe, boolean pagada) {}

  private ResponseStatusException invalida() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "La cita no existe, no te pertenece o no es cancelable.");
  }

  private void propietario(Cita c, Long usuario) {
    if (usuario == null || !Objects.equals(c.getPaciente().getId(), usuario)) throw invalida();
  }

  private void vigente(Cita c) {
    if (!Set.of("EMITIDA", "REPROGRAMADA").contains(c.getEstado().toUpperCase(Locale.ROOT))
        || !politica.futura(c.getTurno().getFecha(), c.getTurno().getHoraInicio()))
      throw invalida();
  }

  @Transactional(readOnly = true)
  public Cita buscar(String codigo, Long usuario) {
    if (codigo == null || codigo.isBlank() || codigo.length() > 40) throw invalida();
    Cita c = citas.findByCodigo(codigo.trim()).orElseThrow(this::invalida);
    propietario(c, usuario);
    vigente(c);
    return c;
  }

  @Transactional(readOnly = true)
  public Evaluacion evaluar(Long id, Long usuario) {
    Cita c = citas.findById(id).orElseThrow(this::invalida);
    propietario(c, usuario);
    vigente(c);
    return evaluar(c);
  }

  private Evaluacion evaluar(Cita c) {
    boolean dentro = politica.dentroPlazo(c.getTurno().getFecha(), c.getTurno().getHoraInicio());
    Optional<ComprobantePago> pago = pagos.findByCitaIdAndEstado(c.getId(), "APROBADO");
    return new Evaluacion(
        c,
        dentro,
        politica.reintegro(
            dentro, pago.map(ComprobantePago::getImporte).orElse(BigDecimal.ZERO.setScale(2))),
        pago.isPresent());
  }

  @Transactional
  public Cancelacion confirmar(
      Long id, Long usuario, String motivo, String detalle, boolean aceptaSinReembolso) {
    Cita c = citas.bloquear(id).orElseThrow(this::invalida);
    propietario(c, usuario);
    // Repetir la misma confirmación devuelve su constancia, sin duplicar eventos.
    Optional<Cancelacion> anterior = bajas.findByCitaId(id);
    if (anterior.isPresent()) return anterior.get();
    em.lock(c.getTurno(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    vigente(c);
    if (!MOTIVOS.contains(motivo)
        || detalle != null && detalle.length() > 1000
        || "Otro".equals(motivo) && (detalle == null || detalle.isBlank()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecciona un motivo válido; Otro requiere un detalle.");
    Evaluacion e = evaluar(c);
    if (!e.dentroPlazo() && !aceptaSinReembolso)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Fuera del plazo: confirma expresamente la cancelación sin reembolso ni nota de"
              + " crédito.");
    Cancelacion b = new Cancelacion();
    b.setCita(c);
    b.setUsuarioId(usuario);
    b.setMotivo(motivo);
    b.setDetalle(detalle);
    b.setFecha(clock.instant());
    b.setDentroPlazo(e.dentroPlazo());
    b.setMontoReintegrable(e.importe());
    b.setEstadoReintegro(e.importe().signum() > 0 ? "PENDIENTE_PROCESAMIENTO" : "NO_CORRESPONDE");
    c.setEstado("CANCELADA");
    c.getTurno().setEstado("DISPONIBLE");
    bajas.saveAndFlush(b);
    if (e.dentroPlazo() && e.pagada() && e.importe().signum() > 0) evento(b, "NOTA_CREDITO");
    evento(b, "CORREO_CONSTANCIA");
    evento(b, "AVISO_CUPO_LIBERADO");
    return b;
  }

  private void evento(Cancelacion b, String tipo) {
    EventoCancelacion e = new EventoCancelacion();
    e.setCancelacion(b);
    e.setTipo(tipo);
    e.setProximoIntento(clock.instant());
    eventos.save(e);
  }

  @Transactional(readOnly = true)
  public Cancelacion constancia(Long id, Long usuario) {
    Cancelacion b = bajas.findById(id).orElseThrow(this::invalida);
    propietario(b.getCita(), usuario);
    return b;
  }
}
