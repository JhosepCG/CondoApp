package pe.condoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CondoTheme { AppRoot() }
        }
    }
}

@Composable
fun CondoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0F4C81),
            secondary = Color(0xFF2E7D6B),
            background = Color(0xFFF4F6F9),
            surface = Color(0xFFFFFFFF)
        ),
        content = content
    )
}
