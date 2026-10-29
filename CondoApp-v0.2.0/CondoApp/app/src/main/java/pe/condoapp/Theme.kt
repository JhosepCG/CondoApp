package pe.condoapp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta de CondoApp: azul noche + verde azulado, ámbar para avisos y rojo solo para emergencias. */
object Palette {
    val Navy = Color(0xFF0B1F3A)
    val NavySoft = Color(0xFFE6EBF3)
    val Teal = Color(0xFF0B6E6D)
    val TealSoft = Color(0xFFE0F2F1)
    val Amber = Color(0xFFF2A541)
    val AmberDark = Color(0xFF9A5B00)
    val AmberSoft = Color(0xFFFFF4E0)
    val Red = Color(0xFFC21F35)
    val RedSoft = Color(0xFFFDE8EB)
    val Green = Color(0xFF1E7B45)
    val GreenSoft = Color(0xFFE5F6EC)
    val Bg = Color(0xFFF4F6FA)
    val Surface = Color(0xFFFFFFFF)
    val Field = Color(0xFFF7F8FB)
    val Ink = Color(0xFF14213D)
    val Muted = Color(0xFF5F6673)
    val Line = Color(0xFFE5E8EF)
}

private val AppColors = lightColorScheme(
    primary = Palette.Teal,
    onPrimary = Color.White,
    primaryContainer = Palette.TealSoft,
    onPrimaryContainer = Palette.Navy,
    secondary = Palette.Navy,
    onSecondary = Color.White,
    secondaryContainer = Palette.NavySoft,
    onSecondaryContainer = Palette.Navy,
    tertiary = Palette.Amber,
    onTertiary = Palette.Navy,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Field,
    onSurfaceVariant = Palette.Muted,
    surfaceTint = Palette.Teal,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Palette.Red,
    onError = Color.White,
    surfaceBright = Palette.Surface,
    surfaceDim = Palette.Bg,
    surfaceContainerLowest = Palette.Surface,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.Surface,
    surfaceContainerHighest = Palette.Field
)

private val AppType = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun CondoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppType, shapes = AppShapes, content = content)
}
