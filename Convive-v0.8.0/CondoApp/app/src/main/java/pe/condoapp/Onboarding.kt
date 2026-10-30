package pe.condoapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ------------------------------------------------------------------
// Introducción de 3 pasos (distinta para administrador e inquilino).
// Estilo: ilustración en círculo suave, puntos, título grande centrado,
// "SALTAR" a la izquierda y "SIGUIENTE" en píldora; en el último paso "EMPEZAR".
// ------------------------------------------------------------------

/** Escena animada de cada paso. Si existe res/raw/onb_<nombre>.json (LottieFiles) se usa esa animación. */
enum class Scene(val lottie: String) {
    REGISTRAR("onb_registrar"),
    COBRAR("onb_cobrar"),
    AVISAR("onb_avisar"),
    HOGAR("onb_hogar"),
    MEDIDOR("onb_medidor"),
    REPORTAR("onb_reportar")
}

class OnboardingStep(val scene: Scene, val title: String, val body: String)

fun onboardingSteps(user: AppUser): List<OnboardingStep> {
    val c = Store.clientOf(user)
    val casa = c?.template == Template.CASA_DEPAS
    return if (user.role == Role.ADMIN) {
        listOf(
            if (casa) OnboardingStep(
                Scene.REGISTRAR,
                "Registra a tus\ninquilinos",
                "Guarda su nombre, DNI, fecha de inicio, garantía, pago mensual y los servicios que le das."
            ) else OnboardingStep(
                Scene.REGISTRAR,
                "Registra a los\nresidentes",
                "Crea las cuentas de inquilinos y de otros administradores de tu condominio."
            ),
            if (casa) OnboardingStep(
                Scene.COBRAR,
                "Cobra y pide\nlos medidores",
                "Registra los pagos y pide la foto del medidor de luz con fecha límite."
            ) else OnboardingStep(
                Scene.COBRAR,
                "Controla\nla cobranza",
                "Genera cuotas, registra pagos y deja que la mora se calcule sola."
            ),
            OnboardingStep(
                Scene.AVISAR,
                "Avisa y mantén\nel orden",
                if (casa) "Publica avisos con foto, atiende reportes y avisa cuándo toca firmar de nuevo el contrato."
                else "Publica avisos con foto, atiende los reportes de servicios y confirma las emergencias."
            )
        )
    } else {
        listOf(
            OnboardingStep(
                Scene.HOGAR,
                "Tu hogar en\nun solo lugar",
                if (casa) "Mira tu próximo pago, tu contrato y los servicios incluidos."
                else "Mira tu próximo pago y quién es el responsable de tu domicilio."
            ),
            if (casa) OnboardingStep(
                Scene.MEDIDOR,
                "Envía la foto\nde tu medidor",
                "Cuando la administración lo pida, toma la foto o súbela de tu galería."
            ) else OnboardingStep(
                Scene.REPORTAR,
                "Reporta\nfallas",
                "Avisa si falla el internet, el cable o el agua, y confirma cuando quede resuelto."
            ),
            OnboardingStep(
                Scene.AVISAR,
                "Entérate de\ntodo a tiempo",
                "Recibe avisos de cortes y novedades, y usa el botón de emergencia si pasa algo."
            )
        )
    }
}

