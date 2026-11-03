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

@SpringBootTest(properties="clinica.prueba.gestion=true")
@org.springframework.test.annotation.DirtiesContext
@ActiveProfiles("test") @AutoConfigureMockMvc
class GestionDisponibilidadWebTest {
 @Autowired MockMvc mvc; @Autowired PacienteRepository pacientes; @Autowired UsuarioPersonalRepository usuarios;
 @Autowired MedicoRepository medicos; @Autowired EspecialidadRepository especialidades; @Autowired TurnoRepository turnos;
 @Autowired CitaRepository citas; @Autowired CitaService reservas; @Autowired DisponibilidadService disponibilidad;
 @Autowired PasswordEncoder encoder;
 @Autowired NotificacionMedicoRepository avisos;
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

 @Test void bajaPersisteOcultaTurnoYRechazaReserva()throws Exception {
  var j=login(jefe.getCorreo(),"/jefe/horarios");
  mvc.perform(get("/jefe/turnos/"+turno.getId()+"/baja").session(j)).andExpect(status().isOk()).andExpect(content().string(containsString("Confirmar baja")));
  mvc.perform(post("/jefe/turnos/"+turno.getId()+"/baja").session(j).with(csrf()).param("version",""+turno.getVersion()).param("motivo","Mantenimiento"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/jefe/horarios?medicoId="+doctor.getMedico().getId()+"&fechaConsulta="+turno.getFecha()));
  var actual=turnos.findById(turno.getId()).orElseThrow();assertFalse(actual.isActivo());assertEquals("Mantenimiento",actual.getMotivoBaja());
  assertTrue(turnos.buscarTurnosDisponibles(null,turno.getFecha(),turno.getFecha()).stream().noneMatch(t->t.getId().equals(turno.getId())));
  assertTrue(turnos.findTurnosByMedicoId(doctor.getMedico().getId(),turno.getFecha()).isEmpty());
  assertThrows(IllegalStateException.class,()->reservas.iniciarProcesoEmision(turno.getId(),paciente));
  assertTrue(avisos.findTop50ByMedicoIdOrderByIdDesc(doctor.getMedico().getId()).stream().anyMatch(a->a.getMensaje().contains("dada de baja")));
 }
 @Test void editarPublicaReemplazoYConservaCitaHistorica()throws Exception {
  var historica=reservas.iniciarProcesoEmision(turno.getId(),paciente,"Asunto histórico");historica.setEstado("CANCELADA");citas.saveAndFlush(historica);
  var libre=turnos.findById(turno.getId()).orElseThrow();libre.setEstado("DISPONIBLE");libre=turnos.saveAndFlush(libre);
  var j=login(jefe.getCorreo(),"/jefe/horarios");mvc.perform(get("/jefe/turnos/"+turno.getId()+"/editar").session(j)).andExpect(status().isOk());
  var fecha=turno.getFecha().plusDays(1);
  mvc.perform(post("/jefe/turnos/"+turno.getId()+"/editar").session(j).with(csrf()).param("version",""+libre.getVersion()).param("fecha",fecha.toString()).param("inicio","11:00").param("fin","11:45").param("consultorio","505"))
    .andExpect(redirectedUrl("/jefe/horarios?medicoId="+doctor.getMedico().getId()+"&fechaConsulta="+fecha));
  var anterior=turnos.findById(turno.getId()).orElseThrow();assertFalse(anterior.isActivo());assertEquals(turno.getFecha(),anterior.getFecha());assertEquals(LocalTime.of(9,0),anterior.getHoraInicio());
  var nuevos=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(doctor.getMedico().getId(),fecha);assertEquals(1,nuevos.size());assertEquals(45,nuevos.getFirst().getDuracionMinutos());assertEquals("505",nuevos.getFirst().getConsultorio());assertTrue(nuevos.getFirst().isActivo());
  assertEquals(turno.getId(),citas.findById(historica.getId()).orElseThrow().getTurno().getId());assertEquals("Asunto histórico",citas.findById(historica.getId()).orElseThrow().getAsunto());
 }
 @Test void citaEnProcesoYConfirmadaImpidenBajaYEdicion() {
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente);
  for(int intento=0;intento<2;intento++) {
   var actual=turnos.findById(turno.getId()).orElseThrow();
   assertThrows(IllegalArgumentException.class,()->disponibilidad.darDeBaja(actual.getId(),actual.getVersion(),"Cambio de jornada"));
   assertThrows(IllegalArgumentException.class,()->disponibilidad.editar(actual.getId(),actual.getVersion(),actual.getFecha(),LocalTime.of(10,0),LocalTime.of(10,30),"203"));
   assertTrue(turnos.findById(turno.getId()).orElseThrow().isActivo());
   if(intento==0)reservas.confirmarPagoYEmision(cita.getId(),"SIMULADO","SIMULADO");
  }
 }
 @Test void rechazaCrucesYVersionAntiguaSinAlterarTurnos() {
  disponibilidad.asignar(doctor.getMedico().getId(),turno.getFecha(),LocalTime.of(10,0),LocalTime.of(10,30),30,"203");
  assertThrows(IllegalArgumentException.class,()->disponibilidad.editar(turno.getId(),turno.getVersion(),turno.getFecha(),LocalTime.of(10,15),LocalTime.of(10,45),"203"));
  assertTrue(turnos.findById(turno.getId()).orElseThrow().isActivo());
  disponibilidad.actualizarConsultorio(turno.getId(),"303");
  assertThrows(IllegalArgumentException.class,()->disponibilidad.darDeBaja(turno.getId(),turno.getVersion(),"Cambio"));
  assertTrue(turnos.findById(turno.getId()).orElseThrow().isActivo());
 }
 @Test void bajaPermitePublicarOtraDisponibilidadEnEseHorario() {
  disponibilidad.darDeBaja(turno.getId(),turno.getVersion(),"Reorganización");
  disponibilidad.asignar(doctor.getMedico().getId(),turno.getFecha(),turno.getHoraInicio(),turno.getHoraFin(),30,"404");
  var visibles=turnos.buscarTurnosDisponibles(null,turno.getFecha(),turno.getFecha()).stream().filter(t->t.getMedico().getId().equals(doctor.getMedico().getId())).toList();assertEquals(1,visibles.size());assertNotEquals(turno.getId(),visibles.getFirst().getId());
 }
 @Test void noReprogramaHaciaTurnoDeBaja() {
  disponibilidad.asignar(doctor.getMedico().getId(),turno.getFecha(),LocalTime.of(11,0),LocalTime.of(11,30),30,"404");
  var destino=turnos.findByMedicoIdAndFechaOrderByHoraInicioAsc(doctor.getMedico().getId(),turno.getFecha()).getLast();
  disponibilidad.darDeBaja(destino.getId(),destino.getVersion(),"No disponible");
  var cita=reservas.iniciarProcesoEmision(turno.getId(),paciente);reservas.confirmarPagoYEmision(cita.getId(),"SIMULADO","SIMULADO");
  assertThrows(IllegalStateException.class,()->reservas.reprogramarCita(cita.getId(),destino.getId(),paciente));
  assertEquals(turno.getId(),citas.findById(cita.getId()).orElseThrow().getTurno().getId());
 }
 @Test void soloJefePuedeAdministrarConCsrf()throws Exception {
  var j=login(jefe.getCorreo(),"/jefe/horarios");mvc.perform(post("/jefe/turnos/"+turno.getId()+"/baja").session(j).param("version","0").param("motivo","Cambio")).andExpect(status().isForbidden());
  var p=login(paciente.getCorreo(),"/citas/agendar");mvc.perform(get("/jefe/turnos/"+turno.getId()+"/editar").session(p)).andExpect(status().isForbidden());
  var m=login(doctor.getCorreo(),"/medico/agenda");mvc.perform(post("/jefe/turnos/"+turno.getId()+"/baja").session(m).with(csrf()).param("version","0").param("motivo","Cambio")).andExpect(status().isForbidden());
 }
 @Test void pasadoIntervaloInvalidoYMotivoVacioNoCambianDisponibilidad() {
  assertThrows(IllegalArgumentException.class,()->disponibilidad.editar(turno.getId(),turno.getVersion(),LocalDate.now().minusDays(1),LocalTime.of(9,0),LocalTime.of(9,30),"203"));
  assertThrows(IllegalArgumentException.class,()->disponibilidad.editar(turno.getId(),turno.getVersion(),turno.getFecha(),LocalTime.of(9,0),LocalTime.of(9,1),"203"));
  assertThrows(IllegalArgumentException.class,()->disponibilidad.darDeBaja(turno.getId(),turno.getVersion()," "));
  assertTrue(turnos.findById(turno.getId()).orElseThrow().isActivo());
 }
 @Test void reservaYBajaConcurrentesNoDejanCitaEnTurnoInactivo()throws Exception {
  var inicio=new java.util.concurrent.CountDownLatch(1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
   var reserva=pool.submit(()->{inicio.await();try{reservas.iniciarProcesoEmision(turno.getId(),paciente);return true;}catch(IllegalStateException e){return false;}});
   var baja=pool.submit(()->{inicio.await();try{disponibilidad.darDeBaja(turno.getId(),turno.getVersion(),"Cierre de jornada");return true;}catch(IllegalArgumentException e){return false;}});
   inicio.countDown();boolean reservada=reserva.get(15,java.util.concurrent.TimeUnit.SECONDS);boolean retirada=baja.get(15,java.util.concurrent.TimeUnit.SECONDS);
   assertNotEquals(reservada,retirada);
   var actual=turnos.findById(turno.getId()).orElseThrow();assertEquals(reservada,actual.isActivo());
   assertEquals(reservada?1:0,citas.contarActivasEnTurno(turno.getId()));
  }
 }

}
