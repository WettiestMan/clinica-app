package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.NotificacionMedicoRepository;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvisosCancelacionService {
    private final NotificacionMedicoRepository avisos;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    public AvisosCancelacionService(NotificacionMedicoRepository avisos) {this.avisos=avisos;}
    @Transactional
    public void registrar(Long eventoId) {
        var evento=em.find(EventoCancelacion.class,eventoId,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(evento==null || !"AVISO_CUPO_LIBERADO".equals(evento.getTipo()) || avisos.existsByEventoCancelacionId(eventoId))return;
        var baja=evento.getCancelacion();var turno=baja.getCita().getTurno();
        var aviso=new NotificacionMedico();aviso.setMedico(turno.getMedico());aviso.setEventoCancelacionId(eventoId);
        aviso.setCreada(LocalDateTime.ofInstant(baja.getFecha(),ZoneId.of("America/Lima")));
        aviso.setMensaje("Cupo liberado por cancelación: " + turno.getFecha() + " de " + turno.getHoraInicio() + " a " + turno.getHoraFin() + ". Consultorio: " + turno.getConsultorioVisible() + ".");
        avisos.saveAndFlush(aviso);
    }
}
