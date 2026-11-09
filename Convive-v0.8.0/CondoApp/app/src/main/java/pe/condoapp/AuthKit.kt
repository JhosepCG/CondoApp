package pe.condoapp

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// Kit visual del ingreso y registro (Figma «01 · Ingreso», «02 · Crear usuarios»).
// Medidas tomadas de los PNG exportados a 2x: título 24, cuerpo 14, etiqueta 12, botón 14.
// ------------------------------------------------------------------

internal val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold)
)

internal object AK {
    val Title = Color(0xFF0B1F3A)
    val Body = Color(0xFF6B7280)
    val Label = Color(0xFF4B5563)
    val Text = Color(0xFF14213D)
    val Placeholder = Color(0xFF9CA3AF)
    val Border = Color(0xFFE5E7EB)
    val Disabled = Color(0xFFD1D5DB)
    val DividerLine = Color(0xFFCBD5E1)
    val DividerText = Color(0xFF64748B)
    val Keypad = Color(0xFFE8EBEF)
    val BackGray = Color(0xFFE8EBF1)
    val Soft = Color(0xFFF3F5F9)
    val Tint = Color(0xFFE6F3F2)
    val PillGreenBg = Color(0xFFE3F4EA)
    val PillGreen = Color(0xFF1E7B45)
    val PillAmberBg = Color(0xFFFFF1DC)
    val PillAmber = Color(0xFF9A5B00)
}

internal val TTitle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp, color = AK.Title)
internal val TBody = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp, color = AK.Body)
internal val TSmall = TextStyle(fontFamily = Geist, fontSize = 12.sp, lineHeight = 16.sp, color = AK.Body)
internal val TLabel = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = AK.Label)
internal val TField = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp, color = AK.Text)
internal val TButton = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)
internal val TName = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = AK.Title)
internal val TLink = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Teal)
internal val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
internal val CtlShape = RoundedCornerShape(12.dp)
internal val CardShape = RoundedCornerShape(16.dp)
internal val EMAIL_RX = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

internal fun authPhone(digits: String): String =
    if (digits.length == 9) "+51 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}" else digits

// ---------------- Íconos de línea (mismo trazo que el Iconography del Figma) ----------------

private fun lineIcon(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    paths.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        )
    }
    return b.build()
}

private const val HOUSE = "M4 10.2 12 4l8 6.2V19a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"

