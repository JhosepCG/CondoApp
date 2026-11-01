# Despliegue — Convive

## APK (GitHub Actions)

Workflow: `.github/workflows/build.yml`.

| Evento | Pruebas | APK (artifact) | Release pública |
|---|---|---|---|
| Push a `main` | ✅ | ✅ | ✅ `apk-<n>` (latest) |
| Push a otra rama | ✅ | ✅ | ❌ |
| Manual (`workflow_dispatch`) | ✅ | ✅ | Solo si es `main` |

Descarga: **Actions → Build APK → ejecución → Artifacts → Convive-apk**, o la última
Release desde el celular.

### Configurar Supabase en el APK

El build lee `SUPABASE_URL` y `SUPABASE_KEY` (publishable) de, en orden:
1. Variables de entorno.
2. Propiedades de Gradle (`-PSUPABASE_URL=...` o `~/.gradle/gradle.properties`).

En GitHub: **Settings → Secrets and variables → Actions → Variables**, crear
`SUPABASE_URL` y `SUPABASE_KEY`. Si faltan, el APK funciona en modo local.

## Supabase

- Proyecto: `nbisetgrrbawczcnyvwg` (región `us-west-2`). Pendiente: renombrarlo a **Convive** en el panel de Supabase.
- Migraciones versionadas en `supabase/migrations/`. Se aplican en orden por nombre.
- Después de cada migración: revisar *Advisors → Security* y *Performance*.
- Rollback: escribir una migración inversa (no editar migraciones ya aplicadas).

## Versionado

`versionCode` / `versionName` en `app/build.gradle.kts`. Subir ambos en cada release.
