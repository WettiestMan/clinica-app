package com.clinica.clinicaapp.service;

import java.math.BigDecimal;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PoliticaCancelacion {
  private final Clock clock;
  private final Duration plazo;
  private final ZoneId zona;

  public PoliticaCancelacion(
      Clock clock,
      @Value("${clinica.cancelacion.horas:24}") long horas,
      @Value("${clinica.zona:America/Lima}") String zona) {
    if (horas < 0) throw new IllegalArgumentException("Plazo inválido");
    this.clock = clock;
    this.plazo = Duration.ofHours(horas);
    this.zona = ZoneId.of(zona);
  }

  public boolean dentroPlazo(LocalDate fecha, LocalTime hora) {
    return !fecha.atTime(hora).atZone(zona).toInstant().isBefore(clock.instant().plus(plazo));
  }

  public boolean futura(LocalDate fecha, LocalTime hora) {
    return fecha.atTime(hora).atZone(zona).toInstant().isAfter(clock.instant());
  }

  public BigDecimal reintegro(boolean dentro, BigDecimal pagado) {
    return dentro ? pagado : BigDecimal.ZERO.setScale(2);
  }
}