internal object AIc {
    val Back = lineIcon("back", "M15 18l-6-6 6-6")
    val House = lineIcon("house", HOUSE, "M10.5 17h3")
    val Building = lineIcon("building", HOUSE, "M9 13h6", "M9 16.5h6")
    val Condo = lineIcon("condo", HOUSE, "M12 17.6s-3-1.7-3-3.7a1.6 1.6 0 0 1 3-.8 1.6 1.6 0 0 1 3 .8c0 2-3 3.7-3 3.7z")
    val Users = lineIcon(
        "users", "M8.5 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", "M2.5 19v-1a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v1",
        "M15.5 5.2a3 3 0 0 1 0 5.6", "M21.5 19v-1a4 4 0 0 0-3-3.9"
    )
    val Zap = lineIcon("zap", "M13 2 4 14h7l-1 8 9-12h-7z")
    val Drop = lineIcon("drop", "M12 21a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5S12.5 4.5 12 2c-.5 2.5-2 4.9-4 6.5S5 12 5 14a7 7 0 0 0 7 7z")
    val Wind = lineIcon("wind", "M12.8 19.6A2 2 0 1 0 14 16H2", "M17.5 8a2.5 2.5 0 1 1 2 4H2", "M9.8 4.4A2 2 0 1 1 11 8H2")
    val Wifi = lineIcon("wifi", "M12 20h.01", "M2 8.8a15 15 0 0 1 20 0", "M5 12.9a10 10 0 0 1 14 0", "M8.5 16.4a5 5 0 0 1 7 0")
    val Monitor = lineIcon("monitor", "M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M8 21h8", "M12 17v4")
    val Gear = lineIcon(
        "gear",
        "M12.2 2h-.4a2 2 0 0 0-2 2v.2a2 2 0 0 1-1 1.7l-.4.3a2 2 0 0 1-2 0l-.2-.1a2 2 0 0 0-2.7.7l-.2.4a2 2 0 0 0 .7 2.7l.2.1a2 2 0 0 1 1 1.7v.5a2 2 0 0 1-1 1.7l-.2.1a2 2 0 0 0-.7 2.7l.2.4a2 2 0 0 0 2.7.7l.2-.1a2 2 0 0 1 2 0l.4.3a2 2 0 0 1 1 1.7v.2a2 2 0 0 0 2 2h.4a2 2 0 0 0 2-2v-.2a2 2 0 0 1 1-1.7l.4-.3a2 2 0 0 1 2 0l.2.1a2 2 0 0 0 2.7-.7l.2-.4a2 2 0 0 0-.7-2.7l-.2-.1a2 2 0 0 1-1-1.7v-.5a2 2 0 0 1 1-1.7l.2-.1a2 2 0 0 0 .7-2.7l-.2-.4a2 2 0 0 0-2.7-.7l-.2.1a2 2 0 0 1-2 0l-.4-.3a2 2 0 0 1-1-1.7V4a2 2 0 0 0-2-2z",
        "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
    )
    val Truck = lineIcon(
        "truck", "M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2", "M15 18H9",
        "M19 18h2a1 1 0 0 0 1-1v-3.6a1 1 0 0 0-.2-.6l-3.5-4.4a1 1 0 0 0-.8-.4H14",
        "M17 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4z", "M7 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"
    )
    val Plus = lineIcon("plus", "M12 5v14", "M5 12h14")
    val Minus = lineIcon("minus", "M5 12h14")
    val Pin = lineIcon("pin", "M20 10c0 5-5.5 10.2-7.4 11.8a1 1 0 0 1-1.2 0C9.5 20.2 4 15 4 10a8 8 0 0 1 16 0z", "M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6z")
    val Lock = lineIcon("lock", "M7 11V7a5 5 0 0 1 10 0v4", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M12 17.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z")
    val Help = lineIcon("help", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3", "M12 17h.01")
    val Calendar = lineIcon("cal", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M16 2v4", "M8 2v4", "M3 10h18", "M10 15h4")
    val Check = lineIcon("check", "M20 6 9 17l-5-5")
    val ShieldCheck = lineIcon("shield", "M20 13c0 5-3.5 7.5-7.7 9a1 1 0 0 1-.6 0C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.2-2.7a1.2 1.2 0 0 1 1.6 0C14.5 3.8 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4")
    val UserPlus = lineIcon("userplus", "M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M2 21v-2a4 4 0 0 1 4-4h6", "M19 14v6", "M16 17h6")
    val User = lineIcon("user", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M5 21a7 7 0 0 1 14 0")
    val Alert = lineIcon("alert", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M12 8v4", "M12 16h.01")
    val Close = lineIcon("close", "M18 6 6 18", "M6 6l12 12")
    val Eye = lineIcon("eye", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z")
    val EyeOff = lineIcon("eyeoff", "M10.7 5.1A10 10 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-2.2 3.2", "M6.6 6.6A17 17 0 0 0 2 12s3.6 7 10 7a9.7 9.7 0 0 0 5.4-1.6", "M9.9 9.9a3 3 0 0 0 4.2 4.2", "M2 2l20 20")
}

/** Ícono de un servicio por nombre (los personalizados usan el más genérico). */
internal fun serviceIconOf(name: String): ImageVector = when (name) {
    "Luz" -> AIc.Zap
    "Agua" -> AIc.Drop
    "Gas" -> AIc.Wind
    "Internet" -> AIc.Wifi
    "Cable" -> AIc.Monitor
    "Mantenimiento" -> AIc.Gear
    "Estacionamiento" -> AIc.Truck
    else -> AIc.Plus
}

/** Logo «G» de Google a color. */
internal val GoogleG: ImageVector by lazy {
    val b = ImageVector.Builder(name = "GoogleG", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 48f, viewportHeight = 48f)
    listOf(
        0xFFEA4335 to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
        0xFF4285F4 to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
        0xFFFBBC05 to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
        0xFF34A853 to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
    ).forEach { (c, d) -> b.addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color(c))) }
    b.build()
}

// ---------------- Estado compartido de los flujos ----------------

internal data class AuthToast(val text: String, val kind: Kind = Kind.Error, val id: Long = System.nanoTime()) {
    enum class Kind { Error, Success, Neutral }
}

/** «Espere un momento» y avisos arriba de la pantalla. */
internal class AuthUi(private val scope: CoroutineScope) {
    var loading by mutableStateOf(false)
    var toast by mutableStateOf<AuthToast?>(null)
    /**
     * Muestra el indicador de carga mientras corre [then] (consultas a la base de datos incluidas).
     * Dura al menos [ms] para que no parpadee; si algo falla, avisa con un mensaje.
     */
    fun busy(ms: Long = 500, then: suspend () -> Unit) {
        if (loading) return
        scope.launch {
            toast = null
            loading = true
            try {
                delay(ms)
                then()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error(Cloud.message(e))
            } finally {
                loading = false
            }
        }
    }
    fun error(msg: String) { toast = AuthToast(msg, AuthToast.Kind.Error) }
    fun info(msg: String) { toast = AuthToast(msg, AuthToast.Kind.Success) }
    fun neutral(msg: String) { toast = AuthToast(msg, AuthToast.Kind.Neutral) }
}

// ---------------- Estructura de pantallas ----------------

@Composable
internal fun AuthBackground() {
    Image(
        painter = painterResource(R.drawable.auth_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    )
}

/** Íconos claros en la barra de estado (sobre la foto u oscuros) o normales en pantallas blancas. */
@Composable
internal fun StatusIcons(light: Boolean, resetOnDispose: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(light) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctrl?.isAppearanceLightStatusBars = !light
        onDispose { if (resetOnDispose) ctrl?.isAppearanceLightStatusBars = true }
    }
}

/** Botón volver translúcido sobre la foto. */
@Composable
internal fun BackDark(onBack: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) { Icon(AIc.Back, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(20.dp)) }
}

/** Botón volver gris en pantallas blancas. */
@Composable
internal fun BackLight(onBack: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(AK.BackGray).clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) { Icon(AIc.Back, contentDescription = "Volver", tint = AK.Title, modifier = Modifier.size(22.dp)) }
}

/**
 * Foto fija + barra (volver, versión) + hoja blanca pegada abajo que crece con su contenido.
 * Si no entra (o aparece el teclado) se desplaza.
 */
@Composable
internal fun SheetPage(
    version: String,
    onBack: (() -> Unit)?,
    onVersionTap: () -> Unit = {},
    centered: Boolean = false,
    below: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val minH = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = minH),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp).height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) BackDark(onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "Versión $version",
                    style = TextStyle(fontFamily = Geist, fontSize = 12.sp, color = Color.White),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onVersionTap).padding(4.dp)
                )
            }
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(SheetShape)
                        .background(Color.White)
                        .then(if (below == null) Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                        content = content
                    )
                }
                below?.invoke()
            }
        }
    }
}

