package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cita", unique = true, length = 30)
    private String codigoCita; // Ej: CIT-2026-X8F2A


    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

    // Getter y Setter
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Column(nullable = false, length = 30)
    private String estado; // EN_PROCESO, CONFIRMADA, EXPIRADA, CANCELADA

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_bloqueo_expiracion")
    private LocalDateTime fechaBloqueoExpiracion;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    public Cita() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoCita() { return codigoCita; }
    public void setCodigoCita(String codigoCita) { this.codigoCita = codigoCita; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDateTime getFechaBloqueoExpiracion() { return fechaBloqueoExpiracion; }
    public void setFechaBloqueoExpiracion(LocalDateTime fechaBloqueoExpiracion) { this.fechaBloqueoExpiracion = fechaBloqueoExpiracion; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }
}