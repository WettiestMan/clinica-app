package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.service.PacienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final PacienteService pacienteService;

    public AuthController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
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
    public String solicitarRecuperacion(@RequestParam String correo) {
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorCorreo(correo);
        
        // Si no existe, vuelve mostrando el error rojo
        if (pacienteOpt.isEmpty()) {
            return "redirect:/recuperar-password?error=no_existe";
        }

        // Si existe, avanza a la pantalla de restablecer enviando el correo
        return "redirect:/restablecer-password?correo=" + correo;
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