package pe.condoapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

// ------------------------------------------------------------------
// Registro: crear cuenta, rol, propietario (3 pasos + plan) e inquilino (llave).
// Figma «02 · Crear usuarios Propietario» y «02 · Crear usuarios Inquilino».
// ------------------------------------------------------------------

/** Lo que el propietario va llenando en el asistente (se guarda al empezar la prueba). */
internal class OwnerSetup {
    var kind by mutableStateOf("Casa")
    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var reference by mutableStateOf("")
    /** Unidades por piso. Un piso puede tener 0 (por ejemplo, el dueño vive en el 1° y alquila arriba). */
    val floors = mutableStateListOf(2, 2, 2)
    /** Precio de cada unidad (solo dígitos) cuando no es el mismo para todas. */
    val price = mutableStateMapOf<String, String>()
    /** Estado elegido a mano con el interruptor; si no se tocó, se deduce del precio. */
    val available = mutableStateMapOf<String, Boolean>()
    var samePrice by mutableStateOf(false)
    /** Precio único (solo dígitos) cuando «Mismo precio para todos» está activo. */
    var common by mutableStateOf("")
    val services = mutableStateListOf<String>()
    val custom = mutableStateListOf<String>()
    val serviceUnits = mutableStateMapOf<String, Set<String>>()
    var annual by mutableStateOf(false)
    /** Propiedad ya guardada en la base de datos (para no duplicarla si reintenta el pago). */
    var cloudClientId: String? = null

    /** Solo las unidades que el propietario indicó (los pisos en 0 no aparecen). */
    val units: List<String>
        get() = floors.flatMapIndexed { f, n -> (0 until n).map { "${f + 1}° Piso ${'A' + it}" } }

    fun priceOf(u: String): String = if (samePrice) common else price[u] ?: ""
    fun rent(u: String): Double = priceOf(u).toDoubleOrNull() ?: 0.0

    /** Disponible para alquilar = con precio, salvo que lo haya apagado (ocupada). */
    fun isAvailable(u: String): Boolean = available[u] ?: priceOf(u).isNotEmpty()

    /** Siguiente: mismo precio escrito, o al menos un precio y ninguna disponible sin precio. */
    fun pricesReady(): Boolean {
        val us = units
        if (us.isEmpty()) return false
        return if (samePrice) (common.toDoubleOrNull() ?: 0.0) > 0
        else us.any { rent(it) > 0 } && us.none { isAvailable(it) && rent(it) <= 0 }
    }

    fun specs(): List<UnitSpec> = units.map { u ->
        UnitSpec("", u, rent(u), isAvailable(u), services.filter { serviceUnits[it]?.contains(u) == true })
    }
}

private val BASE_SERVICES = listOf("Luz", "Agua", "Gas", "Internet", "Cable", "Mantenimiento", "Estacionamiento")

/** Direcciones de ejemplo para las sugerencias (hasta conectar Google Places). */
private val SAMPLE_ADDRESSES = listOf(
    "Av. Los álamos 245" to "San Martín de Porres, Lima",
    "Av. Los álamos 240" to "Comas, Lima",
    "Av. Arequipa 1250" to "Lince, Lima",
    "Jr. Huáscar 312" to "Jesús María, Lima",
    "Calle Las Begonias 415" to "San Isidro, Lima",
    "Av. Universitaria 1801" to "Los Olivos, Lima"
)

private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

// ======================================================================
// Crear cuenta
// ======================================================================

private fun strength(pw: String): Int {
    var s = 0
    if (pw.length >= 8) s++
    if (pw.any { it.isDigit() }) s++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) s++
    if (pw.any { !it.isLetterOrDigit() }) s++
    return s
}

