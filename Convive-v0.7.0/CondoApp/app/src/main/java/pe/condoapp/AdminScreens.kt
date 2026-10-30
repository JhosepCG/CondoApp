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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth

// ------------------------------------------------------------------
// ADMINISTRADOR · Panel
// ------------------------------------------------------------------

@Composable
fun AdminPanel(user: AppUser, go: (Tab) -> Unit, showPersonasShortcut: Boolean) {
    val c = Store.clientOf(user) ?: return
    val meterReq = if (Store.featureOn(c, Feature.MEDIDORES)) Store.activeMeterRequest(c.id) else null
    val list = Store.emergencies.filter { it.clientId == c.id }.reversed()
    val pending = list.filter { it.status == EmergencyStatus.PENDIENTE }
    val active = list.filter { it.status == EmergencyStatus.CONFIRMADA }
    val tenants = Store.tenantsOf(c.id)
    val overdue = Store.charges.count { it.clientId == c.id && Store.isOverdue(it) }
    val openInc = Store.incidents.count {
        it.clientId == c.id && !(it.status == IncidentStatus.RESUELTO && it.confirmed == true)
    }
    val month = YearMonth.now()
    val allCharges = Store.charges.filter { it.clientId == c.id }
    val monthCharges = allCharges.filter { YearMonth.from(it.due) == month }.ifEmpty { allCharges }
    val total = monthCharges.sumOf { it.amount }
    val collected = monthCharges.filter { it.paid }.sumOf { it.amount }
    val lastNotices = Store.notices.filter { it.clientId == c.id }.reversed().take(2)
    val sentCount = meterReq?.let { r -> tenants.count { t -> Store.readingFor(r.id, t.id) != null } } ?: 0
    // Primero los inquilinos con algo pendiente
    val ranked = tenants.sortedByDescending { t ->
        val st = tenantStatus(t)
        st.overdue * 4 + st.pending.size * 2 + st.openIncidents + (if (st.meterSent == false) 1 else 0)
    }

    ScreenColumn {
        Box {
            TenantSearch(c.id)
            CoachMark("ad_buscar", "Busca a un inquilino por nombre, depa o DNI y abre todo su perfil.", above = false)
        }

        DarkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(c.name, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (Feature.COBROS in c.modules) "Cobrado este mes" else c.template.label,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconBadge(templateIcon(c.template), Color.White, Color.White.copy(alpha = 0.12f), 44.dp)
            }
            if (Feature.COBROS in c.modules) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(money(collected), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("de ${money(total)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                }
                ProgressLine(if (total > 0) (collected / total).toFloat() else 0f, Palette.Amber, Color.White.copy(alpha = 0.15f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroInfo("Inquilinos", "${tenants.size}", Modifier.weight(1f))
                if (Feature.COBROS in c.modules) HeroInfo("Vencidos", "$overdue", Modifier.weight(1f))
                if (Feature.INCIDENCIAS in c.modules) HeroInfo("Reportes", "$openInc", Modifier.weight(1f))
            }
        }

        // Recordatorio de suscripción (solo el owner, que es quien paga)
        val subSt = Store.subStatus(c)
        if (Store.isOwner(user) && (subSt == SubStatus.VENCIDO || subSt == SubStatus.PENDIENTE)) {
            val late = subSt == SubStatus.VENCIDO
            AppCard(
                color = if (late) Palette.RedSoft else Palette.AmberSoft,
                borderColor = if (late) Palette.RedSoft else Palette.AmberSoft,
                onClick = { UiState.myProfile = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.Payments, if (late) Palette.Red else Palette.AmberDark, Palette.Surface)
                    Column(Modifier.weight(1f)) {
                        Text(if (late) "Tu suscripción está vencida" else "Paga tu suscripción", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${Store.feeLabel(c)} · vence el ${Store.subDue(Store.periodFor(c)).format(dateFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (late) Palette.Red else Palette.AmberDark
                        )
                    }
                    Pill("Ver", Palette.Teal, Palette.Surface)
                }
            }
        }

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
        }

        // Accesos rápidos
        val tiles = buildList<Triple<ImageVector, String, Pair<Tab, Int>>> {
            if (Feature.COBROS in c.modules) add(Triple(Icons.Rounded.Payments, "Cobros", Tab.COBROS to overdue))
            if (meterReq != null || Store.featureOn(c, Feature.MEDIDORES)) {
                add(Triple(Icons.Rounded.Bolt, "Medidores", Tab.MEDIDORES to (if (meterReq != null) tenants.size - sentCount else 0)))
            }
            if (Feature.INCIDENCIAS in c.modules) add(Triple(Icons.Rounded.Build, "Reportes", Tab.SERVICIOS to openInc))
            if (Feature.AVISOS in c.modules) add(Triple(Icons.Rounded.Campaign, "Avisos", Tab.AVISOS to 0))
            add(Triple(Icons.Rounded.Groups, "Personas", Tab.PERSONAS to 0))
        }
        tiles.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, target) ->
                    QuickTile(icon, label, Modifier.weight(1f), badge = target.second) { go(target.first) }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        val toRenew = if (c.template == Template.CASA_DEPAS) contractsToReview(c.id) else emptyList()
        if (toRenew.isNotEmpty()) {
            AppCard(color = Palette.AmberSoft, borderColor = Palette.Amber.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.EditNote, Palette.AmberDark, Palette.Surface)
                    Column(Modifier.weight(1f)) {
                        Text("Contratos por renovar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Indícales que deben firmar nuevamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.AmberDark
                        )
                    }
                    Pill("${toRenew.size}", Palette.AmberDark, Palette.Surface)
                }
                toRenew.forEach { (t, ct) ->
                    key(t.id) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Palette.Surface)
                                .clickable { UiState.profileId = t.id }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${t.unit ?: "-"} · ${t.name}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    (if (Store.contractExpired(ct)) "Venció el " else "Vence el ") + ct.end.format(dateFmt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Palette.Muted
                                )
                            }
                            Text("Avisar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (meterReq != null) {
            AppCard(onClick = { go(Tab.MEDIDORES) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.Bolt, Palette.AmberDark, Palette.AmberSoft)
                    Column(Modifier.weight(1f)) {
                        Text(meterReq.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$sentCount de ${tenants.size} fotos · hasta el ${meterReq.due.format(dateFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                    Pill("Ver", Palette.Teal, Palette.TealSoft)
                }
                ProgressLine(if (tenants.isEmpty()) 0f else sentCount.toFloat() / tenants.size, Palette.Teal, Palette.TealSoft, 6.dp)
            }
        }

        SectionTitle("Inquilinos", trailing = "${tenants.size}")
        if (tenants.isEmpty()) EmptyState(Icons.Rounded.Groups, "Aún no hay inquilinos.")
        ranked.take(5).forEach { t -> key(t.id) { TenantRow(t, showClient = false, showContract = true) { UiState.profileId = t.id } } }
        if (tenants.size > 5) {
            SecondaryButton("Ver todos los inquilinos", { go(Tab.PERSONAS) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Groups)
        }

        if (Feature.AVISOS in c.modules && lastNotices.isNotEmpty()) {
            SectionTitle("Últimos avisos")
            lastNotices.forEach { key(it.id) { NoticeCard(it) } }
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
    var photo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val list = Store.notices.filter { it.clientId == c.id }.reversed()
    val picker = rememberPhotoPicker(
        onResult = { path ->
            Photos.delete(photo)
            photo = path
            error = null
        },
        onError = { error = it }
    )
    val reset: () -> Unit = {
        title = ""
        body = ""
        urgent = false
        photo = null
        error = null
        showForm = false
    }

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

                Text("Foto o comunicado (opcional)", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
                Text(
                    "Por ejemplo, el comunicado del corte de luz o de agua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
                val p = photo
                if (p != null) {
                    Box {
                        PhotoImage(p, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)))
                        IconButton(
                            onClick = {
                                Photos.delete(p)
                                photo = null
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Tomar foto", picker.camera, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera)
                    SecondaryButton("Galería", picker.gallery, Modifier.weight(1f), icon = Icons.Rounded.PhotoLibrary)
                }
                error?.let { ErrorText(it) }

                ToggleRow("Marcar como urgente", "Se resalta en la app de los inquilinos.", urgent) { urgent = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        "Cancelar",
                        {
                            Photos.delete(photo)
                            reset()
                        },
                        Modifier.weight(1f)
                    )
                    PrimaryButton(
                        "Publicar",
                        {
                            Store.addNotice(c.id, category, title, body, urgent, photo)
                            reset()
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
fun AdminPersonas(user: AppUser, onBack: (() -> Unit)? = null) {
    val c = Store.clientOf(user) ?: return
    var showForm by remember { mutableStateOf(false) }
    val admins = Store.adminsOf(c.id)
    val tenants = Store.tenantsOf(c.id)

    ScreenColumn {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("‹ Volver al panel", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
            }
        }
        SectionTitle("Personas", trailing = "${admins.size + tenants.size} cuentas")
        TenantSearch(c.id)
        if (!showForm) {
            PrimaryButton("Agregar persona", { showForm = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PersonAdd)
        } else {
            AccountForm(fixedClient = c, allowedRoles = listOf(Role.ADMIN, Role.TENANT), onDone = { showForm = false })
        }
        SectionTitle("Administradores", trailing = "${admins.size}")
        admins.forEach { key(it.id) { UserRow(it, canDelete = false) } }
        SectionTitle("Inquilinos", trailing = "${tenants.size}")
        if (tenants.isEmpty()) EmptyState(Icons.Rounded.Groups, "Aún no hay inquilinos.")
        tenants.forEach { t -> key(t.id) { TenantRow(t, showClient = false, showContract = true) { UiState.profileId = t.id } } }
    }
}
