package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Rol;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.MedicoRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import com.clinica.clinicaapp.repository.UsuarioPersonalRepository;
import com.clinica.clinicaapp.service.DisponibilidadService;
import com.clinica.clinicaapp.service.PersonalService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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

    public JefeMedicoController(MedicoRepository medicos,
            EspecialidadRepository especialidades,
            UsuarioPersonalRepository usuarios,
            TurnoRepository turnos,
            PersonalService personal,
            DisponibilidadService disponibilidad) {
        this.medicos = medicos;
        this.especialidades = especialidades;
        this.usuarios = usuarios;
        this.turnos = turnos;
        this.personal = personal;
        this.disponibilidad = disponibilidad;
    }

    // ==============================================================
    // 1. PANTALLA PRINCIPAL: TU PROGRAMACIÓN SEMANAL Y MATRIZ
    // ==============================================================
    @GetMapping({ "/horarios", "/disponibilidad" })
    public String pantallaProgramacionSemanal(
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Model model) {

        ZoneId zonaLima = ZoneId.of("America/Lima");
        LocalDate hoy = LocalDate.now(zonaLima);
        LocalDate lunesSemana = semana == null
                ? hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : semana.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDate viernesSemana = lunesSemana.plusDays(4);

        model.addAttribute("especialidades", especialidades.findAll());
        model.addAttribute("medicos", medicos.findAllByOrderByApellidosAscNombresAsc());
        model.addAttribute("proximoLunes", hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
        model.addAttribute("medicoSeleccionado", medicoId);
        model.addAttribute("lunesActual", lunesSemana);
        model.addAttribute("semanaAnterior", lunesSemana.minusWeeks(1));
        model.addAttribute("semanaSiguiente", lunesSemana.plusWeeks(1));

        // Construir la matriz semanal en Backend solo si hay médico seleccionado
        if (medicoId != null) {
            medicos.findById(medicoId).ifPresent(m -> model.addAttribute("medicoActual", m));
            var turnosSemana = turnos.findByMedicoIdAndFechaBetweenAndActivoTrueOrderByHoraInicioAsc(
                    medicoId, lunesSemana, viernesSemana);

            // Generar cabeceras: Lun 09/11, Mar 10/11, etc.
            List<String> encabezados = List.of(
                    "Lun " + lunesSemana.getDayOfMonth() + "/" + lunesSemana.getMonthValue(),
                    "Mar " + lunesSemana.plusDays(1).getDayOfMonth() + "/" + lunesSemana.plusDays(1).getMonthValue(),
                    "Mié " + lunesSemana.plusDays(2).getDayOfMonth() + "/" + lunesSemana.plusDays(2).getMonthValue(),
                    "Jue " + lunesSemana.plusDays(3).getDayOfMonth() + "/" + lunesSemana.plusDays(3).getMonthValue(),
                    "Vie " + lunesSemana.plusDays(4).getDayOfMonth() + "/" + lunesSemana.plusDays(4).getMonthValue());

            // Armar las filas cada 30 minutos desde 08:00 hasta 18:00
            List<com.clinica.clinicaapp.dto.ParrillaSemanalDto.FilaMatriz> filas = new java.util.ArrayList<>();
            LocalTime horaActual = LocalTime.of(8, 0);
            LocalTime horaLimite = LocalTime.of(22, 0);

            while (horaActual.isBefore(horaLimite)) {
                LocalTime h = horaActual;
                List<com.clinica.clinicaapp.dto.ParrillaSemanalDto.CeldaTurno> celdas = new java.util.ArrayList<>();

                for (int d = 0; d < 5; d++) {
                    LocalDate fechaDia = lunesSemana.plusDays(d);

                    var match = turnosSemana.stream()
                            .filter(t -> t.getFecha().equals(fechaDia) && t.getHoraInicio().equals(h))
                            .findFirst();

                    if (match.isPresent()) {
                        var t = match.get();
                        String estadoParrilla = "DISPONIBLE".equalsIgnoreCase(t.getEstado()) ? "DISPONIBLE" : "OCUPADO";
                        celdas.add(new com.clinica.clinicaapp.dto.ParrillaSemanalDto.CeldaTurno(
                                t.getId(), estadoParrilla, t.getConsultorioVisible(), true));
                    } else {
                        celdas.add(new com.clinica.clinicaapp.dto.ParrillaSemanalDto.CeldaTurno(null, "", "", false));
                    }
                }

                filas.add(new com.clinica.clinicaapp.dto.ParrillaSemanalDto.FilaMatriz(
                        h, String.format("%02d:%02d", h.getHour(), h.getMinute()), celdas));

                horaActual = horaActual.plusMinutes(30);
            }

            model.addAttribute("parrilla",
                    new com.clinica.clinicaapp.dto.ParrillaSemanalDto(lunesSemana, encabezados, filas));
        }

        return "admin/disponibilidad-horaria";
    }

    // ==============================================================
    // 2. ACCIÓN: PUBLICAR DISPONIBILIDAD SEMANAL (POST)
    // ==============================================================
    @PostMapping({ "/horarios/publicar-semanal", "/disponibilidad/publicar" })
    public String publicarSemanal(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSemana,
            @RequestParam List<DayOfWeek> dias,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam String consultorio,
            RedirectAttributes redirect) {

        try {
            int totalTurnos = disponibilidad.generarTurnosSemanales(
                    medicoId, fechaSemana, dias, horaInicio, horaFin, consultorio);

            redirect.addFlashAttribute("exito",
                    "¡Disponibilidad publicada con éxito! Se han generado " + totalTurnos + " turnos.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/jefe/horarios";
    }

    // ==============================================================
    // 3. ACCIONES DE MANTENIMIENTO DEL COMPAÑERO
    // ==============================================================
    @PostMapping("/turnos/{id}/consultorio")
    public String actualizarConsultorio(
            @PathVariable Long id,
            @RequestParam String consultorio,
            RedirectAttributes redirect) {
        var turno = turnos.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND));
        try {
            disponibilidad.actualizarConsultorio(id, consultorio);
            redirect.addFlashAttribute("exito", "Consultorio actualizado. Se notificó al médico.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        redirect.addAttribute("medicoId", turno.getMedico().getId());
        redirect.addAttribute("fechaConsulta", turno.getFecha().toString());
        return "redirect:/jefe/horarios";
    }

    @PostMapping("/medicos")
    public String crearCuentaMedica(
            @RequestParam(required = false) Long medicoId,
            @RequestParam(defaultValue = "") String nombres,
            @RequestParam(defaultValue = "") String apellidos,
            @RequestParam(required = false) Long especialidadId,
            @RequestParam String correo,
            @RequestParam String password,
            RedirectAttributes redirect) {
        try {
            personal.crearMedico(medicoId, nombres, apellidos, especialidadId, correo, password);
            redirect.addFlashAttribute("exito", "Cuenta médica creada exitosamente.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/jefe/horarios";
    }

}