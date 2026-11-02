package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "eventos_cancelacion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cancelacion_id", "tipo"}))
@Getter
@Setter
public class EventoCancelacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "cancelacion_id", nullable = false)
  private Cancelacion cancelacion;

  @Column(nullable = false)
  private String tipo;

  @Column(nullable = false)
  private String estado = "PENDIENTE";

  @Column(nullable = false)
  private int intentos;

  @Column(nullable = false)
  private Instant proximoIntento = Instant.now();

  private String ultimoError;

  @Column(length = 200)
  private String referenciaExterna;
}
