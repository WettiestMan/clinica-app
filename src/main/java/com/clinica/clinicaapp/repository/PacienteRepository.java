package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByCorreo(String correo);
    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
}