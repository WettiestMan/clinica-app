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

    // Filtrar médicos ACTIVOS por especialidad para selectores dinámicos de horarios
    @Query("""
        SELECT m FROM Medico m
        JOIN UsuarioPersonal u ON u.medico = m
        WHERE m.especialidad.id = :especialidadId
          AND u.activo = true
        ORDER BY m.apellidos ASC, m.nombres ASC
    """)
    List<Medico> findByEspecialidadId(@Param("especialidadId") Long especialidadId);

    // Listar todos los médicos activos del sistema (para paneles generales)
    @Query("""
        SELECT m FROM Medico m
        JOIN UsuarioPersonal u ON u.medico = m
        WHERE u.activo = true
        ORDER BY m.apellidos ASC, m.nombres ASC
    """)
    List<Medico> findTodosActivos();

    // Búsqueda para autenticación y login de médicos
    Optional<Medico> findByCorreo(String correo);

    // Listado optimizado con especialidad precargada para vistas administrativas (muestra todos, activos e inactivos)
    @EntityGraph(attributePaths = "especialidad")
    List<Medico> findAllByOrderByApellidosAscNombresAsc();

    // Bloqueo pesimista para evitar colisiones concurrentes al asignar horarios
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medico m WHERE m.id = :id")
    Optional<Medico> bloquear(@Param("id") Long id);

    // Validación preventiva: saber si un médico específico cuenta con usuario activo
    @Query("""
        SELECT COUNT(u) > 0 FROM UsuarioPersonal u
        WHERE u.medico.id = :medicoId
          AND u.activo = true
    """)
    boolean isMedicoActivo(@Param("medicoId") Long medicoId);
}