package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.service.PacienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final PacienteService pacienteService;

    public PerfilController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String verPerfil(HttpSession session, Model model) {
        Paciente pacienteSesion = (Paciente) session.getAttribute("usuarioLogueado");
        if (pacienteSesion == null) {
            return "redirect:/login";
        }

        // Consultar el paciente actualizado desde la base de datos
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorCorreo(pacienteSesion.getCorreo());
        if (pacienteOpt.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("paciente", pacienteOpt.get());
        return "perfil";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual,
                                  @RequestParam String nuevaPassword,
                                  @RequestParam String confirmPassword,
                                  HttpSession session) {
        Paciente pacienteSesion = (Paciente) session.getAttribute("usuarioLogueado");
        if (pacienteSesion == null) {
            return "redirect:/login";
        }

        // Validar que la nueva contraseña coincida con la confirmación
        if (!nuevaPassword.equals(confirmPassword)) {
            return "redirect:/perfil?error=coincidencia";
        }

        // Longitud mínima
        if (nuevaPassword.length() < 6) {
            return "redirect:/perfil?error=longitud";
        }

        boolean cambioExitoso = pacienteService.cambiarPasswordConValidacion(
                pacienteSesion.getCorreo(), 
                passwordActual, 
                nuevaPassword
        );

        if (!cambioExitoso) {
            return "redirect:/perfil?error=clave_invalida";
        }

        return "redirect:/perfil?exito=true";
    }
}