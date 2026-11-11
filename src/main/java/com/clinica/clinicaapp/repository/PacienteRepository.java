package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    boolean existsByDniAndIdNot(String dni, Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Paciente p where p.id = :id")
    Optional<Paciente> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Paciente p where lower(p.correo) = lower(:correo)")
    Optional<Paciente> bloquearPorCorreo(@org.springframework.data.repository.query.Param("correo") String correo);
    Optional<Paciente> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Paciente> findByCorreo(String correo);
    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
}