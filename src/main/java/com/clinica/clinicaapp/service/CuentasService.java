package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CuentasService {
    private final ControlSistemaRepository control;
    private final PacienteRepository pacientes;
    private final UsuarioPersonalRepository personal;

    public CuentasService(ControlSistemaRepository control, PacienteRepository pacientes,
            UsuarioPersonalRepository personal) {
        this.control = control;
        this.pacientes = pacientes;
        this.personal = personal;
    }

    /** Se llama dentro de la transacción de alta. Comparte el espacio de correos. */
    public String reservarCorreo(String correo) {
        control.bloquear().orElseThrow(() -> new IllegalStateException("La configuración inicial no está lista."));
        String normalizado = normalizarCorreo(correo);
        if (pacientes.existsByCorreoIgnoreCase(normalizado) || personal.existsByCorreoIgnoreCase(normalizado))
            throw new IllegalArgumentException("El correo ya está registrado.");
        return normalizado;
    }

    public String normalizarCorreo(String correo) {
        if (correo == null) throw new IllegalArgumentException("Ingresa un correo válido.");
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 254 || !normalizado.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Ingresa un correo válido.");
        return normalizado;
    }

    public String correoParaEdicion(String correo, String tipo, Long id) {
        String normalizado = normalizarCorreo(correo);
        var paciente = pacientes.findByCorreoIgnoreCase(normalizado);
        var usuario = personal.findByCorreoIgnoreCase(normalizado);
        if (paciente.isPresent() && !(tipo.equals("paciente") && paciente.get().getId().equals(id))
                || usuario.isPresent() && !(tipo.equals("personal") && usuario.get().getId().equals(id)))
            throw new IllegalArgumentException("El correo ya está registrado en otra cuenta.");
        return normalizado;
    }

    public void validarPassword(String password, int minimo) {
        if (password == null || password.length() < minimo
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe tener al menos " + minimo
                    + " caracteres y no superar 72 bytes.");
    }

    public String nombre(String valor, String campo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > 100)
            throw new IllegalArgumentException("Completa " + campo + " (máximo 100 caracteres).");
        return valor.trim();
    }
}
