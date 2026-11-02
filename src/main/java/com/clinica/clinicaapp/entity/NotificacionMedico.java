package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_medico")
@Getter
@Setter
public class NotificacionMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;
    @Column(nullable = false, length = 500)
    private String mensaje;
    @Column(nullable = false)
    private LocalDateTime creada;
    @Column(nullable = false)
    private boolean leida = false;
}
