package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.EventoCancelacion;

/**
 * El proveedor debe aceptar la clave de idempotencia y devolver la referencia aceptada.
 * Facturación: validar XML/UBL, firmar y obtener aceptación antes de marcar ENVIADO. Correo:
 * adjuntar constancia y, cuando exista, la nota aprobada.
 */
public interface IntegracionesCancelacion {
  String enviar(EventoCancelacion evento, String claveIdempotencia);
}
