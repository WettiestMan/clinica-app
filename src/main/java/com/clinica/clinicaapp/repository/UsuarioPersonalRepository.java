package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UsuarioPersonalRepository extends JpaRepository<UsuarioPersonal, Long> {
    @EntityGraph(attributePaths = {"medico", "medico.especialidad"})
    Optional<UsuarioPersonal> findByCorreoIgnoreCase(String correo);

    boolean existsByRol(Rol rol);
    long countByRolAndActivoTrue(Rol rol);
    boolean existsByMedicoIdAndActivoFalse(Long medicoId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioPersonal u where u.id = :id")
    Optional<UsuarioPersonal> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByMedicoId(Long medicoId);
    boolean existsByMedicoIdAndActivoTrue(Long medicoId);

    @EntityGraph(attributePaths = {"medico", "medico.especialidad"})
    List<UsuarioPersonal> findByRolOrderByNombreAsc(Rol rol);
}
