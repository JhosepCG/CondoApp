# CondoApp (MVP de demostración)

App Android (Kotlin + Jetpack Compose) para condominios y casas por departamentos.
Una sola app con 4 roles: Super Admin, Administrador, Propietario e Inquilino.

## Cómo generar el APK

1. Instala **Android Studio** (versión reciente).
2. `File > Open` y elige esta carpeta (`CondoApp`). Espera a que termine el "Gradle sync"
   (descarga Gradle 8.10.2 y las librerías; necesita internet la primera vez).
3. Menú `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al celular e instálalo
   (activa "instalar apps desconocidas" para tu gestor de archivos).

Por consola: `./gradlew assembleDebug` (si Android Studio generó el wrapper) o desde la terminal de Android Studio.

## Qué incluye

- **Super Admin:** resumen de clientes, usuarios e ingreso mensual; activar/desactivar funciones por cliente;
  plantillas "Condominio" y "Casa por departamentos"; mensualidad y % de mora por cliente.
- **Administrador:** alertas de emergencia (confirmar o falsa alarma), cobros, reportes de internet/cable/etc.,
  publicar avisos (corte de agua, luz, otros) y agregar residentes.
- **Propietario / Inquilino:** domicilio y responsable, próximo pago (con mora si está activa),
  botón de emergencia, reportes de fallas con confirmación de "resuelto", avisos.
- Flujo de emergencia: el residente alerta → solo el administrador la ve → si la confirma, todos los residentes
  ven un aviso rojo.

## Limitaciones de esta versión

- Los datos viven **en memoria** (se reinician al cerrar la app) y el login es de demostración.
- Aún no hay notificaciones push, backend, ni cuentas reales. El siguiente paso es conectar un backend
  (por ejemplo Supabase) reemplazando `Store.kt`, sin tocar las pantallas.
- Áreas comunes, asambleas y visitas aparecen como "próximamente".
- La app no reemplaza a bomberos, policía ni serenazgo.
