package com.clinica.clinicaapp.config;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CancelacionConfig {
  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(
      com.clinica.clinicaapp.service.IntegracionesCancelacion.class)
  public com.clinica.clinicaapp.service.IntegracionesCancelacion integracionesCancelacion() {
    return new com.clinica.clinicaapp.service.IntegracionesNoConfiguradas();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
