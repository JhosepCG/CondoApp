package pe.condoapp

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.Period

// ------------------------------------------------------------------
// Perfil de inquilino (Administrador y Super Admin)
// El administrador ve la versión completa: foto, depa, tiempo en la casa,
// pagos, medidor, reportes y contrato, y puede editar la información.
// El Super Admin solo ve pagos y reportes (nunca contrato, DNI ni medidores).
// ------------------------------------------------------------------

/** «1 año & 1 mes», «3 meses», «2 años»… desde que vive en el depa. */
fun tenureLabel(since: LocalDate): String {
    val p = Period.between(since, LocalDate.now())
    val y = p.years
    val m = p.months
    val ys = if (y == 1) "1 año" else "$y años"
    val ms = if (m == 1) "1 mes" else "$m meses"
    return when {
        y > 0 && m > 0 -> "$ys & $ms"
        y > 0 -> ys
        m > 0 -> ms
        else -> "Menos de 1 mes"
    }
}

fun contractServiceIcon(s: String): ImageVector = when {
    s.startsWith("Agua") -> Ic.Drop
    s.startsWith("Luz") -> Ic.Bolt
    s.startsWith("Internet") -> Ic.Wifi
    s.startsWith("Cable") -> Ic.Tv
    s.startsWith("Gas") -> Icons.Rounded.Air
    s.startsWith("Limpieza") -> Ic.Home
    s.startsWith("Cochera") -> Ic.Building
    else -> Ic.CheckCircle
}

/** Estado de pago del inquilino: pagado el día X, pendiente hasta el día Y o vencido. */
class PayState(val label: String, val sub: String, val fg: Color, val bg: Color, val icon: ImageVector)

fun payStateOf(u: AppUser): PayState? {
    val next = Store.nextUnpaid(u)
    val last = Store.lastPaid(u)
    return when {
        next != null && Store.isOverdue(next) ->
            PayState("Vencido", "Venció el ${next.due.format(dateFmt)} · ${money(Store.amountDue(next))}", Palette.Red, Palette.RedSoft, Ic.AlertCircle)
        next != null && (last == null || !next.due.isAfter(LocalDate.now().plusDays(7))) ->
            PayState("Pendiente", "Debe pagar el ${next.due.format(dateFmt)} · ${money(next.amount)}", Palette.AmberDark, Palette.AmberSoft, Ic.Clock)
        last != null ->
            PayState("Pagado", "Se pagó el ${(last.paidOn ?: last.due).format(dateFmt)}", Palette.Green, Palette.GreenSoft, Ic.CheckCircle)
        else -> null
    }
}

