package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Paciente;
import com.clinica.clinicaapp.repository.PacienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CuentasService cuentas;
    private final com.clinica.clinicaapp.repository.TokenRecuperacionRepository tokens;

    public PacienteService(PacienteRepository pacienteRepository, PasswordEncoder passwordEncoder, CuentasService cuentas, com.clinica.clinicaapp.repository.TokenRecuperacionRepository tokens) {
        this.tokens = tokens;
        this.cuentas = cuentas;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @org.springframework.transaction.annotation.Transactional
    public Paciente registrarPaciente(Paciente paciente) {
        paciente.setId(null);
        paciente.setActivo(true);
        paciente.setVersionAcceso(0);
        paciente.setCorreo(cuentas.reservarCorreo(paciente.getCorreo()));
        cuentas.validarPassword(paciente.getPassword(), 8);
        paciente.setNombres(cuentas.nombre(paciente.getNombres(), "los nombres"));
        paciente.setApellidos(cuentas.nombre(paciente.getApellidos(), "los apellidos"));
        if (paciente.getDni() == null || !paciente.getDni().matches("[0-9]{8}"))
            throw new IllegalArgumentException("El DNI debe tener ocho dígitos.");
        if (pacienteRepository.existsByDni(paciente.getDni())) throw new IllegalArgumentException("El DNI ya está registrado.");
        // Hasheamos la contraseña con BCrypt antes de guardar
        String passwordHasheada = passwordEncoder.encode(paciente.getPassword());
        paciente.setPassword(passwordHasheada);
        return pacienteRepository.save(paciente);
    }

    public Optional<Paciente> autenticar(String correo, String passwordPlana) {
        Optional<Paciente> optPaciente = pacienteRepository.findByCorreoIgnoreCase(correo);
        if (optPaciente.isPresent()) {
            Paciente p = optPaciente.get();
            // Comparamos el texto plano contra el hash guardado en la BD
            if (p.isActivo() && passwordEncoder.matches(passwordPlana, p.getPassword())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<Paciente> buscarPorCorreo(String correo) {
        return pacienteRepository.findByCorreoIgnoreCase(correo);
    }

    @org.springframework.transaction.annotation.Transactional
    public void actualizarPassword(String correo, String nuevaPassword) {
        cuentas.validarPassword(nuevaPassword, 8);
        Paciente paciente = pacienteRepository.bloquearPorCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
        
        // Encriptar la nueva contraseña con BCrypt antes de guardar
        paciente.setPassword(passwordEncoder.encode(nuevaPassword));
        paciente.setVersionAcceso(paciente.getVersionAcceso() + 1);
        tokens.invalidarPorCorreo(paciente.getCorreo());
        pacienteRepository.save(paciente);
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean cambiarPasswordConValidacion(String correo, String passwordActual, String nuevaPassword) {
    cuentas.validarPassword(nuevaPassword,8);
    Optional<Paciente> pacienteOpt = pacienteRepository.bloquearPorCorreo(correo);
    if (pacienteOpt.isEmpty()) {
        return false;
    }
    
    Paciente paciente = pacienteOpt.get();
    
    // Verificar que la contraseña actual coincida con el hash en la BD
    if (!passwordEncoder.matches(passwordActual, paciente.getPassword())) {
        return false;
    }
    
    // Hashear y guardar la nueva contraseña
    paciente.setPassword(passwordEncoder.encode(nuevaPassword));
    paciente.setVersionAcceso(paciente.getVersionAcceso() + 1);
    tokens.invalidarPorCorreo(paciente.getCorreo());
    pacienteRepository.save(paciente);
    return true;
}
}