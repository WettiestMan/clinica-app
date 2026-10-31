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
-- =====================================================
INSERT INTO medicos (id, nombres, apellidos, especialidad_id, correo, password, es_jefe) VALUES 
(1, 'Dr.', 'Tello', 1, 'tello@saludcitas.com', 'medico123', true),          -- Jefe Médico
(2, 'Dra.', 'Perez', 1, 'perez@saludcitas.com', 'medico123', false),        -- Cardiología
(3, 'Dr.', 'Ramirez', 2, 'ramirez@saludcitas.com', 'medico123', false),     -- Pediatría
(4, 'Dra.', 'Mendoza', 3, 'mendoza@saludcitas.com', 'medico123', false),    -- Medicina General
(5, 'Dr.', 'Castillo', 4, 'castillo@saludcitas.com', 'medico123', false),   -- Dermatología
(6, 'Dr.', 'Vargas', 6, 'vargas@saludcitas.com', 'medico123', false),       -- Oftalmología
(7, 'Dra.', 'Salazar', 7, 'salazar@saludcitas.com', 'medico123', false),    -- Traumatología
(8, 'Dra.', 'Flores', 8, 'flores@saludcitas.com', 'medico123', false),      -- Ginecología
(9, 'Dr.', 'Navarro', 9, 'navarro@saludcitas.com', 'medico123', false)      -- Odontología
ON CONFLICT (id) DO UPDATE SET 
    nombres = EXCLUDED.nombres, 
    apellidos = EXCLUDED.apellidos, 
    especialidad_id = EXCLUDED.especialidad_id,
    correo = EXCLUDED.correo,
    password = EXCLUDED.password,
    es_jefe = EXCLUDED.es_jefe;

-- =====================================================
-- 3. TURNOS (Duración variable según especialidad)
-- =====================================================

-- --- CARDIOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(101, 1, '2026-10-24', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(102, 1, '2026-10-24', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(103, 1, '2026-10-24', '09:30:00', '10:00:00', 30, 'OCUPADO', false),
(104, 1, '2026-10-24', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(105, 1, '2026-10-26', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(106, 1, '2026-10-26', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(107, 1, '2026-10-26', '10:00:00', '10:30:00', 30, 'OCUPADO', false),
(108, 2, '2026-10-25', '14:00:00', '14:30:00', 30, 'DISPONIBLE', false),
(109, 2, '2026-10-25', '14:30:00', '15:00:00', 30, 'DISPONIBLE', false),
(110, 2, '2026-10-27', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false),
(111, 2, '2026-10-27', '15:30:00', '16:00:00', 30, 'OCUPADO', false)
ON CONFLICT (id) DO NOTHING;

-- --- PEDIATRÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(112, 3, '2026-10-24', '08:00:00', '08:30:00', 30, 'DISPONIBLE', false),
(113, 3, '2026-10-24', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(114, 3, '2026-10-24', '09:00:00', '09:30:00', 30, 'OCUPADO', false),
(115, 3, '2026-10-26', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(116, 3, '2026-10-26', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- MEDICINA GENERAL (15 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(117, 4, '2026-10-24', '08:00:00', '08:15:00', 15, 'DISPONIBLE', false),
(118, 4, '2026-10-24', '08:15:00', '08:30:00', 15, 'DISPONIBLE', false),
(119, 4, '2026-10-24', '08:30:00', '08:45:00', 15, 'OCUPADO', false),
(120, 4, '2026-10-24', '08:45:00', '09:00:00', 15, 'DISPONIBLE', false),
(121, 4, '2026-10-25', '09:00:00', '09:15:00', 15, 'DISPONIBLE', false),
(122, 4, '2026-10-25', '09:15:00', '09:30:00', 15, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DERMATOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(123, 5, '2026-10-26', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(124, 5, '2026-10-26', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(125, 5, '2026-10-27', '10:00:00', '10:30:00', 30, 'OCUPADO', false),
(126, 5, '2026-10-27', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- OFTALMOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(127, 6, '2026-10-27', '11:00:00', '11:30:00', 30, 'DISPONIBLE', false),
(128, 6, '2026-10-27', '11:30:00', '12:00:00', 30, 'DISPONIBLE', false),
(129, 6, '2026-10-28', '16:00:00', '16:30:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- TRAUMATOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(130, 7, '2026-10-28', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false),
(131, 7, '2026-10-28', '15:30:00', '16:00:00', 30, 'OCUPADO', false),
(132, 7, '2026-10-29', '16:00:00', '16:30:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- GINECOLOGÍA (30 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(133, 8, '2026-10-29', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(134, 8, '2026-10-29', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(135, 8, '2026-10-30', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- ODONTOLOGÍA (45 min) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(136, 9, '2026-10-30', '10:00:00', '10:45:00', 45, 'DISPONIBLE', false),
(137, 9, '2026-10-30', '10:45:00', '11:30:00', 45, 'OCUPADO', false),
(138, 9, '2026-10-31', '09:00:00', '09:45:00', 45, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 4. SINCRONIZACIÓN OBLIGATORIA DE SECUENCIAS
-- =====================================================
SELECT setval(pg_get_serial_sequence('especialidades', 'id'), COALESCE(MAX(id), 1)) FROM especialidades;
SELECT setval(pg_get_serial_sequence('medicos', 'id'), COALESCE(MAX(id), 1)) FROM medicos;
SELECT setval(pg_get_serial_sequence('turnos', 'id'), COALESCE(MAX(id), 1)) FROM turnos;