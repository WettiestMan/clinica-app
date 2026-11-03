package com.clinica.clinicaapp.controller;

import com.clinica.clinicaapp.service.PersonalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class DatosVistaAdvice {
    private final PersonalService personal;
    private final com.clinica.clinicaapp.repository.UsuarioPersonalRepository usuarios;
    private final com.clinica.clinicaapp.repository.NotificacionMedicoRepository avisos;
    public DatosVistaAdvice(PersonalService personal,com.clinica.clinicaapp.repository.UsuarioPersonalRepository usuarios,
            com.clinica.clinicaapp.repository.NotificacionMedicoRepository avisos) {this.personal=personal;this.usuarios=usuarios;this.avisos=avisos;}
    @ModelAttribute("avisosSinLeer")
    public long pendientes(java.security.Principal principal) {
        if(principal==null)return 0;
        return usuarios.findByCorreoIgnoreCase(principal.getName())
            .filter(u->u.isActivo() && u.getRol()==com.clinica.clinicaapp.entity.Rol.MEDICO && u.getMedico()!=null)
            .map(u->avisos.countByMedicoIdAndLeidaFalse(u.getMedico().getId())).orElse(0L);
    }

    @ModelAttribute("nombreVista")
    public Object nombre(HttpSession session) { return session.getAttribute("nombrePersonal"); }
    @ModelAttribute("correoVista")
    public Object correo(HttpSession session) { return session.getAttribute("correoPersonal"); }
    @ModelAttribute("setupDisponible")
    public boolean setup() { return personal.requiereConfiguracion(); }
}
