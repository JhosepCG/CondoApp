package pe.condoapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ------------------------------------------------------------------
// Estado de interfaz compartido
// ------------------------------------------------------------------

/** Perfil de inquilino abierto (lo abren el buscador, Personas y Cuentas). */
object UiState {
    var profileId by mutableStateOf<String?>(null)
    /** "Mi perfil" del administrador (se abre desde su avatar). */
    var myProfile by mutableStateOf(false)
    /** Volver a ver la introducción desde el menú de la cuenta. */
    var showOnboarding by mutableStateOf(false)
}

// ------------------------------------------------------------------
// Resumen del estado de un inquilino
// ------------------------------------------------------------------

class TenantStatus(
    val pending: List<Charge>,
    val overdue: Int,
    val openIncidents: Int,
    /** null = el cliente no usa medidores o no hay pedido activo. */
    val meterSent: Boolean?
) {
    val upToDate: Boolean get() = pending.isEmpty()
    val owed: Double get() = pending.sumOf { Store.amountDue(it) }
}

fun tenantStatus(u: AppUser): TenantStatus {
    val c = Store.client(u.clientId)
    val pending = Store.charges.filter { it.clientId == u.clientId && it.unit == u.unit && !it.paid }.sortedBy { it.due }
    val openInc = Store.incidents.count {
        it.userId == u.id && !(it.status == IncidentStatus.RESUELTO && it.confirmed == true)
    }
    // El Super Admin no ve información de medidores.
    val req = if (Store.currentUser?.role != Role.SUPER_ADMIN && Store.featureOn(c, Feature.MEDIDORES) && c != null) {
        Store.activeMeterRequest(c.id)
    } else null
    val meter = req?.let { Store.readingFor(it.id, u.id) != null }
    return TenantStatus(pending, pending.count { Store.isOverdue(it) }, openInc, meter)
}

// ------------------------------------------------------------------
// Piezas visuales de Convive
// ------------------------------------------------------------------

/** Buscador tipo píldora. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Palette.Muted)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Palette.Muted, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Teal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Borrar búsqueda",
                tint = Palette.Muted,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onValueChange("") }
            )
        }
    }
}

/** Acceso rápido cuadrado (ícono + texto), con contador opcional. */
@Composable
fun QuickTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Navy,
    bg: Color = Palette.NavySoft,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            IconBadge(icon, tint, bg, 44.dp)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Palette.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (badge > 9) "9+" else "$badge", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tarjeta oscura (azul noche) para resúmenes destacados. */
@Composable
fun DarkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Navy)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Barra de progreso redondeada. */
@Composable
fun ProgressLine(value: Float, color: Color, track: Color, height: Dp = 8.dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Foto guardada en el teléfono (avisos, medidores). */
@Composable
fun PhotoImage(path: String?, modifier: Modifier = Modifier, maxSide: Int = 1200, scale: ContentScale = ContentScale.Crop) {
    val img = remember(path) { path?.let { Photos.load(it, maxSide) } }
    Box(modifier.background(Palette.NavySoft), contentAlignment = Alignment.Center) {
        if (img != null) {
            Image(bitmap = img, contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Palette.Muted)
        }
    }
}

/** Fila de inquilino con su estado (pagos, medidor, reportes). */
@Composable
fun TenantRow(u: AppUser, showClient: Boolean, showContract: Boolean = false, onClick: () -> Unit) {
    val st = tenantStatus(u)
    val c = Store.client(u.clientId)
    AppCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(u.name, bg = roleBg(u.role), fg = roleFg(u.role))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(u.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(u.unit, if (showClient) c?.name else null).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
        }
        val contract = if (showContract) Store.contractOf(u.id) else null
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (contract != null) {
                when {
                    contract.renewRequested -> Pill("Renovación enviada", Palette.Teal, Palette.TealSoft, Icons.Rounded.EditNote)
                    Store.contractExpired(contract) -> Pill("Contrato vencido", Palette.Red, Palette.RedSoft, Icons.Rounded.EditNote)
                    Store.contractEndingSoon(contract) -> Pill("Contrato por vencer", Palette.AmberDark, Palette.AmberSoft, Icons.Rounded.EditNote)
                    else -> {}
                }
            }
            when {
                st.overdue > 0 -> Pill("Debe ${money(st.owed)}", Palette.Red, Palette.RedSoft, Icons.Rounded.ErrorOutline)
                !st.upToDate -> Pill("Pago pendiente", Palette.AmberDark, Palette.AmberSoft, Icons.Rounded.Schedule)
                else -> Pill("Al día", Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
            }
            when (st.meterSent) {
                true -> Pill("Medidor", Palette.Green, Palette.GreenSoft, Icons.Rounded.Bolt)
                false -> Pill("Falta medidor", Palette.AmberDark, Palette.AmberSoft, Icons.Rounded.Bolt)
                null -> {}
            }
            if (st.openIncidents > 0) Pill("${st.openIncidents} reporte", Palette.Navy, Palette.NavySoft, Icons.Rounded.Build)
        }
    }
}

/** Buscador de inquilinos con resultados. clientId = null busca en todos los clientes. */
@Composable
fun TenantSearch(clientId: String?, placeholder: String = "Buscar inquilino o departamento") {
    var q by remember { mutableStateOf("") }
    SearchField(q, { q = it }, placeholder)
    if (q.isNotBlank()) {
        val found = Store.searchTenants(q, clientId)
        if (found.isEmpty()) {
            Text("Sin resultados para «$q».", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        }
        found.take(8).forEach { u ->
            key(u.id) { TenantRow(u, showClient = clientId == null, showContract = clientId != null) { UiState.profileId = u.id } }
        }
    }
}

/** Saludo según la hora. */
fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 12 -> "Buenos días"
        h < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

fun isLate(due: LocalDate): Boolean = LocalDate.now().isAfter(due)

/** Contenedor de la barra de navegación flotante. */
@Composable
fun FloatingNav(content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(16.dp, shape, ambientColor = Palette.Navy.copy(alpha = 0.25f), spotColor = Palette.Navy.copy(alpha = 0.25f))
            .clip(shape)
            .background(Palette.Surface)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
fun NavPill(icon: ImageVector, label: String, selected: Boolean, dot: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Navy else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Box {
            Icon(icon, contentDescription = label, tint = if (selected) Color.White else Palette.Muted)
            if (dot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Palette.Red)
                )
            }
        }
        if (selected) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Estado de la suscripción. */
@Composable
fun SubPill(st: SubStatus) {
    when (st) {
        SubStatus.PAGADO -> Pill(st.label, Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
        SubStatus.PENDIENTE -> Pill(st.label, Palette.AmberDark, Palette.AmberSoft, Icons.Rounded.Schedule)
        SubStatus.VENCIDO -> Pill(st.label, Palette.Red, Palette.RedSoft, Icons.Rounded.ErrorOutline)
        SubStatus.GRATIS -> Pill(st.label, Palette.Teal, Palette.TealSoft, Icons.Rounded.Redeem)
    }
}

/** Rol que se muestra: "Owner" para el administrador responsable del pago. */
fun roleLabel(u: AppUser): String = if (Store.isOwner(u)) "Owner" else u.role.label