/** Pantalla blanca con volver gris arriba y un botón (o pie) fijo abajo. */
@Composable
internal fun WhitePage(
    onBack: (() -> Unit)?,
    scroll: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
            if (onBack != null) BackLight(onBack) else Spacer(Modifier.height(44.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 24.dp),
            content = content
        )
        Column(Modifier.fillMaxWidth().navigationBarsPadding(), content = footer)
    }
}

/** Etiqueta «Paso N - …»: verde (1), ámbar (2) o verde azulado (3). */
@Composable
internal fun StepPill(step: Int) {
    val (bg, fg, t) = when (step) {
        1 -> Triple(AK.PillGreenBg, AK.PillGreen, "Paso 1 - Tu propiedad")
        2 -> Triple(AK.PillAmberBg, AK.PillAmber, "Paso 2 - Unidades y servicios")
        else -> Triple(Palette.TealSoft, Palette.Teal, "Paso 3 - Tu plan")
    }
    Text(
        t,
        style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = fg),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/**
 * Asistente de configuración: volver gris, etiqueta de paso, título, contenido que se desplaza
 * y pie con barra de 3 tramos + Atrás / Siguiente.
 */
@Composable
internal fun WizardPage(
    step: Int,
    progress: Float,
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String = "Siguiente",
    nextEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    WhitePage(
        onBack = onBack,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..3) {
                    val fill = when {
                        i < step -> 1f
                        i == step -> progress
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(AK.Border)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Palette.Teal))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Atrás",
                    style = TButton.copy(color = AK.Title),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(vertical = 8.dp, horizontal = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                AuthButton(nextLabel, nextEnabled, onNext, modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 104.dp), padH = 20.dp)
            }
        }
    ) {
        Spacer(Modifier.height(24.dp))
        StepPill(step)
        Spacer(Modifier.height(12.dp))
        Text(title, style = TTitle)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = TBody)
        }
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