@Composable
internal fun SignUpStep(ui: AuthUi, nav: AuthNav, version: String, onCreated: (SignUpDraft) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var terms by rememberSaveable { mutableStateOf(false) }
    var emailErr by rememberSaveable { mutableStateOf<String?>(null) }
    var passErr by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneErr by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val filled = name.isNotBlank() && email.isNotBlank() && pass.isNotBlank() && phone.isNotBlank() && terms
    val submit: () -> Unit = submit@{
        if (!filled) return@submit
        val e = email.trim().lowercase()
        emailErr = if (!EMAIL_RX.matches(e)) "Revisa tu correo electrónico" else null
        passErr = if (pass.length < 8 || pass.none { it.isDigit() }) "Usa 8 caracteres o más y un número" else null
        phoneErr = if (phone.length != 9) "El celular debe tener 9 números" else null
        if (emailErr != null || passErr != null || phoneErr != null) {
            ui.error("Por favor, verificar tus datos")
            return@submit
        }
        focus.clearFocus()
        ui.busy {
            val draft = SignUpDraft(name.trim(), e, pass, phone)
            // Volvió desde «¿Cómo usarás Convive?»: la cuenta ya existe, sigue con el rol.
            if (Cloud.session?.email.equals(e, ignoreCase = true)) { onCreated(draft); return@busy }
            val taken = Store.checkNewAccount(e, phone) ?: try {
                Cloud.signupCheck(e, phone)
            } catch (x: Cloud.Failure) {
                null // si la consulta falla, Supabase igual rechaza correos repetidos al crear la cuenta
            }
            if (taken != null) {
                if (taken.contains("correo")) emailErr = taken else phoneErr = taken
                ui.error(taken)
                return@busy
            }
            try {
                if (Cloud.signUp(e, pass, name.trim(), phone)) onCreated(draft)
                else ui.info("Te enviamos un correo para confirmar tu cuenta. Confírmalo y luego inicia sesión.")
            } catch (x: Cloud.Failure) {
                when (x.code) {
                    "user_already_exists", "email_exists" -> { emailErr = "Este correo ya tiene una cuenta."; ui.error("Este correo ya tiene una cuenta.") }
                    "weak_password" -> { passErr = "Usa una contraseña más segura"; ui.error("Usa una contraseña más segura") }
                    "email_address_invalid" -> { emailErr = "Revisa tu correo electrónico"; ui.error("Revisa tu correo electrónico") }
                    else -> ui.error(Cloud.message(x))
                }
            }
        }
    }
    val score = strength(pass)
    SheetPage(version, onBack = { nav.back() }) {
        Text("Crear cuenta", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Para continuar necesitamos una breve información", style = TBody)
        Spacer(Modifier.height(20.dp))
        AuthField("Nombre completo", name, { name = it }, "ej. Jhosep Quispe", capitalization = KeyboardCapitalization.Words,
            onIme = { focus.moveFocus(FocusDirection.Down) })
        Spacer(Modifier.height(14.dp))
        AuthField("Correo electrónico", email, { email = it.trim(); emailErr = null }, "ej. jhosep.quispe@correo.com",
            error = emailErr, keyboardType = KeyboardType.Email, onIme = { focus.moveFocus(FocusDirection.Down) })
        Spacer(Modifier.height(14.dp))
        AuthField("Contraseña", pass, { pass = it; passErr = null }, "••••••••", password = true, error = passErr,
            onIme = { focus.moveFocus(FocusDirection.Down) })
        Spacer(Modifier.height(8.dp))
        // Fuerza de la contraseña (4 tramos)
        val (label, color) = when (score) {
            0, 1 -> "Débil" to Palette.Red
            2 -> "Regular" to Palette.AmberDark
            3 -> "Buena" to Palette.Teal
            else -> "Fuerte" to Palette.Teal
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (pass.isNotEmpty() && i < score) color else AK.Border))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (pass.isEmpty()) Palette.Teal else color, fontWeight = FontWeight.SemiBold)) {
                    append(if (pass.isEmpty()) "Buena" else label)
                }
                append(" - 8 caracteres o más y un número")
            },
            style = TSmall.copy(color = AK.Label)
        )
        Spacer(Modifier.height(14.dp))
        AuthField("Celular", phone, { phone = it.filter(Char::isDigit).take(9); phoneErr = null }, "ej. 987 654 321",
            error = phoneErr, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, onIme = submit)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { terms = !terms }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckSquare(terms, size = 24.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                buildAnnotatedString {
                    append("Acepto los ")
                    withStyle(SpanStyle(color = Palette.Teal, fontWeight = FontWeight.SemiBold)) { append("Términos de uso") }
                    append(" y la ")
                    withStyle(SpanStyle(color = Palette.Teal, fontWeight = FontWeight.SemiBold)) { append("Política de Privacidad") }
                },
                style = TField
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Crear cuenta", filled, submit)
        Spacer(Modifier.height(16.dp))
        LinkLine("¿Ya tienes una cuenta?", "Iniciar sesión") { nav.resetTo(AuthStep.Welcome, AuthStep.Login) }
    }
}

// ======================================================================
// Rol
// ======================================================================

