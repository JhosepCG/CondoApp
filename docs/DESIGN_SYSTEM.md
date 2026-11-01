# Design system — Convive

Fuente de verdad en código: `Theme.kt` (`Palette`, tipografía, formas) y
`Components.kt` / `Kit.kt` / `Motion.kt` / `ConviveIcons.kt`.

## Color

| Token | Hex | Uso |
|---|---|---|
| `Navy` | `#0B1F3A` | Marca, encabezados, botones primarios oscuros |
| `Teal` | `#0B6E6D` | Acción primaria (`primary`) |
| `Amber` / `AmberDark` | `#F2A541` / `#9A5B00` | Avisos, pendientes, recordatorios |
| `Red` | `#C21F35` | **Solo** emergencias y vencidos |
| `Green` | `#1E7B45` | Pagado, resuelto, al día |
| `Bg` | `#F4F6FA` | Fondo |
| `Surface` | `#FFFFFF` | Tarjetas |
| `Ink` / `Muted` | `#14213D` / `#5F6673` | Texto principal / secundario |
| `Line` | `#E5E8EF` | Bordes |

Cada color de estado tiene su versión `*Soft` para fondos de píldoras y tarjetas.

## Tipografía

Fuente del sistema (Roboto). Escala: `headlineMedium` 28 · `headlineSmall` 22 ·
`titleLarge` 19 · `titleMedium` 16 · `bodyLarge` 15 · `bodyMedium` 14 · `bodySmall` 12.

## Formas

Radios 12 / 16 / 24 dp. Tarjetas con borde `Line` de 1 dp.

## Componentes clave

`AppCard`, `HeroCard`, `HeroInfo`, `IconBadge`, `Pill`, `PrimaryButton`, `SectionTitle`,
`ScreenColumn`, `CoachMark`, barra inferior flotante tipo píldora.

## Reglas

- Rojo reservado a emergencias/vencidos para no gastar su urgencia.
- Objetivo táctil mínimo 48 dp; contraste AA (4.5:1) en texto.
- Cada pantalla contempla: cargando, vacío, error, sin permiso y sin conexión.
