package pe.condoapp

import androidx.compose.foundation.clickable
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
// SUPER ADMIN · Resumen del negocio
// ------------------------------------------------------------------

@Composable
fun SuperResumen(user: AppUser) {
    var confirmReset by remember { mutableStateOf(false) }
    val active = Store.clients.filter { it.active }
    val income = active.sumOf { it.monthlyFee }
    val people = Store.users.count { it.role != Role.SUPER_ADMIN }
    val alerts = Store.emergencies.count {
        it.status == EmergencyStatus.PENDIENTE || it.status == EmergencyStatus.CONFIRMADA
    }
    val openReports = Store.incidents.count { !(it.status == IncidentStatus.RESUELTO && it.confirmed == true) }

    ScreenColumn {
        HeroCard(
            eyebrow = "Panel del negocio",
            title = "Hola, ${user.name.substringBefore(" ")}",
            subtitle = "Así van tus clientes este mes.",
            icon = Icons.Rounded.BarChart
        ) {
            Spacer(Modifier.height(6.dp))
            Text("Ingreso mensual", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Text(money(income), color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Clientes activos", "${active.size}/${Store.clients.size}", Icons.Rounded.Apartment, Palette.Teal, Palette.TealSoft, Modifier.weight(1f))
            StatCard("Usuarios en la app", "$people", Icons.Rounded.Groups, Palette.Navy, Palette.NavySoft, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Alertas abiertas", "$alerts", Icons.Rounded.Warning, Palette.Red, Palette.RedSoft, Modifier.weight(1f))
            StatCard("Reportes abiertos", "$openReports", Icons.Rounded.Build, Palette.AmberDark, Palette.AmberSoft, Modifier.weight(1f))
        }

        SectionTitle("Uso por cliente", trailing = "${Store.clients.size} clientes")
        if (Store.clients.isEmpty()) EmptyState(Icons.Rounded.Apartment, "Todavía no tienes clientes.")
        Store.clients.forEach { c ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(templateIcon(c.template), Palette.Teal, Palette.TealSoft)
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                        Text(c.template.label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                    if (c.active) Pill("Activo", Palette.Green, Palette.GreenSoft)
                    else Pill("Inactivo", Palette.Muted, Palette.NavySoft)
                }
                LineDivider()
                Row(Modifier.fillMaxWidth()) {
                    MiniStat("Admins", "${Store.adminsOf(c.id).size}", Modifier.weight(1f))
                    MiniStat("Inquilinos", "${Store.tenantsOf(c.id).size}", Modifier.weight(1f))
                    MiniStat("Mensualidad", money(c.monthlyFee), Modifier.weight(1.3f))
                }
            }
        }

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
            Feature.values().forEach { f ->
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
    val list = Store.users.filter { u ->
        filter == "Todos" || Store.client(u.clientId)?.name == filter
    }

    ScreenColumn {
        SectionTitle("Cuentas", trailing = "${Store.users.size} usuarios")
        if (!showForm) {
            PrimaryButton("Crear cuenta", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PersonAdd)
        } else {
            AccountForm(fixedClient = null, allowedRoles = listOf(Role.ADMIN, Role.TENANT), onDone = { showForm = false })
        }
        ChoiceChips(listOf("Todos") + Store.clients.map { it.name }, filter) { filter = it }
        if (list.isEmpty()) EmptyState(Icons.Rounded.Groups, "No hay cuentas en este cliente.")
        list.forEach { u ->
            key(u.id) { UserRow(u, canDelete = u.id != me.id, showClient = filter == "Todos") }
        }
    }
}
