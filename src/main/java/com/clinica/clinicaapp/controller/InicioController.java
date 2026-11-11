package com.clinica.clinicaapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class InicioController {
    @GetMapping("/")
    public String inicio(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JEFE_MEDICO")))
            return "redirect:/jefe/horarios";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO")))
            return "redirect:/medico/agenda";
        return "redirect:/citas/agendar";
    }

    @GetMapping("/error-acceso")
    public String denegado(Model model, HttpServletResponse response) {
        response.setStatus(403);
        model.addAttribute("mensaje", "Tu cuenta no tiene permiso para acceder a esta sección.");
        return "error-operacion";
    }
}
