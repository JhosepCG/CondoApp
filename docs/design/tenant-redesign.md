# Rediseño de la app del inquilino (propuesta, 2026-11-01)

Lienzo: https://claude.ai/artifact/YGdLQwn7EB6iEJCvGNT5mf (privado del dueño).
Basado en el design system oficial **Convive** (Pino `#0B4F47`, Sol `#F5B83D`, Papel `#FAF8F4`,
Manrope + Figtree). Estado: **solo diseño**, no implementado.

## Crítica de la versión actual (v0.8.x)

| Hallazgo | Severidad | Cambio propuesto |
|---|---|---|
| El botón de emergencia está al final del Inicio: hay que hacer scroll en plena crisis | 🔴 Crítico | Botón **SOS** fijo en el centro de la barra inferior, en todas las pantallas |
| Un toque abre la alerta; riesgo de falsa alarma | 🟡 Moderado | Elegir tipo + **mantener presionado** para enviar |
| Tras enviar solo hay un texto de 12 sp | 🟡 Moderado | Pantalla de seguimiento: Enviada → Vista → Confirmada, con pasos de seguridad |
| "Próximo pago" es la 4.ª tarjeta; arriba va "Mi domicilio" (dato fijo) | 🟡 Moderado | El pago es la tarjeta principal, con *Cómo pagar* y *Ya pagué* |
| Carrusel de avisos que pasa solo cada 5 s (WCAG 2.2.2) | 🟡 Moderado | Aviso urgente fijo + lista; sin auto-avance |
| No hay forma de pagar ni de enviar comprobante | 🟡 Moderado | Pestaña **Pagos**: Yape/Plin con copiar, subir comprobante, historial, mora explicada |
| El inquilino no tiene perfil ni historial de reportes | 🟡 Moderado | Pestaña **Yo**: contrato, medidor, mis reportes, ajustes |
| Texto clave en 12 sp (`bodySmall`) | 🟢 Menor | Mínimo 16 px en párrafos, 14 px en metadatos |
| Paleta del código (Navy/Teal, Roboto) ≠ design system (Pino/Sol, Manrope/Figtree) | 🟢 Menor | Migrar `Theme.kt` a los tokens oficiales |
| Botón de emergencia "late" todo el tiempo | 🟢 Menor | Sin animación en reposo: la marca es calma el 99 % del tiempo |

## Nueva navegación

`Inicio · Pagos · [SOS] · Avisos · Yo` — Medidor y Servicios dejan de ser pestañas y pasan a
"Para ti" (Inicio) y a **Yo**.

## Pantallas del lienzo

1. Inicio · 2. Pagos · 3. Avisos · 4. Yo · 5. Emergencia (elegir + mantener) ·
6. Emergencia (seguimiento) · 7. Reportar falla · 8. Estados (sin conexión, cargando, vacío).

## Funciones nuevas que implica (requieren backend)

- Subir comprobante de pago y confirmación por la administración.
- Estado "vista por la administración" de la emergencia.
- Reporte en cola sin conexión.
