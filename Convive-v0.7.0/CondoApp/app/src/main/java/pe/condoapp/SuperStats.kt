package pe.condoapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate

// ------------------------------------------------------------------
// Estadísticas del Home del Super Admin: medios de pago y descargas.
// ------------------------------------------------------------------

private val YapeColor = Color(0xFF742284)
private val PlinColor = Color(0xFF00A5CF)

private fun methodColor(m: String) = when (m) {
    "Yape" -> YapeColor
    "Plin" -> PlinColor
    else -> Palette.Navy
}

private fun methodShort(m: String) = if (m.startsWith("Tarjeta")) "Tarjeta" else m

private fun methodIcon(m: String): ImageVector = if (m.startsWith("Tarjeta")) Icons.Rounded.CreditCard else Icons.Rounded.PhoneIphone

/** Tarjeta blanca con título y subtítulo, igual que "Clientes por cobrar". */
@Composable
private fun StatsCard(title: String, subtitle: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF4A5160))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        }
        content()
    }
}

/** Progreso 0→1 al aparecer, con retraso opcional (barras que crecen en cascada). */
@Composable
private fun growIn(delayMs: Long = 0, durationMs: Int = 900): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        a.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return a.value
}

// ---------------- Medios de pago ----------------

@Composable
fun PaymentMethodsCard(modifier: Modifier = Modifier) {
    val stats = Store.paymentMethodStats()
    val totalClients = stats.sumOf { it.clients }
    StatsCard("Medios de pago", "Con qué te pagan tus clientes la suscripción", modifier) {
        if (totalClients == 0) {
            Text("Aún no hay clientes de pago.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            return@StatsCard
        }
        // Barra dividida por medio de pago
        val g = growIn()
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Palette.Field)
                .graphicsLayer { scaleX = g; transformOrigin = TransformOrigin(0f, 0.5f) },
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            stats.filter { it.clients > 0 }.forEach { st ->
                Box(Modifier.weight(st.clients.toFloat()).fillMaxHeight().background(methodColor(st.method)))
            }
        }
        stats.forEachIndexed { i, st ->
            MethodRow(st, st.clients.toFloat() / totalClients, i)
        }
    }
}

@Composable
private fun MethodRow(st: Store.MethodStat, share: Float, index: Int) {
    val color = methodColor(st.method)
    val g = growIn(250L + index * 140L)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(methodIcon(st.method), contentDescription = null, tint = color, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    methodShort(st.method),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(share * 100 * g).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = color
                )
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Palette.Field)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((share * g).coerceIn(0f, 1f)).clip(CircleShape).background(color))
            }
            Text(
                "${st.clients} ${if (st.clients == 1) "cliente" else "clientes"} · ${money(st.amount)} en el año",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------- Descargas de la app ----------------

/** Total de descargas y cuántas personas la descargan por día (últimos 7 días). */
@Composable
fun DownloadsCard(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val dates = Store.activations.values.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    val days = (6 downTo 0).map { d -> today.minusDays(d.toLong()).let { day -> day to dates.count { it == day } } }
    val week = days.sumOf { it.second }
    val todayCount = days.last().second
    val avg = week / 7.0

    StatsCard("Descargas de la app", "Personas que instalaron Convive", modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIcon(Icons.Rounded.Download, 44.dp)
            Column(Modifier.weight(1f)) {
                CountUpText(
                    dates.size.toDouble(),
                    { v -> v.toInt().toString() },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    color = Palette.Ink
                )
                Text("descargas en total", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MiniStat("$todayCount", "Hoy", Modifier.weight(1f))
            MiniStat(String.format(java.util.Locale.US, "%.1f", avg), "Promedio por día", Modifier.weight(1f))
        }

        DailyDownloads(days)
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Palette.Field).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Palette.Ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Descargas por día de los últimos 7 días. Hoy va en ámbar. */
@Composable
private fun DailyDownloads(days: List<Pair<LocalDate, Int>>) {
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val names = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Descargas por día · últimos 7 días", style = MaterialTheme.typography.labelLarge, color = Palette.Ink)
        Row(
            Modifier.fillMaxWidth().height(104.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { i, (day, n) ->
                val g = growIn(300L + i * 80L, 700)
                val isToday = i == days.lastIndex
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("$n", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Palette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .fillMaxHeight((n.toFloat() / max).coerceAtLeast(0.05f))
                            .graphicsLayer { scaleY = g; transformOrigin = TransformOrigin(0.5f, 1f) }
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isToday) Palette.Amber else if (n == 0) Palette.Line else Palette.Teal)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isToday) "Hoy" else names[day.dayOfWeek.value - 1],
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                        color = Palette.Muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
