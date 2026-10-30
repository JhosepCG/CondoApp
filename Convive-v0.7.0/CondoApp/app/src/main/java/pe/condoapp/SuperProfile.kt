package pe.condoapp

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity

// ------------------------------------------------------------------
// SUPER ADMIN · Planes
// ------------------------------------------------------------------

fun planPrice(p: SubscriptionPlan): String = when (p.cycle) {
    Plan.GRATIS -> "Gratis"
    Plan.ANUAL -> "${money(p.price)}/año"
    Plan.PAGO -> "${money(p.price)}/mes"
}

@Composable
fun SuperPlanes(user: AppUser) {
    val active = Store.clients.filter { it.active }
    val paying = active.count { it.plan != Plan.GRATIS }
    val free = active.count { it.plan == Plan.GRATIS }
    var editing by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var creating by remember { mutableStateOf(false) }

    ScreenColumn {
        SectionTitle("Planes")
        // Solo cuántos pagan y cuántos están gratis
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HomeStat(Icons.Rounded.CreditCard, "$paying", "Pagando", Modifier.weight(1f))
            HomeStat(Icons.Rounded.Redeem, "$free", "Gratis", Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tus planes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF4A5160),
                modifier = Modifier.weight(1f)
            )
            Text(
                "+ Crear plan",
                color = Palette.Teal,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                modifier = Modifier.clip(CircleShape).clickable { creating = true }.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Store.plans.forEach { p ->
            key(p.id) {
                val n = Store.clientsOnPlan(p).size
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Surface)
                        .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                        .clickable { editing = p }
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientIcon(if (p.cycle == Plan.GRATIS) Icons.Rounded.Redeem else Icons.Rounded.CreditCard)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            planPrice(p) + " · " + if (n == 1) "1 cliente" else "$n clientes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Muted
                        )
                    }
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar plan", tint = Palette.Muted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (creating || editing != null) {
        PlanEditor(editing) {
            creating = false
            editing = null
        }
    }
}

