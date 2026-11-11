package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.UsuarioPersonalRepository;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MedicoActualService {
    private final UsuarioPersonalRepository usuarios;
    public MedicoActualService(UsuarioPersonalRepository usuarios) { this.usuarios = usuarios; }

    @Transactional(readOnly = true)
    public Medico obtener(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión.");
        UsuarioPersonal usuario = usuarios.findByCorreoIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere una cuenta médica."));
        if (!usuario.isActivo() || usuario.getRol() != Rol.MEDICO || usuario.getMedico() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere una cuenta médica activa.");
        return usuario.getMedico();
    }
}
