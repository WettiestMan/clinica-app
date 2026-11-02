package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.NotificacionMedico;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionMedicoRepository extends JpaRepository<NotificacionMedico, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update NotificacionMedico n set n.leida=true where n.medico.id=:medico and n.leida=false")
    int leerTodas(@org.springframework.data.repository.query.Param("medico") Long medico);
    boolean existsByEventoCancelacionId(Long eventoCancelacionId);
    List<NotificacionMedico> findTop50ByMedicoIdAndLeidaFalseOrderByIdDesc(Long medicoId);
    List<NotificacionMedico> findTop50ByMedicoIdOrderByIdDesc(Long medicoId);
    List<NotificacionMedico> findTop5ByMedicoIdAndLeidaFalseOrderByIdDesc(Long medicoId);
    Optional<NotificacionMedico> findByIdAndMedicoId(Long id, Long medicoId);
    long countByMedicoIdAndLeidaFalse(Long medicoId);
}
