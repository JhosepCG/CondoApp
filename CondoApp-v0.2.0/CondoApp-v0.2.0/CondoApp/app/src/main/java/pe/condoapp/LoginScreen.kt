package pe.condoapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DEMO_ACCOUNTS = listOf(
    "superadmin" to "Super Admin",
    "pedro" to "Admin casa",
    "ana" to "Admin condominio",
    "lucia" to "Inquilina",
    "marco" to "Inquilino casa"
)

@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForgot by remember { mutableStateOf(false) }
    var showDemo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Navy)
    ) {
        // Fondo: foto del edificio con degradado azul noche → verde azulado
        Image(
            painter = painterResource(R.drawable.login_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Marca sobre la foto
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 104.dp, bottom = 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Convive",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    "Vivir juntos, más fácil.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Hoja blanca con el formulario
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Palette.Surface)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ingresa a Convive.", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Pagos, avisos y emergencias de tu domicilio, todo en un solo lugar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted
                    )
                }

                LabeledField(
                    label = "Usuario",
                    value = username,
                    onValueChange = { username = it; error = null },
                    placeholder = "ej. lucia"
                )
                LabeledField(
                    label = "Contraseña",
                    value = password,
                    onValueChange = { password = it; error = null },
                    placeholder = "••••••••",
                    password = true
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 10.dp),
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
                        Text("Recordarme", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "¿Olvidaste tu contraseña?",
                        color = Palette.Teal,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showForgot = true }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    )
                }

                error?.let { ErrorText(it) }

                Button(
                    onClick = { error = Store.login(username, password, rememberMe) },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.Navy,
                        contentColor = Color.White,
                        disabledContainerColor = Palette.Navy.copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text("Ingresar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDemo = !showDemo }
                        .padding(vertical = 4.dp)
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
                                        error = null
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

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            containerColor = Palette.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = { IconBadge(Icons.Rounded.VpnKey, Palette.Teal, Palette.TealSoft, 52.dp) },
            title = { Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Por seguridad, tu administración es quien te asigna una contraseña nueva. Escríbele y te la enviará.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgot = false }) {
                    Text("Entendido", color = Palette.Teal, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

/** Campo con etiqueta arriba, asterisco de obligatorio y bordes de píldora. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false
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
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("*", style = MaterialTheme.typography.labelLarge, color = Palette.Red)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Palette.Muted) },
            singleLine = true,
            trailingIcon = trailing,
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Teal,
                unfocusedBorderColor = Palette.Line,
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                cursorColor = Palette.Teal
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
