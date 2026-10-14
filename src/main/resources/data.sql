-- =====================================================
-- 0. CORRECCIÓN DE RESTRICCIÓN DE UNICIDAD
-- =====================================================
ALTER TABLE citas DROP CONSTRAINT IF EXISTS uk_bbwh4ytnwechvo8kjw8p6pwdb;

-- =====================================================
-- 1. ESPECIALIDADES (con duraciones estándar)
-- =====================================================
INSERT INTO especialidades (id, nombre, duracion_minutos_default) VALUES 
(1, 'Cardiologia', 30),
(2, 'Pediatria', 30),
(3, 'Medicina General', 15),
(4, 'Dermatologia', 30),
(5, 'Cirugia General', 120),
(6, 'Oftalmologia', 30),
(7, 'Traumatologia', 30),
(8, 'Ginecologia', 30),
(9, 'Odontologia', 45)
ON CONFLICT (id) DO UPDATE SET 
    nombre = EXCLUDED.nombre, 
    duracion_minutos_default = EXCLUDED.duracion_minutos_default;

-- =====================================================
-- 2. MÉDICOS
-- =====================================================
INSERT INTO medicos (id, nombres, apellidos, especialidad_id) VALUES 
(1, 'Dr.', 'Tello', 1),         -- Cardiología
(2, 'Dra.', 'Perez', 1),        -- Cardiología
(3, 'Dr.', 'Ramirez', 2),       -- Pediatría
(4, 'Dra.', 'Mendoza', 3),      -- Medicina General
(5, 'Dr.', 'Castillo', 4),      -- Dermatología
(6, 'Dr.', 'Vargas', 6),        -- Oftalmología
(7, 'Dra.', 'Salazar', 7),      -- Traumatología
(8, 'Dra.', 'Flores', 8),       -- Ginecología
(9, 'Dr.', 'Navarro', 9)        -- Odontología
ON CONFLICT (id) DO UPDATE SET 
    nombres = EXCLUDED.nombres, 
    apellidos = EXCLUDED.apellidos, 
    especialidad_id = EXCLUDED.especialidad_id;

-- =====================================================
-- 3. TURNOS (Del 14 de Octubre de 2026 en adelante)
-- =====================================================

-- --- DR. TELLO (Cardiología - 14 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(1, 1, '2026-10-14', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(2, 1, '2026-10-14', '09:00:00', '09:30:00', 30, 'OCUPADO', false),
(3, 1, '2026-10-14', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(4, 1, '2026-10-14', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(5, 1, '2026-10-14', '10:30:00', '11:00:00', 30, 'OCUPADO', false)
ON CONFLICT (id) DO NOTHING;

-- --- DR. TELLO (Cardiología - 15 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(6, 1, '2026-10-15', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(7, 1, '2026-10-15', '09:30:00', '10:00:00', 30, 'OCUPADO', false),
(8, 1, '2026-10-15', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DRA. PEREZ (Cardiología - 14 y 15 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(9,  2, '2026-10-14', '14:00:00', '14:30:00', 30, 'DISPONIBLE', false),
(10, 2, '2026-10-14', '14:30:00', '15:00:00', 30, 'OCUPADO', false),
(11, 2, '2026-10-15', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false),
(12, 2, '2026-10-15', '15:30:00', '16:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DR. RAMÍREZ (Pediatría - 14 y 16 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(13, 3, '2026-10-14', '08:00:00', '08:30:00', 30, 'DISPONIBLE', false),
(14, 3, '2026-10-14', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(15, 3, '2026-10-14', '09:00:00', '09:30:00', 30, 'OCUPADO', false),
(16, 3, '2026-10-16', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(17, 3, '2026-10-16', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DRA. MENDOZA (Medicina General - Bloques de 15m - 14 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(18, 4, '2026-10-14', '08:00:00', '08:15:00', 15, 'DISPONIBLE', false),
(19, 4, '2026-10-14', '08:15:00', '08:30:00', 15, 'OCUPADO', false),
(20, 4, '2026-10-14', '08:30:00', '08:45:00', 15, 'DISPONIBLE', false),
(21, 4, '2026-10-14', '08:45:00', '09:00:00', 15, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DR. CASTILLO (Dermatología - 15 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(22, 5, '2026-10-15', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(23, 5, '2026-10-15', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(24, 5, '2026-10-15', '10:00:00', '10:30:00', 30, 'OCUPADO', false)
ON CONFLICT (id) DO NOTHING;

-- --- DR. VARGAS (Oftalmología - 16 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(25, 6, '2026-10-16', '11:00:00', '11:30:00', 30, 'DISPONIBLE', false),
(26, 6, '2026-10-16', '11:30:00', '12:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DRA. SALAZAR (Traumatología - 16 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(27, 7, '2026-10-16', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false),
(28, 7, '2026-10-16', '15:30:00', '16:00:00', 30, 'OCUPADO', false)
ON CONFLICT (id) DO NOTHING;

-- --- DRA. FLORES (Ginecología - 17 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(29, 8, '2026-10-17', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(30, 8, '2026-10-17', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO NOTHING;

-- --- DR. NAVARRO (Odontología - 17 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(31, 9, '2026-10-17', '10:00:00', '10:45:00', 45, 'DISPONIBLE', false),
(32, 9, '2026-10-17', '10:45:00', '11:30:00', 45, 'OCUPADO', false)
ON CONFLICT (id) DO NOTHING;