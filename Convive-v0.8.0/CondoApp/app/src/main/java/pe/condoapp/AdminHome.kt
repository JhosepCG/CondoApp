package pe.condoapp

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.time.YearMonth

// ------------------------------------------------------------------
// ADMINISTRADOR · Home (diseño "Index" de Figma)
// Encabezado con degradado, buscador inteligente y accesos rápidos.
// ------------------------------------------------------------------

private val CardBorder = Color(0xFFF2F2F2)

/** Íconos de la barra de estado en blanco mientras se ve el degradado. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity ?: return
    if (view.isInEditMode) return
    DisposableEffect(Unit) {
        val ctrl = WindowCompat.getInsetsController(activity.window, view)
        ctrl.isAppearanceLightStatusBars = false
        onDispose { ctrl.isAppearanceLightStatusBars = true }
    }
}

@Composable
fun AdminHome(user: AppUser, go: (Tab) -> Unit) {
    val c = Store.clientOf(user) ?: return
    LightStatusBarIcons()

    var q by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val searching = focused || q.isNotEmpty()
    val closeSearch = {
        q = ""
        focus.clearFocus()
    }
    BackHandler(enabled = searching) { closeSearch() }

    val pendingAlerts = Store.emergencies.count { it.clientId == c.id && it.status == EmergencyStatus.PENDIENTE }
    val openInc = Store.incidents.count { it.clientId == c.id && !(it.status == IncidentStatus.RESUELTO && it.confirmed == true) }

    Column(Modifier.fillMaxSize()) {
        // ---------- Encabezado con degradado ----------
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(BrandGradient)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)
                .animateContentSize(tween(Motion.QUICK_MS + 60, easing = Motion.EaseOut))
        ) {
            AdminHeroHeader(
                user = user,
                badge = pendingAlerts + openInc,
                onBell = { if (Tab.SERVICIOS in tabsFor(user)) go(Tab.SERVICIOS) }
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.padding(end = 8.dp)) {
                HeroSearch(
                    value = q,
                    onValueChange = { q = it },
                    active = searching,
                    onFocus = { focused = it },
                    onClear = closeSearch
                )
                CoachMark("ad_buscar", "Busca por nombre, piso o DNI. Prueba con «2° piso» o «rosa».", above = false)
            }
            AnimatedVisibility(
                visible = !searching,
                enter = fadeIn(tween(Motion.QUICK_MS)) + expandVertically(tween(Motion.QUICK_MS + 40, easing = Motion.EaseOut)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(Motion.QUICK_MS, easing = Motion.EaseOut))
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, end = 8.dp)) {
                    HeroAction(Ic.CheckCircle, "Suscripción", Modifier.weight(1f), 0) { go(Tab.MI_PERFIL) }
                    if (Store.featureOn(c, Feature.MEDIDORES)) {
                        HeroAction(Ic.Bolt, "Medidores", Modifier.weight(1f), 1) { go(Tab.MEDIDORES) }
                    }
                    HeroAction(Ic.Users, "Mis inquilinos", Modifier.weight(1f), 2) { go(Tab.PERSONAS) }
                }
            }
        }

        // ---------- Contenido ----------
        AnimatedContent(
            targetState = searching,
            transitionSpec = {
                fadeIn(tween(Motion.QUICK_MS, easing = Motion.EaseOut)) togetherWith fadeOut(tween(100))
            },
            label = "adminHome",
            modifier = Modifier.weight(1f)
        ) { s ->
            if (s) SearchResults(c, q) else HomeCards(user, c, go)
        }
    }
}

@Composable
private fun AdminHeroHeader(user: AppUser, badge: Int, onBell: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UserAvatar(user, 42.dp, initialsOf = user.name.substringBefore(" "))
        Column(Modifier.weight(1f).padding(start = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "¡Hola ${user.name.substringBefore(" ")}!",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Administrador", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Color.White)
        }
        HeroCircle(Ic.Bell, "Notificaciones", badge, onBell)
        HeroCircle(Ic.Power, "Cerrar sesión", 0) { confirmLogout = true }
    }
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

@Composable
private fun HeroCircle(icon: ImageVector, label: String, badge: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .pressScale(0.95f)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp).ring(badge > 0))
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .popIn()
                    .padding(top = 6.dp, end = 6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Palette.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(if (badge > 9) "9+" else "$badge", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
            }
        }
    }
}

/** Buscador del encabezado: translúcido en reposo, blanco con borde verde al escribir. */
@Composable
private fun HeroSearch(
    value: String,
    onValueChange: (String) -> Unit,
    active: Boolean,
    onFocus: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    val requester = remember { FocusRequester() }
    val bg by androidx.compose.animation.animateColorAsState(
        if (active) Color.White else Color.White.copy(alpha = 0.2f), tween(Motion.QUICK_MS), label = "searchBg"
    )
    val stroke by androidx.compose.animation.animateColorAsState(
        if (active) Palette.Green else Palette.Line, tween(Motion.QUICK_MS), label = "searchStroke"
    )
    val content = if (active) Palette.Ink else Color.White
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, stroke, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { requester.requestFocus() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Ic.Search, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "Buscar inquilinos o departamento",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = if (active) Palette.Muted else Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Green),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .onFocusChanged { onFocus(it.isFocused) }
            )
        }
        AnimatedVisibility(active, enter = fadeIn() + scaleIn(initialScale = 0.6f), exit = fadeOut() + scaleOut(targetScale = 0.6f)) {
            Icon(
                Ic.Close,
                contentDescription = "Cerrar búsqueda",
                tint = Palette.Ink,
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun HeroAction(icon: ImageVector, label: String, modifier: Modifier, index: Int, onClick: () -> Unit) {
    Column(
        modifier
            .enter(index)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .pressScale(0.94f)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

// ---------------- Resultados del buscador ----------------

@Composable
private fun SearchResults(c: Client, q: String) {
    val focus = LocalFocusManager.current
    val all = Store.tenantsOf(c.id)
    val found = if (q.isBlank()) all.sortedBy { it.unit ?: it.name } else Store.searchTenants(q, c.id)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when {
                q.isBlank() -> "Tus inquilinos · ${all.size}"
                found.size == 1 -> "1 Resultado"
                else -> "${found.size} Resultados"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = Palette.Muted
        )
        if (q.isBlank()) {
            Text(
                "Escribe un nombre, apellido, piso, depa o DNI. Ej.: «2do piso», «rosa h», «piso 3».",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted
            )
        }
        if (q.isNotBlank() && found.isEmpty()) {
            EmptyState(Ic.Search, "No encontramos «${q.trim()}». Prueba con el piso, el nombre o el DNI.")
        }
        found.forEachIndexed { i, u ->
            key(u.id) {
                TenantResultCard(u, i) {
                    focus.clearFocus()
                    UiState.profileId = u.id
                }
            }
        }
    }
}

private class Tag(val text: String, val fg: Color, val bg: Color)

private fun tagsFor(u: AppUser): List<Tag> {
    val c = Store.client(u.clientId)
    val st = tenantStatus(u)
    val out = mutableListOf<Tag>()
    if (c?.template == Template.CASA_DEPAS) {
        Store.contractOf(u.id)?.let { ct ->
            when {
                Store.contractExpired(ct) -> out += Tag("Contrato vencido", Palette.Red, Palette.RedSoft)
                Store.contractEndingSoon(ct) -> out += Tag("Contrato por vencer", Palette.AmberDark, Palette.AmberSoft)
            }
        }
    }
    if (Store.featureOn(c, Feature.COBROS)) {
        out += when {
            st.overdue > 0 -> Tag("Pago vencido", Palette.Red, Palette.RedSoft)
            !st.upToDate -> Tag("Pago pendiente", Palette.AmberDark, Palette.AmberSoft)
            else -> Tag("Pago al día", Palette.Teal, Palette.TealSoft)
        }
    }
    when (st.meterSent) {
        true -> out += Tag("Medidor", Palette.Teal, Palette.TealSoft)
        false -> out += Tag("Falta medidor", Palette.AmberDark, Palette.AmberSoft)
        null -> Unit
    }
    if (st.openIncidents > 0) {
        out += Tag(if (st.openIncidents == 1) "1 reporte" else "${st.openIncidents} reportes", Palette.AmberDark, Palette.AmberSoft)
    }
    return out
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TenantResultCard(u: AppUser, index: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .enter(index.coerceAtMost(6))
            .pressScale(0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserAvatar(u, 42.dp, bg = Palette.TealSoft, fg = Palette.Teal)
            Column(Modifier.weight(1f)) {
                Text(
                    u.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Palette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(u.unit ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted)
            }
            Icon(Ic.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
        }
        val tags = tagsFor(u)
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { t ->
                    Text(
                        t.text,
                        color = t.fg,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(t.bg)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ---------------- Tarjetas del Home ----------------

@Composable
private fun HomeCards(user: AppUser, c: Client, go: (Tab) -> Unit) {
    val list = Store.emergencies.filter { it.clientId == c.id }.reversed()
    val pending = list.filter { it.status == EmergencyStatus.PENDIENTE }
    val active = list.filter { it.status == EmergencyStatus.CONFIRMADA }
    val tenants = Store.tenantsOf(c.id)
    val meterReq = if (Store.featureOn(c, Feature.MEDIDORES)) Store.activeMeterRequest(c.id) else null
    val sentCount = meterReq?.let { r -> tenants.count { t -> Store.readingFor(r.id, t.id) != null } } ?: 0
    val month = YearMonth.now()
    val allCharges = Store.charges.filter { it.clientId == c.id }
    val monthCharges = allCharges.filter { YearMonth.from(it.due) == month }.ifEmpty { allCharges }
    val collected = monthCharges.filter { it.paid }.sumOf { it.amount }
    val subSt = Store.subStatus(c)
    val showSub = Store.isOwner(user) && Store.planOf(c)?.reminder != false &&
        (subSt == SubStatus.VENCIDO || subSt == SubStatus.PENDIENTE)
    val toRenew = if (c.template == Template.CASA_DEPAS) contractsToReview(c.id) else emptyList()
    var i = 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (Feature.EMERGENCIA in c.modules) {
            pending.forEach { e -> key(e.id) { EmergencyReportCard(e, Modifier.enter(i++)) } }
            active.forEach { e -> key(e.id) { ActiveEmergencyCard(e, Modifier.enter(i++)) } }
        }
        if (showSub) {
            val late = subSt == SubStatus.VENCIDO
            Row(
                Modifier
                    .enter(i++)
                    .pressScale(0.98f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (late) Palette.RedSoft else Palette.AmberSoft)
                    .border(1.dp, if (late) Palette.Red else Palette.Amber, RoundedCornerShape(16.dp))
                    .clickable { go(Tab.MI_PERFIL) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(painterResource(R.drawable.ill_sub_late), null, Modifier.size(width = 58.dp, height = 60.dp).floating(3.dp, 2600))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (late) "Tu suscripción está vencida" else "Paga tu suscripción",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Palette.Ink
                    )
                    Text(
                        "${Store.feeLabel(c)} · vence el ${Store.subDue(Store.periodFor(c)).format(dateFmt)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = if (late) Palette.Red else Palette.AmberDark
                    )
                }
                Icon(Ic.ChevronRight, contentDescription = null, tint = if (late) Palette.Red else Palette.AmberDark, modifier = Modifier.size(22.dp))
            }
        }

        if (Feature.COBROS in c.modules) {
            Row(
                Modifier
                    .enter(i++)
                    .pressScale(0.98f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                    .clickable { go(Tab.COBROS) }
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(painterResource(R.drawable.ill_wallet), null, Modifier.size(width = 80.dp, height = 62.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Recaudado del mes",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        color = Palette.Muted
                    )
                    CountUpText(
                        collected,
                        { v -> money(v) },
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = Palette.Ink
                    )
                    Text(
                        if (c.name.contains("·")) c.name else "${c.name} · ${tenants.size} inquilinos",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Palette.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (toRenew.isNotEmpty()) {
            Column(
                Modifier
                    .enter(i++)
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
                        Text(
                            "Contratos por renovar",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            color = Palette.Ink
                        )
                        Text("Indícales que deben firmar nuevamente", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
                toRenew.forEach { (t, ct) ->
                    key(t.id) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Palette.Surface)
                                .border(1.dp, Palette.Amber, RoundedCornerShape(8.dp))
                                .clickable { UiState.profileId = t.id }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${t.unit ?: "-"} - ${t.name}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = Palette.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    (if (Store.contractExpired(ct)) "Venció el " else "Vence el ") + ct.end.format(dateFmt),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = Palette.Muted
                                )
                            }
                            Text(
                                "Avisar",
                                color = Palette.Teal,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { Store.requestRenewal(t.id) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (meterReq != null) {
            Column(
                Modifier
                    .enter(i++)
                    .pressScale(0.98f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                    .clickable { go(Tab.MEDIDORES) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Palette.AmberSoft),
                        contentAlignment = Alignment.Center
                    ) { Icon(Ic.Bolt, contentDescription = null, tint = Palette.AmberDark, modifier = Modifier.size(24.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(meterReq.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Palette.Ink)
                        Text(
                            "$sentCount de ${tenants.size} fotos · hasta el ${meterReq.due.format(dateFmt)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = Palette.Muted
                        )
                    }
                    Icon(Ic.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
                }
                ProgressLine(if (tenants.isEmpty()) 0f else sentCount.toFloat() / tenants.size, Palette.Teal, Palette.TealSoft, 6.dp)
            }
        }

        if (i == 0) {
            EmptyState(Ic.CheckCircle, "Todo en orden. No hay pendientes por ahora.")
        }
    }
}

/** Reporte de emergencia nuevo: el administrador verifica antes de avisar a todos. */
@Composable
private fun EmergencyReportCard(e: Emergency, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.RedSoft)
            .border(1.dp, Palette.Red, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.ill_gas), null, Modifier.size(width = 58.dp, height = 56.dp).heartBeat())
            Column(Modifier.weight(1f)) {
                Text(e.type, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Palette.Ink)
                Text("${e.unit} · ${e.createdAt}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Red)
            }
            Text(
                "Nuevo",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.clip(CircleShape).background(Palette.Red).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Text(
            "Verifica antes de confirmar. Si es real, se avisará a los residentes.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Palette.Ink
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Falsa alarma", { Store.setEmergency(e.id, EmergencyStatus.DESCARTADA) }, Modifier.weight(1f))
            PrimaryButton("Confirmar", { Store.setEmergency(e.id, EmergencyStatus.CONFIRMADA) }, Modifier.weight(1f), color = Palette.Red)
        }
    }
}

@Composable
private fun ActiveEmergencyCard(e: Emergency, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.RedSoft)
            .border(1.dp, Palette.Red, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.ill_gas), null, Modifier.size(width = 58.dp, height = 56.dp))
            Column(Modifier.weight(1f)) {
                Text("Emergencia activa: ${e.type}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Palette.Red)
                Text("${e.unit} · residentes avisados", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Palette.Muted)
            }
        }
        PrimaryButton("Cerrar emergencia", { Store.setEmergency(e.id, EmergencyStatus.CERRADA) }, Modifier.fillMaxWidth(), color = Palette.Navy, icon = Ic.Check)
    }
}
