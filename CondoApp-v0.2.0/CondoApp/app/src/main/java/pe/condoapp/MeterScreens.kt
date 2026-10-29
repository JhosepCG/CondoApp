package pe.condoapp

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

// ------------------------------------------------------------------
// INQUILINO · Medidor
// ------------------------------------------------------------------

@Composable
fun TenantMedidor(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val context = LocalContext.current
    val req = Store.activeMeterRequest(c.id)
    val sentForReq = req?.let { Store.readingFor(it.id, user.id) }
    val mine = Store.meterReadings.filter { it.userId == user.id }.reversed()

    var photo by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    var pendingCamera by remember { mutableStateOf<File?>(null) }

    val usePhoto: (String?) -> Unit = { path ->
        if (path != null) {
            Photos.delete(photo)
            photo = path
            error = null
            sent = false
        } else {
            error = "No pudimos leer la foto. Inténtalo de nuevo."
        }
        Unit
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingCamera
        if (ok && f != null) usePhoto(Photos.importImage(context, Uri.fromFile(f)))
        f?.delete()
        pendingCamera = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usePhoto(Photos.importImage(context, uri))
    }
    val openCamera: () -> Unit = {
        try {
            val f = Photos.newCameraFile(context)
            pendingCamera = f
            cameraLauncher.launch(Photos.uriFor(context, f))
        } catch (e: Exception) {
            pendingCamera = null
            error = "No encontramos una app de cámara. Sube la foto desde tu galería."
        }
        Unit
    }
    val openGallery: () -> Unit = {
        try {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            error = "No pudimos abrir la galería."
        }
        Unit
    }

    ScreenColumn {
        when {
            req == null -> AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.Bolt, Palette.Teal, Palette.TealSoft)
                    Column(Modifier.weight(1f)) {
                        Text("Sin pedidos por ahora", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tu administración te avisará cuándo enviar la foto. Igual puedes enviarla cuando quieras.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted
                        )
                    }
                }
            }
            sentForReq != null -> AppCard(color = Palette.GreenSoft, borderColor = Palette.GreenSoft) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.CheckCircle, Palette.Green, Palette.Surface)
                    Column(Modifier.weight(1f)) {
                        Text("Ya enviaste tu foto", style = MaterialTheme.typography.titleMedium)
                        Text("${req.title} · ${sentForReq.sentAt}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                    if (sentForReq.reviewed) Pill("Revisada", Palette.Green, Palette.Surface)
                }
            }
            else -> {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), req.due)
                val late = days < 0
                val fg = if (late) Palette.Red else Palette.AmberDark
                AppCard(
                    color = if (late) Palette.RedSoft else Palette.AmberSoft,
                    borderColor = if (late) Palette.RedSoft else Palette.AmberSoft
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.Bolt, fg, Palette.Surface)
                        Column(Modifier.weight(1f)) {
                            Text(req.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (late) "Venció el ${req.due.format(dateFmt)}. Envíala lo antes posible."
                                else "Envíala hasta el ${req.due.format(dateFmt)} · ${daysLeft(days)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = fg
                            )
                        }
                    }
                }
            }
        }

        AppCard(padding = 18.dp) {
            Text("Foto del medidor de luz", style = MaterialTheme.typography.titleLarge)
            Text(
                "Que se vean bien los números del medidor.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted
            )
            PhotoBox(photo, 220.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Tomar foto", openCamera, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera)
                SecondaryButton("Galería", openGallery, Modifier.weight(1f), icon = Icons.Rounded.PhotoLibrary)
            }
            AppTextField(
                reading,
                { reading = it.filter { ch -> ch.isDigit() || ch == '.' } },
                "Lectura en kWh (opcional)",
                icon = Icons.Rounded.Bolt,
                keyboardType = KeyboardType.Decimal
            )
            error?.let { ErrorText(it) }
            if (sent) Pill("Foto enviada a la administración", Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
            PrimaryButton(
                "Enviar a la administración",
                {
                    val p = photo
                    if (p != null) {
                        Store.addMeterReading(user, req?.id, p, reading)
                        photo = null
                        reading = ""
                        sent = true
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = photo != null
            )
        }

        SectionTitle("Mis envíos", trailing = "${mine.size}")
        if (mine.isEmpty()) EmptyState(Icons.Rounded.Bolt, "Aún no enviaste fotos del medidor.")
        mine.forEach { m -> key(m.id) { ReadingRow(m, showUser = false, onClick = null) } }
    }
}

private fun daysLeft(days: Long): String = when (days) {
    0L -> "vence hoy"
    1L -> "queda 1 día"
    else -> "quedan $days días"
}

// ------------------------------------------------------------------
// ADMINISTRADOR · Medidores
// ------------------------------------------------------------------

@Composable
fun AdminMedidores(user: AppUser) {
    val c = Store.clientOf(user) ?: return
    val req = Store.activeMeterRequest(c.id)
    val tenants = Store.tenantsOf(c.id)
    var showForm by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<MeterReading?>(null) }
    var confirmClose by remember { mutableStateOf(false) }
    var reminded by remember { mutableStateOf(false) }
    val others = Store.meterReadings.filter { it.clientId == c.id && (req == null || it.requestId != req.id) }.reversed()

    ScreenColumn {
        if (req == null || showForm) {
            NewMeterRequestForm(c, canCancel = req != null) { showForm = false }
        } else {
            val sentCount = tenants.count { t -> Store.readingFor(req.id, t.id) != null }
            val fraction = if (tenants.isEmpty()) 0f else sentCount.toFloat() / tenants.size
            HeroCard("Pedido de fotos", req.title, "Fecha límite: ${req.due.format(dateFmt)}", Icons.Rounded.Bolt) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "$sentCount de ${tenants.size} inquilinos enviaron su foto",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    if (reminded) "Recordatorio enviado" else "Recordar",
                    {
                        Store.remindMeterRequest(req)
                        reminded = true
                    },
                    Modifier.weight(1f),
                    icon = Icons.Rounded.Campaign
                )
                SecondaryButton("Cerrar pedido", { confirmClose = true }, Modifier.weight(1f), icon = Icons.Rounded.Check)
            }

            SectionTitle("Inquilinos", trailing = "$sentCount/${tenants.size} enviados")
            if (tenants.isEmpty()) EmptyState(Icons.Rounded.Groups, "Aún no hay inquilinos.")
            tenants.forEach { t ->
                val r = Store.readingFor(req.id, t.id)
                key(t.id) {
                    if (r != null) ReadingRow(r, showUser = true, onClick = { viewing = r })
                    else PendingMeterRow(t, req)
                }
            }
            TextButton(onClick = { showForm = true }, contentPadding = PaddingValues(0.dp)) {
                Text("Hacer un nuevo pedido", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (others.isNotEmpty()) {
            SectionTitle("Otros envíos", trailing = "${others.size}")
            others.forEach { m -> key(m.id) { ReadingRow(m, showUser = true, onClick = { viewing = m }) } }
        }
    }

    viewing?.let { m -> MeterPhotoDialog(m) { viewing = null } }
    if (confirmClose && req != null) {
        ConfirmDialog(
            title = "Cerrar pedido",
            text = "Los inquilinos ya no verán este pedido como pendiente. Las fotos enviadas se conservan.",
            confirm = "Cerrar pedido",
            onConfirm = { Store.closeMeterRequest(req.id) },
            onDismiss = { confirmClose = false },
            danger = false
        )
    }
}

@Composable
private fun NewMeterRequestForm(c: Client, canCancel: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val month = remember { LocalDate.now().month.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "PE")) }
    var title by remember { mutableStateOf("Medidor de luz · $month") }
    var due by remember { mutableStateOf(LocalDate.now().plusDays(5)) }
    val quick = listOf("En 3 días" to 3L, "En 5 días" to 5L, "En 7 días" to 7L)
    val diff = ChronoUnit.DAYS.between(LocalDate.now(), due)
    val selected = quick.firstOrNull { it.second == diff }?.first ?: "Otra fecha"
    val openPicker: () -> Unit = {
        val dialog = DatePickerDialog(
            context,
            { _, y, m, d -> due = LocalDate.of(y, m + 1, d) },
            due.year, due.monthValue - 1, due.dayOfMonth
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    AppCard(padding = 18.dp) {
        Text("Pedir fotos de medidores", style = MaterialTheme.typography.titleLarge)
        Text(
            "Cada inquilino recibirá un aviso urgente para enviar la foto de su medidor de luz hasta la fecha límite.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Muted
        )
        AppTextField(title, { title = it }, "Título del pedido", icon = Icons.Rounded.Bolt)
        Text("Fecha límite", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
        ChoiceChips(quick.map { it.first } + "Otra fecha", selected) { label ->
            val q = quick.firstOrNull { it.first == label }
            if (q != null) {
                due = LocalDate.now().plusDays(q.second)
            } else {
                openPicker()
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.Field)
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Event, contentDescription = null, tint = Palette.Teal)
            Column(Modifier.weight(1f)) {
                Text("Hasta el", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                Text(due.format(dateFmt), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = openPicker) {
                Text("Cambiar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canCancel) SecondaryButton("Cancelar", onDone, Modifier.weight(1f))
            PrimaryButton(
                "Enviar alerta",
                {
                    Store.addMeterRequest(c.id, title, due)
                    onDone()
                },
                Modifier.weight(1f),
                enabled = title.isNotBlank() && !due.isBefore(LocalDate.now()),
                icon = Icons.Rounded.Campaign
            )
        }
    }
}

@Composable
private fun PendingMeterRow(t: AppUser, req: MeterRequest) {
    val late = LocalDate.now().isAfter(req.due)
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(
                if (late) Icons.Rounded.ErrorOutline else Icons.Rounded.Schedule,
                if (late) Palette.Red else Palette.AmberDark,
                if (late) Palette.RedSoft else Palette.AmberSoft,
                56.dp
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "${t.unit ?: "-"} · ${t.name}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (late) "No envió la foto a tiempo" else "Aún no envía la foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            if (late) Pill("Vencido", Palette.Red, Palette.RedSoft) else Pill("Pendiente", Palette.AmberDark, Palette.AmberSoft)
        }
    }
}

// ------------------------------------------------------------------
// Piezas compartidas
// ------------------------------------------------------------------

@Composable
private fun PhotoBox(path: String?, height: Dp) {
    val shape = RoundedCornerShape(16.dp)
    val img = remember(path) { path?.let { Photos.load(it, 1000) } }
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Line, shape),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = "Foto del medidor",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBadge(Icons.Rounded.AddAPhoto, Palette.Muted, Palette.NavySoft, 56.dp)
                Text("Aún no hay foto", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
    }
}

private fun requestTitle(m: MeterReading): String =
    Store.meterRequests.firstOrNull { it.id == m.requestId }?.title ?: "Envío sin pedido"

@Composable
private fun ReadingRow(m: MeterReading, showUser: Boolean, onClick: (() -> Unit)?) {
    val thumb = remember(m.photoPath) { Photos.load(m.photoPath, 240) }
    val detail = buildString {
        append(m.sentAt)
        if (m.reading.isNotBlank()) append(" · ").append(m.reading).append(" kWh")
    }
    AppCard(padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Field),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = "Foto del medidor",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Palette.Muted)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (showUser) "${m.unit} · ${m.userName}" else requestTitle(m),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            if (m.reviewed) Pill("Revisada", Palette.Green, Palette.GreenSoft) else Pill("Enviada", Palette.Teal, Palette.TealSoft)
        }
    }
}

@Composable
private fun MeterPhotoDialog(m: MeterReading, onDismiss: () -> Unit) {
    val img = remember(m.photoPath) { Photos.load(m.photoPath, 1400) }
    val sub = buildString {
        append("Enviada ").append(m.sentAt)
        if (m.reading.isNotBlank()) append(" · Lectura: ").append(m.reading).append(" kWh")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text("${m.unit} · ${m.userName}", style = MaterialTheme.typography.titleLarge)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
        },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Field),
                contentAlignment = Alignment.Center
            ) {
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "Foto del medidor de ${m.unit}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "La foto no está en este teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted
                    )
                }
            }
        },
        confirmButton = {
            if (!m.reviewed) {
                TextButton(onClick = {
                    Store.setReadingReviewed(m.id)
                    onDismiss()
                }) {
                    Text("Marcar revisada", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            if (!m.reviewed) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
