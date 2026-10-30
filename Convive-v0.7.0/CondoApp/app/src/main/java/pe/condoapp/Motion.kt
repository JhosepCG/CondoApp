package pe.condoapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// Kit de movimiento de Convive.
// Inspirado en Motion (resortes y "whileTap"), anime.js (stagger, contadores
// y trazado de líneas), Animate.css (fadeInUp, headShake, swing, heartBeat)
// y LottieFiles (ilustraciones animadas). Todo nativo en Compose.
// ------------------------------------------------------------------

object Motion {
    /** Resorte suave para entradas y botones (equivalente a Motion "spring"). */
    val Soft = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
    val Bouncy = spring<Float>(dampingRatio = 0.48f, stiffness = 420f)
    const val STAGGER_MS = 55L
}

/** Botón que se hunde un poco al presionarlo y rebota al soltarlo (Motion: whileTap). */
fun Modifier.pressScale(pressed: Float = 0.95f): Modifier = composed {
    var down by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (down) pressed else 1f, Motion.Bouncy, label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                down = true
                waitForUpOrCancellation()
                down = false
            }
        }
}

/**
 * Entrada escalonada: aparece desde abajo con desvanecido (Animate.css fadeInUp + anime.js stagger).
 * [index] define el retraso para que las tarjetas entren una tras otra.
 */
fun Modifier.enter(index: Int = 0, distance: Dp = 18.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { distance.toPx() }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(10) * Motion.STAGGER_MS)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 260f))
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * px
    }
}

/** Ventanas que aparecen con un pequeño rebote (Animate.css zoomIn / Motion spring). */
fun Modifier.popIn(): Modifier = composed {
    val k = remember { Animatable(0f) }
    LaunchedEffect(Unit) { k.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f)) }
    graphicsLayer {
        val s = 0.86f + 0.14f * k.value
        scaleX = s; scaleY = s
        alpha = k.value.coerceIn(0f, 1f)
    }
}

/** Balanceo corto y horizontal para errores (Animate.css headShake). Cambia [trigger] para dispararlo. */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (v in listOf(-1f, 0.8f, -0.6f, 0.4f, -0.2f, 0f)) x.animateTo(v, tween(55))
    }
    graphicsLayer { translationX = x.value * px }
}

/** Campana que se balancea cada pocos segundos cuando hay algo nuevo (Animate.css swing). */
fun Modifier.ring(active: Boolean): Modifier = composed {
    val r = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) { r.snapTo(0f); return@LaunchedEffect }
        while (true) {
            for (v in listOf(16f, -12f, 9f, -6f, 3f, 0f)) r.animateTo(v, tween(90))
            delay(2600)
        }
    }
    graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 0.1f)
        rotationZ = r.value
    }
}

/** Flota de arriba a abajo sin parar (ilustraciones vivas, como un Lottie en bucle). */
fun Modifier.floating(amplitude: Dp = 6.dp, periodMs: Int = 2600, phase: Int = 0): Modifier = composed {
    val t = rememberInfiniteTransition(label = "float")
    val v by t.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(periodMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(phase)),
        label = "y"
    )
    val px = with(LocalDensity.current) { amplitude.toPx() }
    graphicsLayer { translationY = v * px }
}

/** Latido para el botón de emergencia (Animate.css heartBeat). */
fun Modifier.heartBeat(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "beat")
    val v by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 1800
                0f at 0
                1f at 180
                0f at 360
                1f at 540
                0f at 760
                0f at 1800
            }
        ),
        label = "s"
    )
    graphicsLayer { scaleX = 1f + v * 0.06f; scaleY = 1f + v * 0.06f }
}

/** Halo que se expande y desvanece detrás de un elemento (pulso). */
@Composable
fun PulseHalo(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "halo")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "p")
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = 1f + p * 0.55f
                scaleY = 1f + p * 0.55f
                alpha = (1f - p) * 0.6f
            }
            .clip(CircleShape)
            .background(color)
    )
}

/** Número que sube desde 0 hasta su valor (contador de anime.js). */
@Composable
fun CountUpText(
    value: Double,
    format: (Double) -> String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    durationMs: Int = 1100
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) {
        anim.animateTo(value.toFloat(), tween(durationMs, easing = FastOutSlowInEasing))
    }
    Text(format(anim.value.toDouble()), style = style, color = color, maxLines = 1)
}

/**
 * Check animado: el círculo se dibuja y luego el visto (trazado de líneas de anime.js),
 * con un pequeño rebote al final (Animate.css bounceIn).
 */
@Composable
fun AnimatedCheck(
    size: Dp = 72.dp,
    color: Color = Palette.Green,
    bg: Color = Palette.GreenSoft
) {
    val circle = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, Motion.Bouncy) }
        circle.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val stroke = w * 0.09f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * circle.value,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            val path = Path().apply {
                moveTo(w * 0.28f, w * 0.52f)
                lineTo(w * 0.44f, w * 0.67f)
                lineTo(w * 0.73f, w * 0.37f)
            }
            val pm = PathMeasure().apply { setPath(path, false) }
            val part = Path()
            pm.getSegment(0f, pm.length * tick.value, part, true)
            drawPath(part, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Tres puntos que saltan (cargando…). */
@Composable
fun BouncingDots(color: Color = Palette.Teal, dot: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "dots")
    val px = with(LocalDensity.current) { 8.dp.toPx() }
    Row(horizontalArrangement = Arrangement.spacedBy(dot * 0.7f), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 900
                        0f at 0
                        -1f at 180
                        0f at 360
                        0f at 900
                    },
                    initialStartOffset = StartOffset(i * 140)
                ),
                label = "d$i"
            )
            Box(Modifier.size(dot).graphicsLayer { translationY = y * px }.clip(CircleShape).background(color))
        }
    }
}

/**
 * Tip de ayuda que aparece una sola vez sobre un elemento (didáctico).
 * Se coloca dentro del Box del elemento. Al tocar "Entendido" no vuelve a salir.
 */
@Composable
fun CoachMark(
    key: String,
    text: String,
    above: Boolean = true,
    gap: Dp = 6.dp,
    delayMs: Long = 900
) {
    if (Store.tipSeen(key)) return
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(key) { delay(delayMs); show = true }
    if (!show) return
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    Popup(
        popupPositionProvider = remember(above, gapPx) { AnchorPosition(above, gapPx) },
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Column(
            Modifier
                .enter(0, if (above) 10.dp else (-10).dp)
                .floating(3.dp, 2000)
                .widthIn(max = 260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!above) Arrow(up = true)
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Navy)
                    .clickable { Store.markTip(key) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Entendido",
                    color = Palette.Amber,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                )
            }
            if (above) Arrow(up = false)
        }
    }
}

/** Coloca el tip justo encima (o debajo) del elemento, centrado y sin salirse de la pantalla. */
private class AnchorPosition(val above: Boolean, val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16)
        val x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2).coerceIn(16, maxX)
        val y = if (above) anchorBounds.top - popupContentSize.height - gap else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}

@Composable
private fun Arrow(up: Boolean) {
    Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
        val p = Path().apply {
            if (up) {
                moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width / 2, size.height); lineTo(size.width, 0f)
            }
            close()
        }
        drawPath(p, Palette.Navy)
    }
}
