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
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties="clinica.prueba.avisos=true")
@org.springframework.test.annotation.DirtiesContext
@ActiveProfiles("test") @AutoConfigureMockMvc
class NotificacionesFlotantesWebTest {
 @Autowired MockMvc mvc; @Autowired PacienteRepository pacientes; @Autowired UsuarioPersonalRepository usuarios;
 @Autowired MedicoRepository medicos; @Autowired EspecialidadRepository especialidades; @Autowired TurnoRepository turnos;
 @Autowired CitaRepository citas; @Autowired CitaService reservas; @Autowired DisponibilidadService disponibilidad;
 @Autowired PasswordEncoder encoder;
 @Autowired NotificacionMedicoRepository avisos;
 @Autowired CancelacionService cancelacion;
 @Autowired EventoCancelacionRepository eventos;
 @Autowired com.clinica.clinicaapp.config.InicializarAvisosCancelacion inicializador;
 Paciente paciente; UsuarioPersonal doctor,jefe,otro;Turno turno;String suf;
 static final String CLAVE="ClavePruebas123!";
 UsuarioPersonal crear(String nombre,Rol rol,Medico m){var u=new UsuarioPersonal();u.setNombre(nombre);u.setRol(rol);u.setMedico(m);u.setCorreo(nombre+suf+"@example.test");u.setPassword(encoder.encode(CLAVE));return usuarios.saveAndFlush(u);}
 @BeforeEach void preparar(){
  suf=UUID.randomUUID().toString();var e=especialidades.findAll().getFirst();
  var m=new Medico();m.setNombres("Elena");m.setApellidos("Prueba");m.setEspecialidad(e);m=medicos.saveAndFlush(m);doctor=crear("doctor",Rol.MEDICO,m);jefe=crear("jefe",Rol.JEFE_MEDICO,null);
  var ajeno=new Medico();ajeno.setNombres("Otro");ajeno.setApellidos("Medico");ajeno.setEspecialidad(e);ajeno=medicos.saveAndFlush(ajeno);otro=crear("otro",Rol.MEDICO,ajeno);
  paciente=new Paciente();paciente.setNombres("Ana");paciente.setApellidos("Paciente");paciente.setDni(String.format("%08d",Math.abs(suf.hashCode()%100000000)));paciente.setCorreo("pac"+suf+"@example.test");paciente.setPassword(encoder.encode(CLAVE));paciente=pacientes.saveAndFlush(paciente);
  var fecha=LocalDate.now(ZoneId.of("America/Lima")).plusDays(5);disponibilidad.asignar(m.getId(),fecha,LocalTime.of(9,0),LocalTime.of(9,30),30,"Consultorio 203");
  turno=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(m.getId(),fecha).getFirst();
 }
 MockHttpSession login(String correo,String destino)throws Exception{return (MockHttpSession)mvc.perform(formLogin("/login").user("correo",correo).password(CLAVE)).andExpect(redirectedUrl(destino)).andReturn().getRequest().getSession(false);}