@Composable
internal fun RolStep(version: String, onBack: () -> Unit, onContinue: (owner: Boolean) -> Unit) {
    var owner by rememberSaveable { mutableStateOf(true) }
    SheetPage(version, onBack = onBack) {
        Text("¿Cómo usarás Convive?", style = TTitle)
        Spacer(Modifier.height(6.dp))
        Text("Elige tu rol. Podrás sumar el otro más adelante.", style = TBody)
        Spacer(Modifier.height(20.dp))
        ChoiceTile("Soy propietario", "Administro una casa, edificio o condominio", owner, { owner = true }, icon = AIc.House, iconBox = 40.dp,
            titleStyle = TName.copy(fontSize = 15.sp))
        Spacer(Modifier.height(12.dp))
        ChoiceTile("Soy inquilino", "Vivo en una propiedad que usa Convive", !owner, { owner = false }, icon = AIc.Users, iconBox = 40.dp,
            titleStyle = TName.copy(fontSize = 15.sp))
        Spacer(Modifier.height(20.dp))
        AuthButton("Continuar", true, { onContinue(owner) })
    }
}

// ======================================================================
// Propietario
// ======================================================================

@Composable
internal fun OwnerIntroStep(onBack: () -> Unit, onStart: () -> Unit) {
    WhitePage(onBack = onBack, footer = { Box(Modifier.padding(24.dp)) { AuthButton("Empezar", true, onStart) } }) {
        Spacer(Modifier.height(24.dp))
        Text("Configura tu propiedad en 3 pasos", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Toma unos 3 minutos. Puedes guardar y seguir después", style = TBody)
        Spacer(Modifier.height(12.dp))
        IntroRow(1, "Tu propiedad", "Tipo, nombre y dirección", AIc.House)
        IntroRow(2, "Unidad y servicios", "Pisos, precios y qué incluye cada alquiler", AIc.Drop)
        IntroRow(3, "Tu plan", "2 meses gratis. No pagas nada hoy.", AIc.Calendar, last = true)
    }
}

@Composable
private fun IntroRow(n: Int, t: String, d: String, ic: ImageVector, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$n", style = TTitle.copy(fontSize = 22.sp, color = Palette.Teal), modifier = Modifier.width(26.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t, style = TName.copy(fontSize = 15.sp, color = Palette.Teal))
            Spacer(Modifier.height(2.dp))
            Text(d, style = TSmall)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AK.Tint), contentAlignment = Alignment.Center) {
            Icon(ic, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(22.dp))
        }
    }
    if (!last) HorizontalDivider(thickness = 1.dp, color = AK.Border)
}

@Composable
internal fun OwnerTypeStep(o: OwnerSetup, nav: AuthNav) {
    WizardPage(1, 0.5f, "¿Qué tipo de propiedad administras?", onBack = { nav.back() }, onNext = { nav.go(AuthStep.OwnerPlace) }) {
        ChoiceTile("Casa", "Alquilas departamentos o cuartos dentro de una casa", o.kind == "Casa", { o.kind = "Casa" }, icon = AIc.House)
        Spacer(Modifier.height(12.dp))
        ChoiceTile("Edificio", "Varios pisos con departamentos en alquiler", o.kind == "Edificio", { o.kind = "Edificio" }, icon = AIc.Building)
        Spacer(Modifier.height(12.dp))
        ChoiceTile("Condominio", "Los residentes son dueños y pagan una cuota de mantenimiento", o.kind == "Condominio", { o.kind = "Condominio" }, icon = AIc.Condo)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().clip(CtlShape).background(Palette.AmberSoft).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AIc.Help, contentDescription = null, tint = Palette.AmberDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("¿Tienes más de una? Agrega la primera ahora y las demás desde mi perfil.", style = TBody.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Palette.AmberDark))
        }
    }
}

@Composable
internal fun OwnerPlaceStep(o: OwnerSetup, nav: AuthNav) {
    val focus = LocalFocusManager.current
    var addrFocused by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(false) }
    val q = norm(o.address.trim())
    val suggestions = if (addrFocused && !picked && q.length >= 3) SAMPLE_ADDRESSES.filter { norm(it.first).startsWith(q) || norm(it.first).contains(q) }.take(3) else emptyList()
    WizardPage(
        1, 1f, "¿Cómo se llama y dónde está?", onBack = { nav.back() },
        onNext = { focus.clearFocus(); nav.go(AuthStep.OwnerUnits) },
        nextEnabled = o.name.isNotBlank() && o.address.isNotBlank()
    ) {
        AuthField("Nombre de la propiedad", o.name, { o.name = it }, "ej. Casa cumpa", capitalization = KeyboardCapitalization.Words,
            onIme = { focus.moveFocus(FocusDirection.Down) })
        Spacer(Modifier.height(14.dp))
        AuthField("Dirección", o.address, { o.address = it; picked = false }, "ej. Av. Los álamos 245", capitalization = KeyboardCapitalization.Sentences,
            onFocus = { addrFocused = it }, onIme = { focus.moveFocus(FocusDirection.Down) })
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth().shadow(10.dp, CtlShape).clip(CtlShape).background(Color.White).border(1.dp, AK.Border, CtlShape)) {
                suggestions.forEachIndexed { i, (street, district) ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = AK.Border)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            o.address = "$street, ${district.substringBefore(",")}"
                            picked = true
                            focus.moveFocus(FocusDirection.Down)
                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AIc.Pin, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(boldPrefix(street, o.address.trim().length), style = TField)
                            Text(district, style = TSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        AuthField("Referencia", o.reference, { o.reference = it }, "ej. Frente al parque", required = false, imeAction = ImeAction.Done,
            onIme = { focus.clearFocus() })
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AIc.Lock, contentDescription = null, tint = AK.Body, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Solo tus inquilinos vinculados verán la dirección", style = TSmall)
        }
    }
}

