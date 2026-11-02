package com.clinica.clinicaapp.repository;

import com.clinica.clinicaapp.entity.NotificacionMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificacionMedicoRepository extends JpaRepository<NotificacionMedico, Long> {

    List<NotificacionMedico> findTop50ByMedicoIdOrderByIdDesc(Long medico);

    Optional<NotificacionMedico> findByIdAndMedicoId(Long id, Long medico);
}
