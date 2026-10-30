package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------------
// SUPER ADMIN · Clientes (lista, detalle y nuevo cliente) según Figma
// ------------------------------------------------------------------

private val ChipOff = Color(0xFFF0F2F5)
private val BoxBg = Color(0xFFF4F6FA)
private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

/** Funciones que se muestran al crear/editar un cliente (medidores solo en casas). */
private fun editableFeatures(t: Template?): List<Feature> = buildList {
    add(Feature.COBROS)
    add(Feature.MORA)
    add(Feature.INCIDENCIAS)
    add(Feature.AVISOS)
    add(Feature.EMERGENCIA)
    if (t == Template.CASA_DEPAS) add(Feature.MEDIDORES)
    add(Feature.AREAS_COMUNES)
    add(Feature.ASAMBLEAS)
}

private fun categoryLabel(t: Template) = if (t == Template.CONDOMINIO) "Condominio" else "Casa"

/** Chips de selección única (gris claro / azul noche) como en el diseño; pasan a otra línea si no entran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlatChips(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { o ->
            val sel = o == selected
            Text(
                o,
                color = if (sel) Color.White else Palette.Ink,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) Palette.Navy else ChipOff)
                    .clickable { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

/** Chips de selección única en una sola fila que se desliza (suscripciones). */
@Composable
fun ScrollChips(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { o ->
            val sel = o == selected
            Text(
                o,
                color = if (sel) Color.White else Palette.Ink,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) Palette.Navy else ChipOff)
                    .clickable { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        color = Color(0xFF4A5160)
    )
}

/** Interruptor de función (texto a la izquierda, switch verde azulado). */
@Composable
fun FeatureSwitch(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Palette.Teal,
                checkedBorderColor = Palette.Teal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Palette.Line,
                uncheckedBorderColor = Palette.Line
            )
        )
    }
}

// ---------------- Lista ----------------

@Composable
fun SuperClientes() {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todos") }
    val needle = q.trim().lowercase()
    val list = Store.clients.filter { c ->
        (filter == "Todos" || (filter == "Condominios" && c.template == Template.CONDOMINIO) ||
            (filter == "Casa" && c.template == Template.CASA_DEPAS)) &&
            (needle.isEmpty() || c.name.lowercase().contains(needle) ||
                (Store.ownerOf(c)?.name?.lowercase()?.contains(needle) == true))
    }

    ScreenColumn {
        SearchField(q, { q = it }, "Buscar")
        FlatChipsOutlined(listOf("Todos", "Condominios", "Casa"), filter) { filter = it }
        if (list.isEmpty()) EmptyState(Icons.Rounded.Apartment, "No hay clientes que coincidan.")
        list.forEachIndexed { i, c -> key(c.id) { ClientListRow(c, Modifier.enter(i)) { UiState.openClientId = c.id } } }
        Spacer(Modifier.height(8.dp))
    }
}

/** Chips de filtro: seleccionado azul noche, resto blanco con borde. */
@Composable
private fun FlatChipsOutlined(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val sel = o == selected
            Text(
                o,
                color = if (sel) Color.White else Palette.Ink,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) Palette.Navy else Palette.Surface)
                    .border(1.dp, if (sel) Palette.Navy else Palette.Line, CircleShape)
                    .clickable { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun ClientListRow(c: Client, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .pressScale(0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Color(0xFFF2F2F2), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Palette.TealSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (c.template == Template.CONDOMINIO) Icons.Rounded.Apartment else Icons.Rounded.Home,
                contentDescription = null,
                tint = Palette.Teal,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                c.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                categoryLabel(c.template) + " · " + Store.feeLabel(c),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!c.active) StatusTag("Inactivo", Palette.Muted, Palette.NavySoft) else SubTag(Store.subStatus(c))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(26.dp))
    }
}

