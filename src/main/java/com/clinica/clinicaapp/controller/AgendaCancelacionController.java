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

  public AgendaCancelacionController(CitaRepository c, EventoCancelacionRepository e) {
    citas = c;
    eventos = e;
  }

  private Long medico(HttpSession s) {
    Object m = s.getAttribute("medicoLogueado");
    if (!(m instanceof Medico doctor) || doctor.getId() == null)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Se requiere sesión autenticada de médico.");
    return doctor.getId();
  }

  @GetMapping("/agenda")
  public String pantalla(HttpSession s) {
    medico(s);
    return "agenda-sp03";
  }

  public record CitaAgenda(Long id, String paciente, String hora, String estado) {}

  public record Aviso(Long id, String hora, String texto) {}

  @GetMapping("/agenda/datos")
  @ResponseBody
  @Transactional(readOnly = true)
  public Map<String, Object> datos(
      HttpSession s,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha,
      jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    Long doctor = medico(s);
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
