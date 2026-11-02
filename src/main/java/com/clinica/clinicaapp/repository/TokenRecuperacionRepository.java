package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update TokenRecuperacion t set t.usado = true where lower(t.correo) = lower(:correo)")
    int invalidarPorCorreo(@org.springframework.data.repository.query.Param("correo") String correo);

    Optional<TokenRecuperacion> findByToken(String token);
}