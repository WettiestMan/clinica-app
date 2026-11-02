package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByToken(String token);
}