@Composable
private fun PlanEditor(plan: SubscriptionPlan?, onClose: () -> Unit) {
    val members = plan?.let { Store.clientsOnPlan(it).size } ?: 0
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var cycle by remember { mutableStateOf(plan?.cycle ?: Plan.PAGO) }
    var price by remember { mutableStateOf(plan?.price?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var applyAll by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val isFree = plan?.cycle == Plan.GRATIS

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Text(
                if (plan == null) "Nuevo plan" else "Editar plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            AppTextField(name, { name = it; error = null }, "Nombre del plan", icon = Icons.Rounded.Sell)
            if (!isFree) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Cobro")
                    FlatChips(listOf("Mensual", "Anual"), if (cycle == Plan.ANUAL) "Anual" else "Mensual") {
                        cycle = if (it == "Anual") Plan.ANUAL else Plan.PAGO
                    }
                }
                AppTextField(
                    price,
                    { price = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                    if (cycle == Plan.ANUAL) "Precio por año (S/)" else "Precio por mes (S/)",
                    icon = Icons.Rounded.CreditCard,
                    keyboardType = KeyboardType.Decimal
                )
                if (members > 0) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { applyAll = !applyAll },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = applyAll,
                            onCheckedChange = { applyAll = it },
                            colors = CheckboxDefaults.colors(checkedColor = Palette.Teal)
                        )
                        Text(
                            "Aplicar el nuevo precio a ${if (members == 1) "1 cliente" else "$members clientes"} con este plan",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text("El plan Gratis no cobra y tiene todas las funciones de pago.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            error?.let { ErrorText(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line)
                ) { Text("Cancelar", color = Palette.Ink) }
                Button(
                    onClick = {
                        val r = Store.savePlan(plan?.id, name, price.toDoubleOrNull(), if (isFree) Plan.GRATIS else cycle, applyAll)
                        error = r
                        if (r == null) onClose()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal, contentColor = Color.White)
                ) { Text("Guardar") }
            }
            if (plan != null && !isFree) {
                Text(
                    "Eliminar plan",
                    color = Palette.Red,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val r = Store.deletePlan(plan.id)
                            error = r
                            if (r == null) onClose()
                        }
                        .padding(6.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// SUPER ADMIN · Mi perfil
// ------------------------------------------------------------------

@Composable
fun SuperMiPerfil(user: AppUser, go: (Tab) -> Unit) {
    val context = LocalContext.current
    var photoMenu by remember { mutableStateOf(false) }
    var editInfo by remember { mutableStateOf(false) }
    var createAdmin by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var bioOn by remember { mutableStateOf(Store.biometricUserId() == user.id) }
    var bioStatus by remember { mutableStateOf(Biometric.status(context)) }
    var askBioPermission by remember { mutableStateOf(false) }
    // Al volver de los ajustes (después de registrar una huella) se vuelve a revisar
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) bioStatus = Biometric.status(context)
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val picker = rememberPhotoPicker(
        onResult = { path ->
            Photos.delete(user.photoPath)
            Store.updateUser(user.copy(photoPath = path))
        },
        onError = { message = it }
    )

    ScreenColumn {
        // Foto, nombre y rol
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BrandGradient)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.clip(CircleShape).clickable { photoMenu = true }) {
                UserAvatar(user, 92.dp, bg = Color.White.copy(alpha = 0.18f), fg = Color.White)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Cambiar foto", tint = Palette.Teal, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(user.name, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text("Super Admin", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
        }

        message?.let { Pill(it, Palette.Teal, Palette.TealSoft, Icons.Rounded.CheckCircle) }

        // Secciones
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
        ) {
            ProfileRow(Icons.Rounded.Edit, "Editar información", "Nombre y celular") { editInfo = true }
            LineDivider()
            ProfileRow(Icons.Rounded.PersonAddAlt, "Crear super admin", "Nombre completo, DNI y celular") { createAdmin = true }
            LineDivider()
            ProfileRow(
                Icons.Rounded.NotificationsNone,
                "Notificaciones",
                if (Store.unreadSupport() > 0) "${Store.unreadSupport()} pedidos de ayuda sin leer" else "Pedidos de ayuda de los administradores"
            ) { UiState.showNotifications = true }
            LineDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientIcon(Icons.Rounded.Fingerprint)
                Column(Modifier.weight(1f)) {
                    Text("Ingresar con huella dactilar", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
                    Text(
                        when (bioStatus) {
                            Biometric.Status.READY -> if (bioOn) "Activado: al cerrar sesión entras con tu huella" else "Entra sin escribir tu contraseña"
                            Biometric.Status.NOT_ENROLLED -> "Primero registra una huella en tu teléfono"
                            Biometric.Status.NO_HARDWARE -> "Este teléfono no tiene lector de huella"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted
                    )
                }
                Switch(
                    checked = bioOn,
                    enabled = bioStatus != Biometric.Status.NO_HARDWARE,
                    onCheckedChange = { on ->
                        if (!on) {
                            Store.setBiometricUser(null)
                            bioOn = false
                            message = "Ingreso con huella desactivado"
                        } else {
                            // Primero se pide permiso para usar la biometría del teléfono
                            askBioPermission = true
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Teal, checkedThumbColor = Color.White)
                )
            }
            LineDivider()
            ProfileRow(Icons.Rounded.Groups, "Cuentas y accesos", "Todas las cuentas de Convive") { go(Tab.CUENTAS) }
            LineDivider()
            ProfileRow(Icons.Rounded.Refresh, "Restablecer datos de prueba", "Vuelve a los datos de ejemplo") { confirmReset = true }
        }
        PrimaryButton("Cerrar sesión", { confirmLogout = true }, Modifier.fillMaxWidth(), color = Palette.Red, icon = Icons.Rounded.PowerSettingsNew)
        Spacer(Modifier.height(8.dp))
    }

    if (photoMenu) {
        Dialog(onDismissRequest = { photoMenu = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                Text("Foto de perfil", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                SecondaryButton("Tomar foto", { photoMenu = false; picker.camera() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                SecondaryButton("Elegir de la galería", { photoMenu = false; picker.gallery() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoLibrary)
                if (user.photoPath != null) {
                    TextButton(onClick = {
                        Photos.delete(user.photoPath)
                        Store.updateUser(user.copy(photoPath = null))
                        photoMenu = false
                    }) { Text("Quitar foto", color = Palette.Red) }
                }
            }
        }
    }
    if (askBioPermission) {
        Dialog(onDismissRequest = { askBioPermission = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                CircleIcon(Icons.Rounded.Fingerprint, Palette.Teal, Palette.TealSoft)
                Text(
                    "¿Permitir que Convive use tu huella?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    if (bioStatus == Biometric.Status.NOT_ENROLLED)
                        "Tu teléfono aún no tiene una huella registrada. Te llevaremos a los ajustes para registrarla y luego vuelves a activar esta opción."
                    else
                        "Usaremos la biometría de tu teléfono para que entres solo con tu huella. Tu huella no sale del teléfono.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = Palette.Muted,
                    textAlign = TextAlign.Center
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { askBioPermission = false },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = CircleShape
                    ) { Text("No permitir", color = Palette.Ink) }
                    Button(
                        onClick = {
                            askBioPermission = false
                            if (bioStatus == Biometric.Status.NOT_ENROLLED) {
                                runCatching { context.startActivity(Biometric.enrollIntent()) }
                            } else {
                                val act = context as? FragmentActivity
                                if (act != null) {
                                    Biometric.prompt(
                                        act,
                                        "Activar ingreso con huella",
                                        "Coloca tu huella para confirmar",
                                        onSuccess = {
                                            Store.setBiometricUser(user.id)
                                            bioOn = true
                                            message = "Ingreso con huella activado"
                                        },
                                        onError = { message = it }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal)
                    ) { Text("Permitir") }
                }
            }
        }
    }
    if (editInfo) EditInfoDialog(user) { editInfo = false }
    if (createAdmin) CreateSuperAdminDialog { createAdmin = false }
    if (confirmReset) {
        ConfirmDialog(
            title = "Restablecer datos",
            text = "Se borrarán todos los cambios y volverán los datos de ejemplo. Se cerrará tu sesión.",
            confirm = "Restablecer",
            onConfirm = { Store.resetDemo() },
            onDismiss = { confirmReset = false }
        )
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
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GradientIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Palette.Muted)
    }
}

@Composable
private fun EditInfoDialog(user: AppUser, onClose: () -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Text("Editar información", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
            AppTextField(name, { name = it; error = null }, "Nombre completo", icon = Icons.Rounded.PersonOutline)
            AppTextField(
                phone,
                { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null },
                "Celular",
                icon = Icons.Rounded.PhoneAndroid,
                keyboardType = KeyboardType.Phone
            )
            error?.let { ErrorText(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(50.dp), shape = CircleShape) {
                    Text("Cancelar", color = Palette.Ink)
                }
                Button(
                    onClick = {
                        when {
                            name.isBlank() -> error = "Escribe tu nombre."
                            phone.isNotEmpty() && phone.length != 9 -> error = "El celular debe tener 9 números."
                            else -> {
                                Store.updateUser(user.copy(name = name.trim(), phone = phone.ifEmpty { null }))
                                onClose()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal)
                ) { Text("Guardar") }
            }
        }
    }
}

@Composable
private fun CreateSuperAdminDialog(onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<String?>(null) }

    if (done != null) {
        ResultDialog(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Super admin creado",
            text = "Ingresa con su DNI $done.\nSu contraseña inicial también es $done.",
            button = "Aceptar",
            onClose = onClose
        )
        return
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Text("Crear super admin", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
            Text(
                "Tendrá los mismos accesos que tú.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                modifier = Modifier.fillMaxWidth()
            )
            AppTextField(name, { name = it; error = null }, "Nombre completo", icon = Icons.Rounded.PersonOutline)
            AppTextField(
                dni,
                { dni = it.filter { ch -> ch.isDigit() }.take(8); error = null },
                "DNI",
                icon = Icons.Rounded.Badge,
                keyboardType = KeyboardType.Number
            )
            AppTextField(
                phone,
                { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null },
                "Celular",
                icon = Icons.Rounded.PhoneAndroid,
                keyboardType = KeyboardType.Phone
            )
            error?.let { ErrorText(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(50.dp), shape = CircleShape) {
                    Text("Cancelar", color = Palette.Ink)
                }
                Button(
                    onClick = {
                        val r = Store.createSuperAdmin(name, dni, phone)
                        error = r
                        if (r == null) done = dni
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal)
                ) { Text("Crear") }
            }
        }
    }
}

// ------------------------------------------------------------------
// SUPER ADMIN · Notificaciones (pedidos de ayuda)
// ------------------------------------------------------------------

@Composable
fun SuperNotifications(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val context = LocalContext.current
    val list = Store.supportRequests.reversed()

    ScreenColumn {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("‹ Volver", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notificaciones", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (Store.unreadSupport() > 0) {
                Text(
                    "Marcar como leídas",
                    color = Palette.Teal,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).clickable { Store.markSupportRead() }.padding(6.dp)
                )
            }
        }
        if (!Notifier.canNotify(context)) {
            Text(
                "Activa las notificaciones de Convive en los ajustes del teléfono para recibir los avisos.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.AmberDark
            )
        }
        if (list.isEmpty()) EmptyState(Icons.Rounded.NotificationsNone, "No tienes pedidos de ayuda.")
        list.forEach { r ->
            key(r.id) {
                val from = Store.users.firstOrNull { it.id == r.fromUserId }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (r.read) Palette.Surface else Palette.TealSoft)
                        .border(1.dp, if (r.read) Palette.Line else Palette.TealSoft, RoundedCornerShape(12.dp))
                        .clickable { Store.markSupportRead(r.id) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (from != null) UserAvatar(from, 40.dp, bg = Palette.AmberSoft, fg = Palette.AmberDark)
                        else Avatar(r.fromName, 40.dp, bg = Palette.AmberSoft, fg = Palette.AmberDark)
                        Column(Modifier.weight(1f)) {
                            Text("${r.fromName} pide ayuda", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
                            Text("${r.clientName} · ${r.createdAt}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        if (!r.read) Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.Red))
                    }
                    Text(r.message, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        from?.phone?.let { ph ->
                            Text(
                                "Llamar",
                                color = Palette.Teal,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    Store.markSupportRead(r.id)
                                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ph"))) }
                                }.padding(4.dp)
                            )
                        }
                        r.clientId?.let { cid ->
                            if (Store.client(cid) != null) {
                                Text(
                                    "Ver cliente",
                                    color = Palette.Teal,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                        Store.markSupportRead(r.id)
                                        UiState.showNotifications = false
                                        UiState.openClientId = cid
                                    }.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Pedir ayuda al equipo de Convive
// ------------------------------------------------------------------

@Composable
fun HelpRequestDialog(user: AppUser, onClose: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    if (sent) {
        ResultDialog(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Mensaje enviado",
            text = "El equipo de Convive recibió tu pedido y te contactará pronto.",
            button = "Aceptar",
            onClose = onClose
        )
        return
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            CircleIcon(Icons.Rounded.SupportAgent, Palette.Teal, Palette.TealSoft)
            Text("¿En qué te ayudamos?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                "Tu mensaje le llega al equipo de Convive como notificación.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                textAlign = TextAlign.Center
            )
            AppTextField(msg, { msg = it; error = null }, "Escribe tu consulta", singleLine = false, minLines = 3)
            error?.let { ErrorText(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(50.dp), shape = CircleShape) {
                    Text("Cancelar", color = Palette.Ink)
                }
                Button(
                    onClick = {
                        val r = Store.addSupportRequest(user, msg)
                        error = r
                        if (r == null) sent = true
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Teal)
                ) { Text("Enviar") }
            }
        }
    }
}