/** Etiqueta de estado solo con texto (como en Figma). */
@Composable
fun StatusTag(text: String, fg: Color, bg: Color) {
    Text(
        text,
        color = fg,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
fun SubTag(st: SubStatus) = when (st) {
    SubStatus.PAGADO -> StatusTag(st.label, Palette.Green, Palette.GreenSoft)
    SubStatus.PENDIENTE -> StatusTag(st.label, Palette.AmberDark, Palette.AmberSoft)
    SubStatus.VENCIDO -> StatusTag(st.label, Palette.Red, Palette.RedSoft)
    SubStatus.GRATIS -> StatusTag(st.label, Palette.Teal, Palette.TealSoft)
    SubStatus.PRUEBA -> StatusTag(st.label, Palette.Navy, Palette.NavySoft)
}

// ---------------- Detalle (administrador de condominio o casa) ----------------

@Composable
fun SuperClientDetail(clientId: String, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val c = Store.client(clientId)

    Column(Modifier.fillMaxSize()) {
        RegresarButton(onBack, Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
        if (c != null) {
            ClientDetailCard(c) {
                UiState.deleteRequest = DeleteRequest(c.name, { Store.deleteClient(clientId) }, after = onBack)
            }
        }
    }
}

@Composable
private fun ClientDetailCard(c: Client, onDelete: () -> Unit) {
    val owner = Store.ownerOf(c)
    val st = Store.subStatus(c)
    val period = Store.periodFor(c)
    val pay = Store.subPayment(c.id, period)
    var fee by remember(c.id) { mutableStateOf(String.format(java.util.Locale.US, "%.2f", c.monthlyFee)) }
    var late by remember(c.id) { mutableStateOf(c.lateFeePercent.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }) }
    val monthName = if (period.startsWith("A")) "este año"
    else java.time.YearMonth.parse(period).month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("es", "PE"))
        .replaceFirstChar { it.uppercase() }

    ScreenColumn {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Palette.TealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (c.template == Template.CONDOMINIO) Icons.Rounded.Apartment else Icons.Rounded.Home,
                        contentDescription = null,
                        tint = Palette.Teal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                    Text(categoryLabel(c.template) + " · " + Store.feeLabel(c), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                }
                Switch(
                    checked = c.active,
                    onCheckedChange = { Store.updateClient(c.copy(active = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.Teal,
                        checkedBorderColor = Palette.Teal,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Palette.Line,
                        uncheckedBorderColor = Palette.Line
                    )
                )
            }
            LineDivider()

            SectionLabel("Categoría")
            FlatChips(listOf("Condominio", "Casa"), categoryLabel(c.template)) { l ->
                val t = if (l == "Condominio") Template.CONDOMINIO else Template.CASA_DEPAS
                if (t != c.template) Store.updateClient(c.copy(template = t, modules = Store.defaultsFor(t)))
            }

            SectionLabel("Suscripción")
            // Planes del catálogo (Planes → crear o cambiar precio)
            FlatChips(Store.plans.map { it.name }, Store.planOf(c)?.name) { n ->
                Store.plans.firstOrNull { it.name == n }?.let { p ->
                    Store.assignPlan(c.id, p)
                    fee = String.format(java.util.Locale.US, "%.2f", p.price)
                }
            }
            if (c.plan != Plan.GRATIS) {
                AppTextField(
                    fee,
                    { text ->
                        fee = text
                        text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(monthlyFee = v)) }
                    },
                    if (c.plan == Plan.ANUAL) "Anualidad (S/)" else "Mensualidad (S/)",
                    icon = Icons.Rounded.CreditCard,
                    keyboardType = KeyboardType.Decimal
                )
            }

            SectionLabel("Métodos de pagos")
            FlatChips(PAYMENT_METHODS, c.paymentMethod) { m -> Store.updateClient(c.copy(paymentMethod = m)) }

            // Suscripción del periodo
            InfoBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Suscripción de $monthName", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Responsable del pago: ${owner?.name ?: "sin administrador"}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                        Text(
                            if (pay != null) "Facturación: ${parsePaid(pay.paidAt, period)}"
                            else if (c.plan == Plan.GRATIS) "Sin cobro"
                            else "Vence: ${Store.subDue(period).format(shortDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Muted
                        )
                    }
                    SubTag(st)
                }
                if (c.plan != Plan.GRATIS) {
                    Text(
                        if (st == SubStatus.PAGADO) "Deshacer pago" else "Marcar como pagado",
                        color = if (st == SubStatus.PAGADO) Palette.Muted else Palette.Teal,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { Store.toggleSubscriptionPaid(c) }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Información del cliente (owner)
            InfoBox {
                Text("Información del cliente", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Nombre completo: ${owner?.name ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                Text("DNI: ${owner?.dni ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                Text("Celular: ${owner?.phone?.let { formatPhone(it) } ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }

            SectionLabel("Funciones del cliente")
            editableFeatures(c.template).forEach { f ->
                FeatureSwitch(f.label, f in c.modules) { on ->
                    Store.updateClient(c.copy(modules = if (on) c.modules + f else c.modules - f))
                }
            }
            val moraOn = Feature.MORA in c.modules
            AppTextField(
                if (moraOn) late else "0",
                { text ->
                    if (moraOn) {
                        late = text
                        text.toDoubleOrNull()?.let { v -> Store.updateClient(c.copy(lateFeePercent = v)) }
                    }
                },
                "Recargo por mora (%)",
                icon = Icons.Rounded.CreditCard,
                keyboardType = KeyboardType.Decimal
            )

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Red, contentColor = Color.White)
            ) {
                Text("Eliminar usuario", style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun parsePaid(paidAt: String, period: String): String {
    // paidAt se guarda como "dd/MM HH:mm" o "dd/MM"; se completa con el año del periodo
    val dm = paidAt.substringBefore(" ")
    val year = period.removePrefix("A").substringBefore("-").takeLast(2)
    return "$dm/$year"
}

private fun formatPhone(p: String): String =
    if (p.length == 9) "${p.substring(0, 3)} ${p.substring(3, 6)} ${p.substring(6)}" else p

@Composable
private fun InfoBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BoxBg)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

// ---------------- Nuevo cliente (botón +) ----------------

@Composable
fun SuperNewClient(onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf<Template?>(null) }
    var planSel by remember { mutableStateOf<SubscriptionPlan?>(null) }
    val plan = planSel?.cycle
    var fee by remember { mutableStateOf("30.00") }
    var method by remember { mutableStateOf<String?>(null) }
    var owner by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var modules by remember { mutableStateOf(setOf<Feature>()) }
    var late by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    var created by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nuevo cliente", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
            AppTextField(name, { name = it; error = null }, "Nombre del condominio o casa", icon = Icons.Rounded.Apartment, placeholder = "Ej. Casa")

            SectionLabel("Categoría")
            FlatChips(listOf("Condominio", "Casa"), template?.let { categoryLabel(it) }) { l ->
                template = if (l == "Condominio") Template.CONDOMINIO else Template.CASA_DEPAS
                if (template == Template.CONDOMINIO) modules = modules - Feature.MEDIDORES
                else if (planSel?.fullFeatures == true) modules = modules + Feature.MEDIDORES
                error = null
            }

            SectionLabel("Suscripción")
            ScrollChips(Store.plans.map { it.name }, planSel?.name) { n ->
                planSel = Store.plans.firstOrNull { it.name == n }
                planSel?.let { p ->
                    fee = String.format(java.util.Locale.US, "%.2f", p.price)
                    if (p.fullFeatures) modules = editableFeatures(template).toSet()
                }
                error = null
            }
            AppTextField(
                if (plan == Plan.GRATIS) "0.00" else fee,
                { if (plan != Plan.GRATIS) fee = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                if (plan == Plan.ANUAL) "Anualidad (S/)" else "Mensualidad (S/)",
                icon = Icons.Rounded.CreditCard,
                keyboardType = KeyboardType.Decimal
            )
            planSel?.let { p -> if (p.trial) Text("Prueba gratis de $TRIAL_DAYS días; luego aparece como vencido.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted) }

            SectionLabel("Métodos de pagos")
            FlatChips(PAYMENT_METHODS, method) { method = it }

            AppTextField(owner, { owner = it; error = null }, "Nombre completo", icon = Icons.Rounded.PersonOutline, placeholder = "Ej. Pedro")
            AppTextField(
                dni,
                { dni = it.filter { ch -> ch.isDigit() }.take(8); error = null },
                "DNI",
                icon = Icons.Rounded.Badge,
                keyboardType = KeyboardType.Number,
                placeholder = "Ej. 87654321"
            )
            AppTextField(
                phone,
                { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null },
                "Celular",
                icon = Icons.Rounded.PhoneAndroid,
                keyboardType = KeyboardType.Phone,
                placeholder = "Ej. 987 654 321"
            )

            SectionLabel("Funciones del cliente")
            editableFeatures(template).forEach { f ->
                FeatureSwitch(f.label, f in modules) { on -> modules = if (on) modules + f else modules - f }
            }
            val moraOn = Feature.MORA in modules
            AppTextField(
                if (moraOn) late else "0",
                { if (moraOn) late = it },
                "Recargo por mora (%)",
                icon = Icons.Rounded.CreditCard,
                keyboardType = KeyboardType.Decimal
            )

            error?.let { ErrorText(it) }
            Button(
                onClick = {
                    val r = Store.createClientWithOwner(
                        name, template, planSel, fee.toDoubleOrNull(), method, modules,
                        if (moraOn) late.toDoubleOrNull() ?: 0.0 else 0.0,
                        owner, dni, phone
                    )
                    error = r
                    if (r == null) created = Store.lastCreated?.username ?: dni
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal, contentColor = Color.White)
            ) {
                Text("Crear", style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    created?.let { d ->
        ResultDialog(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Cliente creado",
            text = "Usuario: $d\nContraseña: $DEFAULT_PASSWORD\nTambién puede ingresar con su DNI.",
            button = "Aceptar"
        ) {
            created = null
            onDone()
        }
    }
}

// ---------------- Eliminar: confirmar → eliminando → eliminado ----------------

/**
 * Flujo para eliminar un usuario o cliente:
 * 1) ventana "¿Deseas eliminar?" (Sí / No), 2) "Eliminando usuario…", 3) "Se ha eliminado correctamente".
 */
@Composable
fun DeleteUserFlow(name: String, onConfirmDelete: () -> Unit, onCancel: () -> Unit, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(0) } // 0 confirmar, 1 eliminando, 2 listo
    LaunchedEffect(step) {
        if (step == 1) {
            delay(1200)
            onConfirmDelete()
            step = 2
        }
    }
    when (step) {
        0 -> Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                CircleIcon(Icons.Rounded.DeleteOutline, Palette.Red, Palette.RedSoft)
                Text(
                    "¿Deseas eliminar este usuario?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Se eliminará «$name» y sus cuentas ya no podrán ingresar. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                    color = Palette.Muted,
                    textAlign = TextAlign.Center
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line)
                    ) {
                        Text("No", color = Palette.Ink, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Red, contentColor = Color.White)
                    ) {
                        Text("Sí, eliminar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                }
            }
        }
        1 -> Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
        ) {
            DialogCard {
                Box(Modifier.height(52.dp), contentAlignment = Alignment.Center) { BouncingDots(Palette.Teal, 12.dp) }
                Text(
                    "Eliminando usuario…",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
                Text("Espera un momento.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
        else -> ResultDialog(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Se ha eliminado correctamente",
            text = "«$name» ya no está en Convive.",
            button = "Aceptar",
            onClose = onFinished
        )
    }
}

@Composable
fun DialogCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .popIn()
            .fillMaxWidth(0.86f)
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color) {
    Box(Modifier.size(64.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
    }
}

@Composable
fun ResultDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    text: String,
    button: String,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            if (icon == Icons.Rounded.CheckCircle) AnimatedCheck(64.dp, iconTint, iconBg) else CircleIcon(icon, iconTint, iconBg)
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp), color = Palette.Muted, textAlign = TextAlign.Center)
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 2.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Navy, contentColor = Color.White)
            ) {
                Text(button, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            }
        }
    }
}
