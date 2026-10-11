package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final EspecialidadRepository especialidadRepository;
    private final TurnoRepository turnoRepository;

    public CitaController(EspecialidadRepository especialidadRepository, TurnoRepository turnoRepository) {
        this.especialidadRepository = especialidadRepository;
        this.turnoRepository = turnoRepository;
    }

    @GetMapping("/agendar")
    public String agendarCita(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        LocalDate desde = (fechaDesde != null) ? fechaDesde : LocalDate.of(2026, 10, 12);
LocalDate hasta = (fechaHasta != null) ? fechaHasta : LocalDate.of(2026, 10, 18);

        List<Turno> turnos = turnoRepository.buscarTurnosDisponibles(especialidadId, desde, hasta);

        // Agrupar los turnos por cada médico
        Map<Object, List<Turno>> turnosPorMedico = turnos.stream()
                .collect(Collectors.groupingBy(Turno::getMedico, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("turnosPorMedico", turnosPorMedico);
        model.addAttribute("especialidadSeleccionada", especialidadId);
        model.addAttribute("fechaDesde", desde);
        model.addAttribute("fechaHasta", hasta);

        return "agendar-cita";
    }

    @PostMapping("/confirmar")
    public String procesarSeleccionTurno(
            @RequestParam(name = "turnoId", required = false) Long turnoId, 
            Model model) {
        
        if (turnoId == null) {
            return "redirect:/citas/agendar?error=seleccione_turno";
        }

        Turno turno = turnoRepository.findById(turnoId).orElse(null);
        if (turno == null || !"DISPONIBLE".equals(turno.getEstado())) {
            return "redirect:/citas/agendar?error=ocupado";
        }
        
        model.addAttribute("turno", turno);
        return "confirmar-reserva";
    }
}