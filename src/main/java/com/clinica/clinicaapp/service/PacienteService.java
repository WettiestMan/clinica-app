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

    public PacienteService(PacienteRepository pacienteRepository, PasswordEncoder passwordEncoder) {
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Paciente registrarPaciente(Paciente paciente) {
        // Hasheamos la contraseña con BCrypt antes de guardar
        String passwordHasheada = passwordEncoder.encode(paciente.getPassword());
        paciente.setPassword(passwordHasheada);
        return pacienteRepository.save(paciente);
    }

    public Optional<Paciente> autenticar(String correo, String passwordPlana) {
        Optional<Paciente> optPaciente = pacienteRepository.findByCorreo(correo);
        if (optPaciente.isPresent()) {
            Paciente p = optPaciente.get();
            // Comparamos el texto plano contra el hash guardado en la BD
            if (passwordEncoder.matches(passwordPlana, p.getPassword())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<Paciente> buscarPorCorreo(String correo) {
        return pacienteRepository.findByCorreo(correo);
    }

    public void actualizarPassword(String correo, String nuevaPassword) {
        Paciente paciente = pacienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
        
        // Encriptar la nueva contraseña con BCrypt antes de guardar
        paciente.setPassword(passwordEncoder.encode(nuevaPassword));
        pacienteRepository.save(paciente);
    }

    public boolean cambiarPasswordConValidacion(String correo, String passwordActual, String nuevaPassword) {
    Optional<Paciente> pacienteOpt = pacienteRepository.findByCorreo(correo);
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
    pacienteRepository.save(paciente);
    return true;
}
}