private val OnbBg = Color(0xFFF9FAFC)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(user: AppUser, onDone: () -> Unit) {
    val steps = remember(user.id) { onboardingSteps(user) }
    val pager = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == steps.lastIndex

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(OnbBg)
            .systemBarsPadding()
    ) {
        val illus = (maxHeight * 0.42f).coerceIn(200.dp, 330.dp)
        val top = (maxHeight * 0.07f).coerceIn(12.dp, 72.dp)

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    val s = steps[page]
                    val off = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(top))
                        // Ilustración con paralaje: se queda un poco atrás al deslizar
                        Box(
                            Modifier
                                .size(illus)
                                .graphicsLayer {
                                    translationX = off * size.width * 0.45f
                                    val k = 1f - off.absoluteValue * 0.14f
                                    scaleX = k; scaleY = k
                                    alpha = 1f - off.absoluteValue * 0.7f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SceneOrLottie(s.scene, illus, active = pager.currentPage == page)
                        }
                        Spacer(Modifier.height(58.dp)) // lugar de los puntos
                        Text(
                            s.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 27.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Palette.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer {
                                translationX = off * size.width * 0.25f
                                alpha = (1f - off.absoluteValue * 1.6f).coerceIn(0f, 1f)
                            }
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            s.body,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
                            color = Palette.Muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .graphicsLayer {
                                    translationX = off * size.width * 0.35f
                                    alpha = (1f - off.absoluteValue * 1.8f).coerceIn(0f, 1f)
                                }
                        )
                    }
                }
                PageDots(
                    pager,
                    steps.size,
                    Modifier.align(Alignment.TopCenter).padding(top = top + illus + 24.dp)
                )
            }

            // Barra inferior: SALTAR · SIGUIENTE → EMPEZAR (centrado y más ancho)
            val lastF by animateFloatAsState(if (last) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 300f), label = "last")
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp, bottom = 28.dp)
                    .height(60.dp)
            ) {
                Text(
                    "SALTAR",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = Palette.Ink,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { alpha = 1f - lastF; translationX = -lastF * 40f }
                        .clip(CircleShape)
                        .clickable(enabled = !last, onClick = onDone)
                        .padding(horizontal = 6.dp, vertical = 10.dp)
                )
                Box(
                    Modifier
                        .align(BiasAlignment(1f - lastF, 0f))
                        .pressScale()
                        .width(lerp(128.dp, 230.dp, lastF))
                        .height(54.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Palette.Navy.copy(alpha = 0.3f), spotColor = Palette.Navy.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .background(Palette.Navy)
                        .clickable {
                            if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        if (last) "EMPEZAR" else "SIGUIENTE",
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(120)) + slideOutVertically { -it / 2 })
                        },
                        label = "cta"
                    ) { label ->
                        Text(
                            label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        )
                    }
                }
            }
        }
    }
}

/** Puntos: el activo se alarga y se rellena; los demás son círculos con borde. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageDots(pager: PagerState, count: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val sel = pager.currentPage == i
            val w by animateDpAsState(if (sel) 22.dp else 8.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(w)
                    .clip(CircleShape)
                    .then(
                        if (sel) Modifier.background(Palette.Navy)
                        else Modifier.border(1.4.dp, Palette.Ink.copy(alpha = 0.55f), CircleShape)
                    )
            )
        }
    }
}

// ------------------------------------------------------------------
// Ilustraciones animadas
// ------------------------------------------------------------------

/** Usa la animación de LottieFiles si está en res/raw; si no, la escena dibujada en Compose. */
@Composable
private fun SceneOrLottie(scene: Scene, size: Dp, active: Boolean) {
    val ctx = LocalContext.current
    val rawId = remember(scene) { ctx.resources.getIdentifier(scene.lottie, "raw", ctx.packageName) }
    if (rawId != 0) {
        val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(rawId))
        val progress by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever, isPlaying = active)
        if (comp != null) {
            LottieAnimation(comp, { progress }, Modifier.size(size))
            return
        }
    }
    // Escena diseñada a 260dp y escalada al tamaño disponible
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(260.dp)
                .graphicsLayer {
                    val k = size.value / 260f
                    scaleX = k; scaleY = k
                }
        ) {
            when (scene) {
                Scene.REGISTRAR -> SceneRegistrar()
                Scene.COBRAR -> SceneCobrar()
                Scene.AVISAR -> SceneAvisar()
                Scene.HOGAR -> SceneHogar()
                Scene.MEDIDOR -> SceneMedidor()
                Scene.REPORTAR -> SceneReportar()
            }
        }
    }
}

/** Círculo suave de fondo + sombra en el piso (como la referencia). */
@Composable
private fun SceneFrame(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(224.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFD4E9E8), OnbBg)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(width = 176.dp, height = 12.dp)
                .clip(CircleShape)
                .background(Color(0xFFE7EAF0))
        )
        content()
    }
}

/** Progreso 0→1 que se repite (reloj común de cada escena). */
@Composable
private fun loop(durationMs: Int, label: String): State<Float> {
    val t = rememberInfiniteTransition(label = label)
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(durationMs, easing = LinearEasing)), label = label)
}

private fun seg(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun ease(x: Float): Float = FastOutSlowInEasing.transform(x)

@Composable
private fun WhiteCard(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Palette.Navy.copy(alpha = 0.15f), spotColor = Palette.Navy.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
        content = content
    )
}

