# Registro de decisiones

## D-001 · Proyecto activo: `Convive-v0.8.0/CondoApp` (2026-11-01)
El CI compila solo esa carpeta. `CondoApp/` y `Convive-v0.7.0/` quedan como históricos.
**Pendiente:** confirmar con el dueño si se eliminan (Git ya guarda el historial).

## D-002 · Backend: Supabase (2026-11-01)
Ya existía el proyecto "CondoApp" en Supabase y el README lo planteaba.
Postgres + RLS encaja con datos relacionales por condominio. Firebase descartado por ahora.

## D-003 · Cliente Supabase sin SDK (2026-11-01)
`Remote.kt` usa `HttpURLConnection` + `org.json` (ya presentes en Android).
Evita sumar Ktor + kotlinx.serialization (~2 MB y plugin nuevo) mientras solo se usan
REST y Auth. Revisar si se necesita Realtime o Storage avanzado (entonces usar `supabase-kt`).

## D-004 · Ingreso con usuario o DNI sobre Supabase Auth (2026-11-01)
Supabase Auth exige email. Cada cuenta usa `<usuario>@users.convive.app`.
`login_email(identifier)` resuelve usuario o DNI al email sin revelar si existe.

## D-005 · Migración gradual del `Store` (2026-11-01)
No se reescribe `Store` de golpe (1.300 líneas). Se migra por módulos, empezando por
emergencias y avisos, manteniendo SharedPreferences como caché offline.

## D-006 · CI con pruebas en todas las ramas (2026-11-01)
Las pruebas y el APK corren en cada push; la Release pública solo desde `main`.
