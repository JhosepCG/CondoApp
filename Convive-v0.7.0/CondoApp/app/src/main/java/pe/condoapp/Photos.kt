package pe.condoapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Fotos de medidores: se guardan comprimidas dentro de la app (máx. 1600 px, JPEG).
 * Cuando haya backend, aquí se subirán a la nube.
 */
object Photos {
    private const val MAX_SIDE = 1600

    private fun photoDir(context: Context): File = File(context.filesDir, "medidores").apply { mkdirs() }
    private fun cameraDir(context: Context): File = File(context.cacheDir, "camara").apply { mkdirs() }

    /** Archivo temporal donde la cámara deja la foto, y su Uri para la app de cámara. */
    fun newCameraFile(context: Context): File = File(cameraDir(context), "captura_${System.currentTimeMillis()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Copia la imagen (de la cámara o de la galería) reducida y comprimida. Devuelve la ruta o null. */
    fun importImage(context: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > MAX_SIDE || bounds.outHeight / sample > MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val out = File(photoDir(context), "medidor_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            bitmap.recycle()
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Carga una foto guardada en tamaño reducido para mostrarla. */
    fun load(path: String, maxSide: Int): ImageBitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
