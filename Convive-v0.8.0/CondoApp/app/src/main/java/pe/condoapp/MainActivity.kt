package pe.condoapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

/** FragmentActivity para poder mostrar el lector de huella. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla completa: la app dibuja detrás de la barra de estado y de navegación
        // y cada pantalla se acomoda a los márgenes del teléfono (notch, gestos, teclado).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        setContent {
            CondoTheme { AppRoot() }
        }
    }
}
