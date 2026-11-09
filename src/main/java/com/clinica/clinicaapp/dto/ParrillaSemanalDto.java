package com.clinica.clinicaapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ParrillaSemanalDto {

    public record CeldaTurno(
            Long id,
            String estado,      // "DISPONIBLE" o "OCUPADO"
            String consultorio,
            boolean existe
    ) {}

    public record FilaMatriz(
            LocalTime hora,
            String horaFormateada, // "08:00", "08:30"
            List<CeldaTurno> celdasPorDia // 5 elementos: Lun a Vie
    ) {}

    private final LocalDate lunesSemana;
    private final List<String> encabezadosDias; // ["Lun 09/11", "Mar 10/11", ...]
    private final List<FilaMatriz> filas;

    public ParrillaSemanalDto(LocalDate lunesSemana, List<String> encabezadosDias, List<FilaMatriz> filas) {
        this.lunesSemana = lunesSemana;
        this.encabezadosDias = encabezadosDias;
        this.filas = filas;
    }

    public LocalDate getLunesSemana() { return lunesSemana; }
    public List<String> getEncabezadosDias() { return encabezadosDias; }
    public List<FilaMatriz> getFilas() { return filas; }
}