/** Pantalla de éxito: ícono en círculo verde con halo, título, texto y botón abajo. */
@Composable
internal fun SuccessPage(icon: ImageVector, title: String, body: @Composable () -> Unit, button: String, onBack: (() -> Unit)?, onClick: () -> Unit) {
    WhitePage(
        onBack = onBack,
        scroll = false,
        footer = { Box(Modifier.padding(24.dp)) { AuthButton(button, true, onClick) } }
    ) {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(Palette.TealSoft).popIn(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Palette.Teal), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(title, style = TTitle.copy(fontSize = 22.sp, lineHeight = 28.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            body()
        }
        Spacer(Modifier.weight(1.3f))
    }
}

// ---------------- Controles ----------------

@Composable
internal fun FieldLabel(label: String, required: Boolean = true) {
    Row {
        Text(label, style = TLabel)
        if (required) Text(" *", style = TLabel.copy(color = Palette.Red))
    }
}

/** Campo: borde verde y sombra al escribir, rojo con mensaje si hay error. */
@Composable
internal fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    password: Boolean = false,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    onFocus: (Boolean) -> Unit = {},
    onIme: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val isError = error != null
    val borderColor by animateColorAsState(
        when {
            isError -> Palette.Red
            focused -> Palette.Teal
            else -> AK.Border
        }, label = "border"
    )
    val iconTint = when {
        isError -> Palette.Red
        focused -> Palette.Teal
        else -> AK.Body
    }
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            FieldLabel(label, required)
            Spacer(Modifier.height(6.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TField,
            cursorBrush = SolidColor(Palette.Teal),
            visualTransformation = if (password && !visible) PasswordVisualTransformation('•') else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onIme() }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(
                            if (focused && !isError) Modifier.shadow(
                                8.dp, CtlShape, ambientColor = Palette.Teal.copy(alpha = 0.25f), spotColor = Palette.Teal.copy(alpha = 0.3f)
                            ) else Modifier
                        )
                        .background(Color.White, CtlShape)
                        .border(if (focused && !isError) 2.dp else 1.dp, borderColor, CtlShape)
                        .padding(start = 14.dp, end = if (password) 4.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = TField.copy(color = AK.Placeholder))
                        inner()
                    }
                    if (password) {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) AIc.EyeOff else AIc.Eye,
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = iconTint, modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, style = TSmall.copy(color = Palette.Red))
        }
    }
}

/** Botón principal verde azulado; gris (#D1D5DB) mientras falte completar algo. */
@Composable
internal fun AuthButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    padH: Dp = 0.dp,
    textStyle: TextStyle = TButton
) {
    val bg by animateColorAsState(if (enabled) Palette.Teal else AK.Disabled, label = "btn")
    Row(
        modifier
            .height(height)
            .then(if (enabled) Modifier.pressScale(0.98f) else Modifier)
            .clip(CtlShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = padH),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = textStyle, color = Color.White)
    }
}

/** Botón blanco con borde (Cancelar, Continuar con Google…). */
@Composable
internal fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), height: Dp = 48.dp, lead: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .height(height)
            .pressScale(0.98f)
            .clip(CtlShape)
            .background(Color.White)
            .border(1.dp, AK.Border, CtlShape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (lead != null) {
            lead()
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = TField.copy(color = AK.Label))
    }
}

@Composable
internal fun GoogleButton(onClick: () -> Unit) {
    OutlineButton("Continuar con Google", onClick, height = 56.dp, lead = {
        Image(GoogleG, contentDescription = null, modifier = Modifier.size(20.dp))
    })
}

@Composable
internal fun OrDivider(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = AK.DividerLine)
        Text(text, style = TLabel.copy(color = AK.DividerText), modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = AK.DividerLine)
    }
}

/** «¿Nuevo en Convive? Crear cuenta» y similares. */
@Composable
internal fun LinkLine(prefix: String, link: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("$prefix ", style = TBody.copy(color = AK.Label))
        Text(link, style = TLink, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp))
    }
}

@Composable
internal fun CheckSquare(checked: Boolean, size: Dp = 20.dp) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (checked) Palette.Teal else Color.White)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, AK.Disabled, shape)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(AIc.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.7f))
    }
}

@Composable
internal fun RadioDot(on: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (on) Palette.Teal else Color.White)
            .then(if (on) Modifier else Modifier.border(2.dp, AK.Border, CircleShape)),
        contentAlignment = Alignment.Center
    ) { if (on) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White)) }
}

