package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.MedicoActualService;
import java.security.Principal;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/medico")
public class NotificacionMedicoController {
    private final MedicoActualService medicoActual;
    private final NotificacionMedicoRepository notificaciones;
    private final TurnoRepository turnos;

    public NotificacionMedicoController(MedicoActualService medicoActual,
            NotificacionMedicoRepository notificaciones, TurnoRepository turnos) {
        this.medicoActual = medicoActual; this.notificaciones = notificaciones; this.turnos = turnos;
    }

    @GetMapping("/turnos")
    @Transactional(readOnly = true)
    public String turnos(Principal principal, Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(ZoneId.of("America/Lima")) : fecha;
        model.addAttribute("fecha", dia);
        model.addAttribute("turnos", turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoActual.obtener(principal).getId(), dia));
        return "medico-turnos";
    }

    @GetMapping("/notificaciones")
    @Transactional(readOnly = true)
    public String notificaciones(Principal principal, Model model) {
        model.addAttribute("avisos", notificaciones.findTop50ByMedicoIdOrderByIdDesc(medicoActual.obtener(principal).getId()));
        return "medico-notificaciones";
    }

    public record AvisoFlotante(Long id,String mensaje,String fecha) {}
    public record Bandeja(Long medicoId,long pendientes,java.util.List<AvisoFlotante> avisos) {}

    @GetMapping("/notificaciones/datos")
    @ResponseBody
    @Transactional(readOnly = true)
    public Bandeja datos(Principal principal,jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        Long medico=medicoActual.obtener(principal).getId();
        var lista=notificaciones.findTop50ByMedicoIdAndLeidaFalseOrderByIdDesc(medico).stream()
            .map(n->new AvisoFlotante(n.getId(),n.getMensaje(),n.getCreada().toString())).toList();
        return new Bandeja(medico,notificaciones.countByMedicoIdAndLeidaFalse(medico),lista);
    }

    @PostMapping("/notificaciones/leer-todas")
    @Transactional
    public String leerTodas(Principal principal) {
        notificaciones.leerTodas(medicoActual.obtener(principal).getId());
        return "redirect:/medico/notificaciones";
    }

    @PostMapping("/notificaciones/{id}/leer")
    @Transactional
    public String leer(@PathVariable Long id, Principal principal) {
        var aviso = notificaciones.findByIdAndMedicoId(id, medicoActual.obtener(principal).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La notificación no está disponible."));
        aviso.setLeida(true);
        return "redirect:/medico/notificaciones";
    }
}
