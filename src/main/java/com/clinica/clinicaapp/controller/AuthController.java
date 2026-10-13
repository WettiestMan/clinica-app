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
    
}