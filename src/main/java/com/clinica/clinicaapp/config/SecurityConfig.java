package com.clinica.clinicaapp.config;

import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService usuarios(UsuarioPersonalRepository personal, PacienteRepository pacientes) {
        return correo -> {
            var cuenta = personal.findByCorreoIgnoreCase(correo.trim());
            if (cuenta.isPresent()) {
                UsuarioPersonal usuario = cuenta.get();
                return new CuentaPrincipal(usuario.getId(), "personal", usuario.getCorreo(), usuario.getPassword(),
                        usuario.getRol().name(), usuario.isActivo(), usuario.getVersionAcceso());
            }
            Paciente paciente = pacientes.findByCorreoIgnoreCase(correo.trim())
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
            return new CuentaPrincipal(paciente.getId(), "paciente", paciente.getCorreo(), paciente.getPassword(),
                    "PACIENTE", paciente.isActivo(), paciente.getVersionAcceso());
        };
    }

    @Bean
    public SecurityFilterChain acceso(HttpSecurity http, UsuarioPersonalRepository personal,
            PacienteRepository pacientes) throws Exception {
        http.authorizeHttpRequests(reglas -> reglas
                .requestMatchers("/login", "/registro", "/configuracion-inicial", "/recuperar-password/**",
                        "/restablecer-password", "/error-acceso", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/jefe/**").hasRole("JEFE_MEDICO")
                .requestMatchers("/medico/**").hasRole("MEDICO")
                .requestMatchers("/citas/**", "/perfil/**").hasRole("PACIENTE")
                .requestMatchers("/").authenticated()
                .anyRequest().denyAll())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                        .accessDeniedPage("/error-acceso"))
                .formLogin(formulario -> formulario.loginPage("/login")
                        .usernameParameter("correo").passwordParameter("password")
                        .failureUrl("/login?error=credenciales")
                        .successHandler((request, response, auth) -> {
                            var session = request.getSession();
                            session.removeAttribute("usuarioLogueado");
                            session.removeAttribute("medicoLogueado");
                            session.removeAttribute("nombrePersonal");
                            session.removeAttribute("correoPersonal");
                            var cuenta = personal.findByCorreoIgnoreCase(auth.getName());
                            String destino;
                            if (cuenta.isPresent()) {
                                var usuario = cuenta.get();
                                session.setAttribute("rol", usuario.getRol().name());
                                session.setAttribute("nombrePersonal", usuario.getRol() == Rol.MEDICO ? usuario.getMedico().getNombres() : usuario.getNombre());
                                session.setAttribute("correoPersonal", usuario.getCorreo());
                                if (usuario.getRol() == Rol.MEDICO) {
                                    session.setAttribute("medicoLogueado", usuario.getMedico());
                                    destino = "/medico/agenda";
                                } else destino = "/jefe/horarios";
                            } else {
                                session.setAttribute("rol", "PACIENTE");
                                session.setAttribute("usuarioLogueado", pacientes.findByCorreoIgnoreCase(auth.getName()).orElseThrow());
                                destino = "/citas/agendar";
                            }
                            response.sendRedirect(request.getContextPath() + destino);
                        }))
                .logout(salida -> salida.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        http.addFilterAfter(new CuentaVigenteFilter(pacientes, personal),
                org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
