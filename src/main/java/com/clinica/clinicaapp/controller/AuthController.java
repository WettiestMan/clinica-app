package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.service.PacienteService;
import com.clinica.clinicaapp.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final PacienteService pacienteService;
    private final EmailService emailService;

    public AuthController(PacienteService pacienteService, EmailService emailService) {
        this.pacienteService = pacienteService;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo, 
                                @RequestParam String password, 
                                HttpSession session, 
                                Model model) {
        Optional<Paciente> paciente = pacienteService.autenticar(correo, password);
        if (paciente.isPresent()) {
            // Guardamos el paciente en la sesión HTTP
            session.setAttribute("usuarioLogueado", paciente.get());
            return "redirect:/citas/agendar";
        }
        return "redirect:/login?error=credenciales";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Paciente paciente) {
        pacienteService.registrarPaciente(paciente);
        return "redirect:/login?registroExitoso";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destruye la sesión del paciente
        return "redirect:/login?logout";
    }

    // ================= RECUPERACIÓN DE CONTRASEÑA =================

    // Paso 1: Muestra solicitar correo (recuperar-password.html)
    @GetMapping("/recuperar-password")
    public String vistaRecuperarPassword() {
        return "recuperar-password";
    }

    // Paso 2: Valida en la base de datos si el correo existe
    @PostMapping("/recuperar-password/solicitar")
    public String solicitarRecuperacion(@RequestParam String correo, jakarta.servlet.http.HttpServletRequest request) {
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorCorreo(correo);
        
        if (pacienteOpt.isEmpty()) {
            return "redirect:/recuperar-password?error=no_existe";
        }

        // Construir la URL completa: http://localhost:8080/restablecer-password?correo=...
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String enlace = scheme + "://" + serverName + ":" + serverPort + "/restablecer-password?correo=" + correo;

        // Envío asíncrono hacia Mailtrap
        emailService.enviarEnlaceRecuperacion(correo, enlace);

        return "redirect:/recuperar-password?enviado=true";
        //return "redirect:/restablecer-password?correo=" + correo;
    }

    // Paso 3: Muestra la pantalla para crear la nueva clave (restablecer-password.html)
    @GetMapping("/restablecer-password")
    public String vistaRestablecerPassword(@RequestParam(required = false) String correo, Model model) {
        if (correo == null || correo.trim().isEmpty()) {
            return "redirect:/recuperar-password";
        }
        model.addAttribute("correo", correo);
        return "restablecer-password";
    }

    // Paso 4: Actualiza la clave en la BD y redirige a login
    @PostMapping("/recuperar-password/cambiar")
    public String cambiarPassword(@RequestParam String correo, 
                                  @RequestParam String nuevaPassword) {
        pacienteService.actualizarPassword(correo, nuevaPassword);
        return "redirect:/login?passwordRestablecida";
    }
}