private fun boldPrefix(s: String, n: Int): AnnotatedString = buildAnnotatedString {
    val k = n.coerceIn(0, s.length)
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Title)) { append(s.take(k)) }
    append(s.drop(k))
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, if (enabled) AK.Border else AK.Border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = if (enabled) AK.Title else AK.Disabled, modifier = Modifier.size(20.dp)) }
}

@Composable
internal fun OwnerUnitsStep(o: OwnerSetup, nav: AuthNav) {
    val haptic = LocalHapticFeedback.current
    val total = o.units.size
    WizardPage(
        2, 0.33f, "¿Cuántas unidades alquilas?", "Agrúpalas por piso; luego puedes renombrarlas.",
        onBack = { nav.back() }, onNext = { nav.go(AuthStep.OwnerPrices) }, nextEnabled = total > 0
    ) {
        o.floors.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}° Piso", style = TName.copy(fontSize = 15.sp, color = if (n == 0) AK.Body else Palette.Teal))
                    Text(if (n == 0) "No alquilas este piso" else "[$n] ${if (n == 1) "unidad" else "unidades"}", style = TSmall)
                }
                // Se puede bajar hasta 0; en 0, el último piso agregado se quita.
                val canRemove = n == 0 && i == o.floors.lastIndex && o.floors.size > 1
                StepperButton(AIc.Minus, n > 0 || canRemove) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (n > 0) o.floors[i] = n - 1 else o.floors.removeAt(i)
                }
                Text("$n", style = TName.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
                StepperButton(AIc.Plus, n < 12) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    o.floors[i] = n + 1
                }
            }
            HorizontalDivider(thickness = 1.dp, color = AK.Border)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "+ Agregar piso", style = TLink,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = o.floors.size < 30) { o.floors.add(1) }.padding(vertical = 8.dp, horizontal = 2.dp)
        )
        Spacer(Modifier.height(20.dp))
        if (total == 0) {
            Text("Agrega al menos una unidad para continuar.", style = TBody)
        } else {
            Text("Así se verán - [$total] ${if (total == 1) "unidad" else "unidades"}", style = TBody)
            Spacer(Modifier.height(10.dp))
            UnitGrid(o.units, selected = emptySet(), static = true) { }
        }
    }
}

/** Fila de chips de unidades en 3 columnas (vista previa o selección por servicio). */
@Composable
private fun UnitGrid(units: List<String>, selected: Set<String>, static: Boolean = false, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        units.chunked(if (static) 4 else 3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { u ->
                    val on = u in selected
                    val shape = RoundedCornerShape(if (static) 6.dp else 50.dp)
                    Box(
                        Modifier
                            .then(if (static) Modifier else Modifier.weight(1f))
                            .height(if (static) 26.dp else 34.dp)
                            .clip(shape)
                            .background(if (static) AK.Soft else if (on) AK.Title else Color.White)
                            .then(if (static || on) Modifier else Modifier.border(1.dp, AK.Border, shape))
                            .clickable(enabled = !static) { onToggle(u) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(u, style = TSmall.copy(color = if (on) Color.White else AK.Title))
                    }
                }
                if (!static) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** «1250» → «1,250». */
private object Thousands : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val n = raw.length
        val out = StringBuilder()
        val o2t = IntArray(n + 1)
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) out.append(',')
            o2t[i] = out.length
            out.append(raw[i])
        }
        o2t[n] = out.length
        val t2o = IntArray(out.length + 1)
        var oi = 0
        for (t in 0..out.length) {
            while (oi < n && o2t[oi] < t) oi++
            t2o[t] = oi
        }
        return TransformedText(AnnotatedString(out.toString()), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = o2t[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int) = t2o[offset.coerceIn(0, out.length)]
        })
    }
}

