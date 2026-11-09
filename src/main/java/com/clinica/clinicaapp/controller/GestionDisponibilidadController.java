package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.repository.TurnoRepository;
import com.clinica.clinicaapp.service.DisponibilidadService;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jefe/turnos")
public class GestionDisponibilidadController {
    private final TurnoRepository turnos;
    private final DisponibilidadService disponibilidad;
    public GestionDisponibilidadController(TurnoRepository turnos, DisponibilidadService disponibilidad) {
        this.turnos=turnos; this.disponibilidad=disponibilidad;
    }
    @GetMapping("/{id}/{accion:editar|baja}")
    public String formulario(@PathVariable Long id,@PathVariable String accion,Model model) {
        var turno=turnos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("turno",turno); model.addAttribute("accion",accion);
        return "jefe-disponibilidad-gestionar";
    }
    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,@RequestParam long version,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime inicio,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime fin,
        @RequestParam String consultorio,RedirectAttributes flash) {
        try {
            Long nuevo=disponibilidad.editar(id,version,fecha,inicio,fin,consultorio);
            flash.addFlashAttribute("mensaje","Disponibilidad actualizada. Se notificó al médico.");
            return volver(nuevo,flash);
        } catch(IllegalArgumentException e) {
            flash.addFlashAttribute("error",e.getMessage());return "redirect:/jefe/turnos/"+id+"/editar";
        }
    }
    @PostMapping("/{id}/baja")
    public String baja(@PathVariable Long id,@RequestParam long version,@RequestParam String motivo,RedirectAttributes flash) {
        try { disponibilidad.darDeBaja(id,version,motivo);flash.addFlashAttribute("mensaje","Disponibilidad dada de baja. Se conserva el historial y se notificó al médico.");return volver(id,flash); }
        catch(IllegalArgumentException e) {flash.addFlashAttribute("error",e.getMessage());return "redirect:/jefe/turnos/"+id+"/baja";}
    }
    private String volver(Long id,RedirectAttributes flash) {
        var turno=turnos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        flash.addAttribute("medicoId",turno.getMedico().getId());flash.addAttribute("fechaConsulta",turno.getFecha().toString());
        return "redirect:/jefe/horarios";
    }
}
