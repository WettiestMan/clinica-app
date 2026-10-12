package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.CitaRepository;
import com.clinica.clinicaapp.service.*;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/citas")
public class CancelacionController {
  private final CancelacionService servicio;
  private final CitaRepository citas;
  private final ConstanciaPdf pdf;

  public CancelacionController(CancelacionService s, CitaRepository c, ConstanciaPdf p) {
    servicio = s;
    citas = c;
    pdf = p;
  }

  private Long usuario(HttpSession session) {
    Paciente p = (Paciente) session.getAttribute("usuarioLogueado");
    if (p == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
    return p.getId();
  }

  private String csrf(HttpSession s) {
    synchronized (s) {
      String t = (String) s.getAttribute("sp03Csrf");
      if (t == null) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        t = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        s.setAttribute("sp03Csrf", t);
      }
      return t;
    }
  }

  private void verificar(HttpSession s, String token) {
    if (token == null
        || !java.security.MessageDigest.isEqual(
            csrf(s).getBytes(java.nio.charset.StandardCharsets.UTF_8),
            token.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solicitud inválida. Recarga la página.");
  }

  @GetMapping("/mis-citas")
  public String lista(HttpSession s, Model m) {
    if (s.getAttribute("usuarioLogueado") == null) return "redirect:/login";
    m.addAttribute("citas", citas.findByPacienteIdOrderByFechaRegistroDesc(usuario(s)));
    return "mis-citas";
  }

  @GetMapping("/cancelar")
  public String formulario(@RequestParam(required = false) Long id, HttpSession s, Model m) {
    if (s.getAttribute("usuarioLogueado") == null) return "redirect:/login";
    m.addAttribute("csrf", csrf(s));
    m.addAttribute("motivos", CancelacionService.MOTIVOS);
    if (id != null) m.addAttribute("evaluacion", servicio.evaluar(id, usuario(s)));
    return "cancelar-cita";
  }

  @PostMapping("/cancelar/buscar")
  public String buscar(@RequestParam String codigo, @RequestParam String csrf, HttpSession s) {
    Long u = usuario(s);
    verificar(s, csrf);
    return "redirect:/citas/cancelar?id=" + servicio.buscar(codigo, u).getId();
  }

  @PostMapping("/cancelar/confirmar")
  public String confirmar(
      @RequestParam Long id,
      @RequestParam String motivo,
      @RequestParam(defaultValue = "") String detalle,
      @RequestParam(defaultValue = "false") boolean aceptaSinReembolso,
      @RequestParam String csrf,
      HttpSession s) {
    Long u = usuario(s);
    verificar(s, csrf);
    Cancelacion b = servicio.confirmar(id, u, motivo, detalle, aceptaSinReembolso);
    return "redirect:/citas/cancelaciones/" + b.getId();
  }

  @GetMapping("/cancelaciones/{id}")
  public String resultado(@PathVariable Long id, HttpSession s, Model m) {
    m.addAttribute("baja", servicio.constancia(id, usuario(s)));
    return "cancelacion-resultado";
  }

  @GetMapping("/cancelaciones/{id}/constancia.pdf")
  public ResponseEntity<byte[]> descargar(@PathVariable Long id, HttpSession s)
      throws java.io.IOException {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=constancia-CAN-" + id + ".pdf")
        .body(pdf.generar(servicio.constancia(id, usuario(s))));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public String error(
      ResponseStatusException ex, Model m, jakarta.servlet.http.HttpServletResponse response) {
    response.setStatus(ex.getStatusCode().value());
    m.addAttribute("mensaje", ex.getReason());
    return "cancelacion-error";
  }
}
