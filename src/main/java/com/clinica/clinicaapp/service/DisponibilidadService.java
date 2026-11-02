package com.clinica.clinicaapp.service;

import com.clinica.clinicaapp.entity.Medico;
import com.clinica.clinicaapp.entity.Turno;
import com.clinica.clinicaapp.repository.MedicoRepository;
import com.clinica.clinicaapp.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DisponibilidadService {

    private final TurnoRepository turnoRepository;
    private final MedicoRepository medicoRepository;

    public DisponibilidadService(TurnoRepository turnoRepository, MedicoRepository medicoRepository) {
        this.turnoRepository = turnoRepository;
        this.medicoRepository = medicoRepository;
    }

    @Transactional
    public int generarTurnosSemanales(Long medicoId,
            LocalDate fechaSemanaLunes,
            List<DayOfWeek> diasSeleccionados,
            LocalTime horaInicio,
            LocalTime horaFin,
            String consultorio) {

        // 1. Regla de anticipación: Solo programar semanas futuras
        LocalDate inicioSemanaActual = LocalDate.now().with(DayOfWeek.MONDAY);
        if (!fechaSemanaLunes.isAfter(inicioSemanaActual)) {
            throw new IllegalStateException(
                    "La programación médica debe realizarse con mínimo una semana de anticipación.");
        }

        // 2. Validación de coherencia de horario
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new IllegalArgumentException("El médico seleccionado no existe."));

        // 3. Duración dinámica por especialidad (ej. Pediatría = 30m, Medicina General
        // = 15m)
        int duracionMinutos = medico.getEspecialidad().getDuracionMinutosDefault();
        if (duracionMinutos <= 0) {
            duracionMinutos = 30; // Respaldo por defecto
        }

        List<Turno> turnosNuevos = new ArrayList<>();

        // 4. Iterar sobre los 7 días de la semana a programar (Lunes a Domingo)
        for (int i = 0; i < 7; i++) {
            LocalDate fechaDia = fechaSemanaLunes.plusDays(i);
            DayOfWeek diaSemana = fechaDia.getDayOfWeek();

            if (diasSeleccionados.contains(diaSemana)) {
                LocalTime cursor = horaInicio;

                // Generar los bloques mientras el bloque quepa antes de horaFin
                while (cursor.plusMinutes(duracionMinutos).isBefore(horaFin)
                        || cursor.plusMinutes(duracionMinutos).equals(horaFin)) {

                    LocalTime proximoCursor = cursor.plusMinutes(duracionMinutos);

                    // Evitar duplicar turnos si ya fueron creados previamente
                    boolean yaExiste = turnoRepository.existsByMedicoIdAndFechaAndHoraInicio(medicoId, fechaDia,
                            cursor);
                    if (!yaExiste) {
                        Turno turno = new Turno();
                        turno.setMedico(medico);
                        turno.setFecha(fechaDia);
                        turno.setHoraInicio(cursor);
                        turno.setHoraFin(proximoCursor);
                        turno.setDuracionMinutos(duracionMinutos);
                        turno.setEstado("DISPONIBLE");
                        turno.setEsSobrecupo(false);
                        turno.setConsultorio(consultorio);

                        turnosNuevos.add(turno);
                    }

                    cursor = proximoCursor;
                }
            }
        }

        if (turnosNuevos.isEmpty()) {
            throw new IllegalStateException(
                    "No se generó ningún turno. Verifique si los horarios ya estaban previamente registrados.");
        }

        // 5. Inserción masiva en BD
        turnoRepository.saveAll(turnosNuevos);
        return turnosNuevos.size();
    }
}