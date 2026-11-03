package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.config.CuentaPrincipal;
import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class AdministracionUsuariosService {
    private final PacienteRepository pacientes;
    private final UsuarioPersonalRepository personal;
    private final MedicoRepository medicos;
    private final ControlSistemaRepository control;
    private final CitaRepository citas;
    private final TokenRecuperacionRepository tokens;
    private final CuentasService cuentas;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    public AdministracionUsuariosService(PacienteRepository pacientes, UsuarioPersonalRepository personal,
            MedicoRepository medicos, ControlSistemaRepository control, CitaRepository citas,
            TokenRecuperacionRepository tokens, CuentasService cuentas, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.pacientes=pacientes; this.personal=personal; this.medicos=medicos; this.control=control;
        this.citas=citas; this.tokens=tokens; this.cuentas=cuentas; this.encoder=encoder; this.jdbc=jdbc;
    }
    public record Ficha(String tipo, Long id, String nombres, String apellidos, String correo,
            String dni, String rol, boolean activo, long version) {}
    private static final String UNION="(select 'personal' as tipo, id, nombre as nombres, '' as apellidos, correo, '' as dni, rol, activo, version_acceso from usuarios_personal union all select 'paciente', id, nombres, apellidos, correo, dni, 'PACIENTE', activo, version_acceso from pacientes) u";
    public List<Ficha> listar(String q, int pagina) {
        String patron="%"+q.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")+"%";
        // La búsqueda usa parámetros; los identificadores y el orden son fijos.
        return jdbc.query("select * from "+UNION+" where lower(nombres) like ? escape '!' or lower(apellidos) like ? escape '!' or lower(correo) like ? escape '!' order by tipo,id limit 26 offset ?",
            (r,n)->new Ficha(r.getString("tipo"),r.getLong("id"),r.getString("nombres"),r.getString("apellidos"),r.getString("correo"),r.getString("dni"),r.getString("rol"),r.getBoolean("activo"),r.getLong("version_acceso")),patron,patron,patron,Math.min(pagina,100000)*25);
    }
    public Ficha obtener(String tipo, Long id) {
        if ("paciente".equals(tipo)) {
            var u=pacientes.findById(id).orElseThrow(()->new IllegalArgumentException("Paciente inexistente."));
            return new Ficha(tipo,id,u.getNombres(),u.getApellidos(),u.getCorreo(),u.getDni(),"PACIENTE",u.isActivo(),u.getVersionAcceso());
        }
        validarTipo(tipo);
        var u=personal.findById(id).orElseThrow(()->new IllegalArgumentException("Usuario inexistente."));
        return new Ficha(tipo,id,u.getMedico()==null?u.getNombre():u.getMedico().getNombres(),u.getMedico()==null?"":u.getMedico().getApellidos(),u.getCorreo(),"",u.getRol().name(),u.isActivo(),u.getVersionAcceso());
    }
    private void validarTipo(String tipo) { if (!"personal".equals(tipo)) throw new IllegalArgumentException("Tipo de cuenta inválido."); }
    private void autorizar(CuentaPrincipal actor) {
        control.bloquear().orElseThrow();
        if (actor==null || !"personal".equals(actor.getTipoCuenta())) throw new IllegalArgumentException("Sesión inválida.");
        var u=personal.findById(actor.getCuentaId()).orElseThrow();
        if (!u.isActivo() || u.getRol()!=Rol.JEFE_MEDICO || u.getVersionAcceso()!=actor.getVersionAcceso()) throw new IllegalArgumentException("Inicia sesión nuevamente.");
    }
    private void version(long actual,long recibida) { if(actual!=recibida) throw new IllegalArgumentException("La cuenta cambió. Vuelve a abrirla antes de guardar."); }
    @Transactional
    public void editar(CuentaPrincipal actor,String tipo,Long id,long revision,String nombres,String apellidos,String correo,String dni,String password) {
        autorizar(actor);
        String email;
        String nombre=cuentas.nombre(nombres,"los nombres");
        boolean cambiaClave=password!=null&&!password.isEmpty();
        if("paciente".equals(tipo)) {
            var u=pacientes.bloquear(id).orElseThrow(); version(u.getVersionAcceso(),revision);
            email=cuentas.correoParaEdicion(correo,tipo,id);
            if(dni==null||!dni.matches("[0-9]{8}")||pacientes.existsByDniAndIdNot(dni,id)) throw new IllegalArgumentException("El DNI debe tener 8 dígitos y ser único.");
            if(cambiaClave) { cuentas.validarPassword(password,8); u.setPassword(encoder.encode(password)); }
            tokens.invalidarPorCorreo(u.getCorreo());
            u.setNombres(nombre);u.setApellidos(cuentas.nombre(apellidos,"los apellidos"));u.setCorreo(email);u.setDni(dni);u.setVersionAcceso(revision+1);
        } else {
            validarTipo(tipo); var u=personal.bloquear(id).orElseThrow(); version(u.getVersionAcceso(),revision);
            email=cuentas.correoParaEdicion(correo,tipo,id);
            if(cambiaClave) { cuentas.validarPassword(password,12);u.setPassword(encoder.encode(password)); }
            if(u.getMedico()!=null) {
                var m=medicos.bloquear(u.getMedico().getId()).orElseThrow();m.setNombres(nombre);m.setApellidos(cuentas.nombre(apellidos,"los apellidos"));
                u.setNombre(m.getNombres()+" "+m.getApellidos());
            } else u.setNombre(nombre);
            u.setCorreo(email);u.setVersionAcceso(revision+1);
        }
    }
    @Transactional
    public void estado(CuentaPrincipal actor,String tipo,Long id,long revision,boolean activo) {
        autorizar(actor); var ahora=LocalDateTime.now(ZoneId.of("America/Lima"));
        if("paciente".equals(tipo)) {
            var u=pacientes.bloquear(id).orElseThrow();version(u.getVersionAcceso(),revision);
            if(!activo&&citas.contarActivasFuturasPaciente(id,ahora.toLocalDate(),ahora.toLocalTime())>0) throw new IllegalArgumentException("Resuelve las citas futuras del paciente antes de darlo de baja.");
            u.setActivo(activo);u.setVersionAcceso(revision+1);tokens.invalidarPorCorreo(u.getCorreo());
        } else {
            validarTipo(tipo);var u=personal.bloquear(id).orElseThrow();version(u.getVersionAcceso(),revision);
            if(!activo&&actor.getCuentaId().equals(id)) throw new IllegalArgumentException("No puedes dar de baja tu propia cuenta.");
            if(!activo&&u.getRol()==Rol.JEFE_MEDICO&&u.isActivo()&&personal.countByRolAndActivoTrue(Rol.JEFE_MEDICO)<=1) throw new IllegalArgumentException("Debe permanecer un jefe médico activo.");
            if(u.getMedico()!=null) {
                medicos.bloquear(u.getMedico().getId()).orElseThrow();
                if(!activo&&citas.contarActivasFuturasMedico(u.getMedico().getId(),ahora.toLocalDate(),ahora.toLocalTime())>0) throw new IllegalArgumentException("Resuelve las citas futuras del médico antes de darlo de baja.");
            }
            u.setActivo(activo);u.setVersionAcceso(revision+1);
        }
    }
}
