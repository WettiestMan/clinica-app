-- =====================================================
-- 1. ESPECIALIDADES (con duraciones configuradas)
-- =====================================================
INSERT INTO especialidades (id, nombre, duracion_minutos_default) VALUES 
(1, 'Cardiologia', 30),
(2, 'Pediatria', 30),
(3, 'Medicina General', 15),
(4, 'Dermatologia', 30),
(5, 'Cirugia General', 120)
ON CONFLICT (id) DO UPDATE SET 
    nombre = EXCLUDED.nombre, 
    duracion_minutos_default = EXCLUDED.duracion_minutos_default;

-- =====================================================
-- 2. MÉDICOS
-- =====================================================
INSERT INTO medicos (id, nombres, apellidos, especialidad_id) VALUES 
(1, 'Dr.', 'Tello', 1),        -- Cardiología
(2, 'Dra.', 'Perez', 1),        -- Cardiología
(3, 'Dr.', 'Ramirez', 2),      -- Pediatría
(4, 'Dra.', 'Mendoza', 3)      -- Medicina General
ON CONFLICT (id) DO UPDATE SET 
    nombres = EXCLUDED.nombres, 
    apellidos = EXCLUDED.apellidos, 
    especialidad_id = EXCLUDED.especialidad_id;

-- =====================================================
-- 3. TURNOS (De Octubre 2026 en adelante)
-- =====================================================

-- --- DR. TELLO (Cardiología - 12 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(1, 1, '2026-10-12', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(2, 1, '2026-10-12', '09:00:00', '09:30:00', 30, 'OCUPADO', false),
(3, 1, '2026-10-12', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false),
(4, 1, '2026-10-12', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(5, 1, '2026-10-12', '10:30:00', '11:00:00', 30, 'OCUPADO', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;

-- --- DR. TELLO (Cardiología - 13 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(6, 1, '2026-10-13', '09:00:00', '09:30:00', 30, 'DISPONIBLE', false),
(7, 1, '2026-10-13', '09:30:00', '10:00:00', 30, 'OCUPADO', false),
(8, 1, '2026-10-13', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;

-- --- DRA. PEREZ (Cardiología - 12 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(9, 2, '2026-10-12', '14:00:00', '14:30:00', 30, 'DISPONIBLE', false),
(10, 2, '2026-10-12', '14:30:00', '15:00:00', 30, 'OCUPADO', false),
(11, 2, '2026-10-12', '15:00:00', '15:30:00', 30, 'DISPONIBLE', false),
(12, 2, '2026-10-12', '15:30:00', '16:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;

-- --- DR. RAMÍREZ (Pediatría - 12 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(13, 3, '2026-10-12', '08:00:00', '08:30:00', 30, 'DISPONIBLE', false),
(14, 3, '2026-10-12', '08:30:00', '09:00:00', 30, 'DISPONIBLE', false),
(15, 3, '2026-10-12', '09:00:00', '09:30:00', 30, 'OCUPADO', false),
(16, 3, '2026-10-12', '09:30:00', '10:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;

-- --- DR. RAMÍREZ (Pediatría - 14 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(17, 3, '2026-10-14', '10:00:00', '10:30:00', 30, 'DISPONIBLE', false),
(18, 3, '2026-10-14', '10:30:00', '11:00:00', 30, 'DISPONIBLE', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;

-- --- DRA. MENDOZA (Medicina General - Bloques de 15m - 12 de Octubre 2026) ---
INSERT INTO turnos (id, medico_id, fecha, hora_inicio, hora_fin, duracion_minutos, estado, es_sobrecupo) VALUES
(19, 4, '2026-10-12', '08:00:00', '08:15:00', 15, 'DISPONIBLE', false),
(20, 4, '2026-10-12', '08:15:00', '08:30:00', 15, 'OCUPADO', false),
(21, 4, '2026-10-12', '08:30:00', '08:45:00', 15, 'DISPONIBLE', false),
(22, 4, '2026-10-12', '08:45:00', '09:00:00', 15, 'DISPONIBLE', false)
ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado;