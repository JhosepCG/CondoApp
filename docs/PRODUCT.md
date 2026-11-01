# Producto — Convive

## Problema

Administrar un condominio o una casa alquilada por departamentos se hace hoy por
WhatsApp, cuadernos y Excel: cobros que se olvidan, avisos que no llegan, reportes de
fallas sin seguimiento y emergencias que nadie confirma.

## Usuarios

| Rol | Quién es | Qué necesita |
|---|---|---|
| **Super Admin** | Dueño de Convive (SaaS) | Cobrar la suscripción a cada cliente, crear clientes/planes, ver métricas |
| **Administrador** | Administra un condominio o una casa | Registrar inquilinos, cobrar, publicar avisos, atender reportes y emergencias |
| **Inquilino** | Vive en un departamento | Saber cuánto y cuándo paga, enterarse de avisos, reportar fallas, pedir ayuda urgente |

Un **cliente** es un condominio/edificio (`CONDOMINIO`) o una casa por departamentos (`CASA_DEPAS`).

## Funciones actuales (v0.8.x)

- Ingreso con usuario o DNI + contraseña, "Recordar", huella dactilar.
- Introducción (onboarding) por rol y tips de ayuda de una sola vez.
- **Super Admin:** Home con métricas, clientes, planes (gratis, prueba 30 días, mensual,
  anual), cuentas y accesos, pedidos de ayuda, mi perfil.
- **Administrador:** panel, cobros y mora, avisos con foto, reportes de servicios,
  emergencias, medidores de luz (solo casas), contratos y renovación, buscador inteligente
  de inquilinos, perfil del inquilino.
- **Inquilino:** Inicio (domicilio, próximo pago, avisos en carrusel, contrato, botón de
  emergencia), Medidor, Servicios, Avisos.

Funciones activables por cliente: ver `Feature` en `Models.kt`.

## Roadmap

1. **Backend compartido (Supabase)** — en curso. Ver `docs/ARCHITECTURE.md`.
2. Notificaciones push reales (FCM) para emergencias y avisos.
3. Rediseño de la app del inquilino (propuesta en `docs/design/tenant-redesign.md`).
4. Pago en línea (Yape/Plin) y comprobantes.
5. Áreas comunes, asambleas y visitas (`Feature.ready = false`).

## Métricas sugeridas (no instrumentadas aún)

- % de cobros pagados antes del vencimiento.
- Tiempo medio de resolución de reportes.
- Inquilinos activos por semana.
- % de medidores enviados a tiempo.
