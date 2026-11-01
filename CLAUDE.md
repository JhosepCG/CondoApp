# CLAUDE.md — Instrucciones operativas de Convive

App Android **Convive** para condominios y casas por departamentos.
Tres roles: **Super Admin**, **Administrador** e **Inquilino**.

## Dónde está el código

- **Proyecto activo:** `Convive-v0.8.0/Convive` (es el único que compila el CI).
- Las copias antiguas (v0.2 y v0.7.0) se eliminaron; siguen en el historial de Git (D-001).
- Paquete Kotlin: `pe.convive` → `Convive-v0.8.0/Convive/app/src/main/java/pe/convive/`.
- Backend (Supabase): `supabase/migrations/` en la raíz del repo.

## Stack

- Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) · Material 3.
- AGP 8.7.3 · Gradle 8.10.2 · JDK 17 · minSdk 26 · targetSdk 35.
- Sin DI ni ViewModels: el estado vive en el `object Store` (Compose state lists).
- Backend opcional: Supabase (REST, sin SDK). Si no está configurado, la app funciona 100 % local.

## Reglas de trabajo

1. Las pantallas **no** acceden a datos directamente: todo pasa por `Store`.
2. Reutilizar `Components.kt`, `Kit.kt`, `Motion.kt`, `ConviveIcons.kt` y `Palette` (`Theme.kt`).
   No crear colores ni componentes nuevos sin revisar `docs/DESIGN_SYSTEM.md`.
3. Textos de la UI en español (Perú), tono cercano y corto.
4. No subir secretos. La clave *publishable* de Supabase es pública por diseño; la
   `service_role` **nunca** va al repo ni a la app.
5. Toda tabla nueva en Supabase lleva **RLS activado** y políticas por rol/cliente.
6. Lógica nueva en `Store` → agregar test en `app/src/test/`.

## Comandos

```bash
cd Convive-v0.8.0/Convive
gradle testDebugUnitTest   # pruebas unitarias (JVM)
gradle assembleDebug       # APK de depuración
```

El entorno remoto de Claude no tiene Android SDK ni acceso a Google Maven:
la verificación real se hace en GitHub Actions (`.github/workflows/build.yml`).

## Documentación

| Archivo | Contenido |
|---|---|
| `docs/PRODUCT.md` | Visión, roles, funciones y roadmap |
| `docs/ARCHITECTURE.md` | Módulos, datos y flujo |
| `docs/DESIGN_SYSTEM.md` | Colores, tipografía, componentes |
| `docs/SECURITY.md` | Controles y riesgos abiertos |
| `docs/TESTING.md` | Estrategia de pruebas |
| `docs/DEPLOYMENT.md` | CI, APK y Supabase |
| `docs/DECISIONS.md` | Registro de decisiones |
