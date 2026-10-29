package pe.condoapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// ------------------------------------------------------------------
// ADMINISTRADOR · Panel
// ------------------------------------------------------------------

@Composable
fun AdminPanel(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.emergencies.filter { it.clientId == c.id }.reversed()
    val pending = list.filter { it.status == EmergencyStatus.PENDIENTE }
    val active = list.filter { it.status == EmergencyStatus.CONFIRMADA }
    val overdue = Store.charges.count { it.clientId == c.id && Store.isOverdue(it) }
    val openInc = Store.incidents.count {
        it.clientId == c.id && !(it.status == IncidentStatus.RESUELTO && it.confirmed == true)
    }
    val lastNotices = Store.notices.filter { it.clientId == c.id }.reversed().take(2)

    ScreenColumn {
        HeroCard(
            eyebrow = "Administración",
            title = c.name,
            subtitle = c.template.label,
            icon = templateIcon(c.template)
        )

        if (Feature.EMERGENCIA in c.modules) {
            pending.forEach { e -> PendingEmergencyCard(e) }
            active.forEach { e ->
                AppCard(color = Palette.RedSoft, borderColor = Palette.Red.copy(alpha = 0.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(emergencyIcon(e.type), Color.White, Palette.Red)
                        Column(Modifier.weight(1f)) {
                            Text("Emergencia activa: ${e.type}", style = MaterialTheme.typography.titleMedium, color = Palette.Red)
                            Text("${e.unit} · residentes avisados", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                    }
                    PrimaryButton(
                        "Cerrar emergencia",
                        { Store.setEmergency(e.id, EmergencyStatus.CERRADA) },
                        Modifier.fillMaxWidth(),
                        color = Palette.Navy,
                        icon = Icons.Rounded.Check
                    )
                }
            }
            if (pending.isEmpty() && active.isEmpty()) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.VerifiedUser, Palette.Green, Palette.GreenSoft)
                        Column {
                            Text("Todo en calma", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "No hay alertas de emergencia pendientes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.Muted
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (Feature.COBROS in c.modules) {
                StatCard("Cobros vencidos", "$overdue", Icons.Rounded.EventBusy, Palette.Red, Palette.RedSoft, Modifier.weight(1f))
            }
            if (Feature.INCIDENCIAS in c.modules) {
                StatCard("Reportes abiertos", "$openInc", Icons.Rounded.Build, Palette.AmberDark, Palette.AmberSoft, Modifier.weight(1f))
            }
            StatCard("Inquilinos", "${Store.tenantsOf(c.id).size}", Icons.Rounded.Groups, Palette.Teal, Palette.TealSoft, Modifier.weight(1f))
        }

        if (Feature.AVISOS in c.modules && lastNotices.isNotEmpty()) {
            SectionTitle("Últimos avisos")
            lastNotices.forEach { NoticeCard(it) }
        }
    }
}

@Composable
private fun PendingEmergencyCard(e: Emergency) {
    AppCard(color = Palette.RedSoft, borderColor = Palette.Red.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(emergencyIcon(e.type), Color.White, Palette.Red)
            Column(Modifier.weight(1f)) {
                Text(e.type, style = MaterialTheme.typography.titleMedium, color = Palette.Red)
                Text(
                    "${e.unit} · ${e.userName} · ${e.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            Pill("Nueva", Color.White, Palette.Red)
        }
        Text(
            "Verifica antes de confirmar. Si es real, se avisará a todos los residentes.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Falsa alarma", { Store.setEmergency(e.id, EmergencyStatus.DESCARTADA) }, Modifier.weight(1f))
            PrimaryButton(
                "Es real",
                { Store.setEmergency(e.id, EmergencyStatus.CONFIRMADA) },
                Modifier.weight(1f),
                color = Palette.Red,
                icon = Icons.Rounded.Campaign
            )
        }
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Cobros
// ------------------------------------------------------------------

@Composable
fun AdminCobros(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var showForm by remember { mutableStateOf(false) }
    val list = Store.charges.filter { it.clientId == c.id }
        .sortedWith(compareBy<Charge> { it.paid }.thenBy { it.due })
    val pendingTotal = list.filter { !it.paid }.sumOf { Store.amountDue(it) }
    val overdue = list.count { Store.isOverdue(it) }

    ScreenColumn {
        HeroCard(
            eyebrow = "Por cobrar",
            title = money(pendingTotal),
            subtitle = if (overdue > 0) "$overdue cobro(s) vencido(s)" else "Sin cobros vencidos",
            icon = Icons.Rounded.Payments
        )
        if (!showForm) {
            SecondaryButton("Generar cobro", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        } else {
            NewChargeForm(c) { showForm = false }
        }
        SectionTitle("Movimientos", trailing = "${list.size}")
        if (list.isEmpty()) EmptyState(Icons.Rounded.Payments, "Aún no hay cobros.")
        list.forEach { ch ->
            key(ch.id) { ChargeCard(ch, onPay = { Store.markPaid(ch.id) }) }
        }
    }
}

@Composable
private fun NewChargeForm(c: Client, onDone: () -> Unit) {
    val unitNames = Store.unitsOf(c.id).map { it.unit }
    var unit by remember { mutableStateOf(unitNames.firstOrNull() ?: "") }
    var concept by remember {
        mutableStateOf(if (c.template == Template.CONDOMINIO) "Cuota de mantenimiento" else "Renta mensual")
    }
    var amount by remember { mutableStateOf("") }

    AppCard(padding = 18.dp) {
        Text("Nuevo cobro", style = MaterialTheme.typography.titleLarge)
        if (unitNames.isEmpty()) {
            Text("Primero agrega inquilinos con su departamento.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        } else {
            Text("Domicilio", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            ChoiceChips(unitNames, unit) { unit = it }
            AppTextField(concept, { concept = it }, "Concepto", icon = Icons.Rounded.Description)
            AppTextField(amount, { amount = it }, "Monto (S/)", icon = Icons.Rounded.Payments, keyboardType = KeyboardType.Decimal)
            Text("Vence el día 5 del próximo mes.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancelar", onDone, Modifier.weight(1f))
            PrimaryButton(
                "Generar",
                {
                    Store.addCharge(c.id, unit, concept, amount.toDoubleOrNull() ?: 0.0)
                    onDone()
                },
                Modifier.weight(1f),
                enabled = unit.isNotBlank() && concept.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
            )
        }
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Reportes de servicios
// ------------------------------------------------------------------

@Composable
fun AdminServicios(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.incidents.filter { it.clientId == c.id }.reversed()
    val open = list.count { !(it.status == IncidentStatus.RESUELTO && it.confirmed == true) }

    ScreenColumn {
        SectionTitle("Reportes de servicios", trailing = "$open abiertos")
        Text(
            "Los inquilinos te avisan aquí de fallas de internet, cable y otros servicios.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Muted
        )
        if (list.isEmpty()) EmptyState(Icons.Rounded.Build, "No hay reportes de fallas.")
        list.forEach { inc ->
            key(inc.id) {
                AppCard {
                    IncidentHeader(inc, "${inc.unit} · ${inc.userName} · ${inc.createdAt}")
                    if (inc.description.isNotBlank()) Text(inc.description, style = MaterialTheme.typography.bodyMedium)
                    when {
                        inc.status == IncidentStatus.RESUELTO && inc.confirmed == true ->
                            Text("El inquilino confirmó que quedó resuelto.", style = MaterialTheme.typography.bodySmall, color = Palette.Green)
                        inc.status == IncidentStatus.RESUELTO ->
                            Text("Esperando que el inquilino confirme.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (inc.status == IncidentStatus.PENDIENTE) {
                                SecondaryButton(
                                    "En revisión",
                                    { Store.setIncidentStatus(inc.id, IncidentStatus.EN_REVISION) },
                                    Modifier.weight(1f)
                                )
                            }
                            PrimaryButton(
                                "Resuelto",
                                { Store.setIncidentStatus(inc.id, IncidentStatus.RESUELTO) },
                                Modifier.weight(1f),
                                icon = Icons.Rounded.Check
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Avisos
// ------------------------------------------------------------------

@Composable
fun AdminAvisos(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var showForm by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(NOTICE_CATEGORIES.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    val list = Store.notices.filter { it.clientId == c.id }.reversed()

    ScreenColumn {
        SectionTitle("Avisos", trailing = "${list.size} publicados")
        if (!showForm) {
            PrimaryButton("Publicar aviso", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Campaign)
        } else {
            AppCard(padding = 18.dp) {
                Text("Nuevo aviso", style = MaterialTheme.typography.titleLarge)
                ChoiceChips(
                    NOTICE_CATEGORIES,
                    category,
                    icons = NOTICE_CATEGORIES.associateWith { noticeIcon(it) }
                ) { category = it }
                AppTextField(title, { title = it }, "Título", icon = Icons.Rounded.Title)
                AppTextField(body, { body = it }, "Detalle (fecha, hora, zona)", singleLine = false, minLines = 3)
                ToggleRow("Marcar como urgente", "Se resalta en la app de los inquilinos.", urgent) { urgent = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Cancelar", { showForm = false }, Modifier.weight(1f))
                    PrimaryButton(
                        "Publicar",
                        {
                            Store.addNotice(c.id, category, title, body, urgent)
                            title = ""
                            body = ""
                            urgent = false
                            showForm = false
                        },
                        Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    )
                }
            }
        }
        if (list.isEmpty()) EmptyState(Icons.Rounded.Campaign, "Todavía no hay avisos.")
        list.forEach { n ->
            key(n.id) { NoticeCard(n, onArchive = { Store.removeNotice(n.id) }) }
        }
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Personas (crear administradores e inquilinos)
// ------------------------------------------------------------------

@Composable
fun AdminPersonas(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var showForm by remember { mutableStateOf(false) }
    val admins = Store.adminsOf(c.id)
    val tenants = Store.tenantsOf(c.id)

    ScreenColumn {
        SectionTitle("Personas", trailing = "${admins.size + tenants.size} cuentas")
        if (!showForm) {
            PrimaryButton("Agregar persona", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PersonAdd)
        } else {
            AccountForm(fixedClient = c, allowedRoles = listOf(Role.ADMIN, Role.TENANT), onDone = { showForm = false })
        }
        SectionTitle("Administradores", trailing = "${admins.size}")
        admins.forEach { key(it.id) { UserRow(it, canDelete = false) } }
        SectionTitle("Inquilinos", trailing = "${tenants.size}")
        if (tenants.isEmpty()) EmptyState(Icons.Rounded.Groups, "Aún no hay inquilinos.")
        tenants.forEach { key(it.id) { UserRow(it, canDelete = false) } }
    }
}
