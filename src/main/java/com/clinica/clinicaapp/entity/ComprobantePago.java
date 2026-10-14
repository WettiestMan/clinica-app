package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comprobantes_pago")
@Getter
@Setter
public class ComprobantePago {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "cita_id", nullable = false, unique = true)
  private Cita cita;

  @Column(nullable = false, unique = true)
  private String numero;

  @Column(nullable = false)
  private String estado;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal importe;

  @Column(nullable = false)
  private String moneda = "PEN";
}
