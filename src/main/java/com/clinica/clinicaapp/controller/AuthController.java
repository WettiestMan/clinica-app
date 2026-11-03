package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Medico;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.TokenRecuperacion;
import com.clinica.clinicaapp.repository.MedicoRepository;
import com.clinica.clinicaapp.repository.TokenRecuperacionRepository;
import com.clinica.clinicaapp.service.EmailService;
import com.clinica.clinicaapp.service.PacienteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Controller
public class AuthController {

    private final PacienteService pacienteService;
    private final EmailService emailService;
    private final TokenRecuperacionRepository tokenRepo;
    private final MedicoRepository medicoRepository;

    public AuthController(PacienteService pacienteService, 
                          EmailService emailService, 
                          TokenRecuperacionRepository tokenRepo,
                          MedicoRepository medicoRepository) {
        this.pacienteService = pacienteService;
        this.emailService = emailService;
        this.tokenRepo = tokenRepo;
        this.medicoRepository = medicoRepository;
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
        // 1. Validar autenticación de Paciente
        Optional<Paciente> paciente = pacienteService.autenticar(correo, password);
        if (paciente.isPresent()) {
            session.setAttribute("usuarioLogueado", paciente.get());
            session.setAttribute("rol", "PACIENTE");
            return "redirect:/citas/agendar";
        }

        // 2. Si no es paciente, verificar si corresponde a un Médico / Jefe Médico
        Optional<Medico> medicoOpt = medicoRepository.findByCorreo(correo);
        if (medicoOpt.isPresent()) {
            Medico medico = medicoOpt.get();

            if (medico.getPassword() != null && medico.getPassword().equals(password)) {
                session.setAttribute("usuarioLogueado", medico);
                
                if (Boolean.TRUE.equals(medico.getEsJefe())) {
                    session.setAttribute("rol", "JEFE_MEDICO");
                    return "redirect:/personal/disponibilidad";
                } else {
                    session.setAttribute("rol", "MEDICO");
                    return "redirect:/personal/mis-citas-asignadas?medicoId=" + medico.getId();
                }
            }
        }
        return "redirect:/login?error=credenciales";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @InitBinder("paciente")
    public void limitarRegistro(WebDataBinder binder) {
        binder.setAllowedFields("nombres", "apellidos", "dni", "correo", "password");
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute("paciente") Paciente paciente) {
        try { 
            pacienteService.registrarPaciente(paciente); 
        } catch (IllegalArgumentException ex) { 
            return "redirect:/registro?error=datos"; 
        }
        return "redirect:/login?registroExitoso";
    }

    // ================= RECUPERACIÓN DE CONTRASEÑA =================

    @GetMapping("/recuperar-password")
    public String vistaRecuperarPassword() {
        return "recuperar-password";
    }

    @PostMapping("/recuperar-password/solicitar")
    public String solicitarRecuperacion(@RequestParam String correo, HttpServletRequest request) {
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorCorreo(correo);
        if (pacienteOpt.isEmpty()) {
            return "redirect:/recuperar-password?error=no_existe";
        }

        String token = UUID.randomUUID().toString();
        TokenRecuperacion tokenEntity = new TokenRecuperacion(token, correo, 15);
        tokenRepo.save(tokenEntity);

        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        String enlace = baseUrl + "/restablecer-password?token=" + token;

        String nombre = pacienteOpt.get().getNombres() + " " + pacienteOpt.get().getApellidos();
        emailService.enviarEnlaceRecuperacion(correo, nombre, enlace);

        return "redirect:/recuperar-password?enviado=true";
    }

    @GetMapping("/restablecer-password")
    public String vistaRestablecerPassword(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.trim().isEmpty()) {
            return "redirect:/recuperar-password";
        }

        Optional<TokenRecuperacion> tokenOpt = tokenRepo.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().estaExpirado()) {
            return "redirect:/recuperar-password?error=token_expirado";
        }

        model.addAttribute("token", token);
        return "restablecer-password";
    }

    @PostMapping("/recuperar-password/cambiar")
    public String cambiarPassword(@RequestParam String token, 
                                  @RequestParam String nuevaPassword) {
        TokenRecuperacion tokenEntity = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (tokenEntity.estaExpirado()) {
            return "redirect:/recuperar-password?error=token_expirado";
        }

        pacienteService.actualizarPassword(tokenEntity.getCorreo(), nuevaPassword);

        tokenEntity.setUsado(true);
        tokenRepo.save(tokenEntity);

        return "redirect:/login?passwordRestablecida";
    }
}