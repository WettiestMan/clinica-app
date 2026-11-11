package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Medico;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    // Filtrar médicos por especialidad para selectores dinámicos
    List<Medico> findByEspecialidadId(Long especialidadId);

    // Búsqueda para autenticación y login de médicos
    Optional<Medico> findByCorreo(String correo);

    // Listado optimizado con especialidad precargada para vistas administrativas
    @EntityGraph(attributePaths = "especialidad")
    List<Medico> findAllByOrderByApellidosAscNombresAsc();

    // Bloqueo pesimista para evitar colisiones concurrentes al asignar horarios
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medico m WHERE m.id = :id")
    Optional<Medico> bloquear(@Param("id") Long id);
}