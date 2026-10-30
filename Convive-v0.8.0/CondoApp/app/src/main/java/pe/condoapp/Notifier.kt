package pe.condoapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notificaciones del teléfono (push locales).
 * Hoy se muestran en este teléfono; para que lleguen al teléfono del Super Admin
 * desde otro teléfono hace falta un servidor (Firebase Cloud Messaging).
 */
object Notifier {
    private const val CHANNEL_SUPPORT = "ayuda"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(CHANNEL_SUPPORT) == null) {
                nm?.createNotificationChannel(
                    NotificationChannel(CHANNEL_SUPPORT, "Pedidos de ayuda", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Cuando un administrador pide ayuda al Super Admin"
                    }
                )
            }
        }
    }

    fun notifySupport(context: Context, title: String, text: String) {
        if (!canNotify(context) || !Store.pushEnabled()) return
        ensureChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = open?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(context, CHANNEL_SUPPORT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: se ve igual en la campana de la app.
        }
    }
}
