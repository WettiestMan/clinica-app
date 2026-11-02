package com.clinica.clinicaapp.config;

import com.clinica.clinicaapp.repository.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Se incorpora solo a la cadena de seguridad, no como filtro servlet duplicado. */
public class CuentaVigenteFilter extends OncePerRequestFilter {
    private final PacienteRepository pacientes;
    private final UsuarioPersonalRepository personal;

    public CuentaVigenteFilter(PacienteRepository pacientes, UsuarioPersonalRepository personal) {
        this.pacientes = pacientes; this.personal = personal;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CuentaPrincipal principal) {
            boolean vigente;
            if (principal.getTipoCuenta().equals("paciente")) {
                vigente = pacientes.findById(principal.getCuentaId())
                        .map(p -> p.isActivo() && p.getVersionAcceso() == principal.getVersionAcceso()).orElse(false);
            } else {
                vigente = personal.findById(principal.getCuentaId())
                        .map(p -> p.isActivo() && p.getVersionAcceso() == principal.getVersionAcceso()).orElse(false);
            }
            if (!vigente) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?cuentaActualizada");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