@Composable
fun TenantProfile(viewer: AppUser, tenantId: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val u = Store.users.firstOrNull { it.id == tenantId }
    if (u == null) {
        LaunchedEffect(tenantId) { onBack() }
        return
    }
    val isAdmin = viewer.role == Role.ADMIN
    val c = Store.client(u.clientId)
    val st = tenantStatus(u)
    val contract = if (isAdmin && c?.template == Template.CASA_DEPAS) Store.contractOf(u.id) else null
    val showMeters = isAdmin && Store.featureOn(c, Feature.MEDIDORES)
    val req = if (showMeters && c != null) Store.activeMeterRequest(c.id) else null
    val readings = Store.meterReadings.filter { it.userId == u.id }.reversed()
    val incidents = Store.incidents.filter { it.userId == u.id }.reversed()
    val charges = Store.charges.filter { it.clientId == u.clientId && it.unit == u.unit }
        .sortedWith(compareBy<Charge> { it.paid }.thenByDescending { it.due })

    var viewing by remember { mutableStateOf<MeterReading?>(null) }
    var payId by remember { mutableStateOf<String?>(null) }
    var showCharges by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var renewing by remember { mutableStateOf(false) }
    var askSign by remember { mutableStateOf(false) }
    var i = 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Ic.ChevronLeft, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(22.dp))
            Text("Regresar", color = Palette.Teal, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
        }

        // ---------- Cabecera: foto, nombre, depa y tiempo en la casa ----------
        val since = if (isAdmin) Store.tenantSince(u.id) else null
        Row(
            Modifier
                .enter(i++)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.Navy)
                .padding(horizontal = 12.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserAvatar(u, 42.dp, bg = Palette.TealSoft, fg = Palette.Teal)
            Column(Modifier.weight(1f)) {
                Text(
                    u.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(u.unit, since?.let { tenureLabel(it) } ?: if (!isAdmin) c?.name else null).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdmin) {
                Box(
                    Modifier
                        .pressScale(0.94f)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { editing = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.Gear, contentDescription = "Editar información", tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }

        // ---------- Pagos ----------
        if (Store.featureOn(c, Feature.COBROS)) {
            val ps = payStateOf(u)
            InfoCard(
                icon = Ic.Card,
                title = "Pagos",
                sub = ps?.sub ?: "Sin cobros registrados",
                modifier = Modifier.enter(i++),
                onClick = { showCharges = !showCharges },
                trailing = { ps?.let { StatePill(it.label, it.fg, it.bg, it.icon) } }
            )
            AnimatedVisibility(
                showCharges,
                enter = fadeIn(tween(Motion.QUICK_MS)) + expandVertically(tween(Motion.QUICK_MS + 40, easing = Motion.EaseOut)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(Motion.QUICK_MS, easing = Motion.EaseOut))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (charges.isEmpty()) EmptyState(Ic.Cash, "Sin cobros registrados.")
                    charges.take(6).forEach { ch ->
                        key(ch.id) { ChargeCard(ch, onPay = if (isAdmin) ({ payId = ch.id }) else null) }
                    }
                }
            }
        }

        // ---------- Medidor ----------
        if (showMeters) {
            val last = readings.firstOrNull()
            InfoCard(
                icon = Ic.Bolt,
                title = "Medidor",
                sub = when {
                    req != null && st.meterSent == true -> "Envió la foto · ${last?.sentAt ?: ""}"
                    req != null -> "Debe enviar la foto hasta el ${req.due.format(dateFmt)}"
                    last != null -> "Última foto: ${last.sentAt}"
                    else -> "Aún no envió fotos del medidor"
                },
                modifier = Modifier.enter(i++),
                trailing = {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Palette.NavySoft)
                            .alpha(if (last != null) 1f else 0.4f)
                            .clickable(enabled = last != null) { viewing = last },
                        contentAlignment = Alignment.Center
                    ) { Icon(Ic.Camera, contentDescription = "Ver foto del medidor", tint = Palette.Ink, modifier = Modifier.size(22.dp)) }
                }
            )
        }

        // ---------- Reporte ----------
        if (Store.featureOn(c, Feature.INCIDENCIAS)) {
            val open = st.openIncidents
            InfoCard(
                icon = Ic.AlertOctagon,
                title = "Reporte",
                sub = when {
                    open == 1 -> "1 reporte abierto · ${incidents.first().service}"
                    open > 1 -> "$open reportes abiertos"
                    incidents.isNotEmpty() -> "Sin reportes abiertos"
                    else -> "No ha registrado algún reporte"
                },
                modifier = Modifier.enter(i++),
                trailing = if (!isAdmin) null else @Composable {
                    Text(
                        "Reportar",
                        color = Palette.Teal,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { reporting = true }.padding(6.dp)
                    )
                }
            )
        }

        // ---------- Contrato (solo el administrador de una casa) ----------
        if (isAdmin && c != null && c.template == Template.CASA_DEPAS) {
            if (contract == null) {
                ContractAdminSection(u, c)
            } else {
                ContractCard(
                    contract,
                    Modifier.enter(i++),
                    onNotSigned = { askSign = true },
                    onRenewed = { renewing = true }
                )
            }
        }

        if (viewer.role == Role.SUPER_ADMIN) {
            SectionTitle("Cuenta")
            UserRow(u, canDelete = true, showClient = true)
        }
        Spacer(Modifier.height(8.dp))
    }

    viewing?.let { m -> MeterPhotoDialog(m) { viewing = null } }
    payId?.let { id ->
        ConfirmDialog(
            title = "Registrar pago",
            text = "¿Confirmas que ${u.name} pagó este cobro? Quedará como pagado hoy, ${LocalDate.now().format(dateFmt)}.",
            confirm = "Registrar",
            onConfirm = { Store.markPaid(id) },
            onDismiss = { payId = null },
            danger = false
        )
    }
    if (askSign && contract != null) {
        ConfirmDialog(
            title = "No firmó el contrato",
            text = "Le avisaremos a ${u.name.substringBefore(" ")} que debe firmar nuevamente su contrato.",
            confirm = "Avisar",
            onConfirm = { Store.requestRenewal(u.id) },
            onDismiss = { askSign = false },
            danger = false
        )
    }
    if (renewing) {
        Dialog(onDismissRequest = { renewing = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                CircleIcon(Ic.Doc, Palette.Teal, Palette.TealSoft)
                Text("¡Renovó contrato!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text("¿Por cuánto tiempo firmó?", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("6 meses", { Store.renewContract(u.id, 6); renewing = false }, Modifier.weight(1f))
                    PrimaryButton("12 meses", { Store.renewContract(u.id, 12); renewing = false }, Modifier.weight(1f), icon = Ic.Check)
                }
                TextButton(onClick = { renewing = false }) { Text("Cancelar", color = Palette.Muted) }
            }
        }
    }
    if (reporting) ReportForTenantDialog(u) { reporting = false }
    if (editing) EditTenantSheet(u) { editing = false }
}

