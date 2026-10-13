package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Citas Próximas: Confirmadas o recién creadas del paciente logueado
    @Query("SELECT c FROM Cita c WHERE c.paciente.id = :pacienteId " +
           "AND c.estado IN ('CONFIRMADA', 'EN_PROCESO') " +
           "ORDER BY c.turno.fecha ASC, c.turno.horaInicio ASC")
    List<Cita> findProximas(@Param("pacienteId") Long pacienteId);

    // Citas Históricas: Atendidas, Canceladas o Expiradas
    @Query("SELECT c FROM Cita c WHERE c.paciente.id = :pacienteId " +
           "AND c.estado IN ('ATENDIDA', 'CANCELADA', 'EXPIRADA') " +
           "ORDER BY c.turno.fecha DESC, c.turno.horaInicio DESC")
    List<Cita> findHistoricas(@Param("pacienteId") Long pacienteId);
}