@Composable
private fun Switch2(on: Boolean, onToggle: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val track by animateColorAsState(if (on) Palette.Teal else Color(0xFFE5E8EE), label = "track")
    val bias by animateFloatAsState(if (on) 1f else -1f, tween(180), label = "knob")
    Box(
        Modifier.size(52.dp, 28.dp).clip(RoundedCornerShape(50)).background(track)
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggle() }
            .padding(3.dp),
        contentAlignment = androidx.compose.ui.BiasAlignment(bias, 0f)
    ) { Box(Modifier.size(22.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White)) }
}

/** Campo de precio «S/ 1,250»: solo números, borde verde al escribir, rojo si falta el precio. */
@Composable
private fun PriceField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onIme: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        when {
            focused -> Palette.Teal
            error -> Palette.Red
            else -> AK.Border
        }, label = "price-border"
    )
    BasicTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).trimStart('0').take(6)) },
        enabled = enabled,
        singleLine = true,
        textStyle = TField.copy(color = if (enabled) AK.Text else AK.Placeholder),
        cursorBrush = SolidColor(Palette.Teal),
        visualTransformation = Thousands,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onIme() }),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .then(
                        if (focused) Modifier.shadow(8.dp, CtlShape, ambientColor = Palette.Teal.copy(alpha = 0.25f), spotColor = Palette.Teal.copy(alpha = 0.3f))
                        else Modifier
                    )
                    .background(Color.White, CtlShape)
                    .border(if (focused) 2.dp else 1.dp, border, CtlShape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("S/", style = TField.copy(color = if (value.isEmpty() || !enabled) AK.Placeholder else AK.Text))
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1f)) { inner() }
            }
        }
    )
}

@Composable
internal fun OwnerPricesStep(o: OwnerSetup, nav: AuthNav) {
    val focus = LocalFocusManager.current
    val commonFocus = remember { FocusRequester() }
    val units = o.units
    WizardPage(
        2, 0.66f, "Precio y estado de cada uno", "Las ocupadas se completan cuando vincules al inquilino.",
        onBack = { nav.back() },
        onNext = { focus.clearFocus(); nav.go(AuthStep.OwnerServices) },
        nextEnabled = o.pricesReady()
    ) {
        // Mismo precio para todos: aparece un solo campo y el precio se copia a cada unidad.
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AK.Soft).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mismo precio para todos", style = TField, modifier = Modifier.weight(1f))
                Switch2(o.samePrice) {
                    if (!o.samePrice) {
                        if (o.common.isEmpty()) o.common = units.firstNotNullOfOrNull { o.price[it]?.takeIf(String::isNotEmpty) } ?: ""
                        o.samePrice = true
                    } else {
                        // Al volver a individual, cada unidad parte del precio común para ajustarlo.
                        if (o.common.isNotEmpty()) units.forEach { if (o.price[it].isNullOrEmpty()) o.price[it] = o.common }
                        o.samePrice = false
                    }
                }
            }
            AnimatedVisibility(o.samePrice, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    PriceField(
                        o.common, { o.common = it }, Modifier.fillMaxWidth(),
                        focusRequester = commonFocus, imeAction = ImeAction.Done, onIme = { focus.clearFocus() }
                    )
                    LaunchedEffect(Unit) { delay(120); runCatching { commonFocus.requestFocus() } }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = AK.Border)
        units.forEachIndexed { i, u ->
            key(u) {
                Spacer(Modifier.height(12.dp))
                Text(u, style = TLabel)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceField(
                        o.priceOf(u), { o.price[u] = it }, Modifier.weight(1f),
                        enabled = !o.samePrice,
                        error = !o.samePrice && o.available[u] == true && o.rent(u) <= 0,
                        imeAction = if (i == units.lastIndex) ImeAction.Done else ImeAction.Next,
                        onIme = { if (i == units.lastIndex) focus.clearFocus() else focus.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch2(o.isAvailable(u)) { o.available[u] = !o.isAvailable(u) }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = AK.Border)
            }
        }
    }
}

