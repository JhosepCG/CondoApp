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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, shape = RoundedCornerShape(12.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showDemo by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                .systemBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text("🏢 CondoApp", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Tu condominio, en tu bolsillo", color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
        }
        Screen {
            CardBox {
                Text("Iniciar sesión", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Field(username, { username = it; error = false }, "Usuario")
                Field(password, { password = it; error = false }, "Contraseña", password = true)
                if (error) Text("Usuario o contraseña incorrectos (o el cliente está inactivo).", color = RED, fontSize = 13.sp)
                Button(
                    onClick = {
                        val u = Store.login(username, password)
                        if (u == null) error = true else Store.currentUser = u
                    },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Entrar", fontSize = 16.sp) }
            }
            CardBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cuentas de prueba", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showDemo = !showDemo }) { Text(if (showDemo) "Ocultar" else "Ver") }
                }
                if (showDemo) {
                    Text("Toca una cuenta para rellenar los datos.", fontSize = 12.sp)
                    Store.users.filter { it.username.isNotBlank() }.forEach { u ->
                        Column(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                .clickable { username = u.username; password = u.password; error = false }
                                .padding(12.dp)
                        ) {
                            Text(u.role.label + " · " + u.name, fontWeight = FontWeight.Bold)
                            Text("usuario: ${u.username}   contraseña: ${u.password}", fontSize = 13.sp)
                        }
                    }
                }
            }
            Text(
                "Modo demostración: los datos viven solo en el teléfono y se reinician al cerrar la app.",
                fontSize = 12.sp
            )
        }
    }
}

private enum class Tab(val label: String, val icon: String) {
    RESUMEN("Resumen", "📊"),
    CLIENTES("Clientes", "🏢"),
    CUENTAS("Cuentas", "🔑"),
    PANEL("Panel", "🚨"),
    COBROS("Cobros", "💳"),
    INCIDENCIAS("Servicios", "🛠"),
    AVISOS("Avisos", "📣"),
    RESIDENTES("Cuentas", "👥"),
    INICIO("Inicio", "🏠")
}

private fun tabsFor(user: AppUser): List<Tab> {
    val c = Store.clientOf(user)
    fun on(f: Feature): Boolean = c != null && f in c.modules
    return when (user.role) {
        Role.SUPER_ADMIN -> listOf(Tab.RESUMEN, Tab.CLIENTES, Tab.CUENTAS)
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
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, color = Color.White)
                val cl = Store.clientOf(user)
                Text(user.role.label + (if (cl != null) " · " + cl.name else ""), fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            }
            TextButton(onClick = { Store.currentUser = null }) { Text("Salir", color = Color.White) }
        }
        Box(Modifier.weight(1f)) {
            when (current) {
                Tab.RESUMEN -> SuperResumen()
                Tab.CLIENTES -> SuperClientes()
                Tab.CUENTAS -> SuperCuentas()
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
            Text("Inquilinos registrados: ${Store.users.count { it.role != Role.SUPER_ADMIN }}")
        }
        Store.clients.forEach { c ->
            val us = Store.users.filter { it.clientId == c.id }
            CardBox {
                Text(c.name, fontWeight = FontWeight.Bold)
                Text(c.template.label + (if (c.active) "" else " · INACTIVO"))
                Text(
                    "Administradores: ${us.count { it.role == Role.ADMIN }} · " +
                        "Inquilinos: ${us.count { it.role == Role.USER }}"
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
            Text("Inquilinos: ${Store.users.count { it.clientId == c.id && it.role == Role.USER }}")
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
                    Pill("Pagado", GREEN)
                } else {
                    if (Store.isOverdue(ch)) Pill("Vencido", RED) else Pill("Pendiente", Color(0xFFB26A00))
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
private fun AccountForm(fixedClient: Client?) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf(Role.USER.label) }
    var clientName by remember { mutableStateOf(Store.clients.firstOrNull()?.name ?: "") }
    var msg by remember { mutableStateOf<String?>(null) }

    val client = fixedClient ?: Store.clients.firstOrNull { it.name == clientName }
    val isAdmin = roleLabel == Role.ADMIN.label
    val concept = if (client?.template == Template.CASA_DEPAS) "renta mensual" else "cuota mensual"
    val taken = username.isNotBlank() && Store.usernameTaken(username)
    val valid = name.isNotBlank() && username.isNotBlank() && !taken && password.length >= 4 && client != null &&
        (isAdmin || (unit.isNotBlank() && amount.toDoubleOrNull() != null))

    CardBox {
        Text("Tipo de cuenta", fontWeight = FontWeight.Bold)
        Choice(listOf(Role.ADMIN.label, Role.USER.label), roleLabel) { roleLabel = it }
        if (fixedClient == null) {
            Text("Cliente", fontWeight = FontWeight.Bold)
            Choice(Store.clients.map { it.name }, clientName) { clientName = it }
        }
        Field(name, { name = it }, "Nombre completo")
        Field(username, { username = it; msg = null }, "Usuario (para iniciar sesión)")
        if (taken) Text("Ese usuario ya existe.", color = RED, fontSize = 12.sp)
        Field(password, { password = it }, "Contraseña (mínimo 4)")
        if (!isAdmin) {
            Field(unit, { unit = it }, "Departamento / casa")
            Field(amount, { amount = it }, "Monto de la $concept (S/)")
        }
        Button(
            onClick = {
                client?.let { cl ->
                    Store.createAccount(
                        name.trim(), username.trim(), password,
                        if (isAdmin) Role.ADMIN else Role.USER, cl.id,
                        if (isAdmin) null else unit.trim(), amount.toDoubleOrNull() ?: 0.0
                    )
                }
                msg = "Cuenta creada: ${username.trim()}"
                name = ""; username = ""; password = ""; unit = ""; amount = ""
            },
            enabled = valid,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Crear cuenta") }
        if (msg != null) Text(msg!!, color = GREEN)
    }
}

@Composable
private fun AccountRow(p: AppUser, canDelete: Boolean) {
    var confirm by remember { mutableStateOf(false) }
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Bold)
                Text((Store.client(p.clientId)?.name ?: "-") + (if (p.unit != null) " · ${p.unit}" else ""), fontSize = 12.sp)
                Text("usuario: ${p.username}   contraseña: ${p.password}", fontSize = 12.sp)
            }
            Pill(p.role.label, if (p.role == Role.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
        }
        if (canDelete) TextButton(onClick = { confirm = true }) { Text("Eliminar cuenta", color = RED) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("¿Eliminar a ${p.name}?") },
            text = { Text("La cuenta dejará de poder iniciar sesión.") },
            confirmButton = {
                TextButton(onClick = { Store.deleteAccount(p.id); confirm = false }) { Text("Eliminar", color = RED) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SuperCuentas() {
    val people = Store.users.filter { it.role != Role.SUPER_ADMIN }
    Screen {
        H("Crear cuenta")
        AccountForm(null)
        H("Cuentas (${people.size})")
        people.toList().forEach { p -> key(p.id) { AccountRow(p, true) } }
    }
}

@Composable
private fun AdminResidentes(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val people = Store.users.filter { it.clientId == c.id }
    Screen {
        H("Crear cuenta")
        AccountForm(c)
        H("Cuentas (${people.size})")
        people.toList().forEach { p -> key(p.id) { AccountRow(p, p.id != user.id) } }
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
                    Pill("Estás al día", GREEN)
                } else {
                    Text(next.concept)
                    Text("Vence: ${next.due.format(dateFmt)}")
                    Text(money(Store.amountDue(next)), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (Store.isOverdue(next)) Pill("Pago vencido", RED)
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
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(80.dp)
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
