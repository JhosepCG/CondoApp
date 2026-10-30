package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------------
// SUPER ADMIN · Cuentas y accesos + información de cada cuenta
// ------------------------------------------------------------------

private val dmy: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

/** Botón "‹ Regresar" en píldora verde claro (como en Figma). */
@Composable
fun RegresarButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .pressScale(0.96f)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.TealSoft)
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
        Text("Regresar", color = Palette.Teal, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}

/** Colores por rol: Super Admin azul, Administrador ámbar, Inquilino verde azulado. */
private fun roleColors(r: Role): Pair<Color, Color> = when (r) {
    Role.SUPER_ADMIN -> Palette.Navy to Palette.NavySoft
    Role.ADMIN -> Palette.AmberDark to Palette.AmberSoft
    Role.TENANT -> Palette.Teal to Palette.TealSoft
}

private fun roleName(r: Role) = when (r) {
    Role.SUPER_ADMIN -> "Super Admin"
    Role.ADMIN -> "Administrador"
    Role.TENANT -> "Inquilino"
}

private fun accountSubtitle(u: AppUser): String = buildString {
    append("@").append(u.username)
    when (u.role) {
        Role.ADMIN -> Store.client(u.clientId)?.let { append(" · ").append(it.name) }
        Role.TENANT -> u.unit?.let { append(" · ").append(it) }
        Role.SUPER_ADMIN -> {}
    }
}

@Composable
private fun AccountAvatar(u: AppUser, size: androidx.compose.ui.unit.Dp) {
    val (fg, bg) = roleColors(u.role)
    UserAvatar(u, size, bg = bg, fg = fg, initialsOf = if (u.role == Role.SUPER_ADMIN) u.name.substringBefore(" ") else u.name)
}

