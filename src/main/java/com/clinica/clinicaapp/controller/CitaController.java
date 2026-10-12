package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/citas")
public class CitaController {

  private final EspecialidadRepository especialidadRepository;
  private final TurnoRepository turnoRepository;
  private final CitaRepository citaRepository;

  public CitaController(
      EspecialidadRepository especialidadRepository,
      TurnoRepository turnoRepository,
      CitaRepository citaRepository) {
    this.especialidadRepository = especialidadRepository;
    this.turnoRepository = turnoRepository;
    this.citaRepository = citaRepository;
  }

  // ESTE ES EL MÉTODO QUE SPRING NO ESTABA ENCONTRANDO (GET /citas/agendar)
  @GetMapping("/agendar")
  public String agendarCita(
      @RequestParam(required = false) Long especialidadId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fechaDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fechaHasta,
      Model model) {

    LocalDate desde = (fechaDesde != null) ? fechaDesde : LocalDate.of(2026, 10, 12);
    LocalDate hasta = (fechaHasta != null) ? fechaHasta : LocalDate.of(2026, 10, 18);

    List<Turno> turnos = turnoRepository.buscarTurnosDisponibles(especialidadId, desde, hasta);

    Map<Object, List<Turno>> turnosPorMedico =
        turnos.stream()
            .collect(
                Collectors.groupingBy(Turno::getMedico, LinkedHashMap::new, Collectors.toList()));

    model.addAttribute("especialidades", especialidadRepository.findAll());
    model.addAttribute("turnosPorMedico", turnosPorMedico);
    model.addAttribute("especialidadSeleccionada", especialidadId);
    model.addAttribute("fechaDesde", desde);
    model.addAttribute("fechaHasta", hasta);

    return "agendar-cita"; // Retorna src/main/resources/templates/agendar-cita.html
  }

  // MÉTODO PARA PROCESAR LA RESERVA (POST /citas/confirmar)
  @PostMapping("/confirmar")
  @Transactional
  public String procesarSeleccionTurno(
      @RequestParam(name = "turnoId", required = false) Long turnoId,
      HttpSession session,
      Model model) {

    Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
    if (paciente == null) {
      return "redirect:/login";
    }

    if (turnoId == null) {
      return "redirect:/citas/agendar?error=seleccione_turno";
    }

    Turno turno = turnoRepository.bloquear(turnoId).orElse(null);
    if (turno == null || !"DISPONIBLE".equals(turno.getEstado())) {
      return "redirect:/citas/agendar?error=ocupado";
    }

    // Marcar turno como ocupado y apartar cupo
    turno.setEstado("OCUPADO");
    turnoRepository.save(turno);

    // Guardar la cita vinculada al paciente
    Cita cita = new Cita();
    cita.setCodigo("CIT-" + java.util.UUID.randomUUID().toString());
    cita.setPaciente(paciente);
    cita.setTurno(turno);
    cita.setEstado("PENDIENTE_PAGO");
    citaRepository.save(cita);

    model.addAttribute("turno", turno);
    model.addAttribute("cita", cita);

    return "confirmar-reserva";
  }
}
