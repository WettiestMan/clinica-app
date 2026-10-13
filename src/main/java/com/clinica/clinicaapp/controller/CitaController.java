package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import com.clinica.clinicaapp.service.CitaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final EspecialidadRepository especialidadRepository;
    private final TurnoRepository turnoRepository;
    private final CitaRepository citaRepository;
    private final CitaService citaService;

    public CitaController(EspecialidadRepository especialidadRepository,
                          TurnoRepository turnoRepository,
                          CitaRepository citaRepository,
                          CitaService citaService) {
        this.especialidadRepository = especialidadRepository;
        this.turnoRepository = turnoRepository;
        this.citaRepository = citaRepository;
        this.citaService = citaService;
    }

    // PASO 1: Buscar Disponibilidad
    @GetMapping("/agendar")
    public String agendarCita(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        LocalDate desde = (fechaDesde != null) ? fechaDesde : LocalDate.of(2026, 10, 12);
        LocalDate hasta = (fechaHasta != null) ? fechaHasta : LocalDate.of(2026, 10, 18);

        List<Turno> turnos = turnoRepository.buscarTurnosDisponibles(especialidadId, desde, hasta);

        Map<Object, List<Turno>> turnosPorMedico = turnos.stream()
                .collect(Collectors.groupingBy(Turno::getMedico, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("turnosPorMedico", turnosPorMedico);
        model.addAttribute("especialidadSeleccionada", especialidadId);
        model.addAttribute("fechaDesde", desde);
        model.addAttribute("fechaHasta", hasta);

        return "agendar-cita";
    }

    // PASO 2: Confirmar Reserva (Bloquea temporalmente el cupo)
    @PostMapping("/confirmar")
    public String confirmarReserva(
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

        try {
            Cita cita = citaService.iniciarProcesoEmision(turnoId, paciente);
            model.addAttribute("cita", cita);
            return "confirmar-reserva";
        } catch (IllegalStateException e) {
            return "redirect:/citas/agendar?error=ocupado";
        }
    }

    // PASO 3: Pantalla de Cobro y Pago con Temporizador
    @GetMapping("/pago/{citaId}")
    public String pantallaPago(@PathVariable Long citaId, HttpSession session, Model model) {
        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) return "redirect:/login";

        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (cita == null || !"EN_PROCESO".equals(cita.getEstado())) {
            return "redirect:/citas/agendar?error=cita_invalida";
        }

        model.addAttribute("cita", cita);
        return "pago-cita";
    }

    // Procesar Pago Digital (HU-02 -> HU-03)
    @PostMapping("/procesar-pago")
    public String procesarPago(
            @RequestParam Long citaId,
            @RequestParam String numeroTarjeta,
            @RequestParam String metodoPago,
            Model model) {

        try {
            Cita citaEmitida = citaService.confirmarPagoYEmision(citaId, numeroTarjeta, metodoPago);
            return "redirect:/citas/comprobante/" + citaEmitida.getId();
        } catch (IllegalStateException ex) {
            return "redirect:/citas/agendar?error=tiempo_expirado";
        }
    }

    // PASO 4: Comprobante y Constancia Final (HU-03)
    @GetMapping("/comprobante/{citaId}")
    public String verComprobante(@PathVariable Long citaId, HttpSession session, Model model) {
        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) return "redirect:/login";

        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (cita == null || !"CONFIRMADA".equals(cita.getEstado())) {
            return "redirect:/citas/agendar";
        }

        model.addAttribute("cita", cita);
        return "comprobante-cita";
    }

    @GetMapping("/mis-citas")
    public String misCitas(
            @RequestParam(name = "tab", defaultValue = "proximas") String tab,
            HttpSession session, 
            Model model) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) {
            return "redirect:/login";
        }

        List<Cita> citas;

        if ("historicas".equalsIgnoreCase(tab)) {
            citas = citaRepository.findHistoricas(paciente.getId());
        } else {
            tab = "proximas";
            citas = citaRepository.findProximas(paciente.getId());
        }

        model.addAttribute("tabActiva", tab);
        model.addAttribute("citas", citas);
        model.addAttribute("paciente", paciente);

        return "mis-citas";
    }

    // ================== SP-02: POSTERGACIÓN DE CITA ==================

    // 1. Vista de Detalle de Cita (Imagen 1)
    @GetMapping("/detalle/{citaId}")
    public String detalleCita(@PathVariable Long citaId, HttpSession session, Model model) {
        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) return "redirect:/login";

        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (cita == null || !cita.getPaciente().getId().equals(paciente.getId())) {
            return "redirect:/citas/mis-citas";
        }

        model.addAttribute("cita", cita);
        return "detalle-cita";
    }

    // 2. Vista de Reprogramación de Cita (Imagen 2)
    @GetMapping("/reprogramar/{citaId}")
    public String vistaReprogramar(@PathVariable Long citaId, HttpSession session, Model model) {
        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) return "redirect:/login";

        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (cita == null) return "redirect:/citas/mis-citas";

        try {
            // HU-05 / RF-10: Validar regla de las 24h
            citaService.validarPlazoPostergacion(cita, paciente);
        } catch (Exception ex) {
            return "redirect:/citas/detalle/" + citaId + "?error=" + ex.getMessage();
        }

        // HU-06 / RF-11: Cargar turnos del mismo médico
        List<Turno> turnosMedico = turnoRepository.findTurnosByMedicoId(
                cita.getTurno().getMedico().getId(), 
                LocalDate.now()
        );

        model.addAttribute("cita", cita);
        model.addAttribute("turnos", turnosMedico);
        return "reprogramar-cita";
    }

    // 3. Procesar Cambio de Horario (HU-07)
    @PostMapping("/reprogramar/confirmar")
    public String confirmarReprogramacion(
            @RequestParam Long citaId,
            @RequestParam(required = false) Long nuevoTurnoId,
            HttpSession session) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) return "redirect:/login";

        if (nuevoTurnoId == null) {
            return "redirect:/citas/reprogramar/" + citaId + "?error=Debe seleccionar un nuevo horario";
        }

        try {
            Cita citaActualizada = citaService.reprogramarCita(citaId, nuevoTurnoId, paciente);
            return "redirect:/citas/comprobante/" + citaActualizada.getId();
        } catch (Exception ex) {
            return "redirect:/citas/reprogramar/" + citaId + "?error=" + ex.getMessage();
        }
    }
}