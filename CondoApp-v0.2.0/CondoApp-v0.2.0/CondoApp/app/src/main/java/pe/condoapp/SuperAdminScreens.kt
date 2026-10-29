package pe.condoapp

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ------------------------------------------------------------------
// SUPER ADMIN · Resumen del negocio
// ------------------------------------------------------------------

@Composable
fun SuperResumen(user: AppUser, go: (Tab) -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    val active = Store.clients.filter { it.active }
    val income = active.sumOf { it.monthlyFee }
    val people = Store.users.count { it.role != Role.SUPER_ADMIN }
    val tenantsTotal = Store.users.count { it.role == Role.TENANT }
    val alerts = Store.emergencies.count {
        it.status == EmergencyStatus.PENDIENTE || it.status == EmergencyStatus.CONFIRMADA
    }
    val openReports = Store.incidents.count { !(it.status == IncidentStatus.RESUELTO && it.confirmed == true) }
    val paid = Store.charges.filter { it.paid }.sumOf { it.amount }
    val overdue = Store.charges.filter { Store.isOverdue(it) }.sumOf { Store.amountDue(it) }
    val pendingAmt = Store.charges.filter { !it.paid && !Store.isOverdue(it) }.sumOf { it.amount }
    val cobranzaTotal = paid + overdue + pendingAmt
    val maxFee = Store.clients.maxOfOrNull { it.monthlyFee } ?: 0.0
    val houses = Store.clients.filter { Store.featureOn(it, Feature.MEDIDORES) }

    ScreenColumn {
        TenantSearch(null, "Buscar inquilino en todos los clientes")

        DarkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ingreso mensual", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Text(money(income), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                IconBadge(Icons.Rounded.Insights, Palette.Navy, Palette.Amber, 48.dp)
            }
            ProgressLine(
                if (Store.clients.isEmpty()) 0f else active.size.toFloat() / Store.clients.size,
                Palette.Amber,
                Color.White.copy(alpha = 0.15f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroInfo("Proyección anual", money(income * 12), Modifier.weight(1.3f))
                HeroInfo("Activos", "${active.size}/${Store.clients.size}", Modifier.weight(1f))
                HeroInfo("Usuarios", "$people", Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Inquilinos", "$tenantsTotal", Icons.Rounded.Groups, Palette.Teal, Palette.TealSoft, Modifier.weight(1f))
            StatCard("Alertas abiertas", "$alerts", Icons.Rounded.Warning, Palette.Red, Palette.RedSoft, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Reportes abiertos", "$openReports", Icons.Rounded.Build, Palette.AmberDark, Palette.AmberSoft, Modifier.weight(1f))
            StatCard("Por cobrar", money(overdue + pendingAmt), Icons.Rounded.Payments, Palette.Navy, Palette.NavySoft, Modifier.weight(1f))
        }

        // Ingresos por cliente
        SectionTitle("Ingresos por cliente", trailing = "S/ al mes")
        AppCard(padding = 18.dp) {
            if (Store.clients.isEmpty()) {
                Text("Todavía no tienes clientes.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            Store.clients.forEach { c ->
                key(c.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                c.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(money(c.monthlyFee), style = MaterialTheme.typography.titleSmall)
                        }
                        ProgressLine(
                            if (maxFee > 0) (c.monthlyFee / maxFee).toFloat() else 0f,
                            if (c.active) Palette.Teal else Palette.Muted,
                            Palette.Field,
                            10.dp
                        )
                    }
                }
            }
        }

        // Cobranza de inquilinos (dona)
        SectionTitle("Cobranza de inquilinos")
        AppCard(padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Donut(
                    listOf(paid.toFloat() to Palette.Green, pendingAmt.toFloat() to Palette.Amber, overdue.toFloat() to Palette.Red),
                    diameter = 120.dp,
                    stroke = 16.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (cobranzaTotal > 0) "${(paid / cobranzaTotal * 100).roundToInt()}%" else "-",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("cobrado", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendRow(Palette.Green, "Pagado", money(paid))
                    LegendRow(Palette.Amber, "Pendiente", money(pendingAmt))
                    LegendRow(Palette.Red, "Vencido", money(overdue))
                }
            }
        }

        // Usuarios por cliente (barras verticales)
        SectionTitle("Usuarios por cliente")
        AppCard(padding = 18.dp) {
            val counts = Store.clients.map { c -> Triple(c, Store.adminsOf(c.id).size, Store.tenantsOf(c.id).size) }
            val maxCount = (counts.maxOfOrNull { it.second + it.third } ?: 0).coerceAtLeast(1)
            Row(
                Modifier.fillMaxWidth().height(150.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                counts.forEach { (c, admins, tenants) ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("${admins + tenants}", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        val total = admins + tenants
                        val h = 120f * total / maxCount
                        Column(
                            Modifier
                                .width(28.dp)
                                .height(h.coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        ) {
                            if (total > 0) {
                                Box(Modifier.fillMaxWidth().weight(admins.toFloat().coerceAtLeast(0.001f)).background(Palette.Navy))
                                Box(Modifier.fillMaxWidth().weight(tenants.toFloat().coerceAtLeast(0.001f)).background(Palette.Teal))
                            } else {
                                Box(Modifier.fillMaxSize().background(Palette.Line))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                counts.forEach { (c, _, _) ->
                    Text(
                        c.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendRow(Palette.Navy, "Administradores", null)
                LegendRow(Palette.Teal, "Inquilinos", null)
            }
        }

        // Medidores en casas
        if (houses.isNotEmpty()) {
            SectionTitle("Medidores de luz", trailing = "Casas")
            houses.forEach { c ->
                key(c.id) {
                    val req = Store.activeMeterRequest(c.id)
                    val tenants = Store.tenantsOf(c.id)
                    val sent = req?.let { r -> tenants.count { t -> Store.readingFor(r.id, t.id) != null } } ?: 0
                    AppCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(Icons.Rounded.Bolt, Palette.AmberDark, Palette.AmberSoft, 38.dp)
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (req == null) "Sin pedido activo" else "$sent de ${tenants.size} fotos · hasta el ${req.due.format(dateFmt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Palette.Muted
                                )
                            }
                        }
                        if (req != null) {
                            ProgressLine(if (tenants.isEmpty()) 0f else sent.toFloat() / tenants.size, Palette.Teal, Palette.TealSoft, 6.dp)
                        }
                    }
                }
            }
        }

        SecondaryButton("Ver clientes", { go(Tab.CLIENTES) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Apartment)
        SecondaryButton(
            "Restablecer datos de prueba",
            { confirmReset = true },
            Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Refresh
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Restablecer datos",
            text = "Se borrarán todos los cambios y volverán los datos de ejemplo. Se cerrará tu sesión.",
            confirm = "Restablecer",
            onConfirm = { Store.resetDemo() },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        if (value != null) {
            Spacer(Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Gráfico de dona. El tamaño se llama [diameter] para no chocar con DrawScope.size. */
@Composable
private fun Donut(values: List<Pair<Float, Color>>, diameter: Dp, stroke: Dp, center: @Composable () -> Unit) {
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val total = values.sumOf { it.first.toDouble() }.toFloat()
            if (total <= 0f) {
                drawArc(color = Palette.Line, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw))
            } else {
                var start = -90f
                values.forEach { (v, col) ->
                    val sweep = v / total * 360f
                    if (sweep > 0f) {
                        drawArc(color = col, startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw))
                    }
                    start += sweep
                }
            }
        }
        center()
    }
}

// ------------------------------------------------------------------
// SUPER ADMIN · Clientes y funciones
// ------------------------------------------------------------------

@Composable
fun SuperClientes() {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(Template.CONDOMINIO) }
    var fee by remember { mutableStateOf("150") }

    ScreenColumn {
        SectionTitle("Clientes", trailing = "${Store.clients.size} en total")
        if (!showForm) {
            PrimaryButton("Nuevo cliente", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        } else {
            AppCard(padding = 18.dp) {
                Text("Nuevo cliente", style = MaterialTheme.typography.titleLarge)
                AppTextField(name, { name = it }, "Nombre del condominio o casa", icon = Icons.Rounded.Apartment)
                ChoiceChips(Template.values().map { it.label }, template.label) { l ->
                    template = Template.values().first { it.label == l }
                    fee = if (template == Template.CONDOMINIO) "150" else "40"
                }
                Text(
                    if (template == Template.CONDOMINIO) "Incluye áreas comunes y recargo por mora."
                    else "Sin áreas comunes ni recargo automático por mora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
                AppTextField(fee, { fee = it }, "Mensualidad que te paga (S/)", icon = Icons.Rounded.Payments, keyboardType = KeyboardType.Decimal)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Cancelar", { showForm = false }, Modifier.weight(1f))
                    PrimaryButton(
                        "Crear",
                        {
                            Store.addClient(name, template, fee.toDoubleOrNull() ?: 0.0)
                            name = ""
                            showForm = false
                        },
                        Modifier.weight(1f),
                        enabled = name.isNotBlank() && fee.toDoubleOrNull() != null
                    )
                }
            }
        }
        Store.clients.toList().forEach { c ->
            key(c.id) { ClientEditor(c) }
        }
    }
}

@Composable
private fun ClientEditor(c: Client) {
    var expanded by remember { mutableStateOf(false) }
    var fee by remember { mutableStateOf(c.monthlyFee.toString()) }
    var late by remember { mutableStateOf(c.lateFeePercent.toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    AppCard {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(templateIcon(c.template), Palette.Teal, Palette.TealSoft)
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${c.template.label} · ${money(c.monthlyFee)}/mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            if (!c.active) Pill("Inactivo", Palette.Muted, Palette.NavySoft)
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Cerrar" else "Abrir",
                tint = Palette.Muted
            )
        }
        if (expanded) {
            LineDivider()
            ToggleRow(
                "Cliente activo",
                "Si lo desactivas, sus usuarios no podrán ingresar.",
                c.active
            ) { Store.updateClient(c.copy(active = it)) }

            Text("Plantilla", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            ChoiceChips(Template.values().map { it.label }, c.template.label) { l ->
                val t = Template.values().first { it.label == l }
                if (t != c.template) Store.updateClient(c.copy(template = t, modules = Store.defaultsFor(t)))
            }
            AppTextField(
                fee,
                { text ->
                    fee = text
                    text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(monthlyFee = v)) }
                },
                "Mensualidad (S/)",
                icon = Icons.Rounded.Payments,
                keyboardType = KeyboardType.Decimal
            )

            Text("Funciones del cliente", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            // Los medidores solo existen para casas por departamentos.
            Feature.values().filter { it != Feature.MEDIDORES || c.template == Template.CASA_DEPAS }.forEach { f ->
                ToggleRow(f.label, if (f.ready) null else "Próximamente", f in c.modules) { on ->
                    Store.updateClient(c.copy(modules = if (on) c.modules + f else c.modules - f))
                }
            }
            if (Feature.MORA in c.modules) {
                AppTextField(
                    late,
                    { text ->
                        late = text
                        text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(lateFeePercent = v)) }
                    },
                    "Recargo por mora (%)",
                    icon = Icons.Rounded.Payments,
                    keyboardType = KeyboardType.Decimal
                )
            }
            TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Palette.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Eliminar cliente", color = Palette.Red, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar cliente",
            text = "Se borrarán ${c.name} y todas sus cuentas, cobros y avisos. No se puede deshacer.",
            confirm = "Eliminar",
            onConfirm = { Store.deleteClient(c.id) },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ------------------------------------------------------------------
// SUPER ADMIN · Cuentas (crear y eliminar)
// ------------------------------------------------------------------

@Composable
fun SuperCuentas(me: AppUser) {
    var showForm by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Todos") }
    var q by remember { mutableStateOf("") }
    val query = q.trim().lowercase()
    val list = Store.users.filter { u ->
        (filter == "Todos" || Store.client(u.clientId)?.name == filter) &&
            (query.isEmpty() || u.name.lowercase().contains(query) || u.username.contains(query) ||
                (u.unit?.lowercase()?.contains(query) == true))
    }

    ScreenColumn {
        SectionTitle("Cuentas", trailing = "${Store.users.size} usuarios")
        SearchField(q, { q = it }, "Buscar por nombre, usuario o depa")
        if (!showForm) {
            PrimaryButton("Crear cuenta", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PersonAdd)
        } else {
            AccountForm(fixedClient = null, allowedRoles = listOf(Role.ADMIN, Role.TENANT), onDone = { showForm = false })
        }
        ChoiceChips(listOf("Todos") + Store.clients.map { it.name }, filter) { filter = it }
        if (list.isEmpty()) EmptyState(Icons.Rounded.Groups, "No hay cuentas que coincidan.")
        list.forEach { u ->
            key(u.id) {
                if (u.role == Role.TENANT) {
                    TenantRow(u, showClient = filter == "Todos") { UiState.profileId = u.id }
                } else {
                    UserRow(u, canDelete = u.id != me.id, showClient = filter == "Todos")
                }
            }
        }
    }
}
