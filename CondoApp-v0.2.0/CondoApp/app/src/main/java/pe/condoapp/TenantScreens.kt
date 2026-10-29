package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ------------------------------------------------------------------
// INQUILINO · Inicio
// ------------------------------------------------------------------

@Composable
fun TenantHome(user: AppUser, go: (Tab) -> Unit) {
    val c = Store.clientOf(user) ?: return
    val meterReq = if (Feature.MEDIDORES in c.modules) Store.activeMeterRequest(c.id) else null
    val meterPending = meterReq != null && Store.readingFor(meterReq.id, user.id) == null
    var showDialog by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf<String?>(null) }

    val pendingCharges = Store.charges
        .filter { it.clientId == c.id && it.unit == user.unit && !it.paid }
        .sortedBy { it.due }
    val next = pendingCharges.firstOrNull()
    val confirmed = Store.emergencies.filter { it.clientId == c.id && it.status == EmergencyStatus.CONFIRMADA }
    val mine = Store.emergencies.filter {
        it.clientId == c.id && it.userId == user.id && it.status == EmergencyStatus.PENDIENTE
    }
    val admin = Store.adminsOf(c.id).firstOrNull()
    val responsible = Store.responsibleOf(c.id, user.unit)
    val lastNotice = Store.notices.lastOrNull { it.clientId == c.id }

    ScreenColumn {
        confirmed.forEach { e ->
            AppCard(color = Palette.Red, borderColor = Palette.Red) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(emergencyIcon(e.type), Palette.Red, Color.White)
                    Column(Modifier.weight(1f)) {
                        Text("Emergencia confirmada", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        Text(e.type, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                }
                Text(
                    "Reportada en ${e.unit}. Revisa tus pertenencias y sigue las indicaciones de la administración.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        HeroCard(
            eyebrow = "Mi domicilio",
            title = user.unit ?: "-",
            subtitle = c.name,
            icon = Icons.Rounded.Home
        ) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroInfo("Responsable", responsible ?: "-", Modifier.weight(1f))
                HeroInfo("Administración", admin?.name ?: "-", Modifier.weight(1f))
            }
        }

        if (meterPending && meterReq != null) {
            val late = LocalDate.now().isAfter(meterReq.due)
            AppCard(
                color = if (late) Palette.RedSoft else Palette.AmberSoft,
                borderColor = if (late) Palette.RedSoft else Palette.AmberSoft
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.Bolt, if (late) Palette.Red else Palette.AmberDark, Palette.Surface)
                    Column(Modifier.weight(1f)) {
                        Text("Envía la foto de tu medidor", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (late) "Venció el ${meterReq.due.format(dateFmt)}"
                            else "Tienes hasta el ${meterReq.due.format(dateFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (late) Palette.Red else Palette.AmberDark
                        )
                    }
                }
                PrimaryButton(
                    "Enviar foto",
                    { go(Tab.MEDIDOR) },
                    Modifier.fillMaxWidth(),
                    color = Palette.Navy,
                    icon = Icons.Rounded.PhotoCamera
                )
            }
        }

        if (Feature.COBROS in c.modules) {
            AppCard(padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Próximo pago", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (next != null) {
                        val st = chargeState(next)
                        Pill(st.label, st.fg, st.bg, st.icon)
                    }
                }
                if (next == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.CheckCircle, Palette.Green, Palette.GreenSoft)
                        Column {
                            Text("Estás al día", style = MaterialTheme.typography.titleMedium)
                            Text("No tienes pagos pendientes.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                    }
                } else {
                    Text(money(Store.amountDue(next)), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${next.concept} · vence el ${next.due.format(dateFmt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted
                    )
                    if (Store.amountDue(next) > next.amount) {
                        Pill("Incluye ${pct(c.lateFeePercent)} de recargo por mora", Palette.Red, Palette.RedSoft)
                    }
                    if (pendingCharges.size > 1) {
                        Text(
                            "Tienes ${pendingCharges.size} pagos pendientes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                }
            }
        }

        if (Feature.EMERGENCIA in c.modules) {
            EmergencyButton { showDialog = true }
            sent?.let { msg ->
                Pill(msg, Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
            }
            mine.forEach { e ->
                Text(
                    "Tu alerta «${e.type}» espera la confirmación de la administración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            Text(
                "Esta app no reemplaza a los servicios de emergencia.\nBomberos 116 · Policía 105 · SAMU 106",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (Feature.AVISOS in c.modules && lastNotice != null) {
            SectionTitle("Último aviso")
            NoticeCard(lastNotice)
        }
    }

    if (showDialog) {
        EmergencyDialog(
            onPick = { t ->
                Store.addEmergency(user, t)
                sent = "Alerta enviada a la administración"
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun EmergencyButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFE0394E), Palette.Red)))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Palette.Red)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Botón de emergencia", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "Gas, incendio, robo u otra emergencia",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun EmergencyDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        shape = RoundedCornerShape(28.dp),
        icon = { IconBadge(Icons.Rounded.Warning, Palette.Red, Palette.RedSoft, 52.dp) },
        title = { Text("¿Qué está pasando?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Primero se avisa a la administración para confirmar que no sea una falsa alarma.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Muted
                )
                EMERGENCY_TYPES.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.RedSoft)
                            .clickable { onPick(t) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(emergencyIcon(t), contentDescription = null, tint = Palette.Red)
                        Text(t, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

// ------------------------------------------------------------------
// INQUILINO · Reportar fallas de servicios
// ------------------------------------------------------------------

@Composable
fun TenantServicios(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var service by remember { mutableStateOf(SERVICES.first()) }
    var desc by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val mine = Store.incidents.filter { it.clientId == c.id && it.userId == user.id }.reversed()

    ScreenColumn {
        AppCard(padding = 18.dp) {
            Text("Reportar una falla", style = MaterialTheme.typography.titleLarge)
            Text(
                "Avisa a la administración antes de que se vuelva un problema mayor.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted
            )
            ChoiceChips(SERVICES, service, icons = SERVICES.associateWith { serviceIcon(it) }) { service = it }
            AppTextField(
                desc,
                { desc = it; sent = false },
                "¿Qué está fallando?",
                singleLine = false,
                minLines = 3
            )
            if (sent) Pill("Reporte enviado", Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
            PrimaryButton(
                "Enviar a la administración",
                {
                    Store.addIncident(user, service, desc)
                    desc = ""
                    sent = true
                },
                Modifier.fillMaxWidth(),
                enabled = desc.isNotBlank()
            )
        }

        SectionTitle("Mis reportes", trailing = "${mine.size}")
        if (mine.isEmpty()) EmptyState(Icons.Rounded.Build, "No has enviado reportes.")
        mine.forEach { inc ->
            key(inc.id) {
                AppCard {
                    IncidentHeader(inc, inc.createdAt)
                    if (inc.description.isNotBlank()) Text(inc.description, style = MaterialTheme.typography.bodyMedium)
                    if (inc.status == IncidentStatus.RESUELTO && inc.confirmed != true) {
                        LineDivider()
                        Text(
                            "La administración dice que ya está resuelto. ¿Es cierto?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Sigue fallando", { Store.confirmIncident(inc.id, false) }, Modifier.weight(1f))
                            PrimaryButton(
                                "Sí, resuelto",
                                { Store.confirmIncident(inc.id, true) },
                                Modifier.weight(1f),
                                color = Palette.Green
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// INQUILINO · Avisos
// ------------------------------------------------------------------

@Composable
fun TenantAvisos(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.notices.filter { it.clientId == c.id }.reversed()
    ScreenColumn {
        SectionTitle("Avisos", trailing = "${list.size}")
        Text(
            "Cortes de agua, luz y otras novedades de tu edificio.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Muted
        )
        if (list.isEmpty()) EmptyState(Icons.Rounded.Campaign, "No hay avisos por ahora.")
        list.forEach { n -> key(n.id) { NoticeCard(n) } }
    }
}