@Composable
private fun ServiceTile(name: String, icon: ImageVector, on: Boolean, dashed: Boolean = false, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(80.dp)
            .clip(CtlShape)
            .background(if (on) AK.Tint else Color.White)
            .then(
                when {
                    dashed -> Modifier.drawBehind {
                        drawRoundRect(
                            color = AK.Disabled, cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                        )
                    }
                    on -> Modifier.border(1.5.dp, Palette.Teal, CtlShape)
                    else -> Modifier.border(1.dp, AK.Border, CtlShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, contentDescription = null, tint = if (on) Palette.Teal else if (dashed) AK.Body else AK.Title, modifier = Modifier.size(26.dp))
        Text(name, style = TField.copy(fontWeight = FontWeight.Medium, color = if (dashed) AK.Body else AK.Title))
    }
}

@Composable
internal fun OwnerServicesStep(o: OwnerSetup, nav: AuthNav) {
    var askCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    val all = BASE_SERVICES + o.custom
    WizardPage(
        2, 1f, "¿Qué servicios ofrece?", "Marca los que incluyes y cómo se cobra cada uno.",
        onBack = { nav.back() }, onNext = { nav.go(AuthStep.OwnerReview) }, nextEnabled = o.services.isNotEmpty()
    ) {
        val tiles = all.map { it to false } + listOf("Otro servicio" to true)
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (s, dashed) ->
                    val on = s in o.services
                    ServiceTile(s, serviceIconOf(s), on, dashed, Modifier.weight(1f)) {
                        if (dashed) askCustom = true
                        else if (on) { o.services.remove(s); o.serviceUnits.remove(s) }
                        else { o.services.add(s); o.serviceUnits[s] = o.units.toSet() }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        if (o.services.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("¿Qué servicios tiene cada uno?", style = TName.copy(fontSize = 15.sp))
            o.services.forEachIndexed { i, s ->
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(serviceIconOf(s), contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s, style = TField.copy(fontWeight = FontWeight.Medium, color = Palette.Teal))
                }
                Spacer(Modifier.height(10.dp))
                val sel = o.serviceUnits[s] ?: emptySet()
                UnitGrid(o.units, sel) { u -> o.serviceUnits[s] = if (u in sel) sel - u else sel + u }
                if (i < o.services.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.dp, color = AK.Border)
                }
            }
        }
    }
    if (askCustom) {
        AlertDialog(
            onDismissRequest = { askCustom = false },
            title = { Text("Otro servicio", style = TName) },
            text = { AuthField("", customName, { customName = it.take(24) }, "ej. Lavandería", capitalization = KeyboardCapitalization.Sentences) },
            confirmButton = {
                TextButton(onClick = {
                    val n = customName.trim().replaceFirstChar { it.uppercase() }
                    if (n.isNotEmpty() && n !in BASE_SERVICES && n !in o.custom) {
                        o.custom.add(n); o.services.add(n); o.serviceUnits[n] = o.units.toSet()
                    }
                    customName = ""
                    askCustom = false
                }) { Text("Agregar", style = TLink) }
            },
            dismissButton = { TextButton(onClick = { askCustom = false }) { Text("Cancelar", style = TLink.copy(color = AK.Body)) } },
            containerColor = Color.White
        )
    }
}

@Composable
private fun PhotoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(8.dp, CardShape, ambientColor = Color.Black.copy(alpha = 0.12f)).clip(CardShape).background(Color.White)) {
        Image(
            painter = painterResource(R.drawable.auth_bg), contentDescription = null, contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.BiasAlignment(0f, -0.2f),
            modifier = Modifier.fillMaxWidth().height(132.dp)
        )
        Column(Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun ReviewRow(label: String, value: String, onEdit: () -> Unit) {
    HorizontalDivider(thickness = 1.dp, color = AK.Border)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = TSmall.copy(color = AK.Label))
            Spacer(Modifier.height(2.dp))
            Text(value, style = TSmall.copy(color = AK.Title, fontSize = 13.sp, lineHeight = 18.sp))
        }
        Text("Editar", style = TField.copy(fontWeight = FontWeight.SemiBold, color = AK.Title),
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onEdit).padding(8.dp))
    }
}

private fun soles(v: Double): String = "%,.0f".format(java.util.Locale.US, v)

@Composable
internal fun OwnerReviewStep(o: OwnerSetup, nav: AuthNav) {
    val units = o.units
    val free = units.count { o.isAvailable(it) }
    val rents = units.map { o.rent(it) }.filter { it > 0 }
    val prices = when {
        rents.isEmpty() -> "Sin precio"
        rents.min() == rents.max() -> "S/ ${soles(rents.min())} al mes"
        else -> "S/ ${soles(rents.min())} a S/ ${soles(rents.max())} al mes"
    }
    val svc = o.services.map { it.lowercase() }.let { l -> if (l.size > 1) l.dropLast(1).joinToString(", ") + " y " + l.last() else l.joinToString() }
        .replaceFirstChar { it.uppercase() }
    WizardPage(3, 0.5f, "Revisa antes de continuar", onBack = { nav.back() }, onNext = { nav.go(AuthStep.OwnerPlan) }, nextLabel = "Elegir plan") {
        PhotoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.name.trim(), style = TName.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                Text(o.kind, style = TSmall.copy(color = Palette.Teal, fontWeight = FontWeight.Medium),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.TealSoft).padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Text(o.address.trim(), style = TSmall)
            Spacer(Modifier.height(10.dp))
            ReviewRow("Unidades", "${units.size} ${if (units.size == 1) "departamento" else "departamentos"} - $free ${if (free == 1) "disponible" else "disponibles"}") { nav.backTo(AuthStep.OwnerUnits) }
            ReviewRow("Precios", prices) { nav.backTo(AuthStep.OwnerPrices) }
            ReviewRow("Servicios que ofreces", "$svc.") { nav.backTo(AuthStep.OwnerServices) }
        }
    }
}

