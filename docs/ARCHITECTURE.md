# Arquitectura — Convive

## Vista general

```
MainActivity
  └─ ConviveTheme
       └─ Navigation (por rol)  ──►  Pantallas Compose (Tenant*, Admin*, Super*)
                                         │ leen/escriben
                                         ▼
                                     object Store  ── SharedPreferences ("convive", JSON)
                                         │ (opcional)
                                         ▼
                                     Remote (Supabase REST + Auth)  ──►  Postgres con RLS
```

- **Una sola Activity**, navegación propia (`Navigation.kt`) con `Tab` y `Screen`.
- **Estado:** `Store` expone `mutableStateListOf` de cada entidad; Compose recompone solo.
- **Persistencia local:** `Store.save()` serializa todo a JSON en SharedPreferences
  (`DATA_VERSION` controla migraciones; si no carga, se vuelve a sembrar con datos demo).
- **Backend remoto:** `Remote.kt` habla con Supabase por HTTP (sin SDK, sin dependencias
  nuevas). Se activa solo si `SUPABASE_URL` y `SUPABASE_KEY` están definidos en el build
  (`BuildConfig`). Hoy **no** reemplaza a `Store`: es la base para migrar módulo por módulo.

## Archivos principales (`pe.convive`)

| Archivo | Responsabilidad |
|---|---|
| `Models.kt` | Enums y data classes del dominio |
| `Store.kt` | Lógica de negocio, sesión, búsqueda, persistencia local, datos demo |
| `Remote.kt` | Cliente Supabase (config, auth, select/insert/update) |
| `Navigation.kt` | Tabs por rol, transiciones, barra inferior |
| `LoginScreen.kt`, `Biometric.kt` | Ingreso |
| `TenantScreens.kt`, `MeterScreens.kt`, `ContractScreens.kt` | Inquilino |
| `AdminHome.kt`, `AdminScreens.kt`, `ProfileScreen.kt` | Administrador |
| `Super*.kt` | Super Admin |
| `Components.kt`, `Kit.kt`, `Motion.kt`, `ConviveIcons.kt`, `Theme.kt` | Design system |
| `Notifier.kt` | Notificaciones locales |
| `Photos.kt`, `PhotoPicker.kt` | Fotos (archivos en el teléfono) |

## Modelo de datos (Supabase)

Migración: `supabase/migrations/20261101000000_init.sql`.

- `clients`, `profiles` (1:1 con `auth.users`), `units`, `charges`, `incidents`, `notices`,
  `emergencies`, `meter_requests`, `meter_readings`, `contracts`, `plans`,
  `subscription_payments`, `support_requests`.
- Todo dato de un condominio lleva `client_id`; RLS filtra por el `client_id` y el rol del
  perfil autenticado (funciones `private.my_role()` y `private.my_client()`).
- Las contraseñas las gestiona **Supabase Auth**; `profiles` no guarda contraseñas.
- Ingreso con usuario o DNI: `public.login_email(identifier)` devuelve el email interno
  (`<usuario>@users.convive.app`) sin revelar si la cuenta existe.

## Plan de migración del Store al backend

1. Crear cuentas en Supabase Auth (script con `service_role` fuera de la app).
2. Login remoto (`Remote.signIn`) → cargar perfil y datos del cliente en `Store`.
3. Migrar por módulos, de mayor a menor necesidad multi-dispositivo:
   emergencias → avisos → reportes → cobros → medidores (+ Storage para fotos) → contratos.
4. Mantener SharedPreferences como caché offline.
