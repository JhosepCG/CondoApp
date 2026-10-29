package pe.condoapp

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DEMO_ACCOUNTS = listOf(
    "superadmin" to "Super Admin",
    "ana" to "Administradora",
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.Navy, Palette.Teal)))
            .systemBarsPadding()
    ) {
        // Marca sobre el degradado
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CondoApp.",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 40.sp)
            )
            Text(
                "tu condominio en orden",
                color = Color.White.copy(alpha = 0.8f),
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LoginIllustration()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ingresa a CondoApp.", style = MaterialTheme.typography.headlineSmall)
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
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = null,
                        modifier = Modifier.padding(12.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Palette.Teal,
                            uncheckedColor = Palette.Muted,
                            checkmarkColor = Color.White
                        )
                    )
                    Text("Recordarme", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { showForgot = true }) {
                    Text(
                        "¿Olvidaste tu contraseña?",
                        color = Palette.Teal,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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

            // Cuentas de prueba (plegadas para no ensuciar el diseño)
            TextButton(
                onClick = { showDemo = !showDemo },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (showDemo) "Ocultar cuentas de prueba" else "Ver cuentas de prueba",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
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

            Text(
                "© 2026 CondoApp · Emergencias: Bomberos 116 · Policía 105 · SAMU 106",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
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

/** Ilustración pequeña: edificio con candado. */
@Composable
private fun LoginIllustration() {
    Box(Modifier.size(64.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.TealSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Apartment, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(30.dp))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(Palette.Navy),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
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
