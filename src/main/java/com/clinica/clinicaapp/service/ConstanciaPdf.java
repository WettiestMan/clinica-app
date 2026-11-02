package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Cancelacion;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

@Service
public class ConstanciaPdf {
  public byte[] generar(Cancelacion b) throws IOException {
    try (PDDocument d = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDType0Font font;
      try (InputStream stream =
          new org.springframework.core.io.ClassPathResource("fonts/DejaVuSans.ttf")
              .getInputStream()) {
        font = PDType0Font.load(d, stream);
      }
      PDPage page = new PDPage();
      d.addPage(page);
      try (PDPageContentStream s = new PDPageContentStream(d, page)) {
        s.beginText();
        s.setFont(font, 18);
        s.newLineAtOffset(50, 740);
        s.showText("SaludCitas - Constancia de cancelacion");
        s.setFont(font, 11);
        s.setLeading(24);
        String codigo =
            b.getCita().getCodigo() == null ? "ID " + b.getCita().getId() : b.getCita().getCodigo();
        String[] lines = {
          "Constancia: CAN-" + b.getId(),
          "Cita: " + codigo,
          "Estado: CANCELADA",
          "Fecha: "
              + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")
                  .withZone(ZoneId.of("America/Lima"))
                  .format(b.getFecha()),
          "Paciente ID: " + b.getUsuarioId(),
          "Motivo: " + b.getMotivo(),
          "Monto elegible para reintegro: PEN " + b.getMontoReintegrable(),
          "Estado del reintegro: " + b.getEstadoReintegro(),
          b.isDentroPlazo()
              ? "Cancelacion dentro del plazo reglamentario."
              : "Fuera de plazo: sin reembolso ni nota de credito.",
          "El cupo ha sido liberado.",
          "Esta constancia NO es una nota de credito tributaria.",
          "La emision tributaria y el correo dependen del proveedor configurado."
        };
        for (String line : lines) {
          s.newLine();
          s.showText(line);
        }
        s.endText();
      }
      d.save(out);
      return out.toByteArray();
    }
  }
}
