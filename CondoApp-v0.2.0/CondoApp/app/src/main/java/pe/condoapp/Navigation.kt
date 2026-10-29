package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class Tab(val label: String, val icon: ImageVector) {
    RESUMEN("Resumen", Icons.Rounded.Dashboard),
    CLIENTES("Clientes", Icons.Rounded.Apartment),
    CUENTAS("Cuentas", Icons.Rounded.Groups),
    PANEL("Panel", Icons.Rounded.Security),
    COBROS("Cobros", Icons.Rounded.Payments),
    SERVICIOS("Servicios", Icons.Rounded.Build),
    AVISOS("Avisos", Icons.Rounded.Campaign),
    PERSONAS("Personas", Icons.Rounded.Groups),
    INICIO("Inicio", Icons.Rounded.Home),
    MEDIDOR("Medidor", Icons.Rounded.Bolt),
    MEDIDORES("Medidores", Icons.Rounded.Bolt)
}

/** Pestañas de la barra inferior (máximo 5). */
fun tabsFor(user: AppUser): List<Tab> {
    val c = Store.clientOf(user)
    fun on(f: Feature): Boolean = c != null && f in c.modules
    return when (user.role) {
        Role.SUPER_ADMIN -> listOf(Tab.RESUMEN, Tab.CLIENTES, Tab.CUENTAS)
        Role.ADMIN -> {
            val list = buildList {
                add(Tab.PANEL)
                if (on(Feature.COBROS)) add(Tab.COBROS)
                if (on(Feature.MEDIDORES)) add(Tab.MEDIDORES)
                if (on(Feature.INCIDENCIAS)) add(Tab.SERVICIOS)
                if (on(Feature.AVISOS)) add(Tab.AVISOS)
                add(Tab.PERSONAS)
            }
            // Con más de 5 pestañas, "Personas" se abre desde el Panel.
            if (list.size > 5) list - Tab.PERSONAS else list
        }
        Role.TENANT -> buildList {
            add(Tab.INICIO)
            if (on(Feature.MEDIDORES)) add(Tab.MEDIDOR)
            if (on(Feature.INCIDENCIAS)) add(Tab.SERVICIOS)
            if (on(Feature.AVISOS)) add(Tab.AVISOS)
        }
    }
}

@Composable
fun AppRoot() {
    val user = Store.currentUser
    if (user == null) LoginScreen() else MainScaffold(user)
}

@Composable
private fun MainScaffold(user: AppUser) {
    val tabs = tabsFor(user)
    var tab by remember(user.id) { mutableStateOf(tabs.first()) }
    val personasHidden = user.role == Role.ADMIN && Tab.PERSONAS !in tabs
    val current = if (tab in tabs || (tab == Tab.PERSONAS && personasHidden)) tab else tabs.first()
    val go: (Tab) -> Unit = { tab = it }
    val client = Store.clientOf(user)
    val pendingAlerts = if (user.role == Role.ADMIN && client != null) {
        Store.emergencies.count { it.clientId == client.id && it.status == EmergencyStatus.PENDIENTE }
    } else 0

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .systemBarsPadding()
    ) {
        TopBar(user, client)
        Box(Modifier.weight(1f)) {
            when (current) {
                Tab.RESUMEN -> SuperResumen(user)
                Tab.CLIENTES -> SuperClientes()
                Tab.CUENTAS -> SuperCuentas(user)
                Tab.PANEL -> AdminPanel(user, go, personasHidden)
                Tab.COBROS -> AdminCobros(user)
                Tab.SERVICIOS -> if (user.role == Role.ADMIN) AdminServicios(user) else TenantServicios(user)
                Tab.AVISOS -> if (user.role == Role.ADMIN) AdminAvisos(user) else TenantAvisos(user)
                Tab.PERSONAS -> AdminPersonas(user, if (personasHidden) ({ go(Tab.PANEL) }) else null)
                Tab.INICIO -> TenantHome(user, go)
                Tab.MEDIDOR -> TenantMedidor(user)
                Tab.MEDIDORES -> AdminMedidores(user)
            }
        }
        LineDivider()
        NavigationBar(containerColor = Palette.Surface, tonalElevation = 0.dp) {
            tabs.forEach { t ->
                NavigationBarItem(
                    selected = current == t,
                    onClick = { tab = t },
                    icon = {
                        Box {
                            Icon(t.icon, contentDescription = t.label)
                            if (t == Tab.PANEL && pendingAlerts > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Red)
                                )
                            }
                        }
                    },
                    label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.Teal,
                        selectedTextColor = Palette.Teal,
                        indicatorColor = Palette.TealSoft,
                        unselectedIconColor = Palette.Muted,
                        unselectedTextColor = Palette.Muted
                    )
                )
            }
        }
    }
}

@Composable
private fun TopBar(user: AppUser, client: Client?) {
    var confirmLogout by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Surface)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(user.name, bg = roleBg(user.role), fg = roleFg(user.role))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                user.role.label + (client?.let { " · " + it.name } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { confirmLogout = true }) {
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Cerrar sesión", tint = Palette.Muted)
        }
    }
    LineDivider()
    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "¿Quieres salir de tu cuenta?",
            confirm = "Salir",
            onConfirm = { Store.logout() },
            onDismiss = { confirmLogout = false },
            danger = false
        )
    }
}
