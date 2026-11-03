package com.clinica.clinicaapp.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class ErroresOperacionAdvice {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String datos(RuntimeException ex, Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute("mensaje", ex.getMessage());
        return "error-operacion";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridad(Model model, HttpServletResponse response) {
        response.setStatus(409);
        model.addAttribute("mensaje", "La operación no pudo completarse porque los datos ya cambiaron o están duplicados. Actualiza la página y vuelve a intentarlo.");
        return "error-operacion";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String acceso(ResponseStatusException ex, Model model, HttpServletResponse response) {
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("mensaje", ex.getReason());
        return "error-operacion";
    }
}