@Composable
private fun SceneBadge(icon: ImageVector, bg: Color, fg: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .shadow(10.dp, CircleShape, spotColor = Palette.Navy.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
private fun MiniChip(text: String, bg: Color, fg: Color, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .shadow(8.dp, CircleShape, spotColor = Palette.Navy.copy(alpha = 0.2f))
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun FillBar(fraction: Float, color: Color, width: Dp, height: Dp = 7.dp) {
    Box(Modifier.width(width).height(height).clip(CircleShape).background(Color(0xFFEFF2F6))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(color))
    }
}

/** 1 · Registrar: la ficha del inquilino se va llenando sola; reloj y lápiz en movimiento. */
@Composable
private fun SceneRegistrar() = SceneFrame {
    val p by loop(3600, "reg")
    WhiteCard(Modifier.align(Alignment.Center).offset(y = 8.dp).floating(5.dp, 3000).size(width = 150.dp, height = 178.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(46.dp).background(Brush.horizontalGradient(listOf(Palette.Navy, Palette.Teal))))
            Column(Modifier.padding(horizontal = 14.dp).offset(y = (-22).dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White).padding(3.dp).clip(CircleShape).background(Palette.TealSoft),
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.User, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(26.dp)) }
                FillBar(ease(seg(p, 0.05f, 0.25f)), Palette.Navy, 104.dp, 8.dp)
                FillBar(ease(seg(p, 0.22f, 0.40f)), Palette.Teal, 80.dp)
                FillBar(ease(seg(p, 0.37f, 0.55f)), Palette.Teal, 94.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val k = ease(seg(p, 0.55f, 0.68f))
                    Box(Modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = k }) { MiniChip("DNI", Palette.AmberSoft, Palette.AmberDark) }
                    val k2 = ease(seg(p, 0.62f, 0.75f))
                    Box(Modifier.graphicsLayer { scaleX = k2; scaleY = k2; alpha = k2 }) { MiniChip("S/ 800", Palette.TealSoft, Palette.Teal) }
                }
            }
        }
    }
    // Reloj con minutero que gira
    Box(
        Modifier.align(Alignment.TopEnd).offset(x = (-26).dp, y = 22.dp).floating(4.dp, 2400, 400)
            .size(56.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.White).border(2.dp, Palette.Navy, CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.width * 0.32f
            val a = Math.toRadians((p * 360f - 90f).toDouble())
            drawLine(Palette.Navy, c, Offset(c.x + r * Math.cos(a).toFloat(), c.y + r * Math.sin(a).toFloat()), 3.dp.toPx(), StrokeCap.Round)
            drawLine(Palette.Teal, c, Offset(c.x + r * 0.6f, c.y), 3.dp.toPx(), StrokeCap.Round)
            drawCircle(Palette.Navy, 3.dp.toPx(), c)
        }
    }
    // Lápiz que "firma"
    val w = kotlin.math.sin(p * Math.PI.toFloat() * 8f)
    SceneBadge(
        Ic.Edit, Palette.Amber, Palette.Navy, 48.dp,
        Modifier.align(Alignment.BottomStart).offset(x = 30.dp, y = (-34).dp)
            .graphicsLayer { rotationZ = w * 10f; translationX = w * 6f }
    )
}

