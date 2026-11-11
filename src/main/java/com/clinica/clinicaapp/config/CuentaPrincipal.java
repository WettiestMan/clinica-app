package com.clinica.clinicaapp.config;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Identidad estable: el correo puede cambiar y la sesión debe poder revocarse. */
public class CuentaPrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final Long cuentaId;
    private final String tipoCuenta;
    private final long versionAcceso;

    public CuentaPrincipal(Long id, String tipo, String correo, String password,
            String rol, boolean activo, long version) {
        super(correo, password, activo, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.cuentaId = id;
        this.tipoCuenta = tipo;
        this.versionAcceso = version;
    }
    public Long getCuentaId() { return cuentaId; }
    public String getTipoCuenta() { return tipoCuenta; }
    public long getVersionAcceso() { return versionAcceso; }
}
