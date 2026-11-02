package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    // Permite filtrar médicos por su especialidad si se requiere en el selector
    List<Medico> findByEspecialidadId(Long especialidadId);

    // Útil para autenticación o búsquedas por correo
    Optional<Medico> findByCorreo(String correo);
}