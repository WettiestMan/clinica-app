package com.clinica.clinicaapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado;

    public TokenRecuperacion() {}

    public TokenRecuperacion(String token, String correo, int minutosValidez) {
        this.token = token;
        this.correo = correo;
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(minutosValidez);
        this.usado = false;
    }

    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion) || usado;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public String getCorreo() { return correo; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}