# SaludCitas

Proyecto Maven con Spring Boot, Java 21, PostgreSQL, Thymeleaf y Spring Security.

Consulta `../GUIA_DE_INICIO.md` para configurar la conexión y crear el primer Jefe Médico.

1. Copia `src/main/resources/secrets.properties.example` como `secrets.properties` en esa misma carpeta y completa tu conexión local.
2. Ejecuta `mvnw.cmd clean test` y `mvnw.cmd spring-boot:run` (Windows), o `bash mvnw ...` en Linux/macOS.
3. Abre `/configuracion-inicial` para crear el primer jefe. No hay usuario ni contraseña predeterminados.
4. Usa `/login` para los tres roles.

No subas `secrets.properties`, `.env` ni `target` a GitHub. El pago está configurado en modo simulado académico.


## Administración de Usuarios

El menú del Jefe Médico incluye Usuarios con edición, baja lógica y reactivación. Consulta `../GUIA_USUARIOS.md` y `../VALIDACION_USUARIOS.md`. Los cambios incrementales están en `../CORRECCION_02_USUARIOS.patch`; el proyecto de esta carpeta ya los contiene.


## Detalle de agenda, asunto y consultorio

Consulta `../GUIA_DETALLE_Y_ASUNTO.md` para instalar y usar esta actualización. Resultados en `../VALIDACION_DETALLE.md`; archivos modificados en `../ARCHIVOS_DETALLE.txt`. La ubicación se guarda por turno y el asunto por cita.


## Administración de disponibilidades publicadas

Desde Jefe Médico → Disponibilidad registrada se pueden editar turnos futuros libres o darles de baja con motivo. Consulta `../GUIA_ADMINISTRAR_DISPONIBILIDAD.md`, `../ARCHIVOS_DISPONIBILIDAD.txt` y `../VALIDACION_DISPONIBILIDAD.md`.


## Notificaciones flotantes

Consulta `../GUIA_NOTIFICACIONES_FLOTANTES.md`. Incluye ventanas de 60 segundos, cierre sincronizado entre pestañas, contador sin leer y avisos persistentes por cupos liberados. Resultados en `../VALIDACION_NOTIFICACIONES.md`.
