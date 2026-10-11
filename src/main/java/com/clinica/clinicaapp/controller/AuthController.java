package com.clinica.clinicaapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/registro")
    public String showRegistro() {
        return "registro";
    }

    @GetMapping("/recuperar-password")
    public String showSolicitarRecuperacion() {
        return "solicitar-recuperacion";
    }

    @GetMapping("/restablecer-password")
    public String showRestablecerPassword() {
        return "restablecer-password";
    }
}