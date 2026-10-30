package pe.condoapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ------------------------------------------------------------------
// Perfil de inquilino (Administrador y Super Admin)
// ------------------------------------------------------------------

@Composable
fun TenantProfile(viewer: AppUser, tenantId: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val u = Store.users.firstOrNull { it.id == tenantId }
    if (u == null) {
        LaunchedEffect(tenantId) { onBack() }
        return
    }
    val c = Store.client(u.clientId)
    val st = tenantStatus(u)
    val charges = Store.charges.filter { it.clientId == u.clientId && it.unit == u.unit }
        .sortedWith(compareBy<Charge> { it.paid }.thenByDescending { it.due })
    val incidents = Store.incidents.filter { it.userId == u.id }.reversed()
    val readings = Store.meterReadings.filter { it.userId == u.id }.reversed()
    val emergencies = Store.emergencies.filter { it.userId == u.id }.reversed()
    // Medidores: solo el administrador de la casa (el Super Admin no los ve).
    val showMeters = viewer.role == Role.ADMIN && Store.featureOn(c, Feature.MEDIDORES)
    val req = if (showMeters && c != null) Store.activeMeterRequest(c.id) else null
    var viewing by remember { mutableStateOf<MeterReading?>(null) }
    var payId by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        Row(
            Modifier.clip(CircleShape).clickable(onClick = onBack).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
            Text("Volver", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
        }

        DarkCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(u.name, size = 56.dp, bg = Color.White.copy(alpha = 0.14f), fg = Color.White)
                Column(Modifier.weight(1f)) {
                    Text(u.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(u.unit, c?.name).joinToString(" · "),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroInfo("Usuario", "@" + u.username, Modifier.weight(1f))
                HeroInfo("Responsable", Store.responsibleOf(u.clientId ?: "", u.unit) ?: "-", Modifier.weight(1f))
            }
        }

        // Semáforo: pagos, medidor, reportes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusTile(
                "Pagos",
                when {
                    st.overdue > 0 -> "Vencido"
                    !st.upToDate -> "Pendiente"
                    else -> "Al día"
                },
                Icons.Rounded.Payments,
                when {
                    st.overdue > 0 -> Palette.Red
                    !st.upToDate -> Palette.AmberDark
                    else -> Palette.Green
                },
                Modifier.weight(1f)
            )
            if (showMeters) {
                StatusTile(
                    "Medidor",
                    when (st.meterSent) {
                        true -> "Enviado"
                        false -> "Falta foto"
                        null -> "Sin pedido"
                    },
                    Icons.Rounded.Bolt,
                    when (st.meterSent) {
                        true -> Palette.Green
                        false -> Palette.AmberDark
                        null -> Palette.Muted
                    },
                    Modifier.weight(1f)
                )
            }
            StatusTile(
                "Reportes",
                if (st.openIncidents == 0) "Ninguno" else "${st.openIncidents} abierto" + if (st.openIncidents > 1) "s" else "",
                Icons.Rounded.Build,
                if (st.openIncidents == 0) Palette.Green else Palette.Navy,
                Modifier.weight(1f)
            )
        }

        // Contrato: solo el administrador de la casa (el Super Admin no ve esta información).
        if (viewer.role == Role.ADMIN && c != null && c.template == Template.CASA_DEPAS) {
            ContractAdminSection(u, c)
        }

        if (Store.featureOn(c, Feature.COBROS)) {
            SectionTitle("Pagos", trailing = if (st.upToDate) "Al día" else "Debe ${money(st.owed)}")
            if (charges.isEmpty()) EmptyState(Icons.Rounded.Payments, "Sin cobros registrados.")
            charges.take(6).forEach { ch ->
                key(ch.id) { ChargeCard(ch, onPay = if (viewer.role == Role.ADMIN) ({ payId = ch.id }) else null) }
            }
        }

        if (showMeters) {
            SectionTitle("Medidor de luz", trailing = req?.let { "Hasta ${it.due.format(dateFmt)}" })
            if (req != null && st.meterSent == false) {
                AppCard(color = Palette.AmberSoft, borderColor = Palette.AmberSoft, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.Bolt, Palette.AmberDark, Palette.Surface, 38.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Falta tomar la foto", style = MaterialTheme.typography.titleMedium)
                            Text(req.title, style = MaterialTheme.typography.bodySmall, color = Palette.AmberDark)
                        }
                    }
                }
            }
            if (readings.isEmpty()) EmptyState(Icons.Rounded.Bolt, "Aún no envió fotos del medidor.")
            readings.take(4).forEach { m -> key(m.id) { ReadingRow(m, showUser = false) { viewing = m } } }
        }

        if (Store.featureOn(c, Feature.INCIDENCIAS)) {
            SectionTitle("Reportes", trailing = "${incidents.size}")
            if (incidents.isEmpty()) EmptyState(Icons.Rounded.Build, "No tiene reportes.")
            incidents.take(5).forEach { inc ->
                key(inc.id) {
                    AppCard(padding = 14.dp) {
                        IncidentHeader(inc, "${inc.createdAt} · ${inc.description}")
                    }
                }
            }
        }

        if (emergencies.isNotEmpty()) {
            SectionTitle("Alertas de emergencia", trailing = "${emergencies.size}")
            emergencies.take(3).forEach { e ->
                key(e.id) {
                    AppCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(emergencyIcon(e.type), Palette.Red, Palette.RedSoft, 38.dp)
                            Column(Modifier.weight(1f)) {
                                Text(e.type, style = MaterialTheme.typography.titleMedium)
                                Text("${e.createdAt} · ${e.status.label}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                            }
                        }
                    }
                }
            }
        }

        SectionTitle("Cuenta")
        UserRow(u, canDelete = viewer.role == Role.SUPER_ADMIN, showClient = viewer.role == Role.SUPER_ADMIN)
        Spacer(Modifier.height(8.dp))
    }

    viewing?.let { m -> MeterPhotoDialog(m) { viewing = null } }
    payId?.let { id ->
        ConfirmDialog(
            title = "Registrar pago",
            text = "¿Confirmas que ${u.name} pagó este cobro?",
            confirm = "Registrar",
            onConfirm = { Store.markPaid(id) },
            onDismiss = { payId = null },
            danger = false
        )
    }
}

@Composable
private fun StatusTile(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    AppCard(modifier = modifier, padding = 12.dp) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
