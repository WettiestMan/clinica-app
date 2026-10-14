package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cancelaciones")
@Getter
@Setter
public class Cancelacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "cita_id", nullable = false, unique = true)
  private Cita cita;

  @Column(nullable = false)
  private Long usuarioId;

  @Column(nullable = false)
  private String motivo;

  @Column(length = 1000)
  private String detalle;

  @Column(nullable = false)
  private Instant fecha;

  @Column(nullable = false)
  private boolean dentroPlazo;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal montoReintegrable;

  @Column(nullable = false)
  private String estadoReintegro;
}