@Composable
fun SuperCuentas(me: AppUser, onBack: (() -> Unit)? = null) {
    var filter by remember { mutableStateOf("Todos") }
    val list = Store.users.filter { u ->
        val t = Store.client(u.clientId)?.template
        when (filter) {
            "Super admin" -> u.role == Role.SUPER_ADMIN
            "Condominio" -> t == Template.CONDOMINIO
            "Casa" -> t == Template.CASA_DEPAS
            else -> true
        }
    }

    ScreenColumn {
        if (onBack != null) RegresarButton(onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cuentas", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("${Store.users.size} usuarios", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            ScrollChips(listOf("Todos", "Super admin", "Condominio", "Casa"), filter) { filter = it }
            HorizontalDivider(color = Palette.Line)
            if (list.isEmpty()) {
                Text("No hay cuentas en este filtro.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            list.forEachIndexed { i, u ->
                key(u.id) { AccountRow(u, Modifier.enter(i)) { UiState.accountId = u.id } }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AccountRow(u: AppUser, modifier: Modifier, onClick: () -> Unit) {
    val (fg, bg) = roleColors(u.role)
    Row(
        modifier
            .pressScale(0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccountAvatar(u, 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(u.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(accountSubtitle(u), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            StatusTag(roleName(u.role), fg, bg)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(26.dp))
    }
}

/** Información del inquilino / administrador / super admin: editar nombre, usuario y contraseña. */
@Composable
fun AccountInfo(me: AppUser, userId: String, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val u = Store.users.firstOrNull { it.id == userId }
    if (u == null) {
        ScreenColumn { RegresarButton(onBack); EmptyState(Icons.Rounded.PersonOff, "Esta cuenta ya no existe.") }
        return
    }
    val client = Store.client(u.clientId)
    var name by remember(u.id) { mutableStateOf(u.name) }
    var username by remember(u.id) { mutableStateOf(u.username.replaceFirstChar { it.uppercase() }) }
    var password by remember(u.id) { mutableStateOf(u.password) }
    var role by remember(u.id) { mutableStateOf(u.role) }
    var planSel by remember(u.id) { mutableStateOf(client?.let { Store.planOf(it) }) }
    var clientName by remember(u.id) { mutableStateOf(client?.name ?: "") }
    var fee by remember(u.id) { mutableStateOf(String.format(java.util.Locale.US, "%.2f", client?.monthlyFee ?: 0.0)) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val (fg, bg) = roleColors(u.role)

    ScreenColumn {
        RegresarButton(onBack)
        Column(
            Modifier
                .enter(0)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountAvatar(u, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(u.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Text(accountSubtitle(u), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                    Spacer(Modifier.height(2.dp))
                    StatusTag(roleName(u.role), fg, bg)
                }
                if (u.id != me.id) {
                    IconButton(onClick = {
                        UiState.deleteRequest = DeleteRequest(u.name, { Store.deleteAccount(u.id) }, after = onBack)
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar cuenta", tint = Palette.Ink, modifier = Modifier.size(26.dp))
                    }
                }
            }
            HorizontalDivider(color = Palette.Line)

            when (u.role) {
                Role.TENANT -> {
                    // Datos del administrador de su casa o condominio
                    val admin = client?.let { Store.ownerOf(it) }
                    SectionLabel("Administrador")
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.Field)
                            .border(1.dp, Palette.Line, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoLine("Nombre completo", admin?.name ?: "Sin administrador")
                        InfoLine("Celular", admin?.phone?.let { formatCel(it) } ?: "-")
                        InfoLine("Fecha de inicio", client?.since?.format(dmy) ?: "-")
                    }
                }
                Role.ADMIN -> {
                    SectionLabel("Rol")
                    FlatChips(listOf("Administrador", "Inquilinos"), if (role == Role.ADMIN) "Administrador" else "Inquilinos") {
                        role = if (it == "Administrador") Role.ADMIN else Role.TENANT
                    }
                    if (client != null) {
                        SectionLabel("Tipo de plan")
                        ScrollChips(Store.plans.map { it.name }, planSel?.name) { n ->
                            planSel = Store.plans.firstOrNull { it.name == n }
                            planSel?.let { fee = String.format(java.util.Locale.US, "%.2f", it.price) }
                        }
                    }
                }
                Role.SUPER_ADMIN -> {}
            }

            AppTextField(name, { name = it; error = null }, "Nombre completo", icon = Icons.Rounded.PersonOutline)
            AppTextField(username, { username = it.replace(" ", ""); error = null }, "Usuario", icon = Icons.Rounded.AlternateEmail)
            AppTextField(password, { password = it; error = null }, "Contraseña", icon = Icons.Rounded.LockOpen, password = true)
            if (u.role == Role.ADMIN && client != null) {
                AppTextField(
                    clientName, { clientName = it; error = null },
                    if (client.template == Template.CONDOMINIO) "Nombre del condominio" else "Nombre de la casa",
                    icon = Icons.Rounded.MeetingRoom
                )
                AppTextField(
                    fee,
                    { fee = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                    if (planSel?.cycle == Plan.ANUAL) "Cuota anual (S/)" else "Cuota mensual (S/)",
                    icon = Icons.Rounded.CreditCard,
                    keyboardType = KeyboardType.Decimal
                )
            }
            error?.let { ErrorText(it) }
            Button(
                onClick = {
                    var r = Store.updateAccount(u.id, name, username, password)
                    if (r == null && u.role == Role.ADMIN && client != null) {
                        if (clientName.isBlank()) r = "Escribe el nombre del ${if (client.template == Template.CONDOMINIO) "condominio" else "la casa"}."
                        else {
                            planSel?.let { p -> if (p.id != Store.planOf(client)?.id) Store.assignPlan(client.id, p) }
                            val fresh = Store.client(client.id) ?: client
                            val newFee = if (fresh.plan == Plan.GRATIS) 0.0 else fee.toDoubleOrNull() ?: fresh.monthlyFee
                            Store.updateClient(fresh.copy(name = clientName.trim(), monthlyFee = newFee))
                            if (role != u.role) Store.changeRole(u.id, role)
                        }
                    }
                    error = r
                    if (r == null) saved = true
                },
                modifier = Modifier.pressScale(0.97f).fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal, contentColor = Color.White)
            ) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (saved) {
        ResultDialog(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Cambios guardados",
            text = "La cuenta de ${name.trim()} ya está actualizada.",
            button = "Aceptar"
        ) {
            saved = false
            onBack()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$label: ") }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        color = Color(0xFF4A5160)
    )
}

private fun formatCel(p: String): String =
    if (p.length == 9) "${p.substring(0, 3)} ${p.substring(3, 6)} ${p.substring(6)}" else p
