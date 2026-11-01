# Pruebas — Convive

## Niveles

| Nivel | Herramienta | Dónde | Estado |
|---|---|---|---|
| Unitarias (JVM) | JUnit 4 | `app/src/test/java/pe/convive/` | ✅ Activas en CI |
| UI (Compose) | `ui-test-junit4` + emulador | `app/src/androidTest/` | ⏳ Pendiente |
| Backend (RLS) | SQL contra rama de Supabase | `supabase/tests/` | ⏳ Pendiente |
| Manual / humo | APK de Releases | Teléfono real | Cada versión |

## Qué cubren las unitarias

- `StoreLogicTest`: login (usuario, DNI, contraseña incorrecta, cliente desactivado),
  sugerencia de usuario sin tildes y sin duplicados, búsqueda inteligente de inquilinos
  (tildes, pisos, DNI, error de tipeo), recargo por mora, validación de contratos.
- `RemoteTest`: construcción de URLs/consultas PostgREST y email interno de ingreso.

`Store` funciona sin `Context` en las pruebas: si no se llama a `init()`, `save()` no hace nada.
Cada prueba limpia las listas en `@Before`.

## Cómo correrlas

```bash
cd Convive-v0.8.0/Convive
gradle testDebugUnitTest
```

En CI se ejecutan en cada push (cualquier rama) antes de compilar el APK.

## Flujos críticos para prueba manual

1. Inquilino envía emergencia → admin confirma → todos los inquilinos ven el aviso rojo.
2. Admin genera cobro → vence → inquilino ve recargo por mora.
3. Admin pide fotos del medidor → inquilino envía → admin marca revisada.
4. Ingreso con DNI y con huella.
