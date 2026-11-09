package com.clinica.clinicaapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila única utilizada para serializar el alta inicial y las cuentas. */
@Entity
@Table(name = "control_sistema")
public class ControlSistema {
    @Id
    private Long id;

    public ControlSistema() {}
    public ControlSistema(Long id) { this.id = id; }
    public Long getId() { return id; }
}
