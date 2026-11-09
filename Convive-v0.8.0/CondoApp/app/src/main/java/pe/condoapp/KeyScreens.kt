package pe.condoapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ------------------------------------------------------------------
// PROPIETARIO · Generar llave: código de 6 dígitos para que el inquilino se vincule a una unidad.
// ------------------------------------------------------------------

private val KEY_SERVICES = listOf("Luz", "Agua", "Gas", "Internet", "Cable", "Mantenimiento", "Estacionamiento")

/** Chips que se pueden marcar varios a la vez (servicios incluidos). */
@Composable
private fun MultiChips(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o in selected
            Row(
                Modifier.clip(CircleShape).background(if (on) Palette.TealSoft else Palette.Surface)
                    .border(1.dp, if (on) Palette.Teal else Palette.Line, CircleShape)
                    .clickable { onToggle(o) }.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(serviceIconOf(o), contentDescription = null, tint = if (on) Palette.Teal else Palette.Muted, modifier = Modifier.size(16.dp))
                Text(o, color = if (on) Palette.Teal else Palette.Ink, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun KeyGenerator(c: Client, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val specs = Store.unitSpecs.filter { it.clientId == c.id }
    val taken = Store.tenantsOf(c.id).mapNotNull { it.unit }.toSet()
    // Primero las unidades libres.
    val unitNames = (specs.map { it.unit } + Store.unitsOf(c.id).map { it.unit }).distinct()
        .sortedBy { if (it in taken) 1 else 0 }
    var unit by remember { mutableStateOf(unitNames.firstOrNull { it !in taken } ?: unitNames.firstOrNull() ?: "") }
    val spec = specs.firstOrNull { it.unit == unit }
    var rent by remember(unit) { mutableStateOf(spec?.rent?.takeIf { it > 0 }?.let { "%.0f".format(Locale.US, it) } ?: "") }
    var services by remember(unit) { mutableStateOf(spec?.services?.toSet() ?: emptySet()) }
    var payDay by remember { mutableStateOf("5") }
    var startNextMonth by remember { mutableStateOf(false) }
    var months by remember { mutableStateOf("1 año") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<String, String?>?>(null) }

    val start = if (startNextMonth) LocalDate.now().plusMonths(1).withDayOfMonth(1) else LocalDate.now()
    val end = start.plusMonths(when (months) { "6 meses" -> 6L; "2 años" -> 24L; else -> 12L }).minusDays(1)

    result?.let { (code, expires) ->
        KeyResult(c, unit, code, expires, onNew = { result = null }, onClose = onClose)
        return
    }

    AppCard(padding = 18.dp) {
        Text("Generar llave", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tu inquilino la ingresa al crear su cuenta y queda vinculado a la unidad. Dura 48 horas y sirve una sola vez.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Muted
        )
        if (unitNames.isEmpty()) {
            Text("Aún no tienes unidades registradas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Unidad", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(unitNames.map { if (it in taken) "$it · ocupada" else it }, if (unit in taken) "$unit · ocupada" else unit) {
                unit = it.substringBefore(" · ")
            }
            AppTextField(rent, { rent = it.filter(Char::isDigit).take(6) }, "Renta mensual (S/)", keyboardType = KeyboardType.Number, icon = Ic.Cash)
            Text("Día de pago", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(listOf("1", "5", "10", "15", "20", "25", "28"), payDay) { payDay = it }
            Text("Comienza", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(listOf("Hoy", "1 del próximo mes"), if (startNextMonth) "1 del próximo mes" else "Hoy") { startNextMonth = it != "Hoy" }
            Text("Duración del contrato", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(listOf("6 meses", "1 año", "2 años"), months) { months = it }
            Text("Servicios incluidos", style = MaterialTheme.typography.labelLarge)
            MultiChips((KEY_SERVICES + services).distinct(), services) { s -> services = if (s in services) services - s else services + s }
            val f = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE"))
            Text(
                "Del ${start.format(f).replace(".", "")} al ${end.format(f).replace(".", "")}",
                style = MaterialTheme.typography.bodySmall, color = Palette.Muted
            )
        }
        error?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancelar", onClose, Modifier.weight(1f))
            PrimaryButton(
                if (busy) "Generando…" else "Generar llave",
                {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            result = Store.createKey(unit, rent.toDouble(), payDay.toInt(), start, end, services.toList())
                        } catch (e: Exception) {
                            error = Cloud.message(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                Modifier.weight(1f),
                enabled = !busy && unit.isNotEmpty() && (rent.toDoubleOrNull() ?: 0.0) > 0
            )
        }
    }
}

@Composable
private fun KeyResult(c: Client, unit: String, code: String, expiresAt: String?, onNew: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val expires = expiresAt?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() }
    val expiresText = expires?.format(DateTimeFormatter.ofPattern("d 'de' MMMM, h:mm a", Locale("es", "PE"))) ?: "en 48 horas"
    val message = "Hola, esta es tu llave de Convive para ${c.name} - $unit: $code\n" +
        "1) Descarga Convive y crea tu cuenta.\n2) Elige «Soy inquilino» e ingresa la llave.\nVence el $expiresText."
    AppCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Ic.Lock, Palette.Teal, Palette.TealSoft)
            Column(Modifier.weight(1f)) {
                Text("Llave para $unit", style = MaterialTheme.typography.titleMedium)
                Text("Vence el $expiresText", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            code.forEach { d ->
                Box(
                    Modifier.size(44.dp, 54.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Field).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("$d", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Palette.Navy) }
            }
        }
        Text(
            "Compártela solo con tu inquilino. Si generas otra para esta unidad, la anterior deja de funcionar.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Muted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(if (copied) "Copiada" else "Copiar", {
                clipboard.setText(AnnotatedString(code))
                copied = true
            }, Modifier.weight(1f), icon = if (copied) Ic.Check else Ic.Doc)
            PrimaryButton("Compartir", {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
                context.startActivity(Intent.createChooser(send, "Enviar llave"))
            }, Modifier.weight(1f), icon = Ic.Send)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onNew) { Text("Generar otra", color = Palette.Teal, style = MaterialTheme.typography.labelLarge) }
            TextButton(onClick = onClose) { Text("Listo", color = Palette.Teal, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

// ------------------------------------------------------------------
// PROPIETARIO · Suscripción con Google Play (Mi perfil)
// ------------------------------------------------------------------

@Composable
fun PlaySubscriptionCard(c: Client, user: AppUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sub = Cloud.subscription
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val f = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale("es", "PE"))
    fun day(s: String?) = s?.let { runCatching { LocalDate.parse(it.take(10)).format(f) }.getOrNull() }
    val annual = sub?.planId == "anual"
    val price = if (annual) "S/ 319.90 al año" else "S/ 29.90 al mes"
    val viaPlay = sub?.store == "google_play"
    val (title, body, pillFg, pillBg, pill) = when (sub?.status) {
        "activa" -> Quint("Plan ${if (annual) "anual" else "mensual"}", "Se renueva el ${day(sub.periodEnd) ?: "próximo periodo"} con tu cuenta de Google Play.", Palette.Green, Palette.GreenSoft, "Activa")
        "pago_pendiente" -> Quint("Pago pendiente", "Google Play no pudo cobrar. Actualiza tu medio de pago para no perder el acceso.", Palette.AmberDark, Palette.AmberSoft, "Pendiente")
        "cancelacion_programada" -> Quint("Suscripción cancelada", "Tienes acceso hasta el ${day(sub.periodEnd) ?: "fin del periodo"}.", Palette.AmberDark, Palette.AmberSoft, "Termina pronto")
        "vencida", "cancelada", "rechazada" -> Quint("Suscripción terminada", "Vuelve a activarla para seguir usando Convive.", Palette.Red, Palette.RedSoft, "Vencida")
        else -> Quint(
            "Prueba gratis",
            "Termina el ${day(sub?.trialEndsAt) ?: "en 2 meses"}. Luego $price" + if (viaPlay) " con Google Play." else ".",
            Palette.Teal, Palette.TealSoft, "Prueba"
        )
    }
    AppCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Ic.Gift, Palette.Teal, Palette.TealSoft)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            Pill(pill, pillFg, pillBg)
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.Teal) }
        if (viaPlay) {
            SecondaryButton("Administrar en Google Play", {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PlayBilling.manageUrl(context.packageName))))
            }, Modifier.fillMaxWidth(), icon = Ic.Card)
        } else {
            val activity = context as? android.app.Activity
            PrimaryButton(if (busy) "Abriendo Google Play…" else "Activar con Google Play", {
                if (activity == null) return@PrimaryButton
                busy = true
                note = null
                scope.launch {
                    val plan = if (annual) "anual" else "mensual"
                    val r = PlayBilling.subscribe(activity, plan, user.id) { token -> Cloud.verifyPlay(token, PlayBilling.PRODUCT, plan) }
                    note = when (r) {
                        is PlayBilling.Result.Subscribed -> { runCatching { Store.syncAccount() }; "¡Listo! Tu suscripción quedó activa con Google Play." }
                        PlayBilling.Result.Cancelled -> "No se completó el pago con Google."
                        is PlayBilling.Result.Unavailable -> "Google Play se activará cuando Convive esté en Play Store."
                        is PlayBilling.Result.Failed -> "Google Play no pudo completar el pago. Inténtalo de nuevo."
                    }
                    busy = false
                }
            }, Modifier.fillMaxWidth(), enabled = !busy, icon = Ic.Card)
        }
        Text("Se cobra con tu cuenta de Google Play. Cancela cuando quieras.", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
    }
}

private data class Quint(val a: String, val b: String, val c: Color, val d: Color, val e: String)
