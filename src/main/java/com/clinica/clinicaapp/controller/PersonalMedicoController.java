package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Medico;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.MedicoRepository;
import com.clinica.clinicaapp.service.DisponibilidadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/personal")
public class PersonalMedicoController {

    private final MedicoRepository medicoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final DisponibilidadService disponibilidadService;
    private final CitaRepository citaRepository; // Necesario para listar las citas

    public PersonalMedicoController(MedicoRepository medicoRepository, 
                                    EspecialidadRepository especialidadRepository, 
                                    DisponibilidadService disponibilidadService,
                                    CitaRepository citaRepository) {
        this.medicoRepository = medicoRepository;
        this.especialidadRepository = especialidadRepository;
        this.disponibilidadService = disponibilidadService;
        this.citaRepository = citaRepository;
    }

    // ==========================================
    // 1. JEFE MÉDICO: Panel de Disponibilidad
    // ==========================================
    @GetMapping("/disponibilidad")
    public String vistaDisponibilidad(Model model) {
        LocalDate proximoLunes = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);

        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("medicos", medicoRepository.findAll());
        model.addAttribute("proximoLunes", proximoLunes);

        return "admin/disponibilidad-horaria";
    }

    @PostMapping("/disponibilidad/publicar")
    public String publicarDisponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSemana,
            @RequestParam List<String> dias,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            RedirectAttributes redirectAttributes) {

        try {
            List<DayOfWeek> diasEnum = dias.stream()
                    .map(DayOfWeek::valueOf)
                    .toList();

            int cantidadGenerada = disponibilidadService.generarTurnosSemanales(
                    medicoId, fechaSemana, diasEnum, horaInicio, horaFin
            );

            redirectAttributes.addFlashAttribute("exito", 
                    "¡Publicación exitosa! Se han creado " + cantidadGenerada + " turnos disponibles para los pacientes.");

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/personal/disponibilidad";
    }

    // ==========================================
    // 2. MÉDICO: Citas Asignadas (ESTE MÉTODO SOLUCIONA EL 404)
    // ==========================================
    @GetMapping("/mis-citas-asignadas")
    public String vistaCitasAsignadas(
            @RequestParam(required = false) Long medicoId,
            HttpSession session,
            Model model) {

        // 1. Intentar obtener el médico desde la sesión autenticada
        Object usuario = session.getAttribute("usuarioLogueado");
        Medico medicoActual = null;

        if (usuario instanceof Medico m) {
            medicoActual = medicoRepository.findById(m.getId()).orElse(null);
        }

        // 2. Respaldo: si no hay sesión activa pero se pasa un id o se prueba en desarrollo
        if (medicoActual == null && medicoId != null) {
            medicoActual = medicoRepository.findById(medicoId).orElse(null);
        }

        // 3. Si aún es nulo (por ejemplo, entrada directa sin login), redirigir
        if (medicoActual == null) {
            return "redirect:/login";
        }

        final Long idFiltro = medicoActual.getId();

        // 4. Filtrar únicamente las citas confirmadas o reprogramadas de este doctor
        List<Cita> citasAsignadas = citaRepository.findAll().stream()
                .filter(c -> c.getTurno() != null 
                          && c.getTurno().getMedico() != null
                          && c.getTurno().getMedico().getId().equals(idFiltro)
                          && ("CONFIRMADA".equalsIgnoreCase(c.getEstado()) || "REPROGRAMADA".equalsIgnoreCase(c.getEstado())))
                .sorted((c1, c2) -> {
                    int compFecha = c1.getTurno().getFecha().compareTo(c2.getTurno().getFecha());
                    if (compFecha != 0) return compFecha;
                    return c1.getTurno().getHoraInicio().compareTo(c2.getTurno().getHoraInicio());
                })
                .toList();

        model.addAttribute("medico", medicoActual);
        model.addAttribute("citas", citasAsignadas);

        return "medico/citas-asignadas";
    }
}