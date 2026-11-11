package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.config.CuentaPrincipal;
import com.clinica.clinicaapp.service.AdministracionUsuariosService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jefe/usuarios")
public class UsuariosController {

    private final AdministracionUsuariosService usuarios;

    public UsuariosController(AdministracionUsuariosService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(name = "rol", defaultValue = "TODOS") String rol,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        pagina = Math.max(0, Math.min(pagina, 100000));
        q = q.trim();
        if (q.length() > 100) {
            q = q.substring(0, 100);
        }

        // Obtener lista general según búsqueda por texto
        var filasCompletas = usuarios.listar(q, pagina);

        // Filtrar en memoria por el Rol seleccionado en las pestañas
        final String rolFiltro = rol.toUpperCase();
        var filasFiltradas = filasCompletas.stream()
                .filter(u -> "TODOS".equals(rolFiltro) || rolFiltro.equalsIgnoreCase(u.rol()))
                .toList();

        model.addAttribute("hayMas", filasFiltradas.size() > 25);
        model.addAttribute("usuarios", filasFiltradas.stream().limit(25).toList());
        model.addAttribute("q", q);
        model.addAttribute("rolSeleccionado", rolFiltro);
        model.addAttribute("pagina", pagina);

        return "jefe-usuarios";
    }

    @GetMapping("/{tipo}/{id}/{accion:editar|estado}")
    public String formulario(
            @PathVariable String tipo,
            @PathVariable Long id,
            @PathVariable String accion,
            Model model) {

        var usuario = usuarios.obtener(tipo, id);

        // Impedir abrir pantalla de estado si la cuenta es de Jefatura Médica
        if ("estado".equals(accion) && "JEFE_MEDICO".equalsIgnoreCase(usuario.rol())) {
            return "redirect:/jefe/usuarios";
        }

        model.addAttribute("usuario", usuario);
        return "jefe-usuario-" + accion;
    }

    @PostMapping("/{tipo}/{id}/editar")
    public String editar(
            @AuthenticationPrincipal CuentaPrincipal actor,
            @PathVariable String tipo,
            @PathVariable Long id,
            @RequestParam long version,
            @RequestParam String nombres,
            @RequestParam(defaultValue = "") String apellidos,
            @RequestParam String correo,
            @RequestParam(defaultValue = "") String dni,
            @RequestParam(defaultValue = "") String password,
            RedirectAttributes flash) {

        try {
            usuarios.editar(actor, tipo, id, version, nombres, apellidos, correo, dni, password);
            flash.addFlashAttribute("exito", "Cuenta actualizada. La persona deberá iniciar sesión nuevamente.");
            return "redirect:/jefe/usuarios";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/jefe/usuarios/" + tipo + "/" + id + "/editar";
        }
    }

    @PostMapping("/{tipo}/{id}/estado")
    public String estado(
            @AuthenticationPrincipal CuentaPrincipal actor,
            @PathVariable String tipo,
            @PathVariable Long id,
            @RequestParam long version,
            @RequestParam boolean activo,
            RedirectAttributes flash) {

        try {
            var usuario = usuarios.obtener(tipo, id);

            // Blindaje de seguridad en backend: la cuenta de JEFE_MEDICO no se puede desactivar
            if ("JEFE_MEDICO".equalsIgnoreCase(usuario.rol())) {
                flash.addFlashAttribute("error", "No está permitido desactivar una cuenta con rol de Jefatura Médica.");
                return "redirect:/jefe/usuarios";
            }

            usuarios.estado(actor, tipo, id, version, activo);
            flash.addFlashAttribute("exito", activo ? "Cuenta reactivada." : "Cuenta dada de baja. Se conserva su historial.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/jefe/usuarios";
    }
}