package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val RED = Color(0xFFB3261E)
private val GREEN = Color(0xFF2E7D32)

private fun money(v: Double): String = "S/ " + String.format("%.2f", v)

// ------------------------------------------------------------------
// Componentes base
// ------------------------------------------------------------------

@Composable
private fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun H(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun Choice(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { o ->
            if (o == selected) Button(onClick = { onSelect(o) }) { Text(o) }
            else OutlinedButton(onClick = { onSelect(o) }) { Text(o) }
        }
    }
}

// ------------------------------------------------------------------
// Raíz, login y navegación
// ------------------------------------------------------------------

@Composable
fun AppRoot() {
    val user = Store.currentUser
    if (user == null) LoginScreen() else MainScaffold(user)
}

@Composable
private fun LoginScreen() {
    Column(Modifier.fillMaxSize().systemBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Screen {
            Spacer(Modifier.height(16.dp))
            Text("CondoApp", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Modo demostración: elige una cuenta para probar cada rol. Los datos viven solo en el teléfono y se reinician al cerrar la app.")
            Store.users.forEach { u ->
                CardBox(Modifier.clickable { Store.currentUser = u }) {
                    Text(u.name, fontWeight = FontWeight.Bold)
                    val cl = Store.client(u.clientId)
                    Text(u.role.label + (if (cl != null) " · " + cl.name else ""))
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: String) {
    RESUMEN("Resumen", "📊"),
    CLIENTES("Clientes", "🏢"),
    PANEL("Panel", "🚨"),
    COBROS("Cobros", "💳"),
    INCIDENCIAS("Servicios", "🛠"),
    AVISOS("Avisos", "📣"),
    RESIDENTES("Residentes", "👥"),
    INICIO("Inicio", "🏠")
}

private fun tabsFor(user: AppUser): List<Tab> {
    val c = Store.clientOf(user)
    fun on(f: Feature): Boolean = c != null && f in c.modules
    return when (user.role) {
        Role.SUPER_ADMIN -> listOf(Tab.RESUMEN, Tab.CLIENTES)
        Role.ADMIN -> buildList {
            add(Tab.PANEL)
            if (on(Feature.COBROS)) add(Tab.COBROS)
            if (on(Feature.INCIDENCIAS)) add(Tab.INCIDENCIAS)
            if (on(Feature.AVISOS)) add(Tab.AVISOS)
            add(Tab.RESIDENTES)
        }
        else -> buildList {
            add(Tab.INICIO)
            if (on(Feature.INCIDENCIAS)) add(Tab.INCIDENCIAS)
            if (on(Feature.AVISOS)) add(Tab.AVISOS)
        }
    }
}

@Composable
private fun MainScaffold(user: AppUser) {
    val tabs = tabsFor(user)
    var tab by remember(user.id) { mutableStateOf(tabs.first()) }
    val current = if (tab in tabs) tab else tabs.first()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold)
                val cl = Store.clientOf(user)
                Text(user.role.label + (if (cl != null) " · " + cl.name else ""), fontSize = 12.sp)
            }
            TextButton(onClick = { Store.currentUser = null }) { Text("Salir") }
        }
        Box(Modifier.weight(1f)) {
            when (current) {
                Tab.RESUMEN -> SuperResumen()
                Tab.CLIENTES -> SuperClientes()
                Tab.PANEL -> AdminPanel(user)
                Tab.COBROS -> AdminCobros(user)
                Tab.INCIDENCIAS ->
                    if (user.role == Role.ADMIN) AdminIncidencias(user) else ResidentIncidencias(user)
                Tab.AVISOS ->
                    if (user.role == Role.ADMIN) AdminAvisos(user) else NoticeList(user)
                Tab.RESIDENTES -> AdminResidentes(user)
                Tab.INICIO -> ResidentHome(user)
            }
        }
        NavigationBar {
            tabs.forEach { t ->
                NavigationBarItem(
                    selected = current == t,
                    onClick = { tab = t },
                    icon = { Text(t.icon) },
                    label = { Text(t.label) }
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// SUPER ADMIN
// ------------------------------------------------------------------

@Composable
private fun SuperResumen() {
    val active = Store.clients.filter { it.active }
    Screen {
        H("Resumen del negocio")
        CardBox {
            Text("Clientes activos: ${active.size} de ${Store.clients.size}")
            Text("Ingreso mensual: ${money(active.sumOf { it.monthlyFee })}", fontWeight = FontWeight.Bold)
            Text("Usuarios registrados: ${Store.users.count { it.role != Role.SUPER_ADMIN }}")
        }
        Store.clients.forEach { c ->
            val us = Store.users.filter { it.clientId == c.id }
            CardBox {
                Text(c.name, fontWeight = FontWeight.Bold)
                Text(c.template.label + (if (c.active) "" else " · INACTIVO"))
                Text(
                    "Administradores: ${us.count { it.role == Role.ADMIN }} · " +
                        "Propietarios: ${us.count { it.role == Role.OWNER }} · " +
                        "Inquilinos: ${us.count { it.role == Role.TENANT }}"
                )
                Text("Mensualidad: ${money(c.monthlyFee)}")
            }
        }
    }
}

@Composable
private fun SuperClientes() {
    Screen {
        H("Clientes y funciones")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Store.addClient(Template.CONDOMINIO) }) { Text("+ Condominio") }
            Button(onClick = { Store.addClient(Template.CASA_DEPAS) }) { Text("+ Casa por depas") }
        }
        Store.clients.toList().forEach { c ->
            key(c.id) { ClientEditor(c) }
        }
    }
}

@Composable
private fun ClientEditor(c: Client) {
    var fee by remember { mutableStateOf(c.monthlyFee.toString()) }
    var late by remember { mutableStateOf(c.lateFeePercent.toString()) }
    CardBox {
        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cliente activo", Modifier.weight(1f))
            Switch(checked = c.active, onCheckedChange = { Store.updateClient(c.copy(active = it)) })
        }
        Text("Plantilla actual: ${c.template.label}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                Store.updateClient(c.copy(template = Template.CONDOMINIO, modules = Store.defaultsFor(Template.CONDOMINIO)))
            }) { Text("Aplicar condominio") }
            OutlinedButton(onClick = {
                Store.updateClient(c.copy(template = Template.CASA_DEPAS, modules = Store.defaultsFor(Template.CASA_DEPAS)))
            }) { Text("Aplicar casa") }
        }
        OutlinedTextField(
            value = fee,
            onValueChange = { text ->
                fee = text
                text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(monthlyFee = v)) }
            },
            label = { Text("Mensualidad (S/)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Funciones activas", fontWeight = FontWeight.Bold)
        Feature.values().forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.label + (if (f.ready) "" else " (próximamente)"), Modifier.weight(1f))
                Switch(
                    checked = f in c.modules,
                    onCheckedChange = { on ->
                        Store.updateClient(c.copy(modules = if (on) c.modules + f else c.modules - f))
                    }
                )
            }
        }
        if (Feature.MORA in c.modules) {
            OutlinedTextField(
                value = late,
                onValueChange = { text ->
                    late = text
                    text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(lateFeePercent = v)) }
                },
                label = { Text("Recargo por mora (%)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR
// ------------------------------------------------------------------

@Composable
private fun AdminPanel(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.emergencies.filter { it.clientId == c.id }.reversed()
    Screen {
        H(c.name)
        if (Feature.EMERGENCIA in c.modules) {
            Text("Alertas de emergencia", fontWeight = FontWeight.Bold)
            val pending = list.filter { it.status == EmergencyStatus.PENDIENTE }
            if (pending.isEmpty()) Text("Sin alertas pendientes.")
            pending.forEach { e ->
                CardBox {
                    Text("🚨 ${e.type} — ${e.unit}", fontWeight = FontWeight.Bold, color = RED)
                    Text("Reportó: ${e.userName} · ${e.createdAt}")
                    Text("Confirma solo si es real: se avisará a todos los residentes.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { Store.setEmergency(e.id, EmergencyStatus.CONFIRMADA) },
                            colors = ButtonDefaults.buttonColors(containerColor = RED)
                        ) { Text("Es real, avisar") }
                        OutlinedButton(onClick = { Store.setEmergency(e.id, EmergencyStatus.DESCARTADA) }) {
                            Text("Falsa alarma")
                        }
                    }
                }
            }
            list.filter { it.status == EmergencyStatus.CONFIRMADA }.forEach { e ->
                CardBox {
                    Text("Activa: ${e.type} — ${e.unit}", fontWeight = FontWeight.Bold)
                    Button(onClick = { Store.setEmergency(e.id, EmergencyStatus.CERRADA) }) {
                        Text("Cerrar emergencia")
                    }
                }
            }
        } else {
            Text("El botón de emergencia está desactivado para este cliente.")
        }
        CardBox {
            val openInc = Store.incidents.count {
                it.clientId == c.id && (it.status != IncidentStatus.RESUELTO || it.confirmed == null)
            }
            val overdue = Store.charges.count { it.clientId == c.id && Store.isOverdue(it) }
            Text("Resumen", fontWeight = FontWeight.Bold)
            if (Feature.INCIDENCIAS in c.modules) Text("Reportes de servicios abiertos: $openInc")
            if (Feature.COBROS in c.modules) Text("Cobros vencidos: $overdue")
            Text("Residentes: ${Store.users.count { it.clientId == c.id && it.role != Role.ADMIN }}")
        }
    }
}

@Composable
private fun AdminCobros(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.charges.filter { it.clientId == c.id }.sortedBy { it.due }
    Screen {
        H("Cobros")
        if (list.isEmpty()) Text("Aún no hay cobros.")
        list.forEach { ch ->
            CardBox {
                Text("${ch.unit} · ${ch.concept}", fontWeight = FontWeight.Bold)
                Text("Vence: ${ch.due.format(dateFmt)} · ${money(Store.amountDue(ch))}")
                if (ch.paid) {
                    Text("✅ Pagado", color = GREEN)
                } else {
                    if (Store.isOverdue(ch)) Text("⛔ Vencido", color = RED) else Text("⏳ Pendiente")
                    Button(onClick = { Store.markPaid(ch.id) }) { Text("Marcar como pagado") }
                }
            }
        }
    }
}

@Composable
private fun AdminIncidencias(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.incidents.filter { it.clientId == c.id }.reversed()
    Screen {
        H("Reportes de servicios")
        if (list.isEmpty()) Text("No hay reportes.")
        list.forEach { inc ->
            CardBox {
                Text("${inc.service} — ${inc.unit}", fontWeight = FontWeight.Bold)
                Text(inc.description)
                Text("Reportó: ${inc.userName}")
                Text("Estado: ${inc.status.label}")
                when {
                    inc.status == IncidentStatus.RESUELTO && inc.confirmed == true ->
                        Text("✅ El residente confirmó que quedó resuelto", color = GREEN)
                    inc.status == IncidentStatus.RESUELTO ->
                        Text("Esperando confirmación del residente")
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (inc.status == IncidentStatus.PENDIENTE) {
                            OutlinedButton(onClick = { Store.setIncidentStatus(inc.id, IncidentStatus.EN_REVISION) }) {
                                Text("En revisión")
                            }
                        }
                        Button(onClick = { Store.setIncidentStatus(inc.id, IncidentStatus.RESUELTO) }) {
                            Text("Marcar resuelto")
                        }
                    }
                }
            }
        }
    }
}

