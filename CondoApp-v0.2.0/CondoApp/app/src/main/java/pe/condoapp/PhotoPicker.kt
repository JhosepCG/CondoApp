package pe.condoapp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Abre la cámara o la galería y devuelve la ruta de la foto ya comprimida. */
class PhotoPicker(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberPhotoPicker(onResult: (String) -> Unit, onError: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val resultCb by rememberUpdatedState(onResult)
    val errorCb by rememberUpdatedState(onError)
    var pendingCamera by remember { mutableStateOf<File?>(null) }

    val handle: (String?) -> Unit = { path ->
        if (path != null) resultCb(path) else errorCb("No pudimos leer la foto. Inténtalo de nuevo.")
        Unit
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingCamera
        if (ok && f != null) handle(Photos.importImage(context, Uri.fromFile(f)))
        f?.delete()
        pendingCamera = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) handle(Photos.importImage(context, uri))
    }
    return PhotoPicker(
        camera = {
            try {
                val f = Photos.newCameraFile(context)
                pendingCamera = f
                cameraLauncher.launch(Photos.uriFor(context, f))
            } catch (e: Exception) {
                pendingCamera = null
                errorCb("No encontramos una app de cámara. Sube la foto desde tu galería.")
            }
            Unit
        },
        gallery = {
            try {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (e: Exception) {
                errorCb("No pudimos abrir la galería.")
            }
            Unit
        }
    )
}
