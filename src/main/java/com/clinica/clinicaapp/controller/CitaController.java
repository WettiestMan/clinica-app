package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.Cita;
import com.clinica.clinicaapp.entity.ComprobantePago;
import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.repository.ComprobantePagoRepository;
import com.clinica.clinicaapp.repository.EspecialidadRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import com.clinica.clinicaapp.service.CitaService;
import com.clinica.clinicaapp.service.ConstanciaPdf;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final ConstanciaPdf constanciaPdf;

    public CitaController(EspecialidadRepository especialidadRepository,
                          TurnoRepository turnoRepository,
                          CitaRepository citaRepository,
                          CitaService citaService,
                          CancelacionController cancelacionController,
                          ComprobantePagoRepository comprobantePagoRepository,
                          ConstanciaPdf constanciaPdf) {
        this.especialidadRepository = especialidadRepository;
        this.turnoRepository = turnoRepository;
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.cancelacionController = cancelacionController;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.constanciaPdf = constanciaPdf;
    }

    @GetMapping("/agendar")
    public String agendarCita(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        ZoneId zonaLima = ZoneId.of("America/Lima");
        LocalDate desde = fechaDesde != null ? fechaDesde : LocalDate.now(zonaLima);
        LocalDate hasta = fechaHasta != null ? fechaHasta : desde.plusDays(6);

        Map<Object, List<Turno>> turnosPorMedico = new LinkedHashMap<>();

        if (especialidadId != null) {
            LocalDateTime ahora = LocalDateTime.now(zonaLima);
            List<Turno> turnos = turnoRepository.buscarTurnosDisponibles(especialidadId, desde, hasta)
                    .stream()
                    .filter(t -> t.getFecha().atTime(t.getHoraInicio()).isAfter(ahora))
                    .toList();

            turnosPorMedico = turnos.stream().collect(
                    Collectors.groupingBy(
                            Turno::getMedico,
                            LinkedHashMap::new,
                            Collectors.toList()));
        }

        model.addAttribute("especialidades", especialidadRepository.findAll());
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
            Model model,
            RedirectAttributes redirectAttributes) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        if (turnoId == null) {
            redirectAttributes.addFlashAttribute("mensajeAlerta", "Por favor, selecciona un horario de atención.");
            return "redirect:/citas/agendar";}

        Turno turno = turnoRepository.findById(turnoId).orElse(null);
        if (turno == null || !turno.isActivo() || !"DISPONIBLE".equals(turno.getEstado())) {
           redirectAttributes.addFlashAttribute("alertaTurnoGanado", 
                    "El horario seleccionado ya no está disponible. Por favor, elige otro turno.");
            return "redirect:/citas/agendar";
        }

       model.addAttribute("turno", turno);
        model.addAttribute("paciente", paciente);
        model.addAttribute("monto", turno.getMedico().getEspecialidad().getPrecioConsulta());

        return "confirmar-reserva";
    }

    @PostMapping("/iniciar-pago")
    public String iniciarPago(
            @RequestParam Long turnoId,
            @RequestParam(defaultValue = "") String asunto,
            HttpSession session, 
            Model model,
            RedirectAttributes redirectAttributes) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        try {
            Cita cita;
            if (asunto != null && !asunto.isBlank()) {
                cita = citaService.iniciarProcesoEmision(turnoId, paciente, asunto);
            } else {
                cita = citaService.iniciarProcesoEmision(turnoId, paciente);
            }
            return "redirect:/citas/pago/" + cita.getId();

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorAsunto", ex.getMessage());
            model.addAttribute("asunto", asunto);
            return confirmarReserva(turnoId, session, model, redirectAttributes);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertaTurnoGanado", 
                    "El horario que seleccionaste acaba de ser reservado por otro paciente. Por favor, selecciona otro horario disponible.");
            return "redirect:/citas/agendar";
        }
    }

    @GetMapping("/pago/{citaId}")
    public String pantallaPago(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

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
        model.addAttribute("expiracionEpoch", cita.getFechaBloqueoExpiracion() == null ? 0L
                : cita.getFechaBloqueoExpiracion().atZone(ZoneId.of("America/Lima")).toInstant().toEpochMilli());
        return "pago-cita";
    }

    @PostMapping("/procesar-pago")
    public String procesarPago(
            @RequestParam Long citaId,
            @RequestParam String numeroTarjeta,
            @RequestParam String metodoPago,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if ("CONFIRMADA".equals(cita.getEstado()) || "REPROGRAMADA".equals(cita.getEstado())) {
            return "redirect:/citas/comprobante/" + citaId;
        }

        if (!"EN_PROCESO".equals(cita.getEstado())) {
            return "redirect:/citas/agendar?error=cita_invalida";
        }

        try {
            Cita citaEmitida = citaService.confirmarPagoYEmision(citaId, numeroTarjeta, metodoPago);
            return "redirect:/citas/comprobante/" + citaEmitida.getId();
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/citas/agendar";
        }
    }


    // ==============================================================
    // CANCELACIÓN VOLUNTARIA DE RESERVA EN PROCESO DE PAGO (PASO 3)
    // ==============================================================
    @PostMapping("/cancelar-pago")
    public String cancelarPago(
            @RequestParam Long citaId,
            jakarta.servlet.http.HttpSession session) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);
        Long especialidadId = null;

        if (cita != null && cita.getTurno() != null) {
            especialidadId = cita.getTurno().getMedico().getEspecialidad().getId();
            citaService.cancelarReservaEnProceso(citaId, paciente);
        }

        return especialidadId != null
                ? "redirect:/citas/agendar?especialidadId=" + especialidadId
                : "redirect:/citas/agendar";
    }

    @GetMapping("/comprobante/{citaId}")
    public String verComprobante(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

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

        ComprobantePago comprobante = comprobantePagoRepository
                .findByCitaIdAndEstado(cita.getId(), "APROBADO")
                .orElse(null);

        model.addAttribute("cita", cita);
        model.addAttribute("comprobante", comprobante);
        return "comprobante-cita";
    }

    @GetMapping("/descargar-constancia/{citaId}")
    public ResponseEntity<byte[]> descargarConstancia(
            @PathVariable Long citaId,
            HttpSession session) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");
        if (paciente == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (!perteneceAlPaciente(cita, paciente)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!"CONFIRMADA".equalsIgnoreCase(cita.getEstado())
                && !"REPROGRAMADA".equalsIgnoreCase(cita.getEstado())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ComprobantePago comprobante = comprobantePagoRepository
                .findByCitaIdAndEstado(cita.getId(), "APROBADO")
                .orElse(null);

        try {
            byte[] pdfBytes = constanciaPdf.generarComprobante(cita, comprobante);

            String codigo = cita.getCodigoCita() != null ? cita.getCodigoCita() : "CIT-" + cita.getId();
            String nombreArchivo = "Constancia_" + codigo + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            tab = "historicas";
            citas = citaRepository.findHistoricas(paciente.getId());
        } else {
            tab = "proximas";
            citas = citaRepository.findProximas(paciente.getId());
        }

        model.addAttribute("tabActiva", tab);
        model.addAttribute("citas", citas);
        model.addAttribute("paciente", paciente);
        model.addAttribute("csrf", cancelacionController.csrf(session));

        return "mis-citas";
    }

    @GetMapping("/detalle/{citaId}")
    public String detalleCita(
            @PathVariable Long citaId,
            HttpSession session,
            Model model) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        boolean activa = Set.of("CONFIRMADA", "REPROGRAMADA", "EMITIDA").contains(cita.getEstado());
        model.addAttribute("cancelable", activa && cita.getTurno().getFecha().atTime(cita.getTurno().getHoraInicio()).isAfter(ahora));
        model.addAttribute("reprogramable", activa && !cita.getTurno().getFecha().atTime(cita.getTurno().getHoraInicio()).isBefore(ahora.plusHours(24)));
        model.addAttribute("cita", cita);
        return "detalle-cita";
    }

    @GetMapping("/reprogramar/{citaId}")
    public String vistaReprogramar(
            @PathVariable Long citaId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

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

        List<Turno> turnosMedico = turnoRepository.findTurnosByMedicoId(
                cita.getTurno().getMedico().getId(),
                LocalDate.now(ZoneId.of("America/Lima")));

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

        Paciente paciente = (Paciente) session.getAttribute("usuarioLogueado");

        if (paciente == null) {
            return "redirect:/login";
        }

        Cita cita = citaRepository.findById(citaId).orElse(null);

        if (!perteneceAlPaciente(cita, paciente)) {
            return "redirect:/citas/mis-citas";
        }

        if (nuevoTurnoId == null) {
            redirectAttributes.addAttribute("error", "Debe seleccionar un nuevo horario");
            return "redirect:/citas/reprogramar/" + citaId;
        }

        try {
            Cita citaActualizada = citaService.reprogramarCita(citaId, nuevoTurnoId, paciente);
            return "redirect:/citas/comprobante/" + citaActualizada.getId();
        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/citas/reprogramar/" + citaId;
        }
    }

    private boolean perteneceAlPaciente(Cita cita, Paciente paciente) {
        return cita != null
                && cita.getPaciente() != null
                && paciente.getId() != null
                && paciente.getId().equals(cita.getPaciente().getId());
    }
}