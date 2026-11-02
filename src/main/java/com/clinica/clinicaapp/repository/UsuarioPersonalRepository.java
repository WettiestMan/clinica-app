package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface UsuarioPersonalRepository extends JpaRepository<UsuarioPersonal, Long> {

    @EntityGraph(attributePaths = {"medico", "medico.especialidad"})
    Optional<UsuarioPersonal> findByCorreoIgnoreCase(String correo);

    boolean existsByRol(Rol rol);

    boolean existsByMedicoId(Long id);
}
