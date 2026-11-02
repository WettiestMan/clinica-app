package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.TokenRecuperacion;
import com.clinica.clinicaapp.service.PacienteService;
import com.clinica.clinicaapp.service.EmailService;
import com.clinica.clinicaapp.repository.TokenRecuperacionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final PacienteService pacienteService;
    private final EmailService emailService;
    private final TokenRecuperacionRepository tokenRepo;

    public AuthController(PacienteService pacienteService, 
                          EmailService emailService, 
                          TokenRecuperacionRepository tokenRepo) {
        this.pacienteService = pacienteService;
        this.emailService = emailService;
        this.tokenRepo = tokenRepo;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @InitBinder("paciente")
    public void limitarRegistro(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("nombres", "apellidos", "dni", "correo", "password");
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute("paciente") Paciente paciente) {
        try { pacienteService.registrarPaciente(paciente); }
        catch (IllegalArgumentException ex) { return "redirect:/registro?error=datos"; }
        return "redirect:/login?registroExitoso";
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

        // Generar token criptográfico único
        String token = java.util.UUID.randomUUID().toString();
        TokenRecuperacion tokenEntity = new TokenRecuperacion(token, correo, 15); // Validez: 15 minutos
        tokenRepo.save(tokenEntity);

        // Generar enlace seguro con token: http://localhost:8080/restablecer-password?token=xxxx
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        String enlace = baseUrl + "/restablecer-password?token=" + token;

        // Enviar correo HTML con el nombre del paciente
        String nombre = pacienteOpt.get().getNombres() + " " + pacienteOpt.get().getApellidos();
        emailService.enviarEnlaceRecuperacion(correo, nombre, enlace);

        return "redirect:/recuperar-password?enviado=true";
    }

    // 3. Validar token al abrir el enlace del correo
    @GetMapping("/restablecer-password")
    public String vistaRestablecerPassword(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.trim().isEmpty()) {
            return "redirect:/recuperar-password";
        }

        Optional<TokenRecuperacion> tokenOpt = tokenRepo.findByToken(token);
        
        // Si el token no existe, ya expiró (pasaron los 15 min) o ya se usó:
        if (tokenOpt.isEmpty() || tokenOpt.get().estaExpirado()) {
            return "redirect:/recuperar-password?error=token_expirado";
        }

        model.addAttribute("token", token);
        return "restablecer-password";
    }

    // 4. Guardar nueva clave y anular el token
    @PostMapping("/recuperar-password/cambiar")
    public String cambiarPassword(@RequestParam String token, 
                                  @RequestParam String nuevaPassword) {
        TokenRecuperacion tokenEntity = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (tokenEntity.estaExpirado()) {
            return "redirect:/recuperar-password?error=token_expirado";
        }

        // Actualizar clave hasheada en la BD
        pacienteService.actualizarPassword(tokenEntity.getCorreo(), nuevaPassword);

        // Invalidar el token inmediatamente
        tokenEntity.setUsado(true);
        tokenRepo.save(tokenEntity);

        return "redirect:/login?passwordRestablecida";
    }
}