package com.clinica.clinicaapp;
import com.clinica.clinicaapp.entity.*;
import com.clinica.clinicaapp.repository.*;
import com.clinica.clinicaapp.service.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

@SpringBootTest(properties="clinica.prueba.usuarios=true")
@org.springframework.test.annotation.DirtiesContext
@ActiveProfiles("test") @AutoConfigureMockMvc
class AdministracionUsuariosWebTest {
 @Autowired MockMvc mvc; @Autowired UsuarioPersonalRepository personal; @Autowired PacienteRepository pacientes;
 @Autowired MedicoRepository medicos; @Autowired EspecialidadRepository especialidades; @Autowired TurnoRepository turnos;
 @Autowired CitaRepository citas; @Autowired TokenRecuperacionRepository tokens; @Autowired PasswordEncoder encoder;
 @Autowired CitaService reservas; @Autowired AdministracionUsuariosService admin;
 UsuarioPersonal jefe; Paciente paciente; String suf;
 static final String CLAVE="ClaveDePrueba123!";
 @BeforeEach void preparar() {
  suf=UUID.randomUUID().toString(); jefe=new UsuarioPersonal();jefe.setNombre("Jefa Admin");jefe.setRol(Rol.JEFE_MEDICO);jefe.setCorreo("jefa-"+suf+"@example.test");jefe.setPassword(encoder.encode(CLAVE));jefe=personal.saveAndFlush(jefe);
  paciente=new Paciente();paciente.setNombres("Ana");paciente.setApellidos("Prueba");paciente.setCorreo("pac-"+suf+"@example.test");paciente.setDni(String.format("%08d",Math.abs(suf.hashCode()%100000000)));paciente.setPassword(encoder.encode(CLAVE));paciente=pacientes.saveAndFlush(paciente);
 }
 MockHttpSession login(String email,String destino)throws Exception {return (MockHttpSession)mvc.perform(formLogin("/login").user("correo",email).password(CLAVE)).andExpect(redirectedUrl(destino)).andReturn().getRequest().getSession(false);}
 String ruta(String tipo,Long id,String accion){return "/jefe/usuarios/"+tipo+"/"+id+"/"+accion;}
 void estado(MockHttpSession s,String tipo,Long id,long version,boolean activo)throws Exception {mvc.perform(post(ruta(tipo,id,"estado")).session(s).with(csrf()).param("version",""+version).param("activo",""+activo)).andExpect(redirectedUrl("/jefe/usuarios"));}
 @Test void listaYFormulariosUsanDatosPersistidosYCsrf()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");mvc.perform(get("/jefe/usuarios").session(s).param("q",suf)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(paciente.getCorreo())));
  mvc.perform(get(ruta("paciente",paciente.getId(),"editar")).session(s)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
  mvc.perform(get(ruta("personal",jefe.getId(),"estado")).session(s)).andExpect(status().isOk());
  mvc.perform(post(ruta("paciente",paciente.getId(),"estado")).session(s).param("version","0").param("activo","false")).andExpect(status().isForbidden());
 }
 @Test void bajaRevocaSesionConservaDatosYPermiteReactivar()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");var abierta=login(paciente.getCorreo(),"/citas/agendar");
  estado(s,"paciente",paciente.getId(),0,false);assertFalse(pacientes.findById(paciente.getId()).orElseThrow().isActivo());
  mvc.perform(get("/citas/mis-citas").session(abierta)).andExpect(redirectedUrl("/login?cuentaActualizada"));
  mvc.perform(formLogin("/login").user("correo",paciente.getCorreo()).password(CLAVE)).andExpect(redirectedUrl("/login?error=credenciales"));
  estado(s,"paciente",paciente.getId(),1,true);login(paciente.getCorreo(),"/citas/agendar");
 }
 @Test void editarCorreoPasswordRevocaTokensYSesionYRechazaVersionVieja()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");var abierta=login(paciente.getCorreo(),"/citas/agendar");
  var token=tokens.saveAndFlush(new TokenRecuperacion(suf,paciente.getCorreo(),15));String nuevo="edit-"+suf+"@example.test";
  mvc.perform(post(ruta("paciente",paciente.getId(),"editar")).session(s).with(csrf()).param("version","0").param("nombres","Ana Editada").param("apellidos","Prueba").param("correo",nuevo).param("dni",paciente.getDni()).param("password","ClaveNueva123!"))
   .andExpect(redirectedUrl("/jefe/usuarios"));
  var editado=pacientes.findById(paciente.getId()).orElseThrow();assertEquals(nuevo,editado.getCorreo());assertTrue(encoder.matches("ClaveNueva123!",editado.getPassword()));assertTrue(tokens.findById(token.getId()).orElseThrow().isUsado());
  mvc.perform(get("/citas/mis-citas").session(abierta)).andExpect(redirectedUrl("/login?cuentaActualizada"));
  estado(s,"paciente",paciente.getId(),0,false);assertTrue(pacientes.findById(paciente.getId()).orElseThrow().isActivo());
 }
 @Test void correoDuplicadoNoCambiaDatosYClaveVaciaSeConserva()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");String hash=paciente.getPassword();
  for(String correo:new String[]{jefe.getCorreo(),paciente.getCorreo()}) {
   mvc.perform(post(ruta("paciente",paciente.getId(),"editar")).session(s).with(csrf()).param("version","0").param("nombres","Nombre nuevo").param("apellidos","Prueba").param("correo",correo).param("dni",paciente.getDni()).param("password",""))
    .andExpect(status().is3xxRedirection());
  }
  var actual=pacientes.findById(paciente.getId()).orElseThrow();assertEquals(paciente.getCorreo(),actual.getCorreo());assertEquals(hash,actual.getPassword());assertEquals("Nombre nuevo",actual.getNombres());
 }
 @Test void noPermiteBajaPropiaYRestringeOtrosRoles()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");estado(s,"personal",jefe.getId(),0,false);assertTrue(personal.findById(jefe.getId()).orElseThrow().isActivo());
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(get("/jefe/usuarios").session(p)).andExpect(status().isForbidden());
 }
 @Test void bajaMedicoOcultaHorariosBloqueaReservasYConservaTurnos()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");var m=new Medico();m.setNombres("Doctor");m.setApellidos(suf);m.setEspecialidad(especialidades.findAll().getFirst());m=medicos.saveAndFlush(m);
  var u=new UsuarioPersonal();u.setMedico(m);u.setNombre("Doctor "+suf);u.setCorreo("med-"+suf+"@example.test");u.setPassword(encoder.encode(CLAVE));u.setRol(Rol.MEDICO);u=personal.saveAndFlush(u);
  var turno=new Turno();turno.setMedico(m);turno.setFecha(LocalDate.now(ZoneId.of("America/Lima")).plusDays(10));turno.setHoraInicio(LocalTime.of(9,0));turno.setHoraFin(LocalTime.of(9,30));turno.setDuracionMinutos(30);turno.setEstado("DISPONIBLE");turno=turnos.saveAndFlush(turno);
  var medSesion=login(u.getCorreo(),"/medico/agenda");estado(s,"personal",u.getId(),0,false);
  mvc.perform(get("/medico/agenda").session(medSesion)).andExpect(redirectedUrl("/login?cuentaActualizada"));
  Long tid=turno.getId();assertTrue(turnos.buscarTurnosDisponibles(null,turno.getFecha(),turno.getFecha()).stream().noneMatch(t->t.getId().equals(tid)));assertThrows(IllegalStateException.class,()->reservas.iniciarProcesoEmision(tid,paciente));assertTrue(turnos.existsById(tid));
  estado(s,"personal",u.getId(),1,true);assertTrue(turnos.buscarTurnosDisponibles(null,turno.getFecha(),turno.getFecha()).stream().anyMatch(t->t.getId().equals(tid)));var cita=reservas.iniciarProcesoEmision(tid,paciente);
  estado(s,"personal",u.getId(),2,false);assertTrue(personal.findById(u.getId()).orElseThrow().isActivo());
  estado(s,"paciente",paciente.getId(),0,false);assertTrue(pacientes.findById(paciente.getId()).orElseThrow().isActivo());assertTrue(citas.existsById(cita.getId()));
 }
 @Test void editarMiCuentaRevocaSesionYConservaRol()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");
  mvc.perform(post(ruta("personal",jefe.getId(),"editar")).session(s).with(csrf()).param("version","0").param("nombres","Jefa Actualizada").param("correo",jefe.getCorreo()).param("password",""))
   .andExpect(redirectedUrl("/jefe/usuarios"));
  var u=personal.findById(jefe.getId()).orElseThrow();assertEquals("Jefa Actualizada",u.getNombre());assertEquals(Rol.JEFE_MEDICO,u.getRol());
  mvc.perform(get("/jefe/usuarios").session(s)).andExpect(redirectedUrl("/login?cuentaActualizada"));login(jefe.getCorreo(),"/jefe/horarios");
 }
 @Test void editarMedicoActualizaPerfilSinCambiarEspecialidad()throws Exception {
  var s=login(jefe.getCorreo(),"/jefe/horarios");var m=new Medico();m.setNombres("Original");m.setApellidos("Doctor");var e=especialidades.findAll().getFirst();m.setEspecialidad(e);m=medicos.saveAndFlush(m);
  var u=new UsuarioPersonal();u.setMedico(m);u.setNombre("Original Doctor");u.setCorreo("editmed-"+suf+"@example.test");u.setPassword(encoder.encode(CLAVE));u.setRol(Rol.MEDICO);u=personal.saveAndFlush(u);
  mvc.perform(get(ruta("personal",u.getId(),"editar")).session(s)).andExpect(status().isOk());
  mvc.perform(post(ruta("personal",u.getId(),"editar")).session(s).with(csrf()).param("version","0").param("nombres","Elena").param("apellidos","Actualizada").param("correo",u.getCorreo()).param("password",""))
   .andExpect(redirectedUrl("/jefe/usuarios"));
  var actualizado=medicos.findById(m.getId()).orElseThrow();assertEquals("Elena",actualizado.getNombres());assertEquals("Actualizada",actualizado.getApellidos());assertEquals(e.getId(),actualizado.getEspecialidad().getId());
 }

}
