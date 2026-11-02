package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios_personal")
@Getter
@Setter
public class UsuarioPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String correo;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", unique = true)
    private Medico medico;
    @Column(nullable = false)
    private boolean activo = true;
}
