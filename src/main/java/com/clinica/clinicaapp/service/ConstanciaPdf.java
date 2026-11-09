package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Cancelacion;
import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.ComprobantePago;

import java.awt.Color;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ConstanciaPdf {

    // Paleta oficial SaludCitas
    private static final Color VERDE_PRIMARIO = new Color(30, 86, 70);       // #1E5646
    private static final Color VERDE_SECUNDARIO = new Color(45, 115, 92);    // #2D735C
    private static final Color VERDE_SUAVE = new Color(220, 238, 234);        // #DCEEEA
    private static final Color ROJO_ESTADO = new Color(185, 28, 28);          // #B91C1C
    private static final Color ROJO_SUAVE = new Color(254, 242, 242);         // #FEF2F2
    private static final Color ROJO_BORDE = new Color(254, 202, 202);         // #FECACA
    private static final Color GRIS_FONDO = new Color(248, 249, 250);         // #F8F9FA
    private static final Color GRIS_BORDE = new Color(229, 231, 235);         // #E5E7EB
    private static final Color GRIS_TEXTO = new Color(107, 114, 128);         // #6B7280
    private static final Color TEXTO_OSCURO = new Color(17, 24, 39);          // #111827

    public byte[] generar(Cancelacion b) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Cargar fuente UTF-8 si existe en resources, o usar estándar
            PDFont fontRegular;
            PDFont fontBold;
            try {
                ClassPathResource res = new ClassPathResource("fonts/DejaVuSans.ttf");
                if (res.exists()) {
                    try (InputStream stream = res.getInputStream()) {
                        fontRegular = PDType0Font.load(document, stream);
                        fontBold = fontRegular;
                    }
                } else {
                    fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                }
            } catch (Exception ex) {
                fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float x = 50;
                float y = 780;

                // 1. Franja decorativa superior
                stream.setNonStrokingColor(VERDE_PRIMARIO);
                stream.addRect(0, 835, 595, 7);
                stream.fill();

                // 2. Encabezado institucional (Logo + Título)
                stream.beginText();
                stream.setFont(fontBold, 22);
                stream.setNonStrokingColor(VERDE_PRIMARIO);
                stream.newLineAtOffset(x, y);
                stream.showText("SaludCitas");
                stream.endText();

                // Pequeña línea verde distintiva (igual al correo)
                stream.setNonStrokingColor(VERDE_SECUNDARIO);
                stream.addRect(x, y - 8, 38, 3);
                stream.fill();

                // Datos a la derecha: Constancia y fecha de emisión
                String constanciaId = "CAN-" + String.format(java.util.Locale.ROOT, "%06d", b.getId());
                stream.beginText();
                stream.setFont(fontBold, 10);
                stream.setNonStrokingColor(GRIS_TEXTO);
                stream.newLineAtOffset(380, y);
                stream.showText("CONSTANCIA DE CANCELACIÓN");
                stream.endText();

                stream.beginText();
                stream.setFont(fontBold, 12);
                stream.setNonStrokingColor(TEXTO_OSCURO);
                stream.newLineAtOffset(380, y - 16);
                stream.showText(constanciaId);
                stream.endText();

                y -= 45;

                // 3. Banner de estado de anulación
                stream.setNonStrokingColor(ROJO_SUAVE);
                stream.addRect(x, y - 26, 495, 34);
                stream.fill();

                stream.setStrokingColor(ROJO_BORDE);
                stream.setLineWidth(1);
                stream.addRect(x, y - 26, 495, 34);
                stream.stroke();

                stream.beginText();
                stream.setFont(fontBold, 11);
                stream.setNonStrokingColor(ROJO_ESTADO);
                stream.newLineAtOffset(x + 16, y - 14);
                stream.showText("CITA CANCELADA - CUPO ASISTENCIAL LIBERADO");
                stream.endText();

                y -= 45;

                // 4. Tarjeta Principal (Estilo Card con bordes redondeados simulados)
                float altoCard = 275;
                stream.setNonStrokingColor(GRIS_FONDO);
                stream.addRect(x, y - altoCard, 495, altoCard);
                stream.fill();

                stream.setStrokingColor(GRIS_BORDE);
                stream.setLineWidth(1);
                stream.addRect(x, y - altoCard, 495, altoCard);
                stream.stroke();

                // Preparación de datos seguros
                Cita cita = b.getCita();
                String codigoCita = (cita != null && cita.getCodigoCita() != null)
                        ? cita.getCodigoCita()
                        : (cita != null && cita.getCodigo() != null ? cita.getCodigo() : "ID " + b.getCita().getId());

                String pacienteStr = (cita != null && cita.getPaciente() != null)
                        ? cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos()
                        : "Paciente ID: " + b.getUsuarioId();

                String dniStr = (cita != null && cita.getPaciente() != null)
                        ? cita.getPaciente().getDni()
                        : "-";

                String especialidadStr = (cita != null && cita.getTurno() != null && cita.getTurno().getMedico() != null)
                        ? cita.getTurno().getMedico().getEspecialidad().getNombre()
                        : "-";

                String medicoStr = (cita != null && cita.getTurno() != null && cita.getTurno().getMedico() != null)
                        ? cita.getTurno().getMedico().getNombres() + " " + cita.getTurno().getMedico().getApellidos()
                        : "-";

                String fechaRegistroStr = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        .withZone(ZoneId.of("America/Lima"))
                        .format(b.getFecha());

                // Filas de detalle
                float rowY = y - 24;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Código de Cita:", codigoCita);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Paciente:", pacienteStr);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Documento (DNI):", dniStr);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Especialidad Médica:", especialidadStr);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Médico Tratante:", medicoStr);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Fecha de Cancelación:", fechaRegistroStr);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Motivo Registrado:", b.getMotivo() != null ? b.getMotivo() : "Cancelación voluntaria");

                // Línea divisoria dentro de la card
                rowY -= 12;
                stream.setStrokingColor(GRIS_BORDE);
                stream.moveTo(x + 20, rowY);
                stream.lineTo(x + 475, rowY);
                stream.stroke();

                rowY -= 20;

                // Sección Económica y Reintegro
                String plazoTexto = b.isDentroPlazo()
                        ? "Dentro del plazo reglamentario (Aplica reintegro)"
                        : "Fuera de plazo reglamentario (Sin reembolso)";

                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Plazo de Cancelación:", plazoTexto);
                rowY -= 22;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Monto Reintegrable:", "PEN " + (b.getMontoReintegrable() != null ? b.getMontoReintegrable() : "0.00"));
                rowY -= 22;
                dibujarFila(stream, fontBold, fontBold, x + 20, rowY, "Estado del Reintegro:", b.getEstadoReintegro() != null ? b.getEstadoReintegro() : "PENDIENTE");

                y = y - altoCard - 30;

                // 5. Marco Informativo Legal (Aviso en fondo suave)
                stream.setNonStrokingColor(new Color(243, 244, 246));
                stream.addRect(x, y - 55, 495, 60);
                stream.fill();

                stream.setStrokingColor(GRIS_BORDE);
                stream.addRect(x, y - 55, 495, 60);
                stream.stroke();

                stream.beginText();
                stream.setFont(fontBold, 8);
                stream.setNonStrokingColor(TEXTO_OSCURO);
                stream.newLineAtOffset(x + 14, y - 16);
                stream.showText("Información sobre notas de crédito y reembolsos:");
                stream.endText();

                stream.beginText();
                stream.setFont(fontRegular, 8);
                stream.setNonStrokingColor(GRIS_TEXTO);
                stream.newLineAtOffset(x + 14, y - 30);
                stream.showText("• Esta constancia certifica la anulación de la cita y la liberación del cupo médico asistencial.");
                stream.endText();

                stream.beginText();
                stream.setFont(fontRegular, 8);
                stream.newLineAtOffset(x + 14, y - 44);
                stream.showText("• La emisión de la Nota de Crédito fiscal electrónica y el reintegro en cuenta se procesan según la pasarela.");
                stream.endText();

                // 6. Pie de página institucional
                stream.beginText();
                stream.setFont(fontRegular, 8);
                stream.setNonStrokingColor(GRIS_TEXTO);
                stream.newLineAtOffset(x + 120, 60);
                stream.showText("© 2026 SaludCitas. Documento emitido automáticamente por el sistema.");
                stream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private void dibujarFila(PDPageContentStream stream, PDFont fontLabel, PDFont fontValor,
                             float x, float y, String label, String valor) throws IOException {
        stream.beginText();
        stream.setFont(fontLabel, 9);
        stream.setNonStrokingColor(TEXTO_OSCURO);
        stream.newLineAtOffset(x, y);
        stream.showText(label);
        stream.endText();

        stream.beginText();
        stream.setFont(fontValor, 9);
        stream.setNonStrokingColor(VERDE_SECUNDARIO);
        stream.newLineAtOffset(x + 155, y);
        stream.showText(valor != null ? valor : "-");
        stream.endText();
    }

    public byte[] generarComprobante(Cita cita, ComprobantePago comprobante) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDFont fontRegular;
            PDFont fontBold;
            try {
                org.springframework.core.io.ClassPathResource res = 
                        new org.springframework.core.io.ClassPathResource("fonts/DejaVuSans.ttf");
                if (res.exists()) {
                    try (InputStream stream = res.getInputStream()) {
                        fontRegular = PDType0Font.load(document, stream);
                        fontBold = fontRegular;
                    }
                } else {
                    fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                }
            } catch (Exception ex) {
                fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float x = 50;
                float y = 780;

                // Franja decorativa superior institucional
                stream.setNonStrokingColor(VERDE_PRIMARIO);
                stream.addRect(0, 835, 595, 7);
                stream.fill();

                // Encabezado
                stream.beginText();
                stream.setFont(fontBold, 22);
                stream.setNonStrokingColor(VERDE_PRIMARIO);
                stream.newLineAtOffset(x, y);
                stream.showText("SaludCitas");
                stream.endText();

                stream.setNonStrokingColor(VERDE_SECUNDARIO);
                stream.addRect(x, y - 8, 38, 3);
                stream.fill();

                String serie = (comprobante != null && comprobante.getSerieCorrelativo() != null)
                        ? comprobante.getSerieCorrelativo()
                        : "B001-" + String.format(java.util.Locale.ROOT, "%06d", cita.getId());

                stream.beginText();
                stream.setFont(fontBold, 10);
                stream.setNonStrokingColor(GRIS_TEXTO);
                stream.newLineAtOffset(360, y);
                stream.showText("BOLETA DE VENTA ELECTRONICA");
                stream.endText();

                stream.beginText();
                stream.setFont(fontBold, 12);
                stream.setNonStrokingColor(TEXTO_OSCURO);
                stream.newLineAtOffset(360, y - 16);
                stream.showText(serie);
                stream.endText();

                y -= 45;

                // Banner de confirmación verde
                stream.setNonStrokingColor(VERDE_SUAVE);
                stream.addRect(x, y - 26, 495, 34);
                stream.fill();

                stream.setStrokingColor(VERDE_SECUNDARIO);
                stream.setLineWidth(1);
                stream.addRect(x, y - 26, 495, 34);
                stream.stroke();

                stream.beginText();
                stream.setFont(fontBold, 11);
                stream.setNonStrokingColor(VERDE_PRIMARIO);
                stream.newLineAtOffset(x + 16, y - 14);
                stream.showText("PAGO CONFIRMADO - CITA ASISTENCIAL EMITIDA");
                stream.endText();

                y -= 45;

                // Tarjeta principal de datos
                float altoCard = 295;
                stream.setNonStrokingColor(GRIS_FONDO);
                stream.addRect(x, y - altoCard, 495, altoCard);
                stream.fill();

                stream.setStrokingColor(GRIS_BORDE);
                stream.setLineWidth(1);
                stream.addRect(x, y - altoCard, 495, altoCard);
                stream.stroke();

                DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

                String fechaStr = cita.getTurno() != null ? cita.getTurno().getFecha().format(fmtFecha) : "-";
                String horaStr = cita.getTurno() != null ? cita.getTurno().getHoraInicio().format(fmtHora) : "-";
                String medicoStr = (cita.getTurno() != null && cita.getTurno().getMedico() != null)
                        ? cita.getTurno().getMedico().getNombres() + " " + cita.getTurno().getMedico().getApellidos() : "-";
                String especialidadStr = (cita.getTurno() != null && cita.getTurno().getMedico() != null)
                        ? cita.getTurno().getMedico().getEspecialidad().getNombre() : "-";
                String consultorioStr = (cita.getTurno() != null && cita.getTurno().getConsultorio() != null)
                        ? cita.getTurno().getConsultorio() : "Por asignar en admision";

                float rowY = y - 24;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Codigo de Cita:", cita.getCodigoCita() != null ? cita.getCodigoCita() : "CIT-" + cita.getId());
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Paciente:", cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos());
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Documento (DNI):", cita.getPaciente().getDni());
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Especialidad:", especialidadStr);
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Medico Tratante:", medicoStr);
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Consultorio:", consultorioStr);
                rowY -= 20;
                dibujarFila(stream, fontBold, fontRegular, x + 20, rowY, "Fecha y Hora:", fechaStr + " a las " + horaStr + " hrs");

                // Línea divisoria económica
                rowY -= 12;
                stream.setStrokingColor(GRIS_BORDE);
                stream.moveTo(x + 20, rowY);
                stream.lineTo(x + 475, rowY);
                stream.stroke();

                rowY -= 18;
                java.math.BigDecimal montoTotal = cita.getMonto() != null ? cita.getMonto() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal subtotal = montoTotal.divide(new java.math.BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal igv = montoTotal.subtract(subtotal);

                dibujarFila(stream, fontRegular, fontRegular, x + 20, rowY, "Op. Gravada (Subtotal):", "PEN " + subtotal.toPlainString());
                rowY -= 18;
                dibujarFila(stream, fontRegular, fontRegular, x + 20, rowY, "I.G.V. (18%):", "PEN " + igv.toPlainString());
                rowY -= 20;
                dibujarFila(stream, fontBold, fontBold, x + 20, rowY, "Importe Total:", "PEN " + montoTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                rowY -= 18;
                dibujarFila(stream, fontRegular, fontRegular, x + 20, rowY, "Nro. Operacion:", cita.getNumeroOperacion() != null ? cita.getNumeroOperacion() : "SIMULADA");

                // Pie de página
                stream.beginText();
                stream.setFont(fontRegular, 8);
                stream.setNonStrokingColor(GRIS_TEXTO);
                stream.newLineAtOffset(x + 100, 60);
                stream.showText("© 2026 SaludCitas. Documento fiscal electronico con plena validez asistencial.");
                stream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}