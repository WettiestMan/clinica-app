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

@SpringBootTest(properties="clinica.prueba.detalle=true")
@org.springframework.test.annotation.DirtiesContext
@ActiveProfiles("test") @AutoConfigureMockMvc
class DetalleAgendaWebTest {
 @Autowired MockMvc mvc; @Autowired PacienteRepository pacientes; @Autowired UsuarioPersonalRepository usuarios;
 @Autowired MedicoRepository medicos; @Autowired EspecialidadRepository especialidades; @Autowired TurnoRepository turnos;
 @Autowired CitaRepository citas; @Autowired CitaService reservas; @Autowired DisponibilidadService disponibilidad;
 @Autowired PasswordEncoder encoder;
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
 @Test void reservaGuardaAsuntoYAmbosDetallesMuestranConsultorio()throws Exception{
  var p=login(paciente.getCorreo(),"/citas/agendar");
  mvc.perform(post("/citas/confirmar").session(p).with(csrf()).param("turnoId",turno.getId().toString())).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"asunto\""))).andExpect(content().string(containsString("Consultorio 203")));
  mvc.perform(post("/citas/iniciar-pago").session(p).with(csrf()).param("turnoId",turno.getId().toString()).param("asunto","  Control de presión arterial  ")).andExpect(status().is3xxRedirection());
  var cita=citas.findByPacienteIdOrderByFechaRegistroDesc(paciente.getId()).getFirst();assertEquals("Control de presión arterial",cita.getAsunto());
  mvc.perform(get("/citas/detalle/"+cita.getId()).session(p)).andExpect(status().isOk()).andExpect(content().string(containsString("Control de presión arterial"))).andExpect(content().string(containsString("Consultorio 203")));
  reservas.confirmarPagoYEmision(cita.getId(),"SIMULADO","SIMULADO");
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(get("/medico/citas/"+cita.getId()).session(m)).andExpect(status().isOk()).andExpect(content().string(containsString("Control de presión arterial"))).andExpect(content().string(containsString("Consultorio 203")));
  mvc.perform(get("/medico/agenda").session(m)).andExpect(content().string(containsString("data-detalle-base=\"/medico/citas/\"")));
  mvc.perform(get("/medico/agenda/datos").session(m).param("fecha",turno.getFecha().toString())).andExpect(jsonPath("$.citas[0].id").value(cita.getId()));
 }
 @Test void medicoAjenoYPacienteNoAccedenADetallesDeOtroRol()throws Exception{
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente,"Control");
  var ajeno=login(otro.getCorreo(),"/medico/agenda");mvc.perform(get("/medico/citas/"+cita.getId()).session(ajeno)).andExpect(status().isNotFound());
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(get("/medico/citas/"+cita.getId()).session(p)).andExpect(status().isForbidden());
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(get("/medico/citas/999999999").session(m)).andExpect(status().isNotFound());
 }
 @Test void asuntoLargoNoBloqueaTurnoYAsuntoSeMuestraComoTexto()throws Exception{
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(post("/citas/iniciar-pago").session(p).with(csrf()).param("turnoId",turno.getId().toString()).param("asunto","x".repeat(501))).andExpect(status().isOk()).andExpect(model().attributeExists("errorAsunto"));
  assertEquals("DISPONIBLE",turnos.findById(turno.getId()).orElseThrow().getEstado());
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente,"<script>alert(1)</script>");
  mvc.perform(get("/citas/detalle/"+cita.getId()).session(p)).andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
 }
 @Test void jefePuedeCompletarConsultorioExistenteYSePersiste()throws Exception{
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente,"Control");var j=login(jefe.getCorreo(),"/jefe/horarios");
  mvc.perform(post("/jefe/turnos/"+turno.getId()+"/consultorio").session(j).with(csrf()).param("consultorio","Consultorio 404")).andExpect(status().is3xxRedirection());
  assertEquals("Consultorio 404",turnos.findById(turno.getId()).orElseThrow().getConsultorio());
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(get("/medico/citas/"+cita.getId()).session(m)).andExpect(content().string(containsString("Consultorio 404")));
  assertThrows(IllegalArgumentException.class,()->disponibilidad.actualizarConsultorio(turno.getId()," "));
  mvc.perform(post("/jefe/turnos/"+turno.getId()+"/consultorio").session(j).param("consultorio","404")).andExpect(status().isForbidden());
 }
 @Test void datosAnterioresMuestranValoresHonestos()throws Exception{
  turno.setConsultorio(null);turnos.saveAndFlush(turno);var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente);
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(get("/medico/citas/"+cita.getId()).session(m)).andExpect(content().string(containsString("Por asignar"))).andExpect(content().string(containsString("No indicado")));
 }
 @Test void reprogramarConservaAsuntoYTomaConsultorioDelNuevoTurno()throws Exception{
  assertThrows(IllegalArgumentException.class,()->disponibilidad.asignar(doctor.getMedico().getId(),turno.getFecha(),LocalTime.of(11,0),LocalTime.of(11,30),30," "));
  disponibilidad.asignar(doctor.getMedico().getId(),turno.getFecha(),LocalTime.of(11,0),LocalTime.of(11,30),30,"Consultorio 505");
  var nuevo=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(doctor.getMedico().getId(),turno.getFecha()).getLast();
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente,"Control de seguimiento");reservas.confirmarPagoYEmision(cita.getId(),"SIMULADO","SIMULADO");
  reservas.reprogramarCita(cita.getId(),nuevo.getId(),paciente);assertEquals("Control de seguimiento",citas.findById(cita.getId()).orElseThrow().getAsunto());
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(get("/citas/detalle/"+cita.getId()).session(p)).andExpect(content().string(containsString("Consultorio 505"))).andExpect(content().string(containsString("Control de seguimiento")));
 }

}
