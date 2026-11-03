-- =====================================================
-- 0. CORRECCIÓN DE RESTRICCIÓN DE UNICIDAD (SI APLICA)
-- =====================================================
ALTER TABLE citas DROP CONSTRAINT IF EXISTS uk_bbwh4ytnwechvo8kjw8p6pwdb;

-- =====================================================
-- 1. ESPECIALIDADES (Duración y Precios Dinámicos)
-- =====================================================
INSERT INTO especialidades (id, nombre, duracion_minutos_default, precio_consulta) VALUES 
(1, 'Cardiologia', 30, 80.00),
(2, 'Pediatria', 30, 60.00),
(3, 'Medicina General', 15, 40.00),
(4, 'Dermatologia', 30, 75.00),
(5, 'Cirugia General', 120, 100.00),
(6, 'Oftalmologia', 30, 70.00),
(7, 'Traumatologia', 30, 85.00),
(8, 'Ginecologia', 30, 75.00),
(9, 'Odontologia', 45, 50.00)
ON CONFLICT (id) DO UPDATE SET 
    nombre = EXCLUDED.nombre, 
    duracion_minutos_default = EXCLUDED.duracion_minutos_default,
    precio_consulta = EXCLUDED.precio_consulta;

-- =====================================================
-- 2. MÉDICOS (Con credenciales para login y rol de Jefatura)
-- Contraseña encriptada BCrypt para 'medico123': $2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.
-- =====================================================
INSERT INTO medicos (id, nombres, apellidos, especialidad_id, correo, password, es_jefe) VALUES 
(1, 'Dr.', 'Tello', 1, 'tello@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', true),          -- Jefe Médico
(2, 'Dra.', 'Perez', 1, 'perez@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),        -- Cardiología
(3, 'Dr.', 'Ramirez', 2, 'ramirez@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),     -- Pediatría
(4, 'Dra.', 'Mendoza', 3, 'mendoza@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),    -- Medicina General
(5, 'Dr.', 'Castillo', 4, 'castillo@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),   -- Dermatología
(6, 'Dr.', 'Vargas', 6, 'vargas@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),       -- Oftalmología
(7, 'Dra.', 'Salazar', 7, 'salazar@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),    -- Traumatología
(8, 'Dra.', 'Flores', 8, 'flores@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false),      -- Ginecología
(9, 'Dr.', 'Navarro', 9, 'navarro@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', false)      -- Odontología
ON CONFLICT (id) DO UPDATE SET 
    nombres = EXCLUDED.nombres, 
    apellidos = EXCLUDED.apellidos, 
    especialidad_id = EXCLUDED.especialidad_id,
    correo = EXCLUDED.correo,
    password = EXCLUDED.password,
    es_jefe = EXCLUDED.es_jefe;

-- =====================================================
-- 2.1 USUARIOS PERSONAL (Estructura real sin columna version)
-- =====================================================
INSERT INTO usuarios_personal (id, medico_id, nombre, correo, password, rol, activo) VALUES
(1, 1, 'Dr. Tello', 'tello@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'JEFE_MEDICO', true),
(2, 2, 'Dra. Perez', 'perez@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(3, 3, 'Dr. Ramirez', 'ramirez@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(4, 4, 'Dra. Mendoza', 'mendoza@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(5, 5, 'Dr. Castillo', 'castillo@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(6, 6, 'Dr. Vargas', 'vargas@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(7, 7, 'Dra. Salazar', 'salazar@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(8, 8, 'Dra. Flores', 'flores@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true),
(9, 9, 'Dr. Navarro', 'navarro@saludcitas.com', '$2a$10$wT8K5P9N7eI0vBv7qXyKge8Q4c4yM0yG6qZ3X7t8z0vQ4a8b7c6d.', 'MEDICO', true)
ON CONFLICT (id) DO UPDATE SET 
    nombre = EXCLUDED.nombre,
    correo = EXCLUDED.correo,
    password = EXCLUDED.password,
    rol = EXCLUDED.rol,
    activo = EXCLUDED.activo;

-- =====================================================
-- 3. TURNOS (Con asignación de consultorio físico y activo = true)
-- =====================================================

-- --- CARDIOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(101, 1, '2026-11-02', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(102, 1, '2026-11-02', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(103, 1, '2026-11-02', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(104, 1, '2026-11-02', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(105, 1, '2026-11-04', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(106, 1, '2026-11-04', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(107, 1, '2026-11-04', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(108, 2, '2026-11-03', '14:00:00', '14:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)'),
(109, 2, '2026-11-03', '14:30:00', '15:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)'),
(110, 2, '2026-11-05', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)'),
(111, 2, '2026-11-05', '15:30:00', '16:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- PEDIATRÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(112, 3, '2026-11-02', '08:00:00', '08:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(113, 3, '2026-11-02', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(114, 3, '2026-11-02', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(115, 3, '2026-11-04', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(116, 3, '2026-11-04', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- MEDICINA GENERAL (15 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(117, 4, '2026-11-02', '08:00:00', '08:15:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(118, 4, '2026-11-02', '08:15:00', '08:30:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(119, 4, '2026-11-02', '08:30:00', '08:45:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(120, 4, '2026-11-02', '08:45:00', '09:00:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(121, 4, '2026-11-03', '09:00:00', '09:15:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(122, 4, '2026-11-03', '09:15:00', '09:30:00', 15, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- DERMATOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(123, 5, '2026-11-03', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(124, 5, '2026-11-03', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(125, 5, '2026-11-04', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(126, 5, '2026-11-04', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- OFTALMOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(127, 6, '2026-11-03', '11:00:00', '11:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)'),
(128, 6, '2026-11-03', '11:30:00', '12:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)'),
(129, 6, '2026-11-05', '16:00:00', '16:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 102 (Torre A)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- TRAUMATOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(130, 7, '2026-11-04', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(131, 7, '2026-11-04', '15:30:00', '16:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)'),
(132, 7, '2026-11-06', '16:00:00', '16:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 201 (Torre B)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- GINECOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(133, 8, '2026-11-05', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(134, 8, '2026-11-05', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)'),
(135, 8, '2026-11-06', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false, true, 'Cons. 202 (Torre B)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- --- ODONTOLOGÍA (45 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo, activo, consultorio) VALUES
(136, 9, '2026-11-06', '10:00:00', '10:45:00', 45, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(137, 9, '2026-11-06', '10:45:00', '11:30:00', 45, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)'),
(138, 9, '2026-11-07', '09:00:00', '09:45:00', 45, 'DISPONIBLE', false, true, 'Cons. 101 (Torre A)')
ON CONFLICT (id) DO UPDATE SET 
    fecha = EXCLUDED.fecha,
    hora_inicio = EXCLUDED.hora_inicio,
    hora_fin = EXCLUDED.hora_fin,
    activo = EXCLUDED.activo,
    estado = EXCLUDED.estado,
    consultorio = EXCLUDED.consultorio;

-- =====================================================
-- 4. SINCRONIZACIÓN OBLIGATORIA DE SECUENCIAS
-- =====================================================
SELECT setval(pg_get_serial_sequence('especialidades', 'id'), COALESCE(MAX(id), 1)) FROM especialidades;
SELECT setval(pg_get_serial_sequence('medicos', 'id'), COALESCE(MAX(id), 1)) FROM medicos;
SELECT setval(pg_get_serial_sequence('turnos', 'id'), COALESCE(MAX(id), 1)) FROM turnos;
SELECT setval(pg_get_serial_sequence('usuarios_personal', 'id'), COALESCE(MAX(id), 1)) FROM usuarios_personal;