package pe.condoapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ------------------------------------------------------------------
// ADMINISTRADOR · Mi perfil (con la suscripción que se paga a Convive)
// ------------------------------------------------------------------

private fun monthLabel(period: String): String {
    if (period.startsWith("A")) {
        val y = YearMonth.parse(period.removePrefix("A"))
        return "Año desde " + y.month.getDisplayName(TextStyle.SHORT, Locale("es", "PE")) + " " + y.year
    }
    val ym = YearMonth.parse(period)
    val m = ym.month.getDisplayName(TextStyle.FULL, Locale("es", "PE"))
    return m.replaceFirstChar { it.uppercase() } + " " + ym.year
}

@Composable
fun AdminMyProfile(user: AppUser, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = Store.clientOf(user) ?: return
    val owner = Store.ownerOf(c)
    val isOwner = owner?.id == user.id
    val st = Store.subStatus(c)
    val period = Store.periodFor(c)
    var confirmLogout by remember { mutableStateOf(false) }
    var askHelp by remember { mutableStateOf(false) }

    ScreenColumn {
        Row(
            Modifier.clip(CircleShape).clickable(onClick = onBack).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Ic.ChevronLeft, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
            Text("Volver", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
        }

        DarkCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(user.name, size = 56.dp, bg = Color.White.copy(alpha = 0.14f), fg = Color.White)
                Column(Modifier.weight(1f)) {
                    Text("Mi perfil", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Text(user.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@" + user.username, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroInfo("Rol", "Administrador", Modifier.weight(1f))
                HeroInfo("Cliente", c.name, Modifier.weight(1.4f))
            }
        }

        // ---------------- Suscripción ----------------
        SectionTitle("Mi suscripción", trailing = c.plan.label.substringBefore(" ·"))
        when {
            c.plan == Plan.GRATIS -> AppCard(color = Palette.TealSoft, borderColor = Palette.TealSoft) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Ic.Gift, Palette.Teal, Palette.Surface)
                    Column(Modifier.weight(1f)) {
                        val trial = Store.planOf(c)?.trial == true
                        Text(if (trial) "Plan de prueba" else "Plan gratis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (trial) "Te quedan ${Store.trialDaysLeft(c)} días de prueba."
                            else "Tienes todas las funciones de pago sin costo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                    Text(money(0.0), style = MaterialTheme.typography.titleMedium)
                }
            }

            isOwner -> {
                val color = when (st) {
                    SubStatus.PAGADO -> Palette.Green
                    SubStatus.VENCIDO -> Palette.Red
                    else -> Palette.AmberDark
                }
                AppCard(padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Estás pagando", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(money(c.monthlyFee), style = MaterialTheme.typography.headlineMedium)
                                Text(if (c.plan == Plan.ANUAL) "/año" else "/mes", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        SubPill(st)
                    }
                    LineDivider()
                    Row(Modifier.fillMaxWidth()) {
                        MiniStat("Mes", monthLabel(period), Modifier.weight(1f))
                        MiniStat(
                            if (st == SubStatus.PAGADO) "Próximo pago" else "Vence el",
                            (if (st == SubStatus.PAGADO) Store.nextDue(period) else Store.subDue(period)).format(dateFmt),
                            Modifier.weight(1f)
                        )
                    }
                    Text(
                        when (st) {
                            SubStatus.PAGADO -> "Gracias. Tu pago de este mes ya fue confirmado."
                            SubStatus.VENCIDO -> "Tu pago de este mes está vencido. Paga a Convive para mantener tu servicio."
                            else -> "Paga a Convive antes del día ${Store.SUB_DUE_DAY}. La confirmación aparecerá aquí."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }

                SectionTitle("Historial de pagos")
                // Mes actual + meses pagados (los más recientes primero)
                val periods = (listOf(period) + Store.subscriptionPayments.filter { it.clientId == c.id }.map { it.period })
                    .distinct().sortedDescending().take(6)
                AppCard(padding = 14.dp) {
                    periods.forEachIndexed { k, p ->
                        val pay = Store.subPayment(c.id, p)
                        val pst = Store.subStatus(c, p)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(monthLabel(p), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (pay != null) "${money(pay.amount)} · pagado el ${pay.paidAt}" else money(c.monthlyFee),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Palette.Muted
                                )
                            }
                            SubPill(pst)
                        }
                        if (k < periods.lastIndex) LineDivider()
                    }
                }
            }

            else -> AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Ic.Cash, Palette.Navy, Palette.NavySoft)
                    Column(Modifier.weight(1f)) {
                        Text("La paga ${owner?.name ?: "otro administrador"}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${owner?.name ?: "Otro administrador"} es responsable del pago de la suscripción. Tú tienes las mismas funciones de administrador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                }
            }
        }

        // ---------------- Equipo ----------------
        val admins = Store.adminsOf(c.id)
        SectionTitle("Administradores", trailing = "${admins.size}")
        admins.forEach { a ->
            key(a.id) {
                AppCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(a.name, bg = roleBg(a.role), fg = roleFg(a.role))
                        Column(Modifier.weight(1f)) {
                            Text(a.name + if (a.id == user.id) " (tú)" else "", style = MaterialTheme.typography.titleMedium)
                            Text("@" + a.username, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        if (Store.isOwner(a)) Pill("Paga la suscripción", Palette.Navy, Palette.NavySoft, Ic.Card)
                        else Pill("Administrador", roleFg(a.role), roleBg(a.role))
                    }
                }
            }
        }

        // ---------------- Cuenta ----------------
        // Ayuda: le llega al Super Admin como notificación
        SectionTitle("¿Necesitas ayuda?")
        AppCard(onClick = { askHelp = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Ic.Headset, Palette.Teal, Palette.TealSoft)
                Column(Modifier.weight(1f)) {
                    Text("Escríbele al equipo de Convive", style = MaterialTheme.typography.titleMedium)
                    Text("Te responderemos lo antes posible.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                }
                Pill("Pedir ayuda", Palette.Teal, Palette.TealSoft)
            }
        }

        SectionTitle("Cuenta")
        SecondaryButton("Ver introducción", { Store.resetTips(); UiState.showOnboarding = true }, Modifier.fillMaxWidth(), icon = Ic.Help)
        PrimaryButton(
            "Cerrar sesión",
            { confirmLogout = true },
            Modifier.fillMaxWidth(),
            color = Palette.Red,
            icon = Ic.Power
        )
        Spacer(Modifier.height(8.dp))
    }

    if (askHelp) HelpRequestDialog(user) { askHelp = false }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "¿Quieres salir de tu cuenta?",
            confirm = "Salir",
            onConfirm = {
                UiState.myProfile = false
                Store.logout()
            },
            onDismiss = { confirmLogout = false },
            danger = false
        )
    }
}
