package pe.condoapp

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ArrowCircleLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// Ingreso (todos los roles), según «01 · Ingreso» de Figma (v1.0):
// Splash → Bienvenida → Iniciar sesión / Google / Crear cuenta
// → ¿Olvidaste tu contraseña? → código de 6 dígitos → nueva contraseña.
// La foto queda fija de fondo y cada paso es una hoja blanca que sube desde abajo.
// ------------------------------------------------------------------

private val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold)
)

private object AuthColors {
    val Title = Color(0xFF0B1F3A)
    val Body = Color(0xFF6B7280)
    val Label = Color(0xFF4B5563)
    val Text = Color(0xFF14213D)
    val Placeholder = Color(0xFF9CA3AF)
    val Border = Color(0xFFE5E7EB)
    val Disabled = Color(0xFFD1D5DB)
    val DividerLine = Color(0xFFCBD5E1)
    val DividerText = Color(0xFF64748B)
    val Keypad = Color(0xFFE8EBEF)
}

private val TitleStyle = TextStyle(
    fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp,
    letterSpacing = (-0.3).sp, color = AuthColors.Title
)
private val BodyStyle = TextStyle(fontFamily = Geist, fontSize = 15.sp, lineHeight = 21.sp, color = AuthColors.Body)
private val LabelStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, color = AuthColors.Label)
private val FieldStyle = TextStyle(fontFamily = Geist, fontSize = 16.sp, lineHeight = 22.sp, color = AuthColors.Text)
private val ButtonStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp)
private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
private val ControlShape = RoundedCornerShape(14.dp)
private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

/** Cuenta de Google de prueba (hasta conectar Google Sign-In real). */
private const val GOOGLE_DEMO_NAME = "Pedro Quispe"
private const val GOOGLE_DEMO_EMAIL = "pedro.quispe@gmail.com"

private val DEMO_ACCOUNTS = listOf(
    "jhosep" to "Super Admin",
    "pedro" to "Admin casa",
    "ana" to "Admin condominio",
    "lucia" to "Inquilina",
    "marco" to "Inquilino casa"
)

private fun formatPhone(digits: String): String =
    if (digits.length == 9) "+51 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}" else digits

// ---------------- Pasos y navegación ----------------

private data class SignUpDraft(val name: String, val email: String, val password: String, val phone: String)

private sealed interface AuthStep {
    val key: String

    data object Welcome : AuthStep { override val key: String get() = "welcome" }
    data object Login : AuthStep { override val key: String get() = "login" }
    data object SignUp : AuthStep { override val key: String get() = "signup" }
    data object ForgotChoose : AuthStep { override val key: String get() = "forgot" }
    data class ForgotEnter(val byEmail: Boolean) : AuthStep {
        override val key: String get() = if (byEmail) "forgot-mail" else "forgot-phone"
    }
    /** Código de 6 dígitos: para recuperar la contraseña (resetUserId) o para crear la cuenta (draft). */
    data class Otp(val destination: String, val resetUserId: String? = null, val draft: SignUpDraft? = null) : AuthStep {
        override val key: String get() = "otp-$destination"
    }
    data class NewPassword(val userId: String) : AuthStep { override val key: String get() = "newpass-$userId" }
}

private class AuthNav(private val stack: SnapshotStateList<AuthStep>) {
    var forward by mutableStateOf(true)
    val current: AuthStep get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    fun go(s: AuthStep) { forward = true; stack.add(s) }
    fun back() { if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex) } }
    fun resetTo(vararg s: AuthStep) { forward = false; stack.clear(); stack.addAll(s) }
}

private data class AuthToast(val text: String, val error: Boolean = true, val id: Long = System.nanoTime())

/** Estado compartido: "Espere un momento" y mensajes arriba de la pantalla. */
private class AuthUi(private val scope: CoroutineScope) {
    var loading by mutableStateOf(false)
    var toast by mutableStateOf<AuthToast?>(null)
    fun busy(ms: Long = 900, then: () -> Unit) {
        if (loading) return
        scope.launch {
            toast = null
            loading = true
            delay(ms)
            loading = false
            then()
        }
    }
    fun error(msg: String) { toast = AuthToast(msg, true) }
    fun info(msg: String) { toast = AuthToast(msg, false) }
}

