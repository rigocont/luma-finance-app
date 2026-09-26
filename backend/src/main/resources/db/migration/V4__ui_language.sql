-- Idioma de la interfaz (Fase 15): es/en. Separado de `locale` (formato de
-- numero/fecha, p.ej. es-MX), aunque hoy el frontend deriva uno del otro.
ALTER TABLE user_preferences
    ADD COLUMN ui_language VARCHAR(2) NOT NULL DEFAULT 'es' AFTER locale;
