package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalService {

    private final UsuarioPersonalRepository usuarios;
    private final MedicoRepository medicos;
    private final EspecialidadRepository especialidades;
    private final CuentasService cuentas;
    private final PasswordEncoder encoder;

    public PersonalService(UsuarioPersonalRepository usuarios, MedicoRepository medicos,
            EspecialidadRepository especialidades, CuentasService cuentas, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.especialidades = especialidades;
        this.cuentas = cuentas;
        this.encoder = encoder;
    }

    public boolean requiereConfiguracion() {
        return !usuarios.existsByRol(Rol.JEFE_MEDICO);
    }

    @Transactional
    public void crearPrimerJefe(String nombre, String correo, String password) {
        correo = cuentas.reservarCorreo(correo);
        if (!requiereConfiguracion()) {
            throw new IllegalStateException("El jefe médico ya está configurado.");
        }
        cuentas.validarPassword(password, 12);
        UsuarioPersonal usuario = new UsuarioPersonal();
        usuario.setNombre(cuentas.nombre(nombre, "el nombre"));
        usuario.setCorreo(correo);
        usuario.setPassword(encoder.encode(password));
        usuario.setRol(Rol.JEFE_MEDICO);
        usuarios.saveAndFlush(usuario);
    }

    // ==============================================================
    // 1. MÉTODO NUEVO (5 PARÁMETROS): USADO POR LA NUEVA VISTA
    // ==============================================================
    @Transactional
    public void crearMedico(String nombres, String apellidos,
            Long especialidadId, String correo, String password) {

        crearMedico(null, nombres, apellidos, especialidadId, correo, password);
    }

    // ==============================================================
    // 2. MÉTODO COMPATIBLE (6 PARÁMETROS): USADO POR LOS TESTS
    // ==============================================================
    @Transactional
    public void crearMedico(Long medicoId, String nombres, String apellidos,
            Long especialidadId, String correo, String password) {

        correo = cuentas.reservarCorreo(correo);
        cuentas.validarPassword(password, 12);
        Medico medico;

        if (medicoId != null) {
            medico = medicos.bloquear(medicoId)
                    .orElseThrow(() -> new IllegalArgumentException("El médico seleccionado no existe."));
            if (usuarios.existsByMedicoId(medicoId)) {
                throw new IllegalArgumentException("El médico ya tiene una cuenta de acceso.");
            }
        } else {
            if (especialidadId == null) {
                throw new IllegalArgumentException("Selecciona una especialidad.");
            }
            medico = new Medico();
            medico.setNombres(cuentas.nombre(nombres, "los nombres"));
            medico.setApellidos(cuentas.nombre(apellidos, "los apellidos"));
            medico.setEspecialidad(especialidades.findById(especialidadId)
                    .orElseThrow(() -> new IllegalArgumentException("La especialidad no existe.")));
            medicos.save(medico);
        }

        UsuarioPersonal usuario = new UsuarioPersonal();
        usuario.setNombre(medico.getNombres() + " " + medico.getApellidos());
        usuario.setCorreo(correo);
        usuario.setPassword(encoder.encode(password));
        usuario.setMedico(medico);
        usuario.setRol(Rol.MEDICO);
        usuarios.saveAndFlush(usuario);
    }
}