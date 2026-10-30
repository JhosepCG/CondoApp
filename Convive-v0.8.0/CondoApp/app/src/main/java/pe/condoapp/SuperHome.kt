package pe.condoapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

// ------------------------------------------------------------------
// SUPER ADMIN · Home (según el diseño "Index" de Figma)
// ------------------------------------------------------------------

/** Degradado de marca: azul noche abajo-izquierda → verde azulado arriba-derecha. */
val BrandGradient: Brush
    get() = Brush.linearGradient(
        listOf(Palette.Navy, Palette.Teal),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

private val CardLine = Color(0xFFF2F2F2)

/** Encabezado del Super Admin: foto, saludo, campana de notificaciones y botón para salir. */
@Composable
fun SuperHeader(
    user: AppUser,
    subtitle: String = "Super Admin",
    unread: Int = Store.unreadSupport(),
    onBell: () -> Unit = {
        UiState.openClientId = null
        UiState.newClient = false
        UiState.showNotifications = true
    }
) {
    var confirmLogout by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserAvatar(user, 42.dp, initialsOf = user.name.substringBefore(" "))
        Column(Modifier.weight(1f).padding(start = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "¡Hola ${user.name.substringBefore(" ")}!",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                color = Palette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Palette.Muted)
        }
        // Notificaciones (pedidos de ayuda de los administradores)
        Box(
            Modifier
                .pressScale(0.95f)
                .size(44.dp)
                .clip(CircleShape)
                .background(Palette.Surface)
                .clickable(onClick = onBell),
            contentAlignment = Alignment.Center
        ) {
            Icon(Ic.Bell, contentDescription = "Notificaciones", tint = Palette.Muted, modifier = Modifier.size(22.dp).ring(unread > 0))
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Palette.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unread > 9) "9+" else "$unread", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                }
            }
        }
        // Salir: mismo círculo blanco que la campana
        Box(
            Modifier
                .pressScale(0.95f)
                .size(44.dp)
                .clip(CircleShape)
                .background(Palette.Surface)
                .clickable { confirmLogout = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Ic.Power, contentDescription = "Cerrar sesión", tint = Palette.Muted, modifier = Modifier.size(22.dp))
        }
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "¿Quieres salir de tu cuenta?",
            confirm = "Salir",
            onConfirm = { Store.logout() },
            onDismiss = { confirmLogout = false },
            danger = false
        )
    }
}

/** Barra inferior del Super Admin con el botón + al centro (nuevo cliente). */
@Composable
fun SuperNavBar(
    tabs: List<Tab>,
    current: Tab?,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit,
    centerIcon: ImageVector? = null,
    centerLabel: String = "Nuevo cliente",
    coachKey: String = "sa_fab",
    coachText: String = "Toca + para crear un cliente nuevo con su administrador.",
    labelOf: (Tab) -> String = { it.label },
    iconOf: (Tab, Boolean) -> ImageVector = ::superTabIcon,
    dotOn: (Tab) -> Boolean = { false }
) {
    val half = (tabs.size + 1) / 2
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(0.dp), ambientColor = Palette.Navy.copy(alpha = 0.18f), spotColor = Palette.Navy.copy(alpha = 0.18f))
                .background(Palette.Surface)
                .navigationBarsPadding()
                .height(71.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.take(half).forEach { t -> SuperNavItem(labelOf(t), iconOf(t, t == current), t == current, dotOn(t), Modifier.weight(1f)) { onSelect(t) } }
            Spacer(Modifier.weight(1f))
            tabs.drop(half).forEach { t -> SuperNavItem(labelOf(t), iconOf(t, t == current), t == current, dotOn(t), Modifier.weight(1f)) { onSelect(t) } }
        }
        // Botón + (nuevo cliente)
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .pressScale(0.95f)
                .size(65.dp)
                .clip(CircleShape)
                .background(Palette.GreenSoft)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Palette.Teal)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            if (centerIcon != null) {
                Icon(centerIcon, contentDescription = centerLabel, tint = Color.White, modifier = Modifier.size(26.dp))
            } else {
                androidx.compose.foundation.Canvas(Modifier.size(15.dp)) {
                    val w = 2.dp.toPx()
                    drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w, androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).size(65.dp)) {
            CoachMark(coachKey, coachText)
        }
    }
}