// ---------------- Splash ----------------

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LightStatusIcons()
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy)) {
        AuthBackground()
        Column(
            Modifier.align(Alignment.Center).enter(0, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Convive", color = Color.White, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Vivir juntos, más fácil.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp)
                .size(32.dp),
            color = Color.White,
            strokeWidth = 3.dp,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

// ---------------- Flujo de ingreso ----------------

@Composable
fun AuthFlow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stack = remember { mutableStateListOf<AuthStep>(AuthStep.Welcome) }
    val nav = remember { AuthNav(stack) }
    val ui = remember { AuthUi(scope) }
    val holder = rememberSaveableStateHolder()
    var googleOpen by remember { mutableStateOf(false) }
    val prefillUser = remember { mutableStateOf<String?>(null) }
    val prefillSignUp = remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingCode by remember { mutableStateOf("") }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    LightStatusIcons()

    BackHandler(enabled = googleOpen || (nav.canGoBack && !ui.loading)) {
        if (googleOpen) googleOpen = false else nav.back()
    }

    // Huella: si alguien la activó en este teléfono, aparece sola al abrir.
    val bioAvailable = remember { Store.biometricUserId() != null && Biometric.available(context) }
    LaunchedEffect(bioAvailable) {
        if (bioAvailable) {
            delay(500)
            val act = context as? androidx.fragment.app.FragmentActivity
            val name = Store.users.firstOrNull { it.id == Store.biometricUserId() }?.name?.substringBefore(" ")
            if (act != null) Biometric.prompt(
                act, "Ingresar a Convive",
                if (name != null) "Hola $name, coloca tu huella" else "Coloca tu huella",
                onSuccess = { Store.loginWithBiometric()?.let { ui.error(it) } },
                onError = { ui.error(it) }
            )
        }
    }

    /** Envía el código (por ahora llega como notificación). Devuelve true si el aviso se mostró. */
    fun sendCode(destination: String): Boolean {
        pendingCode = (100000..999999).random().toString()
        val shown = Notifier.notifyCode(context, pendingCode, destination)
        if (!shown) ui.info("Modo prueba: tu código es $pendingCode")
        return shown
    }

    val openGoogle: () -> Unit = { googleOpen = true }

    Box(Modifier.fillMaxSize().background(Palette.Navy)) {
        AuthBackground()

        AnimatedContent(
            targetState = nav.current,
            transitionSpec = {
                if (nav.forward) {
                    (slideInVertically(tween(420, easing = EaseOutCubic)) { it / 4 } + fadeIn(tween(260))) togetherWith
                        fadeOut(tween(160))
                } else {
                    fadeIn(tween(260)) togetherWith
                        (slideOutVertically(tween(320)) { it / 4 } + fadeOut(tween(220)))
                }
            },
            label = "auth"
        ) { step ->
            holder.SaveableStateProvider(step.key) {
                when (step) {
                    AuthStep.Welcome -> WelcomeStep(
                        version,
                        onLogin = { nav.go(AuthStep.Login) },
                        onGoogle = openGoogle,
                        onSignUp = { nav.go(AuthStep.SignUp) }
                    )
                    AuthStep.Login -> LoginStep(ui, nav, version, prefillUser, openGoogle)
                    AuthStep.SignUp -> SignUpStep(ui, nav, version, prefillSignUp, openGoogle) { sendCode(it) }
                    AuthStep.ForgotChoose -> ForgotChooseStep(nav, version)
                    is AuthStep.ForgotEnter -> ForgotEnterStep(ui, nav, version, step.byEmail) { sendCode(it) }
                    is AuthStep.Otp -> OtpStep(
                        ui, nav, version, step,
                        currentCode = { pendingCode },
                        resend = { if (sendCode(step.destination)) ui.info("Te enviamos un nuevo código") },
                        onVerified = {
                            when {
                                step.resetUserId != null -> nav.go(AuthStep.NewPassword(step.resetUserId))
                                step.draft != null -> {
                                    val d = step.draft
                                    Store.signUpOwner(d.name, d.email, d.password, d.phone)?.let { ui.error(it) }
                                }
                            }
                        }
                    )
                    is AuthStep.NewPassword -> NewPasswordStep(ui, version, onBack = { nav.back() }) { pw ->
                        Store.resetPassword(step.userId, pw)
                        prefillUser.value = Store.users.firstOrNull { it.id == step.userId }?.username
                        nav.resetTo(AuthStep.Welcome, AuthStep.Login)
                        ui.info("¡Listo! Ingresa con tu nueva contraseña")
                    }
                }
            }
        }

        ToastHost(ui)

        GoogleAccountSheet(
            visible = googleOpen,
            onDismiss = { googleOpen = false },
            onPick = {
                googleOpen = false
                ui.busy {
                    when (val r = Store.loginWithGoogle(GOOGLE_DEMO_EMAIL)) {
                        null -> Unit
                        Store.NO_ACCOUNT -> {
                            prefillSignUp.value = GOOGLE_DEMO_NAME to GOOGLE_DEMO_EMAIL
                            if (nav.current != AuthStep.SignUp) nav.go(AuthStep.SignUp)
                        }
                        else -> ui.error(r)
                    }
                }
            },
            onOther = {
                googleOpen = false
                if (nav.current != AuthStep.SignUp) nav.go(AuthStep.SignUp)
            }
        )

        LoadingOverlay(ui.loading)
    }
}

// ---------------- Pasos ----------------

@Composable
private fun WelcomeStep(version: String, onLogin: () -> Unit, onGoogle: () -> Unit, onSignUp: () -> Unit) {
    AuthPage(version, onBack = null) {
        Text("Bienvenido a Convive", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu casa en orden: pagos, aviso y ayuda, para quien la administra y para quien la vive.",
            style = BodyStyle.copy(fontSize = 16.sp, lineHeight = 21.sp)
        )
        Spacer(Modifier.height(20.dp))
        AuthButton(
            "Iniciar sesión", enabled = true, onClick = onLogin, icon = Ic.User, height = 54.dp,
            textStyle = TextStyle(fontFamily = Geist, fontSize = 17.sp)
        )
        Spacer(Modifier.height(20.dp))
        OrDivider("O inicia sesión con")
        Spacer(Modifier.height(20.dp))
        GoogleButton(onGoogle)
        Spacer(Modifier.height(28.dp))
        SignUpFooter(onSignUp)
    }
}

@Composable
private fun LoginStep(
    ui: AuthUi,
    nav: AuthNav,
    version: String,
    prefill: MutableState<String?>,
    onGoogle: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var showDemo by rememberSaveable { mutableStateOf(false) }
    var shakes by remember { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(prefill.value) {
        prefill.value?.let {
            username = it
            password = ""
            failed = false
            prefill.value = null
        }
    }
    val canSubmit = username.isNotBlank() && password.isNotBlank() && !failed
    val submit: () -> Unit = {
        if (canSubmit) {
            focus.clearFocus()
            ui.busy {
                val r = Store.login(username, password, rememberMe)
                if (r != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (r.startsWith("No encontramos") || r.startsWith("La contraseña")) {
                        failed = true
                        shakes++
                        ui.error("Usuario o contraseña no es correcto")
                    } else ui.error(r)
                }
            }
        }
    }

    AuthPage(version, onBack = { nav.back() }, onVersionTap = { showDemo = !showDemo }) {
        Text("Hola de nuevo", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text("Ingresa con tu DNI o celular", style = BodyStyle)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.shake(shakes)) {
            AuthField(
                label = "Usuario",
                value = username,
                onValueChange = { username = it.trim(); failed = false },
                placeholder = "ej. Jhosep",
                error = if (failed) "Por favor, verificar tus datos" else null,
                onIme = { focus.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(16.dp))
            AuthField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; failed = false },
                placeholder = "••••••••",
                password = true,
                error = if (failed) "Por favor, verificar tu contraseña" else null,
                imeAction = ImeAction.Done,
                onIme = submit
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RememberCheck(rememberMe) { rememberMe = !rememberMe }
            Spacer(Modifier.weight(1f))
            Text(
                "¿Olvidaste tu contraseña?",
                style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Palette.Teal),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { nav.go(AuthStep.ForgotChoose) }
                    .padding(vertical = 8.dp, horizontal = 2.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Ingresar", canSubmit, submit)
        Spacer(Modifier.height(24.dp))
        OrDivider("O inicia sesión con")
        Spacer(Modifier.height(24.dp))
        GoogleButton(onGoogle)
        Spacer(Modifier.height(28.dp))
        SignUpFooter { nav.go(AuthStep.SignUp) }
        // Tocar «Versión» muestra las cuentas de prueba
        if (showDemo) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEMO_ACCOUNTS.forEach { (user, label) ->
                    Column(
                        Modifier
                            .clip(ControlShape)
                            .background(Palette.Field)
                            .border(1.dp, AuthColors.Border, ControlShape)
                            .clickable {
                                username = user
                                password = "1234"
                                failed = false
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(label, style = LabelStyle.copy(color = AuthColors.Text))
                        Text("@$user · 1234", style = BodyStyle.copy(fontSize = 12.sp, lineHeight = 16.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignUpStep(
    ui: AuthUi,
    nav: AuthNav,
    version: String,
    prefill: MutableState<Pair<String, String>?>,
    onGoogle: () -> Unit,
    sendCode: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var emailErr by rememberSaveable { mutableStateOf<String?>(null) }
    var passErr by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneErr by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    LaunchedEffect(prefill.value) {
        prefill.value?.let { (n, e) ->
            name = n
            email = e
            emailErr = null
            prefill.value = null
        }
    }
    val filled = name.isNotBlank() && email.isNotBlank() && pass.isNotBlank() && phone.isNotBlank()
    val submit: () -> Unit = submit@{
        if (!filled) return@submit
        val e = email.trim().lowercase()
        emailErr = when {
            !EMAIL.matches(e) -> "Revisa tu correo electrónico"
            Store.accountByEmail(e) != null -> "Este correo ya tiene una cuenta"
            else -> null
        }
        passErr = if (pass.length < 4) "Usa al menos 4 caracteres" else null
        phoneErr = when {
            phone.length != 9 -> "El celular debe tener 9 números"
            Store.accountByPhone(phone) != null -> "Este celular ya tiene una cuenta"
            else -> null
        }
        if (emailErr != null || passErr != null || phoneErr != null) {
            ui.error("Por favor, verificar tus datos")
        } else {
            focus.clearFocus()
            ui.busy {
                val dest = formatPhone(phone)
                sendCode(dest)
                nav.go(AuthStep.Otp(dest, draft = SignUpDraft(name.trim(), e, pass, phone)))
            }
        }
    }

    AuthPage(version, onBack = { nav.back() }) {
        Text("Crear cuenta", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text("Para continuar necesitamos una breve información", style = BodyStyle)
        Spacer(Modifier.height(24.dp))
        AuthField(
            "Nombre completo", name, { name = it }, "ej. Jhosep Quispe",
            capitalization = KeyboardCapitalization.Words,
            onIme = { focus.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(16.dp))
        AuthField(
            "Correo electrónico", email, { email = it.trim(); emailErr = null }, "ej. jhosep.quispe@correo.com",
            error = emailErr, keyboardType = KeyboardType.Email,
            onIme = { focus.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(16.dp))
        AuthField(
            "Contraseña", pass, { pass = it; passErr = null }, "••••••••",
            password = true, error = passErr,
            onIme = { focus.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(16.dp))
        AuthField(
            "Celular", phone, { phone = it.filter(Char::isDigit).take(9); phoneErr = null }, "ej. 987 654 321",
            error = phoneErr, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, onIme = submit
        )
        Spacer(Modifier.height(24.dp))
        AuthButton("Crear cuenta", filled, submit)
        Spacer(Modifier.height(24.dp))
        OrDivider("Crear cuenta con")
        Spacer(Modifier.height(24.dp))
        GoogleButton(onGoogle)
    }
}

@Composable
private fun ForgotChooseStep(nav: AuthNav, version: String) {
    var byEmail by rememberSaveable { mutableStateOf<Boolean?>(null) }
    AuthPage(version, onBack = { nav.back() }) {
        SadEmoji()
        Spacer(Modifier.height(28.dp))
        Text("¿Olvidaste tu contraseña?", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text("Para recuperar tu contraseña, te enviaremos un código de verificación.", style = BodyStyle)
        Spacer(Modifier.height(24.dp))
        RadioCard("Enviar por correo", byEmail == true) { byEmail = true }
        Spacer(Modifier.height(12.dp))
        RadioCard("Enviar por celular", byEmail == false) { byEmail = false }
        Spacer(Modifier.height(16.dp))
        AuthButton("Continuar", byEmail != null, { byEmail?.let { nav.go(AuthStep.ForgotEnter(it)) } })
    }
}

@Composable
private fun ForgotEnterStep(ui: AuthUi, nav: AuthNav, version: String, byEmail: Boolean, sendCode: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    var err by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val valid = if (byEmail) EMAIL.matches(value.trim()) else value.length == 9
    val submit: () -> Unit = {
        if (valid && err == null) {
            focus.clearFocus()
            ui.busy {
                val u = if (byEmail) Store.accountByEmail(value) else Store.accountByPhone(value)
                if (u == null) {
                    val msg = if (byEmail) "No encontramos una cuenta con este correo" else "No encontramos una cuenta con este celular"
                    err = msg
                    ui.error(msg)
                } else {
                    val dest = if (byEmail) value.trim().lowercase() else formatPhone(value)
                    sendCode(dest)
                    nav.go(AuthStep.Otp(dest, resetUserId = u.id))
                }
            }
        }
    }
    AuthPage(version, onBack = { nav.back() }) {
        SadEmoji()
        Spacer(Modifier.height(28.dp))
        Text("Te enviaremos un código", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text(
            if (byEmail) "Para continuar, escribe tu correo registrado para poder enviarte un código de verificación"
            else "Para continuar, escribe tu número de celular registrado para poder enviarte un código de verificación",
            style = BodyStyle
        )
        Spacer(Modifier.height(24.dp))
        if (byEmail) {
            AuthField(
                "Correo electrónico", value, { value = it.trim(); err = null }, "ej. jhosep.quispe@correo.com",
                error = err, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, onIme = submit
            )
        } else {
            AuthField(
                "Celular", value, { value = it.filter(Char::isDigit).take(9); err = null }, "ej. 987 654 321",
                error = err, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, onIme = submit
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Continuar", valid && err == null, submit)
    }
}

@Composable
private fun OtpStep(
    ui: AuthUi,
    nav: AuthNav,
    version: String,
    step: AuthStep.Otp,
    currentCode: () -> String,
    resend: () -> Unit,
    onVerified: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var shakes by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val canVerify = code.length == 6 && !invalid
    val verify: () -> Unit = {
        if (canVerify) ui.busy {
            if (code == currentCode()) onVerified()
            else {
                invalid = true
                shakes++
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ui.error("Código inválido")
            }
        }
    }
    AuthPage(
        version,
        onBack = { nav.back() },
        centered = true,
        below = {
            NumberPad(
                onDigit = { d ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (invalid) { code = ""; invalid = false }
                    if (code.length < 6) code += d
                },
                onDelete = {
                    invalid = false
                    code = code.dropLast(1)
                }
            )
        }
    ) {
        Text("Ingresa tu código", style = TitleStyle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append("Por favor ingrese los 6 dígitos que se ha enviado a ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AuthColors.Title)) { append(step.destination) }
            },
            style = BodyStyle.copy(fontSize = 16.sp, lineHeight = 21.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        CodeBoxes(code, invalid, Modifier.shake(shakes))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿No recibiste un código? ", style = BodyStyle.copy(fontSize = 16.sp))
            Text(
                "Reenviar",
                style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Palette.Teal),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        code = ""
                        invalid = false
                        resend()
                    }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Verificar código", canVerify, verify)
        Spacer(Modifier.height(20.dp))
        Text(
            "al continuar estás indicando que aceptas nuestros Términos de Uso y nuestra Política de Privacidad",
            style = BodyStyle.copy(fontSize = 13.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NewPasswordStep(ui: AuthUi, version: String, onBack: () -> Unit, onSave: (String) -> Unit) {
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    var err1 by rememberSaveable { mutableStateOf<String?>(null) }
    var err2 by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val can = p1.isNotEmpty() && p2.isNotEmpty() && err1 == null && err2 == null
    val submit: () -> Unit = {
        if (can) {
            when {
                p1.length < 4 -> err1 = "Usa al menos 4 caracteres"
                p1 != p2 -> {
                    err2 = "Las contraseñas no coinciden"
                    ui.error("Las contraseñas no coinciden")
                }
                else -> {
                    focus.clearFocus()
                    ui.busy { onSave(p1) }
                }
            }
        }
    }
    AuthPage(version, onBack = onBack) {
        Text("Crea tu nueva contraseña", style = TitleStyle)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AuthColors.Label)) { append("¡Genial!") }
                append(" ahora puedes modificar tu nueva contraseña.")
            },
            style = BodyStyle.copy(fontSize = 16.sp, lineHeight = 21.sp)
        )
        Spacer(Modifier.height(24.dp))
        AuthField(
            "Nueva contraseña", p1, { p1 = it; err1 = null; err2 = null }, "••••••••",
            password = true, error = err1, onIme = { focus.moveFocus(FocusDirection.Down) }
        )
        Spacer(Modifier.height(16.dp))
        AuthField(
            "Confirmar contraseña", p2, { p2 = it; err2 = null }, "••••••••",
            password = true, error = err2, imeAction = ImeAction.Done, onIme = submit
        )
        Spacer(Modifier.height(16.dp))
        AuthButton("Actualizar contraseña", can, submit)
    }
}

// ---------------- Estructura de página ----------------

/** Foto de fondo fija de todo el ingreso. */
@Composable
private fun AuthBackground() {
    Image(
        painter = painterResource(R.drawable.auth_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Barra superior (volver + versión) y hoja blanca pegada abajo que crece con su contenido.
 * Si no entra en la pantalla (o aparece el teclado) se desplaza; en tablets se centra con ancho máximo.
 */
@Composable
private fun AuthPage(
    version: String,
    onBack: (() -> Unit)?,
    onVersionTap: () -> Unit = {},
    centered: Boolean = false,
    below: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val minH = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minH),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AuthTopBar(version, onBack, onVersionTap)
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(SheetShape)
                        .background(Color.White)
                        .then(if (below == null) Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal = 24.dp)
                        .padding(top = 36.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                        content = content
                    )
                }
                below?.invoke()
            }
        }
    }
}

@Composable
private fun AuthTopBar(version: String, onBack: (() -> Unit)?, onVersionTap: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowCircleLeft, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        } else {
            Spacer(Modifier.height(44.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Versión $version",
            style = TextStyle(fontFamily = Geist, fontSize = 15.sp, color = Color.White),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onVersionTap)
                .padding(4.dp)
        )
    }
}

// ---------------- Componentes ----------------

/** Campo con etiqueta y asterisco; borde verde al escribir y rojo con mensaje si hay error. */
@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    onIme: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val isError = error != null
    val borderColor by animateColorAsState(
        when {
            isError -> Palette.Red
            focused -> Palette.Teal
            else -> AuthColors.Border
        },
        label = "border"
    )
    val iconTint = when {
        isError -> Palette.Red
        focused -> Palette.Teal
        else -> AuthColors.Body
    }
    Column(modifier.fillMaxWidth()) {
        Row {
            Text(label, style = LabelStyle)
            Text(" *", style = LabelStyle.copy(color = Palette.Red))
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = FieldStyle,
            cursorBrush = SolidColor(Palette.Teal),
            visualTransformation = if (password && !visible) PasswordVisualTransformation('•') else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onIme() }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(
                            if (focused && !isError) Modifier.shadow(
                                8.dp, ControlShape,
                                ambientColor = Palette.Teal.copy(alpha = 0.25f),
                                spotColor = Palette.Teal.copy(alpha = 0.3f)
                            ) else Modifier
                        )
                        .background(Color.White, ControlShape)
                        .border(if (focused && !isError) 2.dp else 1.dp, borderColor, ControlShape)
                        .padding(start = 16.dp, end = if (password) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = FieldStyle.copy(color = AuthColors.Placeholder))
                        inner()
                    }
                    if (password) {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Ic.EyeOff else Ic.Eye,
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = iconTint
                            )
                        }
                    }
                }
            }
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 18.sp, color = Palette.Red))
        }
    }
}

/** Botón principal verde; gris cuando falta completar algo. */
@Composable
private fun AuthButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    textStyle: TextStyle = ButtonStyle
) {
    val bg by animateColorAsState(if (enabled) Palette.Teal else AuthColors.Disabled, label = "btn")
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (enabled) Modifier.pressScale(0.98f) else Modifier)
            .clip(ControlShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = textStyle, color = Color.White)
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(0.98f)
            .clip(ControlShape)
            .background(Color.White)
            .border(1.dp, AuthColors.Border, ControlShape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(GoogleG, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text("Continuar con Google", style = TextStyle(fontFamily = Geist, fontSize = 17.sp, color = AuthColors.Label))
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = AuthColors.DividerLine)
        Text(
            text,
            style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = AuthColors.DividerText),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = AuthColors.DividerLine)
    }
}

@Composable
private fun SignUpFooter(onSignUp: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("¿Nuevo en Convive? ", style = BodyStyle.copy(fontSize = 16.sp, color = AuthColors.Label))
        Text(
            "Crear cuenta",
            style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Palette.Teal),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSignUp)
                .padding(vertical = 6.dp, horizontal = 2.dp)
        )
    }
}

@Composable
private fun RememberCheck(checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = RoundedCornerShape(5.dp)
        Box(
            Modifier
                .size(20.dp)
                .clip(shape)
                .background(if (checked) Palette.Teal else Color.White)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, AuthColors.Disabled, shape)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Ic.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text("Recordar", style = TextStyle(fontFamily = Geist, fontSize = 16.sp, color = AuthColors.Text))
    }
}

@Composable
private fun RadioCard(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ControlShape)
            .background(Color.White)
            .border(1.dp, AuthColors.Border, ControlShape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = FieldStyle, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) Palette.Teal else Color.White)
                .then(if (selected) Modifier else Modifier.border(2.dp, AuthColors.Border, CircleShape)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun ColumnScope.SadEmoji() {
    Image(
        painter = painterResource(R.drawable.emoji_pleading),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(104.dp)
            .floating(4.dp, 2800)
    )
}

@Composable
private fun CodeBoxes(code: String, invalid: Boolean, modifier: Modifier = Modifier) {
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "blink"
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) { i ->
            val ch = code.getOrNull(i)
            val active = !invalid && i == code.length
            val color = when {
                invalid -> Palette.Red
                active -> Palette.Teal
                else -> AuthColors.Border
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .border(if (active) 2.dp else 1.dp, color, ControlShape),
                contentAlignment = Alignment.Center
            ) {
                if (ch != null) {
                    Text(ch.toString(), style = TextStyle(fontFamily = Geist, fontSize = 24.sp, color = AuthColors.Title), modifier = Modifier.popIn())
                } else if (active) {
                    Box(Modifier.width(2.dp).height(24.dp).graphicsLayer { alpha = blink }.background(Palette.Teal))
                }
            }
        }
    }
}