// ---------------- Piezas ----------------

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .then(if (onClick != null) Modifier.pressScale(0.98f) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Palette.GreenSoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Palette.Green, modifier = Modifier.size(24.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Palette.Ink)
            Text(sub, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
private fun StatePill(text: String, fg: Color, bg: Color, icon: ImageVector) {
    Row(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContractCard(ct: Contract, modifier: Modifier, onNotSigned: () -> Unit, onRenewed: () -> Unit) {
    val expired = Store.contractExpired(ct)
    val soon = Store.contractEndingSoon(ct)
    val (label, fg, bg, icon) = when {
        expired -> Quad("Vencido", Palette.Red, Palette.RedSoft, Ic.AlertCircle)
        soon -> Quad("Por vencer", Palette.AmberDark, Palette.AmberSoft, Ic.Clock)
        else -> Quad("Al día", Palette.Green, Palette.GreenSoft, Ic.CheckCircle)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Palette.AmberSoft),
                contentAlignment = Alignment.Center
            ) { Icon(Ic.Doc, contentDescription = null, tint = Palette.AmberDark, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text("Contrato", style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Palette.Ink)
                Text(
                    "${ct.start.format(dateFmt)} al ${ct.end.format(dateFmt)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Palette.Muted
                )
            }
            StatePill(label, fg, bg, icon)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
        Row {
            Field("Pago mensual", money(ct.monthlyRent), Modifier.weight(1f))
            Field("Garantía", if (ct.guarantee) money(ct.guaranteeAmount) else "No dejó", Modifier.weight(1f))
        }
        Field("DNI", ct.dni.ifBlank { "-" }, Modifier)
        if (ct.services.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Servicios incluidos", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ct.services.forEach { s ->
                        Row(
                            Modifier.clip(CircleShape).background(Palette.TealSoft).padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(contractServiceIcon(s), contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(16.dp))
                            Text(s, color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
        if (expired || soon) {
            if (ct.renewRequested) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.TealSoft).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Ic.Mail, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
                    Text(
                        "Ya le avisaste que debe firmar nuevamente · ${ct.renewRequestedAt ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Teal
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNotSigned,
                    enabled = !ct.renewRequested,
                    modifier = Modifier.weight(1f).height(48.dp).pressScale(),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Surface, contentColor = Palette.Ink)
                ) { Text("No firmó", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) }
                Button(
                    onClick = onRenewed,
                    modifier = Modifier.weight(1f).height(48.dp).pressScale(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Navy, contentColor = Color.White)
                ) { Text("¡Renovó contrato!", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), maxLines = 1) }
            }
        }
    }
}

private data class Quad(val a: String, val b: Color, val c: Color, val d: ImageVector)

@Composable
private fun Field(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = Palette.Ink)
    }
}

/** El administrador registra un reporte a nombre del inquilino. */
@Composable
private fun ReportForTenantDialog(u: AppUser, onClose: () -> Unit) {
    var service by remember { mutableStateOf(SERVICES.first()) }
    var desc by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Text("Reportar", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("Registra una falla a nombre de ${u.name.substringBefore(" ")}.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center)
            FlatChips(SERVICES, service) { service = it }
            AppTextField(desc, { desc = it }, "¿Qué pasó?", singleLine = false, minLines = 2, placeholder = "Ej. No hay agua caliente")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancelar", onClose, Modifier.weight(1f))
                PrimaryButton(
                    "Reportar",
                    {
                        Store.addIncident(u, service, desc)
                        onClose()
                    },
                    Modifier.weight(1f),
                    enabled = desc.isNotBlank()
                )
            }
        }
    }
}

