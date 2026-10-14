package com.clinica.clinicaapp;

import static org.junit.jupiter.api.Assertions.*;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.service.ConstanciaPdf;
import java.math.BigDecimal;
import java.time.Instant;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class ConstanciaPdfTest {
  @Test
  void documentoDescargableConAuditoria() throws Exception {
    Cita c = new Cita();
    c.setId(1L);
    c.setCodigo("CIT-PRUEBA");
    Cancelacion b = new Cancelacion();
    b.setId(2L);
    b.setCita(c);
    b.setUsuarioId(3L);
    b.setFecha(Instant.parse("2026-10-11T18:00:00Z"));
    b.setMotivo("Imprevisto personal");
    b.setMontoReintegrable(new BigDecimal("100.00"));
    b.setEstadoReintegro("PENDIENTE_PROCESAMIENTO");
    b.setDentroPlazo(true);
    try (PDDocument d = Loader.loadPDF(new ConstanciaPdf().generar(b))) {
      String texto = new PDFTextStripper().getText(d);
      assertTrue(texto.contains("CIT-PRUEBA"));
      assertTrue(texto.contains("100.00"));
      assertTrue(texto.contains("NO es una nota"));
    }
  }
}