private val NOTICE_CATEGORIES = listOf("Corte de agua", "Corte de luz", "Internet / cable", "Mantenimiento", "Seguridad", "Otro")

@Composable
private fun AdminAvisos(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var category by remember { mutableStateOf(NOTICE_CATEGORIES.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    val list = Store.notices.filter { it.clientId == c.id }.reversed()
    Screen {
        H("Publicar aviso")
        CardBox {
            Choice(NOTICE_CATEGORIES, category) { category = it }
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("Título") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body, onValueChange = { body = it }, label = { Text("Detalle (fecha, hora, zona)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Marcar como urgente", Modifier.weight(1f))
                Switch(checked = urgent, onCheckedChange = { urgent = it })
            }
            Button(
                onClick = {
                    Store.addNotice(c.id, category, title.trim(), body.trim(), urgent)
                    title = ""; body = ""; urgent = false
                },
                enabled = title.isNotBlank()
            ) { Text("Publicar") }
        }
        H("Avisos publicados")
        if (list.isEmpty()) Text("Todavía no hay avisos.")
        list.forEach { n ->
            CardBox {
                Text((if (n.urgent) "❗ " else "") + n.title, fontWeight = FontWeight.Bold)
                Text("${n.category} · ${n.createdAt}", fontSize = 12.sp)
                if (n.body.isNotBlank()) Text(n.body)
                TextButton(onClick = { Store.removeNotice(n.id) }) { Text("Archivar") }
            }
        }
    }
}

@Composable
private fun AdminResidentes(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf(Role.OWNER.label) }
    val people = Store.users.filter { it.clientId == c.id && it.role != Role.ADMIN }
    val concept = if (c.template == Template.CONDOMINIO) "cuota mensual" else "renta mensual"
    Screen {
        H("Agregar residente")
        CardBox {
            Choice(listOf(Role.OWNER.label, Role.TENANT.label), roleLabel) { roleLabel = it }
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nombre") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = unit, onValueChange = { unit = it }, label = { Text("Departamento / casa") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Monto de la $concept (S/)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val role = if (roleLabel == Role.OWNER.label) Role.OWNER else Role.TENANT
                    Store.addResident(c, name.trim(), role, unit.trim(), amount.toDoubleOrNull() ?: 0.0)
                    name = ""; unit = ""; amount = ""
                },
                enabled = name.isNotBlank() && unit.isNotBlank() && amount.toDoubleOrNull() != null
            ) { Text("Agregar") }
        }
        H("Residentes (${people.size})")
        people.forEach { p ->
            CardBox {
                Text(p.name, fontWeight = FontWeight.Bold)
                Text("${p.role.label} · ${p.unit ?: "-"}")
            }
        }
    }
}