 @Test void feedYContadorSeCompartenEnTodasLasPantallas()throws Exception {
  var m=login(doctor.getCorreo(),"/medico/agenda");
  mvc.perform(get("/medico/notificaciones/datos").session(m)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.pendientes").value(1)).andExpect(jsonPath("$.medicoId").value(doctor.getMedico().getId())).andExpect(jsonPath("$.avisos[0].mensaje").value(containsString("Disponibilidad asignada")));
  for(String url:new String[]{"/medico/agenda","/medico/turnos","/medico/notificaciones"})
   mvc.perform(get(url).session(m)).andExpect(status().isOk()).andExpect(content().string(containsString("id=\"sc-toast-region\""))).andExpect(content().string(containsString("id=\"sc-notif-count\""))).andExpect(content().string(containsString("/js/notificaciones-medico.js")));
 }
 @Test void cancelacionGeneraUnSoloAvisoYActualizaContador()throws Exception {
  var c=reservas.iniciarProcesoEmision(turno.getId(),paciente);reservas.confirmarPagoYEmision(c.getId(),"SIMULADO","SIMULADO");
  cancelacion.confirmar(c.getId(),paciente.getId(),"Ya no lo necesito",null,true);
  cancelacion.confirmar(c.getId(),paciente.getId(),"Ya no lo necesito",null,true);
  assertEquals(2,avisos.countByMedicoIdAndLeidaFalse(doctor.getMedico().getId()));
  var m=login(doctor.getCorreo(),"/medico/agenda");
  mvc.perform(get("/medico/notificaciones/datos").session(m)).andExpect(jsonPath("$.pendientes").value(2)).andExpect(jsonPath("$.avisos[0].mensaje").value(containsString("Cupo liberado por cancelación")));
  mvc.perform(get("/medico/notificaciones").session(m)).andExpect(content().string(containsString("Cupo liberado por cancelación")));
 }
 @Test void leerReduceContadorYPreservaHistorial()throws Exception {
  var m=login(doctor.getCorreo(),"/medico/agenda");var aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(doctor.getMedico().getId()).getFirst();
  mvc.perform(post("/medico/notificaciones/"+aviso.getId()+"/leer").session(m).with(csrf())).andExpect(redirectedUrl("/medico/notificaciones"));
  mvc.perform(get("/medico/notificaciones/datos").session(m)).andExpect(jsonPath("$.pendientes").value(0)).andExpect(jsonPath("$.avisos.length()").value(0));assertTrue(avisos.existsById(aviso.getId()));
 }
 @Test void feedNoPermiteVerAvisosAjenosYLeerExigePropiedadYCsrf()throws Exception {
  var ajeno=login(otro.getCorreo(),"/medico/agenda");var aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(doctor.getMedico().getId()).getFirst();
  mvc.perform(get("/medico/notificaciones/datos").session(ajeno).param("medicoId",doctor.getMedico().getId().toString())).andExpect(jsonPath("$.pendientes").value(0));
  mvc.perform(post("/medico/notificaciones/"+aviso.getId()+"/leer").session(ajeno).with(csrf())).andExpect(status().isNotFound());
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(post("/medico/notificaciones/"+aviso.getId()+"/leer").session(m)).andExpect(status().isForbidden());
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(get("/medico/notificaciones/datos").session(p)).andExpect(status().isForbidden());
 }
 @Test void avisosDeCancelacionesAnterioresSeRecuperanSinDuplicarse()throws Exception {
  var c=reservas.iniciarProcesoEmision(turno.getId(),paciente);reservas.confirmarPagoYEmision(c.getId(),"SIMULADO","SIMULADO");cancelacion.confirmar(c.getId(),paciente.getId(),"Ya no lo necesito",null,true);
  var aviso=avisos.findTop50ByMedicoIdOrderByIdDesc(doctor.getMedico().getId()).stream().filter(a->a.getEventoCancelacionId()!=null).findFirst().orElseThrow();avisos.deleteById(aviso.getId());
  inicializador.run(null);inicializador.run(null);assertEquals(2,avisos.countByMedicoIdAndLeidaFalse(doctor.getMedico().getId()));
 }
 @Test void leerTodasAfectaSoloAlMedicoAutenticado()throws Exception {
  var aviso=new NotificacionMedico();aviso.setMedico(otro.getMedico());aviso.setCreada(LocalDateTime.now());aviso.setMensaje("Aviso de otro médico");aviso=avisos.saveAndFlush(aviso);
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(post("/medico/notificaciones/leer-todas").session(m).with(csrf())).andExpect(redirectedUrl("/medico/notificaciones"));
  assertEquals(0,avisos.countByMedicoIdAndLeidaFalse(doctor.getMedico().getId()));assertFalse(avisos.findById(aviso.getId()).orElseThrow().isLeida());
 }

}