// ---------------- Editar información (hoja inferior) ----------------

private val EDIT_SERVICES = listOf("Agua", "Luz", "Internet", "Cable", "Gas")

private fun num(v: Double): String = if (v % 1.0 == 0.0) String.format(java.util.Locale.US, "%.2f", v) else v.toString()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditTenantSheet(u: AppUser, onClose: () -> Unit) {
    val context = LocalContext.current
    val c = Store.client(u.clientId)
    val ct = if (c?.template == Template.CASA_DEPAS) Store.contractOf(u.id) else null
    val cobros = Store.featureOn(c, Feature.COBROS)
    val initialDate = remember { Store.nextUnpaid(u)?.due ?: LocalDate.now().plusMonths(1).withDayOfMonth(minOf(5, 28)) }

    // Valores iniciales (para saber si algo cambió)
    val init = remember {
        listOf(
            u.name,
            u.dni ?: ct?.dni ?: "",
            u.phone ?: "",
            ct?.monthlyRent?.let { num(it) } ?: "",
            ct?.guaranteeAmount?.takeIf { it > 0 }?.let { num(it) } ?: ""
        )
    }
    var name by remember { mutableStateOf(init[0]) }
    var dni by remember { mutableStateOf(init[1]) }
    var phone by remember { mutableStateOf(init[2]) }
    var rent by remember { mutableStateOf(init[3]) }
    var guarantee by remember { mutableStateOf(init[4]) }
    val initServices = remember { ct?.services?.toSet() ?: emptySet() }
    var services by remember { mutableStateOf(initServices) }
    var payDate by remember { mutableStateOf(initialDate) }
    var photo by remember { mutableStateOf(u.photoPath) }
    var newPhoto by remember { mutableStateOf<String?>(null) }
    var photoMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var askLeave by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val dirty = listOf(name, dni, phone, rent, guarantee) != init || services != initServices ||
        payDate != initialDate || photo != u.photoPath

    val picker = rememberPhotoPicker(
        onResult = { path ->
            newPhoto?.let { Photos.delete(it) }
            newPhoto = path
            photo = path
        },
        onError = { error = it }
    )

    val discard: () -> Unit = {
        newPhoto?.let { Photos.delete(it) }
        onClose()
    }
    val save: () -> Unit = {
        val r = Store.updateTenantInfo(
            u.id, name, dni, phone, photo,
            rent = if (ct != null) rent.toDoubleOrNull() ?: 0.0 else null,
            guarantee = if (ct != null) guarantee.toDoubleOrNull() ?: 0.0 else null,
            services = if (ct != null) {
                (EDIT_SERVICES.filter { it in services } + services.filter { it !in EDIT_SERVICES })
            } else null,
            payDate = if (cobros && payDate != initialDate) payDate else null
        )
        error = r
        askLeave = false
        if (r == null) {
            // Si cambió la foto, borrar la anterior
            if (photo != u.photoPath) u.photoPath?.let { Photos.delete(it) }
            onClose()
        }
    }
    val tryClose: () -> Unit = { if (dirty) askLeave = true else onClose() }

    Dialog(
        onDismissRequest = tryClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = tryClose)
            )
            AnimatedVisibility(
                visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(Motion.SCREEN_MS, easing = Motion.EaseDrawer)) { it } + fadeIn(tween(Motion.QUICK_MS))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 760.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Palette.Surface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Editar Información", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Palette.Ink)

                    // Foto del inquilino
                    Box(Modifier.size(96.dp).clip(CircleShape).clickable { photoMenu = true }) {
                        val shown = u.copy(photoPath = photo)
                        key(photo) { UserAvatar(shown, 88.dp, bg = Palette.TealSoft, fg = Palette.Teal) }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(Palette.Navy),
                            contentAlignment = Alignment.Center
                        ) { Icon(Ic.Camera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                    Text(
                        if (photo == null) "Toca para subir una foto" else "Toca para cambiar la foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted
                    )

                    AppTextField(name, { name = it; error = null }, "Nombre del inquilino", Modifier.fillMaxWidth(), icon = Ic.User, placeholder = "Ej. Pedro Cumpa")
                    AppTextField(
                        dni, { dni = it.filter { ch -> ch.isDigit() }.take(8); error = null }, "DNI", Modifier.fillMaxWidth(),
                        icon = Ic.IdCard, keyboardType = KeyboardType.Number, placeholder = "Ej. 87654321"
                    )
                    AppTextField(
                        phone, { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null }, "Celular", Modifier.fillMaxWidth(),
                        icon = Ic.Phone, keyboardType = KeyboardType.Phone, placeholder = "Ej. 987 654 321"
                    )
                    if (ct != null) {
                        AppTextField(
                            rent, { rent = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null }, "Mensualidad (S/)", Modifier.fillMaxWidth(),
                            icon = Ic.Card, keyboardType = KeyboardType.Decimal, placeholder = "Ej. 650.00"
                        )
                        AppTextField(
                            guarantee, { guarantee = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null }, "Garantía (S/)", Modifier.fillMaxWidth(),
                            icon = Ic.Card, keyboardType = KeyboardType.Decimal, placeholder = "Ej. 650.00"
                        )
                    }

                    // Fecha de pago
                    if (cobros) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Fecha de pago", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = Palette.Muted)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Palette.Field)
                                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d -> payDate = LocalDate.of(y, m + 1, d) },
                                            payDate.year, payDate.monthValue - 1, payDate.dayOfMonth
                                        ).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Ic.Calendar, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(24.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Debe pagar el", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                                    Text(payDate.format(dateFmt), style = MaterialTheme.typography.bodyLarge, color = Palette.Ink)
                                }
                                Icon(Ic.ChevronDown, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                if (Store.nextUnpaid(u) != null) "Cambia la fecha de su próximo pago pendiente."
                                else "Se creará su próximo cobro con esta fecha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.Muted
                            )
                        }
                    }

                    if (ct != null) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Servicios", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = Palette.Muted)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                (EDIT_SERVICES + initServices.filter { it !in EDIT_SERVICES }).forEach { s ->
                                    val on = s in services
                                    Text(
                                        if (s == "Gas") "Gas Calidda" else s,
                                        color = if (on) Color.White else Palette.Ink,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier
                                            .pressScale(0.95f)
                                            .clip(CircleShape)
                                            .background(if (on) Palette.Navy else Palette.Field)
                                            .clickable { services = if (on) services - s else services + s }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    error?.let { ErrorText(it) }

                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = discard,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Surface, contentColor = Palette.Ink)
                        ) { Text("Cancelar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) }
                        // Desactivado (30 % de opacidad) hasta que se cambie algo
                        Button(
                            onClick = save,
                            enabled = dirty,
                            modifier = Modifier.weight(1f).height(48.dp).alpha(if (dirty) 1f else 0.3f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Palette.Teal,
                                contentColor = Color.White,
                                disabledContainerColor = Palette.Teal,
                                disabledContentColor = Color.White
                            )
                        ) { Text("Guardar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) }
                    }
                }
            }
        }

        if (photoMenu) {
            Dialog(onDismissRequest = { photoMenu = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                DialogCard {
                    Text("Foto del inquilino", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    SecondaryButton("Tomar foto", { photoMenu = false; picker.camera() }, Modifier.fillMaxWidth(), icon = Ic.Camera)
                    SecondaryButton("Elegir de la galería", { photoMenu = false; picker.gallery() }, Modifier.fillMaxWidth(), icon = Ic.Image)
                    if (photo != null) {
                        TextButton(onClick = {
                            newPhoto?.let { Photos.delete(it) }
                            newPhoto = null
                            photo = null
                            photoMenu = false
                        }) { Text("Quitar foto", color = Palette.Red) }
                    }
                }
            }
        }

        // Salir sin guardar: debe guardar o cancelar
        if (askLeave) {
            Dialog(onDismissRequest = { askLeave = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                DialogCard {
                    CircleIcon(Ic.Warning, Palette.AmberDark, Palette.AmberSoft)
                    Text("Tienes cambios sin guardar", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                    Text(
                        "Debes guardar o cancelar. Si cancelas, no se guardará ningún cambio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Cancelar", { askLeave = false; discard() }, Modifier.weight(1f))
                        PrimaryButton("Guardar", save, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
