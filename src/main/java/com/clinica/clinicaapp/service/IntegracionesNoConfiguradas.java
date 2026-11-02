package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.EventoCancelacion;

public class IntegracionesNoConfiguradas implements IntegracionesCancelacion {
  public String enviar(EventoCancelacion e, String clave) {
    throw new IllegalStateException(
        "Proveedor no configurado. Documento/correo pendiente; no se ha enviado.");
  }
}
