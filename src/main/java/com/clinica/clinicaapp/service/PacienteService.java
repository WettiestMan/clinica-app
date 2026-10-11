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
}