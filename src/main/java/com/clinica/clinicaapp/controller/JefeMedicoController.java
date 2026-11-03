package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Rol;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import java.time.*;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jefe")
public class JefeMedicoController {
    private final MedicoRepository medicos;
    private final EspecialidadRepository especialidades;
    private final UsuarioPersonalRepository usuarios;
    private final TurnoRepository turnos;
    private final PersonalService personal;
    private final DisponibilidadService disponibilidad;

    public JefeMedicoController(MedicoRepository medicos, EspecialidadRepository especialidades,
            UsuarioPersonalRepository usuarios, TurnoRepository turnos, PersonalService personal,
            DisponibilidadService disponibilidad) {
        this.medicos = medicos; this.especialidades = especialidades; this.usuarios = usuarios;
        this.turnos = turnos; this.personal = personal; this.disponibilidad = disponibilidad;
    }

    @GetMapping("/horarios")
    public String pantalla(@RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaConsulta,
            Model model) {
        LocalDate dia = fechaConsulta == null ? LocalDate.now(ZoneId.of("America/Lima")) : fechaConsulta;
        model.addAttribute("ahoraGestion", LocalDateTime.now(ZoneId.of("America/Lima")));
        model.addAttribute("medicos", medicos.findAllByOrderByApellidosAscNombresAsc());
        model.addAttribute("especialidades", especialidades.findAll());
        model.addAttribute("cuentasMedicas", usuarios.findByRolOrderByNombreAsc(Rol.MEDICO));
        model.addAttribute("medicoSeleccionado", medicoId);
        model.addAttribute("fechaConsulta", dia);
        model.addAttribute("turnosAsignados", medicoId == null ? List.of()
                : turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(medicoId, dia));
        return "jefe-horarios";
    }

    @PostMapping("/horarios")
    public String asignar(@RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fin,
            @RequestParam int minutos, @RequestParam(defaultValue = "") String consultorio, RedirectAttributes redirect) {
        try {
            int cantidad = disponibilidad.asignar(medicoId, fecha, inicio, fin, minutos, consultorio);
            redirect.addFlashAttribute("mensaje", cantidad + " turnos guardados. Se notificó al médico en su agenda.");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        redirect.addAttribute("medicoId", medicoId);
        redirect.addAttribute("fechaConsulta", fecha.toString());
        return "redirect:/jefe/horarios";
    }

    @PostMapping("/turnos/{id}/consultorio")
    public String consultorio(@PathVariable Long id, @RequestParam String consultorio, RedirectAttributes redirect) {
        var turno = turnos.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        try { disponibilidad.actualizarConsultorio(id, consultorio); redirect.addFlashAttribute("mensaje", "Consultorio actualizado. Se notificó al médico."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        redirect.addAttribute("medicoId", turno.getMedico().getId());
        redirect.addAttribute("fechaConsulta", turno.getFecha().toString());
        return "redirect:/jefe/horarios";
    }

    @PostMapping("/medicos")
    public String crear(@RequestParam(required = false) Long medicoId,
            @RequestParam(defaultValue = "") String nombres, @RequestParam(defaultValue = "") String apellidos,
            @RequestParam(required = false) Long especialidadId, @RequestParam String correo,
            @RequestParam String password, RedirectAttributes redirect) {
        try {
            personal.crearMedico(medicoId, nombres, apellidos, especialidadId, correo, password);
            redirect.addFlashAttribute("mensaje", "Cuenta médica creada. El médico ya puede iniciar sesión con su correo.");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/jefe/horarios#cuentas-medicas";
    }
}