/** 2 · Cobrar: barras que crecen en cascada, moneda que cae y check de pagado. */
@Composable
private fun SceneCobrar() = SceneFrame {
    val p by loop(3200, "pay")
    WhiteCard(Modifier.align(Alignment.Center).offset(y = 12.dp).floating(5.dp, 3000).size(width = 168.dp, height = 150.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cobrado del mes", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = Palette.Muted)
            Text(
                money(2400.0 * ease(seg(p, 0.05f, 0.6f))).substringBefore("."),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = Palette.Ink
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.height(62.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.45f, 0.7f, 0.55f, 0.9f, 1f).forEachIndexed { i, h ->
                    val k = ease(seg(p, 0.05f + i * 0.08f, 0.3f + i * 0.08f))
                    Box(
                        Modifier.width(18.dp).fillMaxHeight(h * k.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(if (i == 4) Palette.Amber else Palette.Teal)
                    )
                }
            }
        }
    }
    // Moneda que cae
    val fall = ease(seg(p, 0.0f, 0.35f))
    Box(
        Modifier.align(Alignment.TopEnd).offset(x = (-34).dp, y = 14.dp)
            .graphicsLayer {
                translationY = (fall - 1f) * 80f
                alpha = if (p > 0.85f) (1f - seg(p, 0.85f, 1f)) else fall
                rotationY = fall * 360f
            }
            .size(46.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Palette.Amber)
            .border(3.dp, Color(0xFFFFD48A), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("S/", color = Palette.Navy, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
    // Check de pagado
    val x = seg(p, 0.62f, 0.75f)
    val k = if (x <= 0f) 0f else 1f + 0.25f * kotlin.math.sin(x * Math.PI.toFloat())
    Box(Modifier.align(Alignment.BottomStart).offset(x = 26.dp, y = (-28).dp).graphicsLayer { scaleX = k; scaleY = k }) {
        MiniChip("Pagado", Palette.GreenSoft, Palette.Green, Ic.CheckCircle)
    }
}

/** 3 · Avisar: comunicado con foto y campana que suena con ondas. */
@Composable
private fun SceneAvisar() = SceneFrame {
    WhiteCard(Modifier.align(Alignment.Center).offset(x = (-8).dp, y = 14.dp).floating(5.dp, 3000).size(width = 158.dp, height = 168.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Palette.Navy, Palette.Teal))),
                contentAlignment = Alignment.Center
            ) { Icon(Ic.Megaphone, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) }
            MiniChip("Urgente", Palette.RedSoft, Palette.Red)
            FillBar(1f, Color(0xFFDDE2EA), 120.dp, 7.dp)
            FillBar(1f, Color(0xFFDDE2EA), 86.dp, 7.dp)
        }
    }
    Box(Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = 16.dp), contentAlignment = Alignment.Center) {
        PulseHalo(Palette.Amber, 62.dp)
        Box(
            Modifier.size(62.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Ic.BellRing, contentDescription = null, tint = Palette.Navy,
                modifier = Modifier.size(32.dp).ring(true)
            )
        }
        Box(Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(Palette.Red).border(2.dp, Color.White, CircleShape))
    }
    SceneBadge(
        Ic.Camera, Palette.Teal, Color.White, 44.dp,
        Modifier.align(Alignment.BottomStart).offset(x = 28.dp, y = (-30).dp).floating(4.dp, 2200, 600)
    )
}

/** Inquilino 1 · Hogar: casa con ventanas que se encienden y chips de "Al día" y próximo pago. */
@Composable
private fun SceneHogar() = SceneFrame {
    val p by loop(3000, "home")
    Canvas(Modifier.align(Alignment.Center).offset(y = 16.dp).floating(4.dp, 3200).size(150.dp, 150.dp)) {
        val w = size.width; val h = size.height
        val roof = Path().apply { moveTo(w * 0.06f, h * 0.44f); lineTo(w * 0.5f, h * 0.08f); lineTo(w * 0.94f, h * 0.44f); close() }
        drawRoundRect(Color.White, Offset(w * 0.14f, h * 0.38f), Size(w * 0.72f, h * 0.56f), CornerRadius(10f, 10f))
        drawRoundRect(Palette.Line, Offset(w * 0.14f, h * 0.38f), Size(w * 0.72f, h * 0.56f), CornerRadius(10f, 10f), style = Stroke(3f))
        drawPath(roof, Palette.Navy)
        drawRoundRect(Palette.Teal, Offset(w * 0.42f, h * 0.62f), Size(w * 0.16f, h * 0.32f), CornerRadius(8f, 8f))
        val on1 = if (p < 0.5f) 1f else 0.35f
        val on2 = if (p >= 0.25f && p < 0.75f) 1f else 0.35f
        drawRoundRect(Palette.Amber.copy(alpha = on1), Offset(w * 0.2f, h * 0.52f), Size(w * 0.15f, h * 0.14f), CornerRadius(6f, 6f))
        drawRoundRect(Palette.Amber.copy(alpha = on2), Offset(w * 0.65f, h * 0.52f), Size(w * 0.15f, h * 0.14f), CornerRadius(6f, 6f))
        drawCircle(Palette.Amber, w * 0.05f, Offset(w * 0.5f, h * 0.29f))
    }
    MiniChip(
        "Al día", Palette.GreenSoft, Palette.Green, Ic.CheckCircle,
        Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = 36.dp).floating(4.dp, 2400, 300)
    )
    MiniChip(
        "Pago: 05 nov", Color.White, Palette.Navy, Ic.Calendar,
        Modifier.align(Alignment.BottomStart).offset(x = 18.dp, y = (-34).dp).floating(4.dp, 2600, 900)
    )
    SceneBadge(
        Ic.Doc, Palette.Navy, Color.White, 42.dp,
        Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 44.dp).floating(5.dp, 2800, 500)
    )
}

