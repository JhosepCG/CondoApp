package pe.condoapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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

/** Texto bajo el nombre del plan: precio, prueba o funciones. */
fun planSubtitle(p: SubscriptionPlan): String = when {
    p.trial -> "Prueba de $TRIAL_DAYS días"
    p.cycle != Plan.GRATIS -> planPrice(p)
    p.fullFeatures -> "Funciones completas"
    else -> "Gratis"
}

@Composable
fun SuperPlanes(user: AppUser) {
    var editing by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var creating by remember { mutableStateOf(false) }

    ScreenColumn {
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
                Text(
                    "Planes a clientes",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "+ Crear plan",
                    color = Palette.Teal,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clip(CircleShape).clickable { creating = true }.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
            HorizontalDivider(color = Palette.Line, modifier = Modifier.padding(bottom = 4.dp))
            Store.plans.forEachIndexed { i, p ->
                key(p.id) { PlanRow(p, Modifier.enter(i + 1)) { editing = p } }
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
private fun PlanRow(p: SubscriptionPlan, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .pressScale(0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Palette.TealSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (p.cycle == Plan.GRATIS && !p.trial) Ic.HandHeart else Ic.Dollar,
                contentDescription = null,
                tint = Palette.Teal,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(planSubtitle(p), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Ic.ChevronRight, contentDescription = "Editar plan", tint = Palette.Ink, modifier = Modifier.size(26.dp))
    }
}

/** Crear o editar plan: ilustración, nombre, mensualidad y funciones del cliente. */
@Composable
private fun PlanEditor(plan: SubscriptionPlan?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var price by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", plan?.price ?: 0.0)) }
    var full by remember { mutableStateOf(plan?.fullFeatures ?: false) }
    var trial by remember { mutableStateOf(plan?.trial ?: false) }
    var reminder by remember { mutableStateOf(plan?.reminder ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    val annual = plan?.cycle == Plan.ANUAL

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .popIn()
                .fillMaxWidth(0.9f)
                .widthIn(max = 440.dp)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Palette.Surface)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (plan == null) "Crear plan" else "Editar plan",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ill_plan),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.66f)
                    .aspectRatio(510f / 486f)
                    .floating(4.dp, 3000)
            )
            AppTextField(
                name, { name = it; error = null }, "Nombre del plan",
                icon = Ic.User,
                placeholder = "Ej. " + (plan?.name ?: "Plan mayo")
            )
            AppTextField(
                price,
                { price = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                if (annual) "Anualidad (S/)" else "Mensualidad (S/)",
                icon = Ic.Card,
                keyboardType = KeyboardType.Decimal
            )
            SectionLabel("Funciones del cliente")
            FeatureSwitch("Funciones completas", full) { full = it }
            FeatureSwitch("Prueba de $TRIAL_DAYS días", trial) {
                trial = it
                if (it) price = "0.00"
            }
            FeatureSwitch("Recordatorio de pago", reminder) { reminder = it }
            error?.let { ErrorText(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) {
                    Text("Cancelar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                }
                TextButton(onClick = {
                    val r = Store.savePlan(plan?.id, name, price.toDoubleOrNull(), full, trial, reminder)
                    error = r
                    if (r == null) onClose()
                }) {
                    Text("Guardar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
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
    var photoMenu by remember { mutableStateOf<String?>(null) } // "photo" o "cover"
    var editInfo by remember { mutableStateOf(false) }
    var createAdmin by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pushOn by remember { mutableStateOf(Store.pushEnabled()) }
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
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { ok -> if (!ok) message = "Activa las notificaciones de Convive en los ajustes del teléfono" }

    var target by remember { mutableStateOf("photo") }
    val picker = rememberPhotoPicker(
        onResult = { path ->
            val me = Store.users.firstOrNull { it.id == user.id } ?: user
            if (target == "cover") {
                Photos.delete(me.coverPath)
                Store.updateUser(me.copy(coverPath = path))
            } else {
                Photos.delete(me.photoPath)
                Store.updateUser(me.copy(photoPath = path))
            }
        },
        onError = { message = it }
    )

    ScreenColumn {
        // Portada y foto de perfil
        Box(Modifier.fillMaxWidth().height(142.dp).enter(0)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (user.coverPath != null) {
                    PhotoImage(user.coverPath, Modifier.fillMaxSize())
                } else {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.sa_cover),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { photoMenu = "cover" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Ic.Edit, contentDescription = "Cambiar portada", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .pressScale(0.95f)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Palette.Bg)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .clickable { photoMenu = "photo" },
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(user, 96.dp, initialsOf = user.name.substringBefore(" "))
            }
        }

        message?.let { Pill(it, Palette.Teal, Palette.TealSoft, Ic.CheckCircle) }

        // Secciones
        Column(
            Modifier
                .enter(1)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            ProfileRow(Ic.User, "Editar información") { editInfo = true }
            LineDivider()
            ProfileRow(Ic.UserPlus, "Crear super admin") { createAdmin = true }
            LineDivider()
            ProfileRow(Ic.Bell, "Notificaciones push", trailing = {
                AppSwitch(pushOn) { on ->
                    pushOn = on
                    Store.setPush(on)
                    if (on && !Notifier.canNotify(context) && android.os.Build.VERSION.SDK_INT >= 33) {
                        notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    message = if (on) "Te avisaremos cuando un administrador pida ayuda" else "Notificaciones push desactivadas"
                }
            })
            LineDivider()
            ProfileRow(Ic.Fingerprint, "Ingresar con huella dactilar", trailing = {
                AppSwitch(bioOn, enabled = bioStatus != Biometric.Status.NO_HARDWARE) { on ->
                    if (!on) {
                        Store.setBiometricUser(null)
                        bioOn = false
                        message = "Ingreso con huella desactivado"
                    } else {
                        // Primero se pide permiso para usar la biometría del teléfono
                        askBioPermission = true
                    }
                }
            })
            LineDivider()
            ProfileRow(Ic.Users, "Cuentas y accesos") { go(Tab.CUENTAS) }
        }
        if (bioStatus == Biometric.Status.NO_HARDWARE) {
            Text("Este teléfono no tiene lector de huella.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.enter(2).pressScale(0.97f).fillMaxWidth().height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Red, contentColor = Color.White)
        ) {
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
        Spacer(Modifier.height(8.dp))
    }

    photoMenu?.let { kind ->
        val me = Store.users.firstOrNull { it.id == user.id } ?: user
        val current = if (kind == "cover") me.coverPath else me.photoPath
        Dialog(onDismissRequest = { photoMenu = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                Text(if (kind == "cover") "Foto de portada" else "Foto de perfil", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                SecondaryButton("Tomar foto", { target = kind; photoMenu = null; picker.camera() }, Modifier.fillMaxWidth(), icon = Ic.Camera)
                SecondaryButton("Elegir de la galería", { target = kind; photoMenu = null; picker.gallery() }, Modifier.fillMaxWidth(), icon = Ic.Image)
                if (current != null) {
                    TextButton(onClick = {
                        Photos.delete(current)
                        Store.updateUser(if (kind == "cover") me.copy(coverPath = null) else me.copy(photoPath = null))
                        photoMenu = null
                    }) { Text("Quitar foto", color = Palette.Red) }
                }
            }
        }
    }
    if (askBioPermission) {
        Dialog(onDismissRequest = { askBioPermission = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DialogCard {
                CircleIcon(Ic.Fingerprint, Palette.Teal, Palette.TealSoft)
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
    if (editInfo) EditInfoDialog(user, onPhoto = { photoMenu = "photo" }) { editInfo = false }
    if (createAdmin) CreateSuperAdminDialog { createAdmin = false }
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

/** Interruptor verde azulado de la app. */
@Composable
fun AppSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        enabled = enabled,
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

/** Fila de "Mi perfil": ícono gris en cuadrado claro, título y flecha (o un interruptor). */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Palette.Field), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(24.dp))
        }
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        else Icon(Ic.ChevronRight, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(26.dp))
    }
}

/** Botones de texto "Cancelar · Guardar" alineados a la derecha (como en Figma). */
@Composable
fun TextActions(onCancel: () -> Unit, onSave: () -> Unit, saveLabel: String = "Guardar") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) {
            Text("Cancelar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        }
        TextButton(onClick = onSave) {
            Text(saveLabel, color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun EditInfoDialog(user: AppUser, onPhoto: () -> Unit, onClose: () -> Unit) {
    val me = Store.users.firstOrNull { it.id == user.id } ?: user
    var name by remember { mutableStateOf(user.name) }
    var dni by remember { mutableStateOf(user.dni ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Box(Modifier.size(104.dp).clip(CircleShape).clickable(onClick = onPhoto)) {
                UserAvatar(me, 96.dp, initialsOf = me.name.substringBefore(" "))
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Palette.Navy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Ic.Camera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Text("Editar información", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            AppTextField(name, { name = it; error = null }, "Nombre completo", icon = Ic.User)
            AppTextField(
                dni,
                { dni = it.filter { ch -> ch.isDigit() }.take(8); error = null },
                "DNI",
                icon = Ic.Card,
                keyboardType = KeyboardType.Number,
                placeholder = "Ej. 87654321"
            )
            AppTextField(
                phone,
                { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null },
                "Celular",
                icon = Ic.Card,
                keyboardType = KeyboardType.Phone,
                placeholder = "Ej. 987 654 321"
            )
            error?.let { ErrorText(it) }
            TextActions(onClose, {
                val r = Store.updateAccount(me.id, name, me.username, me.password, dni, phone)
                error = r
                if (r == null) onClose()
            })
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
            icon = Ic.CheckCircle,
            iconTint = Palette.Green,
            iconBg = Palette.GreenSoft,
            title = "Super admin creado",
            text = "Usuario: $done\nContraseña: $DEFAULT_PASSWORD\nTambién puede ingresar con su DNI.",
            button = "Aceptar",
            onClose = onClose
        )
        return
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogCard {
            Text("Crear super admin", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text("Tendrá los mismos accesos que tú", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center)
            AppTextField(name, { name = it; error = null }, "Nombre completo", icon = Ic.User, placeholder = "Ej. Pedro")
            AppTextField(
                dni,
                { dni = it.filter { ch -> ch.isDigit() }.take(8); error = null },
                "DNI",
                icon = Ic.Card,
                keyboardType = KeyboardType.Number,
                placeholder = "Ej. 87654321"
            )
            AppTextField(
                phone,
                { phone = it.filter { ch -> ch.isDigit() }.take(9); error = null },
                "Celular",
                icon = Ic.Card,
                keyboardType = KeyboardType.Phone,
                placeholder = "Ej. 987 654 321"
            )
            if (name.isNotBlank()) {
                Text(
                    "Usuario: ${Store.suggestUsername(name)} · Contraseña: $DEFAULT_PASSWORD",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            error?.let { ErrorText(it) }
            TextActions(onClose, {
                val r = Store.createSuperAdmin(name, dni, phone)
                error = r
                if (r == null) done = Store.lastCreated?.username
            })
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
        if (list.isEmpty()) EmptyState(Ic.Bell, "No tienes pedidos de ayuda.")
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
            icon = Ic.CheckCircle,
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
            CircleIcon(Ic.Headset, Palette.Teal, Palette.TealSoft)
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
