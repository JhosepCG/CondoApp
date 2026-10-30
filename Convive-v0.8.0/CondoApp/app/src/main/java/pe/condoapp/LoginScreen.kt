package pe.condoapp

import android.app.Activity
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

private val DEMO_ACCOUNTS = listOf(
    "jhosep" to "Super Admin",
    "pedro" to "Admin casa",
    "ana" to "Admin condominio",
    "lucia" to "Inquilina",
    "marco" to "Inquilino casa"
)

/** Gris del botón desactivado (como en Figma). */
private val DisabledGray = Color(0xFFB3BAC4)

/**
 * Ingreso (todos los roles), según la sección "01 · Ingreso" de Figma:
 * Login → escribir DNI/usuario → escribir contraseña → validando → error / olvidé mi contraseña.
 *
 * Se adapta a cualquier pantalla: la foto ocupa todo el fondo (también detrás de la barra de estado),
 * la parte de arriba usa el 46 % del alto como en el diseño (mínimo 200 dp), el formulario se
 * desplaza si la pantalla es baja o aparece el teclado, y en tablets se centra con un ancho máximo.
 */
@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var shakes by remember { mutableStateOf(0) }
    LaunchedEffect(error) { if (error != null) shakes++ }
    var showForgot by remember { mutableStateOf(false) }
    var showDemo by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    val canSubmit = username.isNotBlank() && password.isNotBlank()
    // Ingreso con huella: si alguien la activó en este teléfono
    val bioUser = remember { Store.biometricUserId() }
    val bioAvailable = remember { bioUser != null && Biometric.available(context) }
    val bioName = remember { Store.users.firstOrNull { it.id == bioUser }?.name?.substringBefore(" ") }
    val askFinger: () -> Unit = {
        val act = context as? androidx.fragment.app.FragmentActivity
        if (act != null) {
            Biometric.prompt(
                act,
                "Ingresar a Convive",
                if (bioName != null) "Hola $bioName, coloca tu huella" else "Coloca tu huella",
                onSuccess = { error = Store.loginWithBiometric() },
                onError = { error = it }
            )
        }
    }
    // Al abrir el login (por ejemplo, después de cerrar sesión) aparece sola la ventana flotante de la huella.
    LaunchedEffect(bioAvailable) {
        if (bioAvailable) {
            delay(350)
            askFinger()
        }
    }

    // Íconos claros en la barra de estado sobre la foto; se restauran al salir del login.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctrl?.isAppearanceLightStatusBars = false
        onDispose { ctrl?.isAppearanceLightStatusBars = true }
    }

    // "Validando tus datos": breve espera y luego se valida
    LaunchedEffect(loading) {
        if (loading) {
            delay(900)
            val result = Store.login(username, password, rememberMe)
            loading = false
            if (result != null) error = result
        }
    }
    val submit: () -> Unit = {
        if (canSubmit && !loading) {
            focus.clearFocus()
            loading = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Navy)
    ) {
        Image(
            painter = painterResource(R.drawable.login_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            val screenH = maxHeight
            val heroH = maxOf(200.dp, screenH * 0.46f)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Marca sobre la foto
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(heroH)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(Modifier.enter(0, 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Convive",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 36.sp,
                                lineHeight = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "Vivir juntos, más fácil.",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                        )
                    }
                }

                // Hoja blanca: ocupa el resto de la pantalla
                Column(
                    Modifier
                        .enter(2, 60.dp)
                        .fillMaxWidth()
                        .heightIn(min = screenH - heroH)
                        .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                        .background(Palette.Surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth().shake(shakes),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LabeledField(
                            label = "Usuario",
                            value = username,
                            onValueChange = { username = it.trim() },
                            placeholder = "ej. 87654321",
                            imeAction = ImeAction.Next,
                            onIme = { focus.moveFocus(FocusDirection.Down) }
                        )
                        LabeledField(
                            label = "Contraseña",
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "••••••••",
                            password = true,
                            imeAction = ImeAction.Done,
                            onIme = submit
                        )

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { rememberMe = !rememberMe }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Palette.Teal,
                                        uncheckedColor = Palette.Muted,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Text("Recordar", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp))
                            }
                            Text(
                                "¿Olvidaste tu contraseña?",
                                color = Palette.Teal,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showForgot = true }
                                    .padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }

                        Button(
                            onClick = submit,
                            enabled = canSubmit,
                            modifier = Modifier.pressScale(0.97f).fillMaxWidth().height(52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Palette.Navy,
                                contentColor = Color.White,
                                disabledContainerColor = DisabledGray,
                                disabledContentColor = Color.White
                            )
                        ) {
                            Text("Ingresar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
                        }
                        if (bioAvailable) {
                            OutlinedButton(
                                onClick = askFinger,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line)
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ingresar con huella", color = Palette.Ink, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                            }
                        }
                    }

                    Column(
                        Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row {
                            Text("¿No tienes cuenta? ", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                            Text(
                                "Pídela a tu administración",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.Teal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Tocar la versión muestra u oculta las cuentas de prueba
                        Text(
                            "Versión $version",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDemo = !showDemo }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        if (showDemo) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DEMO_ACCOUNTS.forEach { (user, label) ->
                                    val shape = RoundedCornerShape(16.dp)
                                    Column(
                                        Modifier
                                            .clip(shape)
                                            .background(Palette.Field)
                                            .border(1.dp, Palette.Line, shape)
                                            .clickable {
                                                username = user
                                                password = "1234"
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelLarge)
                                        Text("@$user · 1234", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Validando tus datos
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Palette.Navy.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BouncingDots(Color.White, 12.dp)
                    Text(
                        "Validando tus datos",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    // Error al ingresar
    error?.let { msg ->
        val wrongPassword = msg.startsWith("La contraseña")
        val notFound = msg.startsWith("No encontramos")
        IllustrationDialog(
            image = R.drawable.ill_security,
            title = when {
                wrongPassword -> "Contraseña inválida"
                notFound -> "Usuario no encontrado"
                else -> "No pudimos ingresar"
            },
            text = when {
                wrongPassword -> "Vuelve a ingresar la contraseña registrada"
                notFound -> "Revisa tu usuario o número de DNI."
                else -> msg
            },
            onDismiss = { error = null }
        ) {
            Button(
                onClick = {
                    if (wrongPassword) password = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Navy, contentColor = Color.White)
            ) {
                Text("Intentar nuevamente", style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp))
            }
        }
    }

    // Olvidé mi contraseña
    if (showForgot) {
        IllustrationDialog(
            image = R.drawable.ill_forgot,
            title = "¿Olvidaste tu contraseña?",
            text = "Por seguridad, tu administración es quien te asigna una contraseña nueva. Escríbele y te la enviará.",
            onDismiss = { showForgot = false }
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { showForgot = false }) {
                    Text("Entendido", color = Palette.Teal, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                }
            }
        }
    }
}

/** Ventana blanca con ilustración, título, texto y acción (error y olvidé mi contraseña). */
@Composable
private fun IllustrationDialog(
    image: Int,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .popIn()
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Palette.Surface)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.62f).aspectRatio(1.08f).floating(5.dp, 2800)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = Palette.Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            actions()
        }
    }
}

/** Campo con etiqueta arriba, asterisco de obligatorio y bordes de píldora. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onIme: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    val trailing: (@Composable () -> Unit)? = if (password) {
        {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = Palette.Muted
                )
            }
        }
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
            Text("*", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = Palette.Red)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9AA1AC)) },
            singleLine = true,
            trailingIcon = trailing,
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onIme() }),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Teal,
                unfocusedBorderColor = Palette.Line,
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                cursorColor = Palette.Teal
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        )
    }
}
