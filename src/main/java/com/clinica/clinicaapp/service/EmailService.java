package com.clinica.clinicaapp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Inyecta el usuario/correo configurado en Brevo desde application/secrets.properties
    @Value("${clinica.mail.from}")
    private String remitenteVerificado;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEnlaceRecuperacion(String destino, String nombrePaciente, String enlace) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            // Remitente visible: SaludCitas
            helper.setFrom(remitenteVerificado, "SaludCitas");
            helper.setTo(destino);
            helper.setSubject("SaludCitas - Restablece tu contraseña");

            // Plantilla HTML visualmente cuidada
            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin: 0; padding: 0; background-color: #F4F6F6; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="padding: 40px 15px;">
                        <tr>
                            <td align="center">
                                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 520px; background-color: #FFFFFF; border-radius: 28px; box-shadow: 0 4px 15px rgba(0,0,0,0.06); padding: 35px; border: 1px solid #E5E7EB;">
                                    
                                    <!-- Encabezado con logo/nombre -->
                                    <tr>
                                        <td align="center" style="padding-bottom: 25px;">
                                            <div style="font-size: 26px; font-weight: 700; color: #1E5646; letter-spacing: -0.5px;">
                                                SaludCitas
                                            </div>
                                            <div style="width: 40px; height: 3px; background-color: #2D735C; margin-top: 8px; border-radius: 2px;"></div>
                                        </td>
                                    </tr>

                                    <!-- Mensaje -->
                                    <tr>
                                        <td style="color: #374151; font-size: 15px; line-height: 24px;">
                                            <p style="margin-top: 0; font-size: 16px; font-weight: 600; color: #111827;">
                                                Hola, %s:
                                            </p>
                                            <p style="margin-bottom: 20px;">
                                                Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>SaludCitas</strong>. Haz clic en el botón a continuación para crear tu nueva clave:
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Botón Principal -->
                                    <tr>
                                        <td align="center" style="padding: 15px 0 25px 0;">
                                            <a href="%s" target="_blank" style="display: inline-block; background-color: #2D735C; color: #FFFFFF; text-decoration: none; font-size: 14px; font-weight: 600; padding: 14px 32px; border-radius: 14px; box-shadow: 0 3px 6px rgba(45, 115, 92, 0.25);">
                                                Restablecer mi contraseña
                                            </a>
                                        </td>
                                    </tr>

                                    <!-- Aviso de expiración -->
                                    <tr>
                                        <td style="color: #6B7280; font-size: 12px; line-height: 18px; border-top: 1px solid #F3F4F6; padding-top: 20px;">
                                            <p style="margin: 0 0 8px 0;">
                                                ⏰ <strong>Importante:</strong> Este enlace expirará en <strong>15 minutos</strong> y solo puede ser utilizado una vez.
                                            </p>
                                            <p style="margin: 0;">
                                                Si no solicitaste este cambio, puedes ignorar este mensaje de forma segura; tu contraseña seguirá siendo la misma.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                                
                                <p style="margin-top: 20px; font-size: 11px; color: #9CA3AF; text-align: center;">
                                    © 2026 SaludCitas. Todos los derechos reservados.
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
            """.formatted(nombrePaciente != null ? nombrePaciente : "Usuario", enlace);

            helper.setText(html, true); // true = formato HTML
            mailSender.send(mensaje);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Error al enviar el correo de recuperación", e);
        }
    }
}