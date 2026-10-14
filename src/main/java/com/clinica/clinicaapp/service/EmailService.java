package com.clinica.clinicaapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Inyecta el usuario/correo configurado en Brevo desde application/secrets.properties
    @Value("${clinica.mail.from}")
    private String remitenteVerificado;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEnlaceRecuperacion(String destino, String enlace) {
        System.out.println(">>> Enviando correo desde: [" + remitenteVerificado + "] hacia: [" + destino + "]");

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitenteVerificado);
        mensaje.setTo(destino);
        mensaje.setSubject("SaludCitas - Restablecer Contraseña");
        mensaje.setText("Hola,\n\n"
                + "Hemos recibido una solicitud para restablecer tu contraseña en SaludCitas.\n\n"
                + "Para crear una nueva contraseña, ingresa al siguiente enlace:\n"
                + enlace + "\n\n"
                + "Si no solicitaste este cambio, puedes ignorar este mensaje.\n\n"
                + "Atentamente,\nEquipo de SaludCitas");

        mailSender.send(mensaje);
        System.out.println(">>> Correo enviado exitosamente a Brevo");
    }
}