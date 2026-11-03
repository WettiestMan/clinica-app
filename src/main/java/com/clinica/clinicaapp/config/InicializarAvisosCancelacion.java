package com.clinica.clinicaapp.config;

import com.clinica.clinicaapp.service.AvisosCancelacionService;
import java.util.List;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Incorpora los cupos liberados anteriores sin duplicar avisos al reiniciar. */
@Component
public class InicializarAvisosCancelacion implements ApplicationRunner {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private final AvisosCancelacionService avisos;
    private final TransactionTemplate tx;
    public InicializarAvisosCancelacion(AvisosCancelacionService avisos, TransactionTemplate tx) {this.avisos=avisos;this.tx=tx;}
    public void run(ApplicationArguments args) {
        while(true) {
            List<Long> ids=tx.execute(status->em.createQuery("select e.id from EventoCancelacion e where e.tipo='AVISO_CUPO_LIBERADO' and not exists (select n.id from NotificacionMedico n where n.eventoCancelacionId=e.id) order by e.id",Long.class).setMaxResults(100).getResultList());
            if(ids==null||ids.isEmpty())return;
            ids.forEach(avisos::registrar);
        }
    }
}
