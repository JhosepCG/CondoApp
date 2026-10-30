package pe.condoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class Tab(val label: String, val icon: ImageVector) {
    RESUMEN("Home", Icons.Rounded.Home),
    CLIENTES("Clientes", Icons.Rounded.Apartment),
    PLANES("Planes", Icons.Rounded.CreditCard),
    MI_PERFIL("Mi perfil", Icons.Rounded.PersonOutline),
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
    fun on(f: Feature): Boolean = Store.featureOn(c, f)
    return when (user.role) {
        // Super Admin: Home, Clientes, [+], Planes, Mi perfil. "Cuentas" se abre desde Mi perfil.
        Role.SUPER_ADMIN -> listOf(Tab.RESUMEN, Tab.CLIENTES, Tab.PLANES, Tab.MI_PERFIL)
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
    when {
        user == null -> LoginScreen()
        user.role != Role.SUPER_ADMIN && (user.id !in Store.onboarded || UiState.showOnboarding) ->
            OnboardingScreen(user) {
                Store.markOnboarded(user.id)
                UiState.showOnboarding = false
            }
        else -> MainScaffold(user)
    }
}

@Composable
private fun MainScaffold(user: AppUser) {
    val tabs = tabsFor(user)
    var tab by remember(user.id) { mutableStateOf(tabs.first()) }
    LaunchedEffect(user.id) {
        UiState.profileId = null
        UiState.myProfile = false
    }
    val personasHidden = user.role == Role.ADMIN && Tab.PERSONAS !in tabs
    val current = if (tab in tabs || (tab == Tab.PERSONAS && personasHidden) || (tab == Tab.CUENTAS && user.role == Role.SUPER_ADMIN)) tab else tabs.first()
    val go: (Tab) -> Unit = {
        UiState.profileId = null
        UiState.accountId = null
        UiState.myProfile = false
        UiState.newClient = false
        UiState.openClientId = null
        UiState.showNotifications = false
        tab = it
    }
    // El Super Admin recibe los pedidos de ayuda como notificación: pedir permiso (Android 13+)
    if (user.role == Role.SUPER_ADMIN) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val askPermission = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(user.id) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(ctx)) {
                askPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val client = Store.clientOf(user)
    val pendingAlerts = if (user.role == Role.ADMIN && client != null) {
        Store.emergencies.count { it.clientId == client.id && it.status == EmergencyStatus.PENDIENTE }
    } else 0
    val urgentNotices = if (user.role == Role.TENANT && client != null) {
        Store.notices.count { it.clientId == client.id && it.urgent }
    } else 0
    val profileId = UiState.profileId

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .statusBarsPadding()
            .imePadding()
    ) {
        // El Super Admin tiene su propio encabezado (avatar, saludo y salir).
        if (user.role == Role.SUPER_ADMIN) {
            // En el detalle del cliente va la barra "Regresar" en lugar del saludo.
            SuperHeader(user)
        } else {
            TopBar(
                user = user,
                client = client,
                bellDot = pendingAlerts > 0 || urgentNotices > 0,
                onBell = when (user.role) {
                    Role.ADMIN -> ({ go(Tab.PANEL) })
                    Role.TENANT -> if (Tab.AVISOS in tabs) ({ go(Tab.AVISOS) }) else null
                    Role.SUPER_ADMIN -> null
                }
            )
        }
        Box(Modifier.weight(1f)) {
            val openClient = UiState.openClientId
            // Cada pantalla nueva entra con un desvanecido suave hacia arriba
            val screenKey = when {
                user.role == Role.SUPER_ADMIN && UiState.showNotifications -> "notif"
                user.role == Role.SUPER_ADMIN && UiState.newClient -> "new"
                user.role == Role.SUPER_ADMIN && openClient != null -> "client:$openClient"
                user.role == Role.SUPER_ADMIN && UiState.accountId != null -> "account:${UiState.accountId}"
                UiState.myProfile && user.role == Role.ADMIN -> "me"
                profileId != null && user.role != Role.TENANT -> "profile:$profileId"
                else -> "tab:${current.name}"
            }
            key(screenKey) { Box(Modifier.fillMaxSize().enter(0, 14.dp)) {
            if (user.role == Role.SUPER_ADMIN && UiState.showNotifications) {
                SuperNotifications { UiState.showNotifications = false }
            } else if (user.role == Role.SUPER_ADMIN && UiState.newClient) {
                SuperNewClient {
                    UiState.newClient = false
                    tab = Tab.CLIENTES
                }
            } else if (user.role == Role.SUPER_ADMIN && openClient != null) {
                SuperClientDetail(openClient) { UiState.openClientId = null }
            } else if (user.role == Role.SUPER_ADMIN && UiState.accountId != null) {
                AccountInfo(user, UiState.accountId!!) { UiState.accountId = null }
            } else if (UiState.myProfile && user.role == Role.ADMIN) {
                AdminMyProfile(user) { UiState.myProfile = false }
            } else if (profileId != null && user.role != Role.TENANT) {
                TenantProfile(user, profileId) { UiState.profileId = null }
            } else when (current) {
                Tab.RESUMEN -> SuperHome(user, go)
                Tab.CLIENTES -> SuperClientes()
                Tab.PLANES -> SuperPlanes(user)
                Tab.MI_PERFIL -> SuperMiPerfil(user, go)
                Tab.CUENTAS -> SuperCuentas(user, onBack = { go(Tab.MI_PERFIL) })
                Tab.PANEL -> AdminPanel(user, go, personasHidden)
                Tab.COBROS -> AdminCobros(user)
                Tab.SERVICIOS -> if (user.role == Role.ADMIN) AdminServicios(user) else TenantServicios(user)
                Tab.AVISOS -> if (user.role == Role.ADMIN) AdminAvisos(user) else TenantAvisos(user)
                Tab.PERSONAS -> AdminPersonas(user, if (personasHidden) ({ go(Tab.PANEL) }) else null)
                Tab.INICIO -> TenantHome(user, go)
                Tab.MEDIDOR -> TenantMedidor(user)
                Tab.MEDIDORES -> AdminMedidores(user)
            }
            } }
        }
        UiState.deleteRequest?.let { req ->
            key(req) {
                DeleteUserFlow(
                    name = req.name,
                    onConfirmDelete = req.action,
                    onCancel = { UiState.deleteRequest = null },
                    onFinished = {
                        UiState.deleteRequest = null
                        req.after()
                    }
                )
            }
        }
        if (user.role == Role.SUPER_ADMIN) {
            SuperNavBar(
                tabs = tabs,
                current = when {
                    UiState.newClient || UiState.showNotifications -> null
                    UiState.openClientId != null -> Tab.CLIENTES
                    UiState.accountId != null -> Tab.MI_PERFIL
                    profileId != null -> null
                    current == Tab.CUENTAS -> Tab.MI_PERFIL
                    else -> current
                },
                onSelect = go,
                onAdd = {
                    UiState.showNotifications = false
                    UiState.openClientId = null
                    UiState.accountId = null
                    UiState.profileId = null
                    UiState.newClient = true
                }
            )
        } else {
            Box(Modifier.navigationBarsPadding()) {
                FloatingNav {
                    tabs.forEach { t ->
                        val sel = current == t && profileId == null && !UiState.myProfile
                        NavPill(
                            icon = t.icon,
                            label = t.label,
                            selected = sel,
                            dot = t == Tab.PANEL && pendingAlerts > 0,
                            modifier = Modifier.weight(if (sel) 2.2f else 1f)
                        ) { go(t) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(user: AppUser, client: Client?, bellDot: Boolean, onBell: (() -> Unit)?) {
    var accountOpen by remember { mutableStateOf(false) }
    val firstName = user.name.substringBefore(" ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${greeting()},",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted
            )
            Text(
                firstName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                client?.name ?: user.role.label,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Teal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onBell != null) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Line, CircleShape)
                    .clickable(onClick = onBell),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Avisos", tint = Palette.Ink)
                if (bellDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 11.dp, end = 12.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Palette.Red)
                    )
                }
            }
        }
        // El administrador abre "Mi perfil"; los demás, el menú de la cuenta.
        Box(Modifier.clip(CircleShape).clickable {
            if (user.role == Role.ADMIN) UiState.myProfile = true else accountOpen = true
        }) {
            Avatar(user.name, size = 46.dp, bg = Palette.Navy, fg = Color.White)
        }
    }
    if (accountOpen) {
        AlertDialog(
            onDismissRequest = { accountOpen = false },
            containerColor = Palette.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = { Avatar(user.name, size = 56.dp, bg = Palette.Navy, fg = Color.White) },
            title = { Text(user.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("@" + user.username, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                    Spacer(Modifier.height(8.dp))
                    Pill(user.role.label, roleFg(user.role), roleBg(user.role))
                    if (client != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            listOfNotNull(user.unit, client.name).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                    if (user.role != Role.SUPER_ADMIN) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = {
                            accountOpen = false
                            Store.resetTips()
                            UiState.showOnboarding = true
                        }) {
                            Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver introducción", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    accountOpen = false
                    Store.logout()
                }) {
                    Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Palette.Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cerrar sesión", color = Palette.Red, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountOpen = false }) {
                    Text("Cancelar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}
