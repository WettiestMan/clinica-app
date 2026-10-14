package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import com.clinica.clinicaapp.service.CitaService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final EspecialidadRepository especialidadRepository;
    private final TurnoRepository turnoRepository;
    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final CancelacionController cancelacionController;

    public CitaController(
            EspecialidadRepository especialidadRepository,
            TurnoRepository turnoRepository,
            CitaRepository citaRepository,
            CitaService citaService,
            CancelacionController cancelacionController) {

        this.especialidadRepository = especialidadRepository;
        this.turnoRepository = turnoRepository;
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.cancelacionController = cancelacionController;
    }

    @GetMapping("/agendar")
    public String agendarCita(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        LocalDate desde = fechaDesde != null
                ? fechaDesde : LocalDate.now();
        LocalDate hasta = fechaHasta != null
                ? fechaHasta : desde.plusDays(6);

        List<Turno> turnos
                = turnoRepository.buscarTurnosDisponibles(
                        especialidadId, desde, hasta);

        Map<Object, List<Turno>> turnosPorMedico
                = turnos.stream().collect(
                        Collectors.groupingBy(
                                Turno::getMedico,
                                LinkedHashMap::new,
                                Collectors.toList()));

        model.addAttribute(
                "especialidades", especialidadRepository.findAll());
        model.addAttribute("turnosPorMedico", turnosPorMedico);
        model.addAttribute("especialidadSeleccionada", especialidadId);
        model.addAttribute("fechaDesde", desde);
        model.addAttribute("fechaHasta", hasta);

        return "agendar-cita";
    }

    @PostMapping("/confirmar")
    public String confirmarReserva(
            @RequestParam(name = "turnoId", required = false) Long turnoId,
            HttpSession session,
            Model model) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        if (turnoId == null) {
            return "redirect:/citas/agendar?error=seleccione_turno";
        }

        try {
            Cita cita
                    = citaService.iniciarProcesoEmision(turnoId, paciente);

            model.addAttribute("cita", cita);
            return "confirmar-reserva";

        } catch (IllegalStateException ex) {
            return "redirect:/citas/agendar?error=ocupado";
        }
    }

    @GetMapping("/pago/{citaId}")
    public String pantallaPago(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if (!"EN_PROCESO".equals(cita.getEstado())) {
            return "redirect:/citas/agendar?error=cita_invalida";
        }

        model.addAttribute("cita", cita);
        return "pago-cita";
    }

    @PostMapping("/procesar-pago")
    public String procesarPago(
            @RequestParam Long citaId,
            @RequestParam String numeroTarjeta,
            @RequestParam String metodoPago,
            HttpSession session) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if (!"EN_PROCESO".equals(cita.getEstado())) {
            return "redirect:/citas/agendar?error=cita_invalida";
        }

        try {
            Cita citaEmitida
                    = citaService.confirmarPagoYEmision(
                            citaId, numeroTarjeta, metodoPago);

            return "redirect:/citas/comprobante/"
                    + citaEmitida.getId();

        } catch (IllegalStateException ex) {
            return "redirect:/citas/agendar?error=tiempo_expirado";
        }
    }

    @GetMapping("/comprobante/{citaId}")
    public String verComprobante(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if (!"CONFIRMADA".equalsIgnoreCase(cita.getEstado())
                && !"REPROGRAMADA".equalsIgnoreCase(cita.getEstado())) {
            return "redirect:/citas/mis-citas";
        }

        model.addAttribute("cita", cita);
        return "comprobante-cita";
    }

    @GetMapping("/mis-citas")
    public String misCitas(
            @RequestParam(name = "tab", defaultValue = "proximas") String tab,
            HttpSession session,
            Model model) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        List<Cita> citas;

        if ("historicas".equalsIgnoreCase(tab)) {
            tab = "historicas";
            citas = citaRepository.findHistoricas(paciente.getId());
        } else {
            tab = "proximas";
            citas = citaRepository.findProximas(paciente.getId());
        }

        model.addAttribute("tabActiva", tab);
        model.addAttribute("citas", citas);
        model.addAttribute("paciente", paciente);

        model.addAttribute(
                "csrf", cancelacionController.csrf(session));

        return "mis-citas";
    }

    @GetMapping("/detalle/{citaId}")
    public String detalleCita(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        model.addAttribute("cita", cita);
        return "detalle-cita";
    }

    @GetMapping("/reprogramar/{citaId}")
    public String vistaReprogramar(
            @PathVariable Long citaId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        try {
            citaService.validarPlazoPostergacion(cita, paciente);
        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/citas/detalle/" + citaId;
        }

        List<Turno> turnosMedico
                = turnoRepository.findTurnosByMedicoId(
                        cita.getTurno().getMedico().getId(),
                        LocalDate.now());

        model.addAttribute("cita", cita);
        model.addAttribute("turnos", turnosMedico);

        return "reprogramar-cita";
    }

    @PostMapping("/reprogramar/confirmar")
    public String confirmarReprogramacion(
            @RequestParam Long citaId,
            @RequestParam(required = false) Long nuevoTurnoId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Paciente paciente
                = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if (nuevoTurnoId == null) {
            redirectAttributes.addAttribute(
                    "error", "Debe seleccionar un nuevo horario");

            return "redirect:/citas/reprogramar/" + citaId;
        }

        try {
            Cita citaActualizada
                    = citaService.reprogramarCita(
                            citaId, nuevoTurnoId, paciente);

            return "redirect:/citas/comprobante/"
                    + citaActualizada.getId();

        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/citas/reprogramar/" + citaId;
        }
    }

    private boolean perteneceAlPaciente(
            Cita cita, Paciente paciente) {

        return cita != null
                && cita.getPaciente() != null
                && paciente.getId() != null
                && paciente.getId().equals(cita.getPaciente().getId());
    }
}