@Composable
private fun SuperNavItem(label: String, icon: ImageVector, selected: Boolean, dot: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) Palette.Teal else Palette.Muted.copy(alpha = 0.5f)
    val k by androidx.compose.animation.core.animateFloatAsState(
        if (selected) 1.08f else 1f,
        androidx.compose.animation.core.tween(Motion.QUICK_MS, easing = Motion.EaseOut),
        label = "tab"
    )
    Column(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = k; scaleY = k })
            if (dot) {
                Box(Modifier.align(Alignment.TopEnd).popIn().size(9.dp).clip(CircleShape).background(Palette.Red))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/** Íconos de la barra del Super Admin: rellenos si están activos, de línea si no (como en Figma). */
fun superTabIcon(t: Tab, selected: Boolean): ImageVector = when (t) {
    Tab.RESUMEN, Tab.PANEL -> if (selected) Ic.HomeFill else Ic.Home
    Tab.COBROS -> if (selected) Ic.CardFill else Ic.Card
    Tab.SERVICIOS -> Ic.AlertOctagon
    Tab.CLIENTES -> if (selected) Ic.UsersFill else Ic.Users
    Tab.PLANES -> if (selected) Ic.CardFill else Ic.Card
    Tab.MI_PERFIL -> if (selected) Ic.UserFill else Ic.User
    else -> t.icon
}

/** Ícono blanco sobre cuadrado con el degradado de marca. */
@Composable
fun GradientIcon(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(BrandGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

fun templateShort(t: Template): String = if (t == Template.CONDOMINIO) "Condominio" else "Casa"

/** Fila de cliente (suscriptor) con su estado del mes. */
@Composable
fun SubscriberRow(c: Client, onClick: () -> Unit) {
    val st = Store.subStatus(c)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Surface)
            .border(1.dp, CardLine, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.TealSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (c.template == Template.CONDOMINIO) Ic.Building else Ic.Home,
                contentDescription = null,
                tint = Palette.Teal,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SubPill(st)
            Text(
                c.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                templateShort(c.template) + " · " + Store.feeLabel(c),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Ic.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun SuperHome(user: AppUser, go: (Tab) -> Unit) {
    var q by remember { mutableStateOf("") }
    val year = LocalDate.now().year.toString()
    val period = Store.periodOf()
    val collectedYear = Store.subscriptionPayments.filter { it.period.startsWith(year) }.sumOf { it.amount }
    val users = Store.users.count { it.role != Role.SUPER_ADMIN }
    val toCollect = Store.clients.filter {
        it.active && it.plan != Plan.GRATIS &&
            Store.subStatus(it).let { s -> s == SubStatus.PENDIENTE || s == SubStatus.VENCIDO }
    }
    val openClient: (Client) -> Unit = { c ->
        go(Tab.CLIENTES)
        UiState.openClientId = c.id
    }

    ScreenColumn {
        SearchField(q, { q = it }, "Buscar suscriptores")
        if (q.isNotBlank()) {
            val needle = q.trim().lowercase()
            val found = Store.clients.filter { c ->
                c.name.lowercase().contains(needle) || (Store.ownerOf(c)?.name?.lowercase()?.contains(needle) == true)
            }
            if (found.isEmpty()) {
                Text("Sin resultados para «$q».", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            found.forEach { c -> key(c.id) { SubscriberRow(c) { openClient(c) } } }
        }

        // Monto recaudado del año
        Row(
            Modifier
                .enter(1)
                .fillMaxWidth()
                .height(122.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandGradient)
                .padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Monto recaudado del año",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                CountUpText(
                    collectedYear,
                    ::money,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Image(
                painter = painterResource(R.drawable.ill_superadmin_hero),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 98.dp, height = 100.dp).floating(4.dp, 3000)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HomeStat(Ic.Users, "$users", "Usuarios registrados", Modifier.weight(1f).enter(2))
            HomeStat(Ic.Card, money(toCollect.sumOf { it.monthlyFee }), "Por cobrar", Modifier.weight(1f).enter(3))
        }

        // Estadísticas: medios de pago y descargas de la app
        PaymentMethodsCard(Modifier.enter(4))
        DownloadsCard(Modifier.enter(5))

        // Clientes por cobrar
        Column(
            Modifier
                .enter(6)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Clientes por cobrar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF4A5160),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            if (toCollect.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Ic.CheckCircle, Palette.Green, Palette.GreenSoft, 40.dp)
                    Text("Todos los clientes están al día.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                }
            }
            toCollect.forEach { c -> key(c.id) { SubscriberRow(c) { openClient(c) } } }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun HomeStat(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GradientIcon(icon)
        Column {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

