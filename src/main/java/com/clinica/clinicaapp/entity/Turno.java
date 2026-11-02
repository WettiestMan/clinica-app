package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Getter @Setter
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("true")
    private boolean activo = true;

    @Version
    @org.hibernate.annotations.ColumnDefault("0")
    private long version;

    @Column(length = 300)
    private String motivoBaja;


    @Column(length = 60)
    private String consultorio;

    @Transient
    public String getConsultorioVisible() {
        return consultorio == null || consultorio.isBlank() ? "Por asignar" : consultorio;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private Integer duracionMinutos; // Duración fija una vez creado

    @Column(nullable = false)
    private String estado; // "DISPONIBLE", "EN_PROCESO", "OCUPADO"

    @Column(nullable = false)
    private Boolean esSobrecupo = false; // Sobrecupo habilitado por Jefe Médico
}
