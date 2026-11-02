package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "medicos")
@Getter
@Setter
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(unique = true, length = 120)
    private String correo;

    @Column(length = 255)
    private String password;

    @Column(name = "es_jefe")
    private Boolean esJefe = false;
}