package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.service.PersonalService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ConfiguracionInicialController {
    private final PersonalService personal;
    public ConfiguracionInicialController(PersonalService personal) { this.personal = personal; }

    @GetMapping("/configuracion-inicial")
    public String pantalla() {
        if (!personal.requiereConfiguracion()) return "redirect:/login";
        return "configuracion-inicial";
    }

    @PostMapping("/configuracion-inicial")
    public String configurar(@RequestParam String nombre, @RequestParam String correo,
            @RequestParam String password, @RequestParam String confirmPassword, Model model) {
        if (!personal.requiereConfiguracion())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La configuración inicial ya se completó.");
        try {
            if (!password.equals(confirmPassword)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
            personal.crearPrimerJefe(nombre, correo, password);
            return "redirect:/login?jefeCreado";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("correo", correo);
            return "configuracion-inicial";
        }
    }
}
