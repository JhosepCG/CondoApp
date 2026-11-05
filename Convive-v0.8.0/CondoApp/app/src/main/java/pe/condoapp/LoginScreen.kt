package pe.condoapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------------------------------------------------------
// Ingreso y registro (Figma «01 · Ingreso», «02 · Crear usuarios Propietario / Inquilino»).
// Splash → Bienvenida → Iniciar sesión / Google / Crear cuenta → rol →
//   propietario: configuración en 3 pasos y plan · inquilino: llave de 6 dígitos.
// ¿Olvidaste tu contraseña? → código → nueva contraseña → «Contraseña actualizada».
// ------------------------------------------------------------------

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

internal sealed interface AuthStep {
    val key: String

    data object Welcome : AuthStep { override val key: String get() = "welcome" }
    data object Login : AuthStep { override val key: String get() = "login" }
    data object SignUp : AuthStep { override val key: String get() = "signup" }
    data object ForgotChoose : AuthStep { override val key: String get() = "forgot" }
    data class ForgotEnter(val byEmail: Boolean) : AuthStep { override val key: String get() = if (byEmail) "forgot-mail" else "forgot-phone" }
    data class Otp(val destination: String, val resetUserId: String) : AuthStep { override val key: String get() = "otp-$destination" }
    data class NewPassword(val userId: String) : AuthStep { override val key: String get() = "newpass-$userId" }
    data object PasswordUpdated : AuthStep { override val key: String get() = "pw-ok" }
    data object Rol : AuthStep { override val key: String get() = "rol" }
    data object OwnerIntro : AuthStep { override val key: String get() = "o-intro" }
    data object OwnerType : AuthStep { override val key: String get() = "o-type" }
    data object OwnerPlace : AuthStep { override val key: String get() = "o-place" }
    data object OwnerUnits : AuthStep { override val key: String get() = "o-units" }
    data object OwnerPrices : AuthStep { override val key: String get() = "o-prices" }
    data object OwnerServices : AuthStep { override val key: String get() = "o-services" }
    data object OwnerReview : AuthStep { override val key: String get() = "o-review" }
    data object OwnerPlan : AuthStep { override val key: String get() = "o-plan" }
    data class OwnerDone(val userId: String) : AuthStep { override val key: String get() = "o-done" }
    data object TenantKey : AuthStep { override val key: String get() = "t-key" }
    data object TenantActive : AuthStep { override val key: String get() = "t-active" }
    data object TenantConfirm : AuthStep { override val key: String get() = "t-confirm" }
    data class TenantDone(val userId: String) : AuthStep { override val key: String get() = "t-done" }
}

internal class AuthNav(private val stack: SnapshotStateList<AuthStep>) {
    var forward by mutableStateOf(true)
    val current: AuthStep get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    fun go(s: AuthStep) { forward = true; stack.add(s) }
    fun back() { if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex) } }
    /** Vuelve a un paso anterior (por ejemplo «Editar» del resumen). */
    fun backTo(s: AuthStep) {
        if (s !in stack) return
        forward = false
        while (stack.last() != s) stack.removeAt(stack.lastIndex)
    }
    fun resetTo(vararg s: AuthStep) { forward = false; stack.clear(); stack.addAll(s) }
}

// ---------------- Splash ----------------