// ------------------------------------------------------------------
// PROPIETARIO / INQUILINO
// ------------------------------------------------------------------

private val EMERGENCY_TYPES = listOf("Fuga de gas", "Incendio", "Robo", "Otra emergencia")
private val SERVICES = listOf("Internet", "Cable", "Agua", "Luz", "Otro")

@Composable
private fun ResidentHome(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var showDialog by remember { mutableStateOf(false) }
    var sentMsg by remember { mutableStateOf<String?>(null) }
    val myCharges = Store.charges.filter { it.clientId == c.id && it.unit == user.unit }.sortedBy { it.due }
    val next = myCharges.firstOrNull { !it.paid }
    val confirmed = Store.emergencies.filter { it.clientId == c.id && it.status == EmergencyStatus.CONFIRMADA }
    val mine = Store.emergencies.filter {
        it.clientId == c.id && it.userName == user.name && it.status == EmergencyStatus.PENDIENTE
    }

    Screen {
        confirmed.forEach { e ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E0))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚠ EMERGENCIA CONFIRMADA: ${e.type}", fontWeight = FontWeight.Bold, color = RED)
                    Text("Reportada en ${e.unit}. Revisa tus pertenencias y sigue las indicaciones del administrador.")
                }
            }
        }

        CardBox {
            Text("Tu domicilio", fontWeight = FontWeight.Bold)
            Text(user.unit ?: "-")
            val resp = Store.responsibleOf(c.id, user.unit)
            if (resp != null) Text("Responsable: $resp")
            Text("Administración: ${Store.users.firstOrNull { it.clientId == c.id && it.role == Role.ADMIN }?.name ?: "-"}")
        }

        if (Feature.COBROS in c.modules) {
            CardBox {
                Text("Próximo pago", fontWeight = FontWeight.Bold)
                if (next == null) {
                    Text("Estás al día ✅", color = GREEN)
                } else {
                    Text(next.concept)
                    Text("Vence: ${next.due.format(dateFmt)}")
                    Text(money(Store.amountDue(next)), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (Store.isOverdue(next)) Text("⛔ Pago vencido", color = RED)
                    if (Store.amountDue(next) > next.amount) {
                        Text("Incluye recargo por mora de ${c.lateFeePercent}%", fontSize = 12.sp)
                    }
                }
            }
        }

        if (Feature.EMERGENCIA in c.modules) {
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = RED),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("🚨 BOTÓN DE EMERGENCIA", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            if (sentMsg != null) Text(sentMsg!!, color = GREEN)
            mine.forEach { e -> Text("Tu alerta «${e.type}» espera la confirmación del administrador.") }
            Text(
                "Esta app NO reemplaza a los servicios de emergencia. Llama: Bomberos 116 · Policía 105 · SAMU 106.",
                fontSize = 12.sp
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Qué está pasando?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se avisará primero al administrador para que lo confirme.")
                    EMERGENCY_TYPES.forEach { t ->
                        Button(
                            onClick = {
                                Store.addEmergency(user, t)
                                sentMsg = "Alerta enviada al administrador: $t"
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(t) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ResidentIncidencias(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    var service by remember { mutableStateOf(SERVICES.first()) }
    var desc by remember { mutableStateOf("") }
    val mine = Store.incidents.filter { it.clientId == c.id && it.userName == user.name }.reversed()
    Screen {
        H("Reportar una falla")
        CardBox {
            Choice(SERVICES, service) { service = it }
            OutlinedTextField(
                value = desc, onValueChange = { desc = it }, label = { Text("¿Qué está fallando?") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    Store.addIncident(user, service, desc.trim())
                    desc = ""
                },
                enabled = desc.isNotBlank()
            ) { Text("Enviar al administrador") }
        }
        H("Mis reportes")
        if (mine.isEmpty()) Text("No has enviado reportes.")
        mine.forEach { inc ->
            CardBox {
                Text(inc.service, fontWeight = FontWeight.Bold)
                Text(inc.description)
                Text("Estado: ${inc.status.label}")
                if (inc.status == IncidentStatus.RESUELTO) {
                    if (inc.confirmed == true) {
                        Text("✅ Confirmado por ti", color = GREEN)
                    } else {
                        Text("El administrador dice que ya está resuelto. ¿Es cierto?")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { Store.confirmIncident(inc.id, true) }) { Text("Sí, resuelto") }
                            OutlinedButton(onClick = { Store.confirmIncident(inc.id, false) }) { Text("Sigue fallando") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeList(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val list = Store.notices.filter { it.clientId == c.id }.reversed()
    Screen {
        H("Avisos")
        if (list.isEmpty()) Text("No hay avisos por ahora.")
        list.forEach { n ->
            CardBox {
                Text((if (n.urgent) "❗ " else "") + n.title, fontWeight = FontWeight.Bold)
                Text("${n.category} · ${n.createdAt}", fontSize = 12.sp)
                if (n.body.isNotBlank()) Text(n.body)
            }
        }
    }
}
