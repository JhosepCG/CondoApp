package pe.condoapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    RESUMEN("Home", Ic.Home),
    CLIENTES("Clientes", Ic.Building),
    PLANES("Planes", Ic.Card),
    MI_PERFIL("Mi perfil", Ic.User),
    CUENTAS("Cuentas", Ic.Users),
    PANEL("Panel", Ic.Shield),
    COBROS("Cobros", Ic.Cash),
    SERVICIOS("Servicios", Ic.Gear),
    AVISOS("Avisos", Ic.Megaphone),
    PERSONAS("Personas", Ic.Users),
    INICIO("Inicio", Ic.Home),
    MEDIDOR("Medidor", Ic.Bolt),
    MEDIDORES("Medidores", Ic.Bolt)
}

/** Pestañas de la app según el rol. */
fun tabsFor(user: AppUser): List<Tab> {
    val c = Store.clientOf(user)
    fun on(f: Feature): Boolean = Store.featureOn(c, f)
    return when (user.role) {
        // Super Admin: Home, Clientes, [+], Planes, Mi perfil. "Cuentas" se abre desde Mi perfil.
        Role.SUPER_ADMIN -> listOf(Tab.RESUMEN, Tab.CLIENTES, Tab.PLANES, Tab.MI_PERFIL)
        // Administrador: Home, Pagos, [Avisos al centro], Reportes, Mi perfil.
        // Medidores y Mis inquilinos se abren desde los accesos del Home.
        Role.ADMIN -> buildList {
            add(Tab.PANEL)
            if (on(Feature.COBROS)) add(Tab.COBROS)
            if (on(Feature.AVISOS)) add(Tab.AVISOS)
            if (on(Feature.INCIDENCIAS)) add(Tab.SERVICIOS)
            add(Tab.MI_PERFIL)
            if (on(Feature.MEDIDORES)) add(Tab.MEDIDORES)
            add(Tab.PERSONAS)
        }
        Role.TENANT -> buildList {
            add(Tab.INICIO)
            if (on(Feature.MEDIDORES)) add(Tab.MEDIDOR)
            if (on(Feature.INCIDENCIAS)) add(Tab.SERVICIOS)
            if (on(Feature.AVISOS)) add(Tab.AVISOS)
        }
    }
}

/** Pantalla visible dentro del contenedor principal (define el sentido de la transición). */
private sealed interface Screen {
    data object Notif : Screen
    data object NewClient : Screen
    data class ClientDetail(val id: String) : Screen
    data class Account(val id: String) : Screen
    data object Me : Screen
    data class Profile(val id: String) : Screen
    data class TabScreen(val tab: Tab) : Screen
}

