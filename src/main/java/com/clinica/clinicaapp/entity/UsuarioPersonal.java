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

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("''")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", unique = true)
    private Medico medico;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("true")
    private boolean activo = true;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private long versionAcceso = 0;

}
