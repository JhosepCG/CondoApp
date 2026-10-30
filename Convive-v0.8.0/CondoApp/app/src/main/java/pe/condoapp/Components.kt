package pe.condoapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

// ------------------------------------------------------------------
// Formatos
// ------------------------------------------------------------------

val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun money(v: Double): String = "S/ " + String.format(Locale.US, "%,.2f", v)

fun pct(v: Double): String = if (v % 1.0 == 0.0) "${v.toInt()}%" else "$v%"

// ------------------------------------------------------------------
// Íconos por tipo
// ------------------------------------------------------------------

fun serviceIcon(s: String): ImageVector = when (s) {
    "Internet" -> Ic.Wifi
    "Cable" -> Ic.Tv
    "Agua" -> Ic.Drop
    "Luz" -> Ic.Bolt
    else -> Ic.Gear
}

fun noticeIcon(category: String): ImageVector = when (category) {
    "Corte de agua" -> Ic.Drop
    "Corte de luz" -> Ic.Bolt
    "Medidor de luz" -> Ic.Bolt
    "Internet / cable" -> Ic.Wifi
    "Mantenimiento" -> Ic.Gear
    "Seguridad" -> Ic.Shield
    else -> Ic.Megaphone
}

fun emergencyIcon(type: String): ImageVector = when (type) {
    "Fuga de gas" -> Ic.Warning
    "Incendio" -> Ic.AlertOctagon
    "Robo" -> Ic.Shield
    else -> Ic.AlertOctagon
}

fun templateIcon(t: Template): ImageVector =
    if (t == Template.CONDOMINIO) Ic.Building else Ic.Home

fun roleFg(r: Role): Color = when (r) {
    Role.SUPER_ADMIN -> Palette.Navy
    Role.ADMIN -> Palette.AmberDark
    Role.TENANT -> Palette.Teal
}

fun roleBg(r: Role): Color = when (r) {
    Role.SUPER_ADMIN -> Palette.NavySoft
    Role.ADMIN -> Palette.AmberSoft
    Role.TENANT -> Palette.TealSoft
}

// ------------------------------------------------------------------
// Bloques base
// ------------------------------------------------------------------

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Palette.Surface,
    borderColor: Color = Palette.Line,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    var m = (if (onClick != null) modifier.pressScale(0.98f) else modifier)
        .fillMaxWidth()
        .clip(shape)
        .background(color)
        .border(1.dp, borderColor, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(
        modifier = m.padding(padding).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun LineDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, bg: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Pill(text: String, fg: Color, bg: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun Avatar(name: String, size: Dp = 42.dp, bg: Color = Palette.TealSoft, fg: Color = Palette.Teal) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() && it.first().isLetter() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(icon, tint, bg, 38.dp)
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1)
    }
}

@Composable
fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tarjeta principal con degradado azul noche → verde azulado. */
@Composable
fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String?,
    icon: ImageVector,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Palette.Navy, Palette.Teal)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    eyebrow.uppercase(),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            IconBadge(icon, Color.White, Color.White.copy(alpha = 0.15f), 52.dp)
        }
        if (extra != null) extra()
    }
}