/** Tarjeta elegible (rol, tipo de propiedad, opción): borde verde azulado de 2dp al elegirla. */
@Composable
internal fun ChoiceTile(
    title: String,
    desc: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconBox: Dp = 44.dp,
    titleStyle: TextStyle = TName.copy(fontWeight = FontWeight.Medium)
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.Teal else AK.Border, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(iconBox).clip(RoundedCornerShape(10.dp)).background(AK.Tint), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(iconBox * 0.55f))
            }
            Spacer(Modifier.width(14.dp))
        } else Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = titleStyle)
            if (desc != null) {
                Spacer(Modifier.height(2.dp))
                Text(desc, style = TSmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        RadioDot(selected)
    }
}

@Composable
internal fun CodeBoxes(code: String, invalid: Boolean, modifier: Modifier = Modifier) {
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "blink"
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) { i ->
            val ch = code.getOrNull(i)
            val active = !invalid && i == code.length
            val color = when {
                invalid -> Palette.Red
                active -> Palette.Teal
                else -> AK.Border
            }
            Box(
                Modifier.weight(1f).height(52.dp).clip(CtlShape).background(Color.White).border(if (active) 2.dp else 1.dp, color, CtlShape),
                contentAlignment = Alignment.Center
            ) {
                if (ch != null) {
                    Text(ch.toString(), style = TextStyle(fontFamily = Geist, fontSize = 22.sp, color = AK.Title), modifier = Modifier.popIn())
                } else if (active) {
                    Box(Modifier.width(2.dp).height(22.dp).graphicsLayer { alpha = blink }.background(Palette.Teal))
                }
            }
        }
    }
}

/** Teclado numérico propio (código y llave). */
@Composable
internal fun NumberPad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
    val keyShape = RoundedCornerShape(6.dp)
    Column(
        Modifier.fillMaxWidth().background(AK.Keypad).navigationBarsPadding().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(46.dp)) {
                        when (k) {
                            "" -> Unit
                            "del" -> Box(Modifier.fillMaxSize().clip(keyShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Borrar", tint = Color(0xFF1F2937))
                            }
                            else -> Box(
                                Modifier.fillMaxSize().shadow(1.dp, keyShape).clip(keyShape).background(Color.White).clickable { onDigit(k[0]) },
                                contentAlignment = Alignment.Center
                            ) { Text(k, fontSize = 24.sp, color = Color(0xFF111827)) }
                        }
                    }
                }
            }
        }
    }
}

/** Avatar de iniciales (PQ). */
@Composable
internal fun InitialsAvatar(name: String, size: Dp = 40.dp) {
    val ini = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size).clip(CircleShape).background(Palette.NavySoft), contentAlignment = Alignment.Center) {
        Text(ini, style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AK.Title))
    }
}

// ---------------- Capas encima ----------------

@Composable
internal fun BoxScope.ToastHost(ui: AuthUi) {
    val t = ui.toast
    val last = remember { mutableStateOf<AuthToast?>(null) }
    SideEffect { if (t != null) last.value = t }
    LaunchedEffect(t?.id) {
        if (t != null) {
            delay(4500)
            if (ui.toast?.id == t.id) ui.toast = null
        }
    }
    AnimatedVisibility(
        visible = t != null,
        modifier = Modifier.align(Alignment.TopCenter),
        enter = slideInVertically(tween(320, easing = EaseOutCubic)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(240)) { -it } + fadeOut()
    ) {
        val shown = t ?: last.value ?: return@AnimatedVisibility
        val (bg, accent, close) = when (shown.kind) {
            AuthToast.Kind.Error -> Triple(Palette.RedSoft, Palette.Red, Palette.Red)
            AuthToast.Kind.Success -> Triple(Palette.TealSoft, Palette.Teal, Palette.Teal)
            AuthToast.Kind.Neutral -> Triple(Color(0xFFF1F3F6), AK.Title, AK.Title)
        }
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                    Icon(if (shown.kind == AuthToast.Kind.Success) AIc.Check else AIc.Alert, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(shown.text, style = TBody.copy(color = AK.Label), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                IconButton(onClick = { ui.toast = null }) {
                    Icon(AIc.Close, contentDescription = "Cerrar", tint = close, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
internal fun LoadingOverlay(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp, trackColor = Color.White.copy(alpha = 0.2f))
                Spacer(Modifier.height(18.dp))
                Text("Espere un momento", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

/** Hoja inferior genérica sobre un velo oscuro. */
@Composable
internal fun BottomSheetOverlay(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(360, easing = EaseOutCubic)) { it },
        exit = slideOutVertically(tween(260)) { it }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(SheetShape)
                    .background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(CircleShape).background(AK.Disabled))
                Spacer(Modifier.height(20.dp))
                content()
            }
        }
    }
}