@Composable
fun SplashScreen(onDone: () -> Unit) {
    StatusIcons(light = true, resetOnDispose = false)
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Palette.Navy)) {
        AuthBackground()
        Column(Modifier.align(Alignment.Center).enter(0, 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Convive", color = Color.White, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Vivir juntos, más fácil.", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp).size(32.dp),
            color = Color.White, strokeWidth = 3.dp, trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

// ---------------- Flujo ----------------

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
    var pendingCode by remember { mutableStateOf("") }
    // Registro en curso: nada se guarda hasta terminar el flujo del rol.
    var draft by remember { mutableStateOf<SignUpDraft?>(null) }
    val owner = remember { OwnerSetup() }
    var invite by remember { mutableStateOf<Store.InviteCheck.Ok?>(null) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    BackHandler(enabled = googleOpen || (nav.canGoBack && !ui.loading)) {
        if (googleOpen) {
            googleOpen = false
            ui.neutral("No se completó el ingreso con Google. Inténtelo de nuevo")
        } else nav.back()
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

    fun sendCode(destination: String): Boolean {
        pendingCode = (100000..999999).random().toString()
        val shown = Notifier.notifyCode(context, pendingCode, destination)
        if (!shown) ui.info("Modo prueba: tu código es $pendingCode")
        return shown
    }

    val openGoogle: () -> Unit = { googleOpen = true }
    val light = nav.current in listOf(
        AuthStep.OwnerIntro, AuthStep.OwnerType, AuthStep.OwnerPlace, AuthStep.OwnerUnits, AuthStep.OwnerPrices,
        AuthStep.OwnerServices, AuthStep.OwnerReview, AuthStep.OwnerPlan, AuthStep.PasswordUpdated
    ) || nav.current is AuthStep.OwnerDone || nav.current is AuthStep.TenantDone
    StatusIcons(light = !light)

    Box(Modifier.fillMaxSize().background(Palette.Navy)) {
        AuthBackground()

        AnimatedContent(
            targetState = nav.current,
            transitionSpec = {
                if (nav.forward) {
                    (slideInVertically(tween(420, easing = EaseOutCubic)) { it / 5 } + fadeIn(tween(260))) togetherWith fadeOut(tween(160))
                } else {
                    fadeIn(tween(260)) togetherWith (slideOutVertically(tween(320)) { it / 5 } + fadeOut(tween(220)))
                }
            },
            label = "auth"
        ) { step ->
            holder.SaveableStateProvider(step.key) {
                when (step) {
                    AuthStep.Welcome -> WelcomeStep(version, onLogin = { nav.go(AuthStep.Login) }, onGoogle = openGoogle, onSignUp = { nav.go(AuthStep.SignUp) })
                    AuthStep.Login -> LoginStep(ui, nav, version, prefillUser, openGoogle)
                    AuthStep.SignUp -> SignUpStep(ui, nav, version) { d -> draft = d; nav.go(AuthStep.Rol) }
                    AuthStep.ForgotChoose -> ForgotChooseStep(nav, version)
                    is AuthStep.ForgotEnter -> ForgotEnterStep(ui, nav, version, step.byEmail) { sendCode(it) }
                    is AuthStep.Otp -> OtpStep(
                        ui, nav, version, step,
                        currentCode = { pendingCode },
                        resend = { if (sendCode(step.destination)) ui.info("Te enviamos un nuevo código") },
                        onVerified = { nav.go(AuthStep.NewPassword(step.resetUserId)) }
                    )
                    is AuthStep.NewPassword -> NewPasswordStep(ui, version, onBack = { nav.back() }) { pw ->
                        Store.resetPassword(step.userId, pw)
                        prefillUser.value = Store.users.firstOrNull { it.id == step.userId }?.username
                        nav.go(AuthStep.PasswordUpdated)
                    }
                    AuthStep.PasswordUpdated -> SuccessPage(
                        AIc.Lock, "¡Contraseña actualizada!",
                        body = { Text("Cerramos tus sesiones en otros teléfonos.\nIngresa con tu nueva contraseña.", style = TBody, textAlign = TextAlign.Center) },
                        button = "Ir a login",
                        onBack = { nav.resetTo(AuthStep.Welcome, AuthStep.Login) },
                        onClick = { nav.resetTo(AuthStep.Welcome, AuthStep.Login) }
                    )
                    AuthStep.Rol -> RolStep(version, onBack = { nav.back() }) { isOwner ->
                        nav.go(if (isOwner) AuthStep.OwnerIntro else AuthStep.TenantKey)
                    }
                    AuthStep.OwnerIntro -> OwnerIntroStep(onBack = { nav.back() }) { nav.go(AuthStep.OwnerType) }
                    AuthStep.OwnerType -> OwnerTypeStep(owner, nav)
                    AuthStep.OwnerPlace -> OwnerPlaceStep(owner, nav)
                    AuthStep.OwnerUnits -> OwnerUnitsStep(owner, nav)
                    AuthStep.OwnerPrices -> OwnerPricesStep(owner, nav)
                    AuthStep.OwnerServices -> OwnerServicesStep(owner, nav)
                    AuthStep.OwnerReview -> OwnerReviewStep(owner, nav)
                    AuthStep.OwnerPlan -> OwnerPlanStep(owner, onBack = { nav.back() }) {
                        val d = draft ?: return@OwnerPlanStep
                        ui.busy {
                            val id = Store.signUpOwner(d, owner.kind, owner.name, owner.address, owner.specs(), if (owner.annual) "p_anual" else "p_mes")
                            nav.go(AuthStep.OwnerDone(id))
                        }
                    }
                    is AuthStep.OwnerDone -> SuccessPage(
                        AIc.Check, "¡${owner.name.trim().ifEmpty { "Tu propiedad" }} está lista!",
                        body = { Text("Tu prueba gratis termina el ${longDate(java.time.LocalDate.now().plusDays(60))}.\nTe avisaremos antes.", style = TBody, textAlign = TextAlign.Center) },
                        button = "Ir a mi inicio",
                        onBack = { Store.loginAs(step.userId) },
                        onClick = { Store.loginAs(step.userId) }
                    )
                    AuthStep.TenantKey -> TenantKeyStep(ui, version, onBack = { nav.back() }) { ok ->
                        invite = ok
                        nav.go(if (ok.inv.demoActiveRental != null) AuthStep.TenantActive else AuthStep.TenantConfirm)
                    }
                    AuthStep.TenantActive -> invite?.let { ok ->
                        TenantActiveStep(version, ok, onBack = { nav.back() }) { nav.go(AuthStep.TenantConfirm) }
                    }
                    AuthStep.TenantConfirm -> invite?.let { ok ->
                        TenantConfirmStep(version, ok, onBack = { nav.backTo(AuthStep.TenantKey) }) {
                            val d = draft ?: return@TenantConfirmStep
                            ui.busy { nav.go(AuthStep.TenantDone(Store.signUpTenant(d, ok.inv))) }
                        }
                    }
                    is AuthStep.TenantDone -> invite?.let { ok ->
                        SuccessPage(
                            AIc.ShieldCheck, "¡Bienvenido a ${ok.client.name}!",
                            body = {
                                Text(
                                    buildAnnotatedString {
                                        append("Ya estás vinculado al ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Title)) { append(ok.inv.unit) }
                                        append(". Tu primer pago es el ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Title)) { append(longDate(Store.firstPayment(ok.inv))) }
                                    },
                                    style = TBody, textAlign = TextAlign.Center
                                )
                            },
                            button = "Ir a mi inicio",
                            onBack = { Store.loginAs(step.userId) },
                            onClick = { Store.loginAs(step.userId) }
                        )
                    }
                }
            }
        }

        ToastHost(ui)

        GoogleAccountSheet(
            visible = googleOpen,
            onDismiss = {
                googleOpen = false
                ui.neutral("No se completó el ingreso con Google. Inténtelo de nuevo")
            },
            onPick = {
                googleOpen = false
                ui.busy {
                    when (val r = Store.loginWithGoogle(GOOGLE_DEMO_EMAIL)) {
                        null -> Unit
                        Store.NO_ACCOUNT -> {
                            // Primera vez con Google: elige si es propietario o inquilino.
                            draft = SignUpDraft(GOOGLE_DEMO_NAME, GOOGLE_DEMO_EMAIL, null, null)
                            nav.go(AuthStep.Rol)
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

/** «4 de enero». */
internal fun longDate(d: java.time.LocalDate): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM", java.util.Locale("es", "PE")))

/** «5 nov». */
internal fun shortDate(d: java.time.LocalDate): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es", "PE"))).replace(".", "")

// ---------------- Pasos de ingreso ----------------

@Composable
private fun WelcomeStep(version: String, onLogin: () -> Unit, onGoogle: () -> Unit, onSignUp: () -> Unit) {
    SheetPage(version, onBack = null) {
        Text("Bienvenido a Convive", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Tu casa en orden: pagos, aviso y ayuda, para quien la administra y para quien la vive.", style = TBody)
        Spacer(Modifier.height(16.dp))
        AuthButton("Iniciar sesión", true, onLogin, icon = AIc.User, height = 54.dp, textStyle = TButton.copy(fontWeight = FontWeight.Normal))
        Spacer(Modifier.height(10.dp))
        GoogleButton(onGoogle)
        Spacer(Modifier.height(20.dp))
        OrDivider("O inicia sesión con")
        Spacer(Modifier.height(20.dp))
        LinkLine("¿Nuevo en Convive?", "Crear cuenta", onSignUp)
    }
}

@Composable
private fun LoginStep(ui: AuthUi, nav: AuthNav, version: String, prefill: MutableState<String?>, onGoogle: () -> Unit) {
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

    SheetPage(version, onBack = { nav.back() }, onVersionTap = { showDemo = !showDemo }) {
        Text("Hola de nuevo", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Ingresa con tu DNI o celular", style = TBody)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.shake(shakes)) {
            AuthField(
                "Usuario", username, { username = it.trim(); failed = false }, "ej. Jhosep",
                error = if (failed) "Por favor, verificar tus datos" else null,
                onIme = { focus.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(14.dp))
            AuthField(
                "Contraseña", password, { password = it; failed = false }, "••••••••", password = true,
                error = if (failed) "Por favor, verificar tu contraseña" else null,
                imeAction = ImeAction.Done, onIme = submit
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { rememberMe = !rememberMe }.padding(vertical = 8.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckSquare(rememberMe)
                Spacer(Modifier.width(10.dp))
                Text("Recordar", style = TField)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "¿Olvidaste tu contraseña?", style = TLink.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.go(AuthStep.ForgotChoose) }.padding(vertical = 8.dp, horizontal = 2.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        AuthButton("Ingresar", canSubmit, submit)
        Spacer(Modifier.height(20.dp))
        OrDivider("O inicia sesión con")
        Spacer(Modifier.height(20.dp))
        GoogleButton(onGoogle)
        Spacer(Modifier.height(20.dp))
        LinkLine("¿Nuevo en Convive?", "Crear cuenta") { nav.go(AuthStep.SignUp) }
        if (showDemo) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEMO_ACCOUNTS.forEach { (user, label) ->
                    Column(
                        Modifier.clip(CtlShape).background(Palette.Field).border(1.dp, AK.Border, CtlShape)
                            .clickable { username = user; password = "1234"; failed = false }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(label, style = TLabel.copy(color = AK.Text))
                        Text("@$user · 1234", style = TSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgotChooseStep(nav: AuthNav, version: String) {
    var byEmail by rememberSaveable { mutableStateOf<Boolean?>(null) }
    SheetPage(version, onBack = { nav.back() }) {
        SadEmoji()
        Spacer(Modifier.height(24.dp))
        Text("¿Olvidaste tu contraseña?", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text("Para recuperar tu contraseña, te enviaremos un código de verificación.", style = TBody)
        Spacer(Modifier.height(20.dp))
        ChoiceTile("Enviar por correo", null, byEmail == true, { byEmail = true }, titleStyle = TField)
        Spacer(Modifier.height(12.dp))
        ChoiceTile("Enviar por celular", null, byEmail == false, { byEmail = false }, titleStyle = TField)
        Spacer(Modifier.height(16.dp))
        AuthButton("Continuar", byEmail != null, { byEmail?.let { nav.go(AuthStep.ForgotEnter(it)) } })
    }
}

@Composable
private fun ForgotEnterStep(ui: AuthUi, nav: AuthNav, version: String, byEmail: Boolean, sendCode: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    var err by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val valid = if (byEmail) EMAIL_RX.matches(value.trim()) else value.length == 9
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
                    val dest = if (byEmail) value.trim().lowercase() else authPhone(value)
                    sendCode(dest)
                    nav.go(AuthStep.Otp(dest, u.id))
                }
            }
        }
    }
    SheetPage(version, onBack = { nav.back() }) {
        SadEmoji()
        Spacer(Modifier.height(24.dp))
        Text("Te enviaremos un código", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            if (byEmail) "Para continuar, escribe tu correo registrado para poder enviarte un código de verificación"
            else "Para continuar, escribe tu número de celular registrado para poder enviarte un código de verificación",
            style = TBody
        )
        Spacer(Modifier.height(20.dp))
        if (byEmail) {
            AuthField("Correo electrónico", value, { value = it.trim(); err = null }, "ej. jhosep.quispe@correo.com",
                error = err, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, onIme = submit)
        } else {
            AuthField("Celular", value, { value = it.filter(Char::isDigit).take(9); err = null }, "ej. 987 654 321",
                error = err, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, onIme = submit)
        }
        Spacer(Modifier.height(16.dp))
        AuthButton("Continuar", valid && err == null, submit)
    }
}

@Composable
private fun OtpStep(
    ui: AuthUi, nav: AuthNav, version: String, step: AuthStep.Otp,
    currentCode: () -> String, resend: () -> Unit, onVerified: () -> Unit
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
    SheetPage(
        version, onBack = { nav.back() }, centered = true,
        below = {
            NumberPad(
                onDigit = { d ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (invalid) { code = ""; invalid = false }
                    if (code.length < 6) code += d
                },
                onDelete = { invalid = false; code = code.dropLast(1) }
            )
        }
    ) {
        Text("Ingresa tu código", style = TTitle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append("Por favor ingrese los 6 dígitos que se ha enviado a ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Title)) { append(step.destination) }
            },
            style = TBody, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        CodeBoxes(code, invalid, Modifier.shake(shakes))
        Spacer(Modifier.height(14.dp))
        LinkLine("¿No recibiste un código?", "Reenviar") { code = ""; invalid = false; resend() }
        Spacer(Modifier.height(14.dp))
        AuthButton("Verificar código", canVerify, verify)
        Spacer(Modifier.height(16.dp))
        Text("al continuar estás indicando que aceptas nuestros Términos de Uso y nuestra Política de Privacidad", style = TSmall, textAlign = TextAlign.Center)
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
                p1 != p2 -> { err2 = "Las contraseñas no coinciden"; ui.error("Las contraseñas no coinciden") }
                else -> { focus.clearFocus(); ui.busy { onSave(p1) } }
            }
        }
    }
    SheetPage(version, onBack = onBack) {
        Text("Crea tu nueva contraseña", style = TTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AK.Label)) { append("¡Genial!") }
                append(" ahora puedes modificar tu nueva contraseña.")
            },
            style = TBody
        )
        Spacer(Modifier.height(20.dp))
        AuthField("Nueva contraseña", p1, { p1 = it; err1 = null; err2 = null }, "••••••••", password = true, error = err1,
            onIme = { focus.moveFocus(FocusDirection.Down) })
        Spacer(Modifier.height(14.dp))
        AuthField("Confirmar contraseña", p2, { p2 = it; err2 = null }, "••••••••", password = true, error = err2,
            imeAction = ImeAction.Done, onIme = submit)
        Spacer(Modifier.height(16.dp))
        AuthButton("Actualizar contraseña", can, submit)
    }
}

@Composable
private fun ColumnScope.SadEmoji() {
    Image(
        painter = painterResource(R.drawable.emoji_pleading),
        contentDescription = null,
        modifier = Modifier.align(Alignment.CenterHorizontally).size(104.dp).floating(4.dp, 2800)
    )
}

/** Selector de cuenta de Google (de prueba hasta conectar Google Sign-In). */
@Composable
private fun GoogleAccountSheet(visible: Boolean, onDismiss: () -> Unit, onPick: () -> Unit, onOther: () -> Unit) {
    BottomSheetOverlay(visible, onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(GoogleG, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Acceder con Google", style = TBody)
        }
        Spacer(Modifier.height(14.dp))
        Text("Elige una cuenta", style = TTitle)
        Spacer(Modifier.height(4.dp))
        Text("Para continuar en Convive", style = TBody)
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(CtlShape).clickable(onClick = onPick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(GOOGLE_DEMO_NAME)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(GOOGLE_DEMO_NAME, style = TName.copy(fontSize = 15.sp))
                Text(GOOGLE_DEMO_EMAIL, style = TSmall)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp), thickness = 1.dp, color = AK.Border)
        Row(
            Modifier.fillMaxWidth().clip(CtlShape).clickable(onClick = onOther).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).border(1.dp, AK.Border, CircleShape), contentAlignment = Alignment.Center) {
                Icon(AIc.UserPlus, contentDescription = null, tint = AK.Body, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Usar otra cuenta", style = TField)
        }
        Spacer(Modifier.height(14.dp))
        Text("Google compartirá tu nombre, correo y foto con Convive. Si ya tienes cuenta, entras directo a tu home.", style = TSmall)
    }
}