/** Inquilino 2 (casa) · Medidor: teléfono que toma la foto con flash y la envía. */
@Composable
private fun SceneMedidor() = SceneFrame {
    val p by loop(3400, "meter")
    Box(
        Modifier.align(Alignment.Center).offset(y = 8.dp).floating(4.dp, 3200)
            .size(width = 118.dp, height = 196.dp)
            .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = Palette.Navy.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp)).background(Palette.Navy).padding(7.dp)
            .clip(RoundedCornerShape(18.dp)).background(Color.White)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Palette.AmberSoft), contentAlignment = Alignment.Center) {
                Icon(Ic.Bolt, contentDescription = null, tint = Palette.AmberDark, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(10.dp))
            val n = (482 * ease(seg(p, 0.0f, 0.4f))).toInt()
            Text(
                n.toString().padStart(4, '0'),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = Palette.Ink
            )
            Text("kWh", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        }
        // Visor de cámara
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val l = 14.dp.toPx(); val s = 3.dp.toPx()
            val c = Palette.Teal
            listOf(Offset(0f, 0f) to Offset(1f, 1f), Offset(size.width, 0f) to Offset(-1f, 1f),
                Offset(0f, size.height) to Offset(1f, -1f), Offset(size.width, size.height) to Offset(-1f, -1f)).forEach { (o, d) ->
                drawLine(c, o, Offset(o.x + d.x * l, o.y), s, StrokeCap.Round)
                drawLine(c, o, Offset(o.x, o.y + d.y * l), s, StrokeCap.Round)
            }
        }
        // Flash
        val flash = if (p in 0.5f..0.62f) 1f - seg(p, 0.5f, 0.62f) else 0f
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash }.background(Color.White))
    }
    SceneBadge(
        Ic.Camera, Palette.Teal, Color.White, 50.dp,
        Modifier.align(Alignment.BottomEnd).offset(x = (-34).dp, y = (-30).dp)
            .graphicsLayer { val k = if (p in 0.46f..0.54f) 0.86f else 1f; scaleX = k; scaleY = k }
    )
    val sent = ease(seg(p, 0.66f, 0.76f)) * (1f - seg(p, 0.94f, 1f))
    Box(Modifier.align(Alignment.TopStart).offset(x = 22.dp, y = 48.dp).graphicsLayer { scaleX = sent; scaleY = sent; alpha = sent }) {
        MiniChip("Enviado", Palette.GreenSoft, Palette.Green, Ic.CheckCircle)
    }
}

/** Inquilino 2 (condominio) · Reportar: la falla pasa de "En revisión" a "Resuelto". */
@Composable
private fun SceneReportar() = SceneFrame {
    var solved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) { delay(1800); solved = !solved }
    }
    WhiteCard(Modifier.align(Alignment.Center).offset(y = 12.dp).floating(5.dp, 3000).size(width = 168.dp, height = 140.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Palette.NavySoft), contentAlignment = Alignment.Center) {
                    Icon(if (solved) Ic.Wifi else Ic.WifiOff, contentDescription = null, tint = Palette.Navy)
                }
                Column {
                    Text("Internet", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = Palette.Ink)
                    Text("Depa 302", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                }
            }
            AnimatedContent(
                solved,
                transitionSpec = { (scaleIn(initialScale = 0.6f, animationSpec = Motion.Bouncy) + fadeIn()) togetherWith fadeOut(tween(100)) },
                label = "status"
            ) { ok ->
                if (ok) MiniChip("Resuelto", Palette.GreenSoft, Palette.Green, Ic.CheckCircle)
                else MiniChip("En revisión", Palette.AmberSoft, Palette.AmberDark, Ic.Clock)
            }
        }
    }
    val t = rememberInfiniteTransition(label = "wrench")
    val r by t.animateFloat(-18f, 18f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    SceneBadge(
        Ic.Gear, Palette.Amber, Palette.Navy, 50.dp,
        Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 30.dp).graphicsLayer { rotationZ = if (solved) 0f else r }
    )
}