@Composable
private fun PlanToggle(annual: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50)).background(AK.Soft).padding(4.dp)) {
        listOf(false to "Mensual", true to "Anual").forEach { (value, label) ->
            val on = annual == value
            Row(
                Modifier.weight(1f).fillMaxHeight()
                    .then(if (on) Modifier.shadow(3.dp, RoundedCornerShape(50)) else Modifier)
                    .clip(RoundedCornerShape(50)).background(if (on) Color.White else Color.Transparent)
                    .clickable { onChange(value) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = TButton.copy(color = AK.Title))
                if (value) {
                    Spacer(Modifier.width(6.dp))
                    Text("-11%", style = TSmall.copy(fontSize = 11.sp, color = AK.PillAmber, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(AK.PillAmberBg).padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(date: String, title: String, desc: String, last: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Palette.Teal), contentAlignment = Alignment.Center) {
                Icon(AIc.Zap, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(Palette.Teal))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = if (last) 0.dp else 18.dp)) {
            Text(date, style = TSmall.copy(color = Palette.Teal, fontWeight = FontWeight.Medium))
            Text(title, style = TField.copy(fontWeight = FontWeight.SemiBold, color = AK.Title))
            Text(desc, style = TSmall)
        }
    }
}

@Composable
internal fun OwnerPlanStep(o: OwnerSetup, onBack: () -> Unit, onStart: () -> Unit) {
    val today = LocalDate.now()
    val price = if (o.annual) "S/319.90 al año" else "S/29.90 al mes"
    WhitePage(
        onBack = onBack,
        footer = {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                AuthButton("Empezar prueba gratis", true, onStart)
                Spacer(Modifier.height(10.dp))
                Text("Se cobra con tu cuenta de Google Play. Cancela cuando quieras.", style = TSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Prueba Convive gratis por 2 meses", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Todas las funciones para ${o.name.trim().ifEmpty { "tu propiedad" }}. No pagas nada hoy.", style = TBody)
        Spacer(Modifier.height(20.dp))
        PlanToggle(o.annual) { o.annual = it }
        Spacer(Modifier.height(20.dp))
        TimelineRow("Hoy - ${shortDate(today)}", "Acceso completo", "Cobros, contratos, medidores, avisos y reporte", false)
        TimelineRow(shortDate(today.plusDays(53)), "Te avisamos", "Un recordatorio una semana antes de que termine la prueba.", false)
        TimelineRow(shortDate(today.plusDays(60)), "Empieza tu plan", "$price. Cancela antes y no se cobra nada.", true)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(CtlShape).background(AK.Tint).border(1.5.dp, Palette.Teal, CtlShape).padding(14.dp)) {
            Text(price, style = TName.copy(fontSize = 18.sp, color = Palette.Teal))
            Spacer(Modifier.height(2.dp))
            Text(
                if (o.annual) "Equivalente a S/ 26.66 al mes - Ahorras S/ 38.90" else "Sin permanencia. Cambia a anual cuando quieras.",
                style = TSmall
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ======================================================================
// Inquilino
// ======================================================================

private val DEMO_KEYS = listOf("482917" to "Válida", "111111" to "Vencida", "333333" to "Ya alquila")

@Composable
internal fun TenantKeyStep(ui: AuthUi, version: String, onBack: () -> Unit, onValid: (Store.InviteCheck.Ok) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var left by rememberSaveable { mutableIntStateOf(5) }
    var showDemo by rememberSaveable { mutableStateOf(false) }
    var shakes by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val canValidate = code.length == 6 && !invalid && left != 0
    val validate: () -> Unit = {
        if (canValidate) ui.busy {
            when (val r = Store.checkKey(code)) {
                is Store.InviteCheck.Ok -> onValid(r)
                is Store.InviteCheck.Fail -> {
                    invalid = true
                    detail = r.detail
                    if (r.attemptsLeft >= 0) left = r.attemptsLeft
                    shakes++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    ui.error(r.toast)
                }
            }
        }
    }
    SheetPage(
        version, onBack = onBack, onVersionTap = { if (!Cloud.signedIn) showDemo = !showDemo }, centered = true,
        below = {
            NumberPad(
                onDigit = { d ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (invalid) { code = ""; invalid = false; detail = null }
                    if (code.length < 6) code += d
                },
                onDelete = { invalid = false; detail = null; code = code.dropLast(1) }
            )
        }
    ) {
        Text("Ingresa tu llave", style = TTitle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Es el código de 6 dígitos que te envió el propietario", style = TBody, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        CodeBoxes(code, invalid, Modifier.shake(shakes))
        if (detail != null) {
            Spacer(Modifier.height(12.dp))
            Text(detail!!, style = TBody.copy(color = Palette.Red), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (left in 0..4) Text(if (left == 1) "Te queda 1 intento" else "Te quedan $left intentos", style = TBody.copy(color = AK.Label), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Validar llave", canValidate, validate)
        if (showDemo) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEMO_KEYS.forEach { (k, label) ->
                    Column(
                        Modifier.clip(CtlShape).background(Palette.Field).border(1.dp, AK.Border, CtlShape)
                            .clickable { code = k; invalid = false; detail = null }.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(label, style = TLabel.copy(color = AK.Text))
                        Text(k, style = TSmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TenantActiveStep(version: String, ok: Store.InviteCheck.Ok, onBack: () -> Unit, onContinue: () -> Unit) {
    var move by rememberSaveable { mutableStateOf(true) }
    val other = ok.inv.demoActiveRental ?: ""
    SheetPage(version, onBack = onBack) {
        Text("Ya tienes un alquiler activo", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append("Estás vinculado a ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Title)) { append(other) }
                append(". ¿Qué quieres hacer?")
            },
            style = TBody
        )
        Spacer(Modifier.height(20.dp))
        ChoiceTile("Me mudo a ${ok.client.name}", "Tu propietario anterior recibe el aviso de tu salida.", move, { move = true }, titleStyle = TName.copy(fontSize = 15.sp))
        Spacer(Modifier.height(12.dp))
        ChoiceTile("Mantener en ${other.substringBefore(" -")}", "La información se mantendrá", !move, { move = false }, titleStyle = TName.copy(fontSize = 15.sp))
        Spacer(Modifier.height(20.dp))
        AuthButton("Continuar", true, onContinue)
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = TSmall.copy(fontSize = 11.sp, color = AK.Label))
        Text(value, style = TField.copy(fontWeight = FontWeight.SemiBold, color = AK.Title, fontSize = 13.sp))
    }
}

@Composable
internal fun TenantConfirmStep(version: String, ok: Store.InviteCheck.Ok, onBack: () -> Unit, onConfirm: () -> Unit) {
    val inv = ok.inv
    val dateF = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("es", "PE"))
    SheetPage(version, onBack = onBack) {
        PhotoCard {
            Text("${ok.client.name} - ${inv.unit}", style = TName.copy(fontSize = 15.sp))
            ok.client.address?.let { Text(it, style = TSmall) }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(ok.owner?.name ?: "Propietario")
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(ok.owner?.name ?: "Tu propietario", style = TName.copy(fontSize = 15.sp))
                    Text("Propietario", style = TSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = AK.Border)
            Spacer(Modifier.height(10.dp))
            Row {
                InfoCell("Pago mensual", "S/ ${soles(inv.rent)} al mes", Modifier.weight(1f))
                InfoCell("Pagas", "${inv.payDay} de cada mes", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row {
                InfoCell("Comienzo", inv.start.format(dateF).replace(".", ""), Modifier.weight(1f))
                InfoCell("Culmina", inv.end.format(dateF).replace(".", ""), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text("Servicios incluidos", style = TSmall.copy(fontSize = 11.sp, color = AK.Label))
            Spacer(Modifier.height(6.dp))
            inv.services.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { s ->
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Palette.TealSoft).padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(serviceIconOf(s), contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s, style = TSmall.copy(fontSize = 11.sp, color = Palette.Teal, fontWeight = FontWeight.Medium))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text("Si algo no coincide, avísale al propietario antes de confirmar", style = TSmall.copy(color = AK.Title))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton("Cancelar", onBack, modifier = Modifier.weight(1f))
            AuthButton("Confirmar", true, onConfirm, modifier = Modifier.weight(1f))
        }
    }
}