@Composable
fun HeroInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, Palette.Teal, Palette.TealSoft, 36.dp)
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ChoiceChips(
    options: List<String>,
    selected: String,
    icons: Map<String, ImageVector> = emptyMap(),
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { o ->
            val sel = o == selected
            // El color cambia con un fundido corto (ease) en lugar de saltar.
            val chipSpec = tween<Color>(Motion.QUICK_MS, easing = FastOutSlowInEasing)
            val bg by animateColorAsState(if (sel) Palette.Navy else Palette.Surface, chipSpec, label = "chipBg")
            val bd by animateColorAsState(if (sel) Palette.Navy else Palette.Line, chipSpec, label = "chipBd")
            val fg by animateColorAsState(if (sel) Color.White else Palette.Ink, chipSpec, label = "chipFg")
            val ic by animateColorAsState(if (sel) Color.White else Palette.Muted, chipSpec, label = "chipIc")
            Row(
                modifier = Modifier
                    .pressScale()
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, bd, CircleShape)
                    .clickable { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val icn = icons[o]
                if (icn != null) {
                    Icon(icn, contentDescription = null, tint = ic, modifier = Modifier.size(16.dp))
                }
                Text(o, color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    val leading: (@Composable () -> Unit)? = if (icon != null) {
        { Icon(icon, contentDescription = null) }
    } else null
    val trailing: (@Composable () -> Unit)? = if (password) {
        {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Ic.EyeOff else Ic.Eye,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        }
    } else null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { ph -> { Text(ph, color = Palette.Muted.copy(alpha = 0.7f)) } },
        leadingIcon = leading,
        trailingIcon = trailing,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Teal,
            unfocusedBorderColor = Palette.Line,
            focusedContainerColor = Palette.Surface,
            unfocusedContainerColor = Palette.Field,
            focusedLabelColor = Palette.Teal,
            focusedLeadingIconColor = Palette.Teal,
            unfocusedLeadingIconColor = Palette.Muted,
            cursorColor = Palette.Teal
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Teal,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = (if (enabled) modifier.pressScale() else modifier).heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Palette.Line,
            disabledContentColor = Palette.Muted
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Palette.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Surface, contentColor = Palette.Ink)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
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

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(icon, Palette.Muted, Palette.NavySoft, 56.dp)
        Text(text, color = Palette.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Ic.AlertCircle, contentDescription = null, tint = Palette.Red, modifier = Modifier.size(18.dp))
        Text(text, color = Palette.Red, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(confirm, color = if (danger) Palette.Red else Palette.Teal, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

// ------------------------------------------------------------------
// Tarjetas compartidas
// ------------------------------------------------------------------

class ChargeState(val label: String, val fg: Color, val bg: Color, val icon: ImageVector)

fun chargeState(ch: Charge): ChargeState = when {
    ch.paid -> ChargeState("Pagado", Palette.Green, Palette.GreenSoft, Ic.CheckCircle)
    Store.isOverdue(ch) -> ChargeState("Vencido", Palette.Red, Palette.RedSoft, Ic.AlertCircle)
    else -> ChargeState("Pendiente", Palette.AmberDark, Palette.AmberSoft, Ic.Clock)
}

@Composable
fun ChargeCard(ch: Charge, onPay: (() -> Unit)? = null) {
    val st = chargeState(ch)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(st.icon, st.fg, st.bg)
            Column(Modifier.weight(1f)) {
                Text(ch.unit, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${ch.concept} · vence ${ch.due.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(money(Store.amountDue(ch)), style = MaterialTheme.typography.titleMedium)
                Pill(st.label, st.fg, st.bg)
            }
        }
        if (onPay != null && !ch.paid) {
            SecondaryButton("Registrar pago", onPay, Modifier.fillMaxWidth(), icon = Ic.Check)
        }
    }
}

@Composable
fun NoticeCard(n: Notice, onArchive: (() -> Unit)? = null) {
    AppCard(
        color = if (n.urgent) Palette.AmberSoft else Palette.Surface,
        borderColor = if (n.urgent) Palette.Amber.copy(alpha = 0.45f) else Palette.Line
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(
                noticeIcon(n.category),
                if (n.urgent) Palette.AmberDark else Palette.Teal,
                if (n.urgent) Palette.Surface else Palette.TealSoft
            )
            Column(Modifier.weight(1f)) {
                Text(n.title, style = MaterialTheme.typography.titleMedium)
                Text("${n.category} · ${n.createdAt}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            if (n.urgent) Pill("Urgente", Palette.AmberDark, Palette.Surface)
        }
        if (n.body.isNotBlank()) Text(n.body, style = MaterialTheme.typography.bodyMedium)
        if (n.photoPath != null) {
            var zoom by remember { mutableStateOf(false) }
            PhotoImage(
                n.photoPath,
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { zoom = true }
            )
            if (zoom) PhotoViewer(n.photoPath, n.title) { zoom = false }
        }
        if (onArchive != null) {
            TextButton(onClick = onArchive, contentPadding = PaddingValues(0.dp)) {
                Icon(Ic.Archive, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Archivar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

class IncidentState(val label: String, val fg: Color, val bg: Color)

fun incidentState(inc: Incident): IncidentState = when {
    inc.status == IncidentStatus.RESUELTO && inc.confirmed == true ->
        IncidentState("Resuelto", Palette.Green, Palette.GreenSoft)
    inc.status == IncidentStatus.RESUELTO -> IncidentState("Por confirmar", Palette.Teal, Palette.TealSoft)
    inc.status == IncidentStatus.EN_REVISION -> IncidentState("En revisión", Palette.Navy, Palette.NavySoft)
    else -> IncidentState("Pendiente", Palette.AmberDark, Palette.AmberSoft)
}

@Composable
fun IncidentHeader(inc: Incident, subtitle: String) {
    val st = incidentState(inc)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(serviceIcon(inc.service), Palette.Navy, Palette.NavySoft)
        Column(Modifier.weight(1f)) {
            Text(inc.service, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Pill(st.label, st.fg, st.bg)
    }
}

/** Formulario para crear cuentas (lo usan Super Admin y Administrador). */
@Composable
fun AccountForm(fixedClient: Client?, allowedRoles: List<Role>, onDone: () -> Unit) {
    var role by remember { mutableStateOf(allowedRoles.last()) }
    var clientId by remember { mutableStateOf(fixedClient?.id ?: Store.clients.firstOrNull()?.id) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var userEdited by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(DEFAULT_PASSWORD) }
    var unit by remember { mutableStateOf("") }
    var responsible by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val client = fixedClient ?: Store.client(clientId)
    // Contrato: solo cuando el administrador de una casa registra a un inquilino (el Super Admin no lo ve).
    val contractForm = rememberContractFormState(null)
    val withContract = fixedClient != null && fixedClient.template == Template.CASA_DEPAS && role == Role.TENANT

    AppCard(padding = 18.dp) {
        Text("Nueva cuenta", style = MaterialTheme.typography.titleLarge)
        if (fixedClient == null) {
            Text("Cliente", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            ChoiceChips(Store.clients.map { it.name }, client?.name ?: "") { n ->
                clientId = Store.clients.firstOrNull { it.name == n }?.id
            }
        }
        Text("Rol", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
        ChoiceChips(
            options = allowedRoles.map { it.label },
            selected = role.label,
            icons = mapOf(Role.ADMIN.label to Ic.Shield, Role.TENANT.label to Ic.User)
        ) { l -> role = allowedRoles.first { it.label == l } }

        AppTextField(name, {
            name = it
            if (!userEdited) username = if (it.isBlank()) "" else Store.suggestUsername(it)
            error = null
        }, "Nombre completo", icon = Ic.User)
        AppTextField(
            username,
            { username = it.lowercase().replace(" ", ""); userEdited = true; error = null },
            "Usuario",
            icon = Ic.UserSquare
        )
        AppTextField(password, { password = it; error = null }, "Contraseña", icon = Ic.Lock, password = true)
        Text(
            "El usuario es su primer nombre y la contraseña inicial es $DEFAULT_PASSWORD. También puede entrar con su DNI.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Muted
        )

        if (role == Role.TENANT) {
            AppTextField(unit, { unit = it; error = null }, "Departamento o casa", icon = Ic.Home)
            AppTextField(responsible, { responsible = it }, "Responsable del domicilio (opcional)", icon = Ic.IdCard)
            if (withContract) {
                LineDivider()
                Text("Contrato", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Solo lo ven tú y el inquilino.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
                ContractFields(contractForm)
            } else if (client != null && Feature.COBROS in client.modules) {
                AppTextField(
                    amount,
                    { amount = it },
                    if (client.template == Template.CONDOMINIO) "Cuota mensual (S/)" else "Renta mensual (S/)",
                    icon = Ic.Cash,
                    keyboardType = KeyboardType.Decimal
                )
            }
        }
        error?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancelar", onDone, Modifier.weight(1f))
            PrimaryButton(
                "Crear cuenta",
                {
                    val result = Store.createAccount(
                        name, username, password, role, client?.id,
                        unit, responsible,
                        if (withContract) contractForm.rent.toDoubleOrNull() else amount.toDoubleOrNull(),
                        if (withContract && client != null) contractForm.toContract("", client.id) else null
                    )
                    error = result
                    if (result == null) onDone()
                },
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun UserRow(
    u: AppUser,
    canDelete: Boolean,
    showClient: Boolean = false,
    canReset: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var confirm by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    val c = Store.client(u.clientId)
    val detail = buildString {
        append("@").append(u.username)
        if (u.unit != null) append(" · ").append(u.unit)
        if (showClient && c != null) append(" · ").append(c.name)
    }
    AppCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(u.name, bg = roleBg(u.role), fg = roleFg(u.role))
            Column(Modifier.weight(1f)) {
                Text(u.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Pill(roleLabel(u), roleFg(u.role), roleBg(u.role))
            if (canReset) {
                IconButton(onClick = { resetOpen = true }) {
                    Icon(Ic.Lock, contentDescription = "Cambiar contraseña", tint = Palette.Muted)
                }
            }
            if (canDelete) {
                IconButton(onClick = { confirm = true }) {
                    Icon(Ic.Trash, contentDescription = "Eliminar cuenta", tint = Palette.Muted)
                }
            }
        }
    }
    if (confirm) {
        // Confirmar → "Eliminando usuario…" → "Se ha eliminado correctamente"
        LaunchedEffect(Unit) {
            UiState.deleteRequest = DeleteRequest(u.name, { Store.deleteAccount(u.id) })
            confirm = false
        }
    }
    if (resetOpen) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            containerColor = Palette.Surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Nueva contraseña", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Para ${u.name} (@${u.username}). Compártela por un medio seguro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted
                    )
                    AppTextField(newPass, { newPass = it }, "Contraseña nueva", icon = Ic.Lock, password = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Store.resetPassword(u.id, newPass)
                        resetOpen = false
                    },
                    enabled = newPass.length >= 4
                ) {
                    Text("Guardar", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetOpen = false }) {
                    Text("Cancelar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

/** Foto a pantalla completa dentro de un diálogo. */
@Composable
fun PhotoViewer(path: String, title: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            PhotoImage(
                path,
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 460.dp)
                    .clip(RoundedCornerShape(16.dp)),
                maxSide = 1600,
                scale = androidx.compose.ui.layout.ContentScale.Fit
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
