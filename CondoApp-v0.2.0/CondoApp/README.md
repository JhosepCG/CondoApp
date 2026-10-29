# Convive v0.6.1

> Antes "CondoApp". La app ahora se llama **Convive**.

App Android (Kotlin + Jetpack Compose) para condominios, edificios y casas que se alquilan por departamento.
Una sola app con **3 roles**: Super Admin, Administrador e Inquilino.

## Novedades de la v0.6.1

- Inicio del Super Admin más simple: suscripciones al mes, usuarios, suscripciones por cobrar,
  ingresos por cliente y la dona de suscripciones del mes.
- El Super Admin ya no ve alertas, reportes, usuarios por cliente ni nada de medidores de luz.

## Novedades de la v0.6.0

- **Login nuevo** con la foto del edificio de fondo.
- **Suscripciones**: el Super Admin solo cobra la suscripción mensual de cada cliente (vence el día 5).
  Con un solo botón la marca como **pagada**, y puede poner a un cliente en **Gratis** (con todas las funciones de pago).
- **Owner**: si un cliente tiene dos o más administradores, uno es el **owner** (responsable del pago);
  los demás tienen las mismas funciones.
- **Mi perfil** del administrador (tocando su foto): cuánto paga, estado del mes, historial de pagos,
  equipo de administradores, ver introducción y cerrar sesión.
- Cuenta de prueba nueva: `sofia` (administradora de Casa Quispe, no owner).

## Novedades de la v0.5.0

- **Introducción de 3 pasos** al entrar por primera vez: una para el administrador y otra para el inquilino
  (se puede volver a ver desde el avatar → "Ver introducción").
- **Contrato del inquilino** (casas por departamentos): al registrarlo, el administrador indica DNI, nombre completo,
  fecha de inicio y fin, si dejó garantía y el monto, pago mensual y servicios incluidos.
- **Renovación de contrato**: el administrador ve los contratos vencidos o por vencer, marca
  "Debe firmar nuevamente el contrato" y toca **Enviar**; recién ahí el inquilino ve el aviso en su Inicio.
- El inquilino ve "Mi contrato" en su Inicio. **El Super Admin no ve esta información.**

## Novedades de la v0.4.0

- **Nuevo nombre: Convive**, con los mismos colores (azul noche + verde azulado + ámbar).
- **Interfaz renovada**: saludo con campana y avatar, barra inferior flotante tipo píldora, buscador y accesos rápidos.
- **Buscador de inquilinos** para Administrador y Super Admin.
- **Perfil del inquilino**: el administrador ve si pagó, sus reportes, si falta la foto del medidor, sus alertas y su cuenta.
- **Avisos con foto**: el administrador puede adjuntar una foto (por ejemplo, el comunicado del corte).
- **Avisos en carrusel** en el Inicio del inquilino (pasan solos cada 5 segundos).
- **Medidores solo para casas** por departamentos (ya no aparece en condominios).
- **Super Admin con estadísticas**: ingresos por cliente, cobranza (gráfico de dona), usuarios por cliente y avance de medidores.

## Novedades de la v0.3.0

- **Medidor de luz**: el inquilino toma la foto de su medidor (o la sube de su galería), puede anotar la lectura y la envía a la administración.
- **Pedido con fecha límite**: el administrador pide las fotos con una fecha; a los inquilinos les llega un aviso urgente y un recordatorio en su Inicio.
- El administrador ve quién envió, quién falta o está vencido, abre cada foto y la marca como revisada.
- El Super Admin puede activar o desactivar "Fotos del medidor de luz" por cliente.

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
| `pedro` | Administrador **owner** · Casa Quispe (con medidores) |
| `sofia` | Administradora · Casa Quispe (no owner) |
| `marco`, `rosa`, `diego` | Inquilinos · Casa Quispe |

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
| `MeterScreens.kt`, `ProfileScreen.kt` | Medidores y perfil del inquilino |
| `Kit.kt`, `PhotoPicker.kt`, `Photos.kt` | Buscador, barra flotante, fotos |

## Pendiente (siguientes versiones)

- Backend en la nube (por ejemplo Supabase) para que los datos se compartan entre teléfonos.
- Notificaciones push para emergencias y avisos.
- Áreas comunes, asambleas y visitas.

> Convive no reemplaza a Bomberos (116), Policía (105) ni SAMU (106).
