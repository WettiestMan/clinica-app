package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "especialidades")
@Getter
@Setter
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer duracionMinutosDefault = 30; // 15 rápida, 30 regular, 120 cirugía

    @Column(name = "precio_consulta", precision = 10, scale = 2)
    private java.math.BigDecimal precioConsulta;

    public java.math.BigDecimal getPrecioConsulta() {
        return precioConsulta != null ? precioConsulta : new java.math.BigDecimal("70.00");
    }

    public void setPrecioConsulta(java.math.BigDecimal precioConsulta) {
        this.precioConsulta = precioConsulta;
    }
}