/** Teclado numérico propio para el código (como en el diseño). */
@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
    val keyShape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(AuthColors.Keypad)
            .navigationBarsPadding()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(48.dp)) {
                        when (k) {
                            "" -> Unit
                            "del" -> Box(
                                Modifier.fillMaxSize().clip(keyShape).clickable(onClick = onDelete),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Borrar", tint = Color(0xFF1F2937))
                            }
                            else -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .shadow(1.dp, keyShape)
                                    .clip(keyShape)
                                    .background(Color.White)
                                    .clickable { onDigit(k[0]) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(k, fontSize = 26.sp, color = Color(0xFF111827))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Capas encima ----------------

@Composable
private fun BoxScope.ToastHost(ui: AuthUi) {
    val t = ui.toast
    val last = remember { mutableStateOf<AuthToast?>(null) }
    SideEffect { if (t != null) last.value = t }
    LaunchedEffect(t?.id) {
        if (t != null) {
            delay(4500)
            if (ui.toast?.id == t.id) ui.toast = null
        }
    }
    AnimatedVisibility(
        visible = t != null,
        modifier = Modifier.align(Alignment.TopCenter),
        enter = slideInVertically(tween(320, easing = EaseOutCubic)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(240)) { -it } + fadeOut()
    ) {
        val shown = t ?: last.value ?: return@AnimatedVisibility
        val accent = if (shown.error) Palette.Red else Palette.Teal
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(50))
                    .background(if (shown.error) Palette.RedSoft else Palette.TealSoft)
                    .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                    Icon(if (shown.error) Ic.AlertCircle else Ic.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Text(
                    shown.text,
                    style = TextStyle(fontFamily = Geist, fontSize = 16.sp, lineHeight = 20.sp, color = AuthColors.Label),
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                )
                IconButton(onClick = { ui.toast = null }) {
                    Icon(Ic.Close, contentDescription = "Cerrar", tint = accent)
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = Color.White,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(20.dp))
                Text("Espere un momento", color = Color.White, fontSize = 17.sp)
            }
        }
    }
}

/** Selector de cuenta de Google (de prueba hasta conectar Google Sign-In). */
@Composable
private fun GoogleAccountSheet(visible: Boolean, onDismiss: () -> Unit, onPick: () -> Unit, onOther: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(360, easing = EaseOutCubic)) { it },
        exit = slideOutVertically(tween(260)) { it }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(SheetShape)
                    .background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 28.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(40.dp, 4.dp)
                        .clip(CircleShape)
                        .background(AuthColors.Disabled)
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(GoogleG, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Acceder con Google", style = BodyStyle.copy(fontSize = 16.sp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Elige una cuenta", style = TitleStyle)
                Spacer(Modifier.height(6.dp))
                Text("Para continuar en Convive", style = BodyStyle.copy(fontSize = 16.sp))
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(ControlShape)
                        .clickable(onClick = onPick)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Palette.NavySoft), contentAlignment = Alignment.Center) {
                        Text("PQ", style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AuthColors.Title))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(GOOGLE_DEMO_NAME, style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AuthColors.Title))
                        Text(GOOGLE_DEMO_EMAIL, style = BodyStyle.copy(fontSize = 14.sp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), thickness = 1.dp, color = AuthColors.Border)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(ControlShape)
                        .clickable(onClick = onOther)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).border(1.dp, AuthColors.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Ic.UserPlus, contentDescription = null, tint = AuthColors.Body, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Usar otra cuenta", style = FieldStyle)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Google compartirá tu nombre, correo y foto con Convive. Si ya tienes cuenta, entras directo a tu home.",
                    style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 19.sp)
                )
            }
        }
    }
}

// ---------------- Utilidades ----------------

/** Íconos claros en la barra de estado sobre la foto; se restauran al salir del ingreso. */
@Composable
private fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctrl?.isAppearanceLightStatusBars = false
        onDispose { ctrl?.isAppearanceLightStatusBars = true }
    }
}

/** Logo «G» de Google a color. */
private val GoogleG: ImageVector by lazy {
    val b = ImageVector.Builder(
        name = "GoogleG", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 48f, viewportHeight = 48f
    )
    listOf(
        0xFFEA4335 to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
        0xFF4285F4 to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
        0xFFFBBC05 to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
        0xFF34A853 to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
    ).forEach { (c, d) ->
        b.addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color(c)))
    }
    b.build()
}
