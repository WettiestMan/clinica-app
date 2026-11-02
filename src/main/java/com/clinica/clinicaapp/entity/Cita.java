package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "citas")
@Getter
@Setter
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código utilizado por SP-03.
    @Column(unique = true, length = 40)
    private String codigo;

    // Código utilizado por el flujo de emisión de citas.
    @Column(name = "codigo_cita", unique = true, length = 30)
    private String codigoCita;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE_PAGO";

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_bloqueo_expiracion")
    private LocalDateTime fechaBloqueoExpiracion;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    public Cita() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    @Transient
    public String getCodigoVisible() {
        if (id != null) {
            return String.format(java.util.Locale.ROOT, "CIT-%06d", id);
        }

        return codigo != null ? codigo : codigoCita;
    }
}