/** 0 = pestaña de la barra inferior; 1 = pantalla que se apila encima (detalle). */
private fun Screen.depth(tabs: List<Tab>): Int =
    if (this is Screen.TabScreen && tab in tabs) 0 else 1

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScaffold(user: AppUser) {
    val imeVisible = WindowInsets.isImeVisible
    val tabs = tabsFor(user)
    var tab by remember(user.id) { mutableStateOf(tabs.first()) }
    LaunchedEffect(user.id) {
        UiState.profileId = null
        UiState.myProfile = false
    }
    val current = if (tab in tabs || (tab == Tab.CUENTAS && user.role == Role.SUPER_ADMIN)) tab else tabs.first()
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
            .imePadding()
    ) {
        // El Super Admin tiene su propio encabezado (avatar, saludo y salir).
        // Home del administrador: el encabezado con degradado va dentro de la pantalla.
        val adminHome = user.role == Role.ADMIN && current == Tab.PANEL && profileId == null && !UiState.myProfile
        if (user.role == Role.SUPER_ADMIN) {
            Box(Modifier.statusBarsPadding()) { SuperHeader(user) }
        } else if (user.role == Role.ADMIN) {
            if (!adminHome) {
                AdminTopHeader(user, go)
            }
        } else Box(Modifier.statusBarsPadding()) {
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
        // Atrás en una sección del administrador vuelve al Home.
        BackHandler(enabled = user.role == Role.ADMIN && current != Tab.PANEL && profileId == null && !UiState.myProfile) {
            go(Tab.PANEL)
        }
        Box(Modifier.weight(1f)) {
            val openClient = UiState.openClientId
            val accountId = UiState.accountId
            val screen: Screen = when {
                user.role == Role.SUPER_ADMIN && UiState.showNotifications -> Screen.Notif
                user.role == Role.SUPER_ADMIN && UiState.newClient -> Screen.NewClient
                user.role == Role.SUPER_ADMIN && openClient != null -> Screen.ClientDetail(openClient)
                user.role == Role.SUPER_ADMIN && accountId != null -> Screen.Account(accountId)
                UiState.myProfile && user.role == Role.ADMIN -> Screen.Me
                profileId != null && user.role != Role.TENANT -> Screen.Profile(profileId)
                else -> Screen.TabScreen(current)
            }
            val reduce = reduceMotion()
            // Transiciones con sentido espacial:
            // · entre pestañas la pantalla se desliza hacia el lado de la pestaña tocada;
            // · un detalle entra desde la derecha y al regresar sale por donde vino.
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val from = initialState.depth(tabs)
                    val to = targetState.depth(tabs)
                    (when {
                        reduce -> fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                        from == 0 && to == 0 -> {
                            val a = tabs.indexOf((initialState as Screen.TabScreen).tab)
                            val b = tabs.indexOf((targetState as Screen.TabScreen).tab)
                            val dir = if (b >= a) 1 else -1
                            (slideInHorizontally(tween(Motion.QUICK_MS + 40, easing = Motion.EaseOut)) { w -> dir * w / 14 } +
                                fadeIn(tween(Motion.QUICK_MS, easing = Motion.EaseOut))) togetherWith
                                (slideOutHorizontally(tween(Motion.QUICK_MS, easing = Motion.EaseOut)) { w -> -dir * w / 14 } +
                                    fadeOut(tween(120)))
                        }
                        to > from -> // abrir detalle
                            (slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseDrawer)) { w -> w / 4 } +
                                fadeIn(tween(Motion.QUICK_MS, easing = Motion.EaseOut))) togetherWith
                                (slideOutHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseDrawer)) { w -> -w / 10 } +
                                    fadeOut(tween(150)))
                        to < from -> // regresar
                            (slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseDrawer)) { w -> -w / 10 } +
                                fadeIn(tween(Motion.QUICK_MS, easing = Motion.EaseOut))) togetherWith
                                (slideOutHorizontally(tween(Motion.SCREEN_MS - 60, easing = Motion.EaseDrawer)) { w -> w / 4 } +
                                    fadeOut(tween(150)))
                        else -> // de un detalle a otro
                            (fadeIn(tween(Motion.QUICK_MS, easing = Motion.EaseOut)) +
                                slideInVertically(tween(Motion.QUICK_MS + 40, easing = Motion.EaseOut)) { h -> h / 40 }) togetherWith
                                fadeOut(tween(120))
                    }) using SizeTransform(clip = false)
                },
                label = "screen"
            ) { s ->
            Box(Modifier.fillMaxSize()) {
            when (s) {
                Screen.Notif -> SuperNotifications { UiState.showNotifications = false }
                Screen.NewClient -> SuperNewClient {
                    UiState.newClient = false
                    tab = Tab.CLIENTES
                }
                is Screen.ClientDetail -> SuperClientDetail(s.id) { UiState.openClientId = null }
                is Screen.Account -> AccountInfo(user, s.id) { UiState.accountId = null }
                Screen.Me -> AdminMyProfile(user) { UiState.myProfile = false }
                is Screen.Profile -> TenantProfile(user, s.id) { UiState.profileId = null }
                is Screen.TabScreen -> when (s.tab) {
                    Tab.RESUMEN -> SuperHome(user, go)
                    Tab.CLIENTES -> SuperClientes()
                    Tab.PLANES -> SuperPlanes(user)
                    Tab.MI_PERFIL -> if (user.role == Role.ADMIN) AdminMyProfile(user) { go(Tab.PANEL) } else SuperMiPerfil(user, go)
                    Tab.CUENTAS -> SuperCuentas(user, onBack = { go(Tab.MI_PERFIL) })
                    Tab.PANEL -> AdminHome(user, go)
                    Tab.COBROS -> AdminCobros(user)
                    Tab.SERVICIOS -> if (user.role == Role.ADMIN) AdminServicios(user) else TenantServicios(user)
                    Tab.AVISOS -> if (user.role == Role.ADMIN) AdminAvisos(user) else TenantAvisos(user)
                    Tab.PERSONAS -> AdminPersonas(user) { go(Tab.PANEL) }
                    Tab.INICIO -> TenantHome(user, go)
                    Tab.MEDIDOR -> TenantMedidor(user)
                    Tab.MEDIDORES -> AdminMedidores(user)
                }
            }
            }
            }
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
        // Con el teclado abierto se oculta la barra inferior (más espacio para los resultados).
        if (imeVisible) {
            // nada
        } else if (user.role == Role.SUPER_ADMIN) {
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
        } else if (user.role == Role.ADMIN) {
            val barTabs = tabs.filter { it in listOf(Tab.PANEL, Tab.COBROS, Tab.SERVICIOS, Tab.MI_PERFIL) }
            val hasAvisos = Tab.AVISOS in tabs
            SuperNavBar(
                tabs = barTabs,
                current = when {
                    UiState.myProfile -> Tab.MI_PERFIL
                    profileId != null -> null
                    current == Tab.MEDIDORES || current == Tab.PERSONAS -> Tab.PANEL
                    else -> current
                },
                onSelect = go,
                onAdd = { if (hasAvisos) go(Tab.AVISOS) },
                centerIcon = Ic.Megaphone,
                centerLabel = "Avisos",
                coachKey = "ad_avisos",
                coachText = "Toca aquí para enviar un aviso a todos tus inquilinos.",
                labelOf = { t ->
                    when (t) {
                        Tab.PANEL -> "Home"
                        Tab.COBROS -> "Pagos"
                        Tab.SERVICIOS -> "Reportes"
                        else -> t.label
                    }
                },
                dotOn = { t -> t == Tab.PANEL && pendingAlerts > 0 }
            )
        } else {
            Box(Modifier.navigationBarsPadding()) {
                FloatingNav {
                    tabs.forEach { t ->
                        key(t) {
                            val sel = current == t && profileId == null && !UiState.myProfile
                            // La pastilla activa se ensancha con suavidad (se mueve en pantalla: ease-in-out)
                            val w by animateFloatAsState(
                                if (sel) 2.2f else 1f,
                                tween(Motion.QUICK_MS + 20, easing = Motion.EaseInOut),
                                label = "pillWeight"
                            )
                            NavPill(
                                icon = t.icon,
                                label = t.label,
                                selected = sel,
                                dot = t == Tab.PANEL && pendingAlerts > 0,
                                modifier = Modifier.weight(w)
                            ) { go(t) }
                        }
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
                    .pressScale(0.95f)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Line, CircleShape)
                    .clickable(onClick = onBell),
                contentAlignment = Alignment.Center
            ) {
                Icon(Ic.Bell, contentDescription = "Avisos", tint = Palette.Ink, modifier = Modifier.ring(bellDot))
                if (bellDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .popIn()
                            .padding(top = 11.dp, end = 12.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Palette.Red)
                    )
                }
            }
        }
        // El administrador abre "Mi perfil"; los demás, el menú de la cuenta.
        Box(Modifier.pressScale(0.95f).clip(CircleShape).clickable {
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
                            Icon(Ic.Help, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
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
                    Icon(Ic.Power, contentDescription = null, tint = Palette.Red, modifier = Modifier.size(18.dp))
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
