# Registro de decisiones

## D-001 · Proyecto activo: `Convive-v0.8.0/Convive` (2026-11-01)
El CI compila solo esa carpeta. Las copias antiguas (v0.2 en la raíz y `Convive-v0.7.0/`) se
eliminaron por decisión del dueño; se recuperan desde el historial de Git.

## D-002 · Backend: Supabase (2026-11-01)
Ya existía un proyecto en Supabase para la app y el README lo planteaba.
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

## D-007 · Nombre único: Convive (2026-11-01)
Carpeta `Convive-v0.8.0/Convive`, paquete `pe.convive`, tema `Theme.Convive` / `ConviveTheme`.
Los datos locales pasan de `condoapp` a `convive`; `Store.migrateLegacyPrefs` los copia una vez
(por eso el nombre antiguo sigue en una constante). El `applicationId` se **mantiene** en `pe.condoapp`
(decisión del dueño): cambiarlo hace que Android lo trate como otra app (no actualiza la
instalada y se pierden los datos locales). Revisar antes de publicar en Play Store.
