# CondoApp v0.2.0

App Android (Kotlin + Jetpack Compose) para condominios, edificios y casas que se alquilan por departamento.
Una sola app con **3 roles**: Super Admin, Administrador e Inquilino.

## Novedades de la v0.2.0

- **Nuevo diseño**: paleta azul noche + verde azulado, tarjetas redondeadas, íconos, encabezados con degradado
  e ícono propio de la app.
- **3 roles** (se quitó "Propietario"): Super Admin, Administrador e Inquilino.
- **Login con usuario y contraseña** (con cuentas de prueba para demostrar).
- **Los datos ya no se borran** al cerrar la app: se guardan en el teléfono.
- **Super Admin**: crea y **elimina cuentas**, crea/elimina clientes, ve ingreso mensual y usuarios por cliente,
  activa o desactiva funciones por cliente y define la mensualidad y el % de mora.
- **Administrador**: crea otros administradores e inquilinos, confirma o descarta emergencias,
  genera cobros y registra pagos, atiende reportes de servicios y publica avisos.
- **Inquilino**: ve su domicilio, responsable y próximo pago, usa el botón de emergencia,
  reporta fallas de internet/cable y confirma si se resolvieron, y lee los avisos.

## Cuentas de prueba (contraseña `1234`)

| Usuario | Rol |
|---|---|
| `superadmin` | Super Admin |
| `ana` | Administradora · Condominio Los Álamos |
| `lucia`, `carlos` | Inquilinos · Condominio Los Álamos |
| `pedro` | Administrador · Casa Quispe |
| `marco` | Inquilino · Casa Quispe |

## Flujo de emergencia

El inquilino toca el botón → solo la administración ve la alerta → si la confirma como real,
todos los residentes ven un aviso rojo. Si es falsa alarma, nadie más se entera.

## Cómo obtener el APK

Al subir los cambios a la rama `main`, GitHub Actions compila solo el APK:
pestaña **Actions → Build APK → (última ejecución) → Artifacts → app-debug**.

También se puede abrir esta carpeta en Android Studio y usar `Build > Build APK(s)`.

## Estructura del código

| Archivo | Qué contiene |
|---|---|
| `Models.kt` | Datos: roles, clientes, cuentas, cobros, reportes, avisos, emergencias |
| `Store.kt` | Toda la lógica y el guardado local (reemplazable por un backend) |
| `Theme.kt` | Colores, tipografía y formas |
| `Components.kt` | Piezas de diseño reutilizables |
| `LoginScreen.kt`, `Navigation.kt` | Ingreso y navegación por rol |
| `SuperAdminScreens.kt`, `AdminScreens.kt`, `TenantScreens.kt` | Pantallas de cada rol |

## Pendiente (siguientes versiones)

- Backend en la nube (por ejemplo Supabase) para que los datos se compartan entre teléfonos.
- Notificaciones push para emergencias y avisos.
- Áreas comunes, asambleas y visitas.

> CondoApp no reemplaza a Bomberos (116), Policía (105) ni SAMU (106).
