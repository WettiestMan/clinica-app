package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notificaciones_medico", indexes = @Index(name = "ix_aviso_medico", columnList = "medico_id,id"))
@Getter
@Setter
public class NotificacionMedico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long eventoCancelacionId;

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
