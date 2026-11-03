package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Medico;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.MedicoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/personal")
public class PersonalMedicoController {

    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;

    public PersonalMedicoController(MedicoRepository medicoRepository, CitaRepository citaRepository) {
        this.medicoRepository = medicoRepository;
        this.citaRepository = citaRepository;
    }

    @GetMapping("/mis-citas-asignadas")
    public String vistaCitasAsignadas(
            @RequestParam(required = false) Long medicoId,
            HttpSession session,
            Model model) {

        Object usuario = session.getAttribute("usuarioLogueado");
        Medico medicoActual = null;

        if (usuario instanceof Medico m) {
            medicoActual = medicoRepository.findById(m.getId()).orElse(null);
        }

        if (medicoActual == null && medicoId != null) {
            medicoActual = medicoRepository.findById(medicoId).orElse(null);
        }

        if (medicoActual == null) {
            return "redirect:/login";
        }

        final Long idFiltro = medicoActual.getId();

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