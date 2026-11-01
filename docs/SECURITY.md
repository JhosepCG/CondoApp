# Seguridad — Convive

## Riesgos abiertos (app local actual)

| # | Severidad | Riesgo | Mitigación prevista |
|---|---|---|---|
| S-1 | **Alta** | Contraseñas en texto plano en `Store` y SharedPreferences | Migrar a Supabase Auth (hash bcrypt en servidor) |
| S-2 | **Alta** | Contraseña inicial fija `1234` para todas las cuentas | Forzar cambio en el primer ingreso; contraseña temporal aleatoria |
| S-3 | **Alta** | Datos demo con cuentas conocidas en el APK publicado | Desactivar `seed()` en builds de producción |
| S-4 | Media | Login distingue "usuario no existe" vs "contraseña incorrecta" (enumeración) | Mensaje único; con backend, `login_email` no revela existencia |
| S-5 | Media | `android:allowBackup="true"` permite extraer SharedPreferences por backup | Excluir `convive.xml` con `dataExtractionRules` o cifrar |
| S-6 | Media | Sin límite de intentos de ingreso | Rate limit de Supabase Auth + bloqueo temporal en la app |
| S-7 | Baja | APK debug firmado con clave de depuración publicado en Releases | Firma release con keystore en secretos de GitHub |

## Controles en el backend (Supabase)

- **RLS activado en todas las tablas** de `public`. Sin políticas para `anon`.
- Funciones de ayuda `security definer` en el esquema `private` (no expuesto por la API).
- Trigger `profiles_guard`: nadie (salvo el Super Admin) se cambia su propio `role` o `unit`,
  ni mueve una cuenta a otro `client_id`.
- El Super Admin **no** ve contratos (regla de negocio y de privacidad).
- `login_email()` es la única función accesible por `anon`; devuelve un email sintético
  incluso si la cuenta no existe.
- La app solo usa la clave **publishable**. La `service_role` nunca se incluye en la app.

## Advisors de Supabase (2026-11-01, tras la migración `init`)

- ⚠️ `public.login_email` ejecutable por `anon` — **intencional** (necesario para ingresar).
- ⚠️ `public.rls_auto_enable()` ejecutable por `anon`/`authenticated` — **ya existía** en el
  proyecto (no es de Convive). Revisar si se puede revocar `EXECUTE` o moverla de `public`.
- ℹ️ Rendimiento: varias políticas permisivas por tabla/acción (p. ej. `*_read` + `*_super`).
  Correcto en seguridad; consolidarlas en una política por acción cuando haya volumen.
- ℹ️ Índices sin uso: normal, aún no hay datos.

## Checklist antes de publicar

- [ ] Advisors de seguridad de Supabase sin alertas.
- [ ] Ninguna tabla nueva sin RLS.
- [ ] Sin secretos en el diff (`git diff | grep -iE "service_role|secret|password ="`).
- [ ] Pruebas unitarias en verde en CI.
