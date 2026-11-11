package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Medico;
import com.clinica.clinicaapp.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/medico")
public class AgendaCancelacionController {
  private final CitaRepository citas;
  private final EventoCancelacionRepository eventos;
  private final com.clinica.clinicaapp.service.MedicoActualService medicoActual;
  private final NotificacionMedicoRepository notificaciones;

  public AgendaCancelacionController(CitaRepository c, EventoCancelacionRepository e,
      com.clinica.clinicaapp.service.MedicoActualService medicoActual, NotificacionMedicoRepository notificaciones) {
    this.medicoActual = medicoActual;
    this.notificaciones = notificaciones;
    citas = c;
    eventos = e;
  }

  @GetMapping("/agenda")
  public String pantalla(java.security.Principal principal, org.springframework.ui.Model model) {
    Long medicoId = medicoActual.obtener(principal).getId();
    model.addAttribute("asignaciones", notificaciones.findTop5ByMedicoIdAndLeidaFalseOrderByIdDesc(medicoId));
    return "agenda-sp03";
  }

  @GetMapping("/citas/{id}")
  @Transactional(readOnly = true)
  public String detalle(@PathVariable Long id, java.security.Principal principal, org.springframework.ui.Model model) {
    Long doctor = medicoActual.obtener(principal).getId();
    var cita = citas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!doctor.equals(cita.getTurno().getMedico().getId()))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    model.addAttribute("cita", cita);
    return "medico-detalle-cita";
  }

  public record CitaAgenda(Long id, String paciente, String hora, String estado) {}

  public record Aviso(Long id, String hora, String texto) {}

  @GetMapping("/agenda/datos")
  @ResponseBody
  @Transactional(readOnly = true)
  public Map<String, Object> datos(
      java.security.Principal principal,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha,
      jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    Long doctor = medicoActual.obtener(principal).getId();
    LocalDate dia = fecha == null ? LocalDate.now(ZoneId.of("America/Lima")) : fecha;
    var agenda =
        citas.agenda(doctor, dia).stream()
            .map(
                c ->
                    new CitaAgenda(
                        c.getId(),
                        c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                        c.getTurno().getHoraInicio().toString(),
                        c.getEstado()))
            .toList();
    var avisos =
        eventos
            .findTop20ByTipoAndCancelacionCitaTurnoMedicoIdOrderByIdDesc(
                "AVISO_CUPO_LIBERADO", doctor)
            .stream()
            .filter(e -> e.getCancelacion().getCita().getTurno().getFecha().equals(dia))
            .map(
                e ->
                    new Aviso(
                        e.getId(),
                        e.getCancelacion().getCita().getTurno().getHoraInicio().toString(),
                        "Cupo liberado por cancelación"))
            .toList();
    return Map.of("citas", agenda, "avisos", avisos, "contador", agenda.size());
  }
}
