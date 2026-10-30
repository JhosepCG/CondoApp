package pe.condoapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Ingreso con huella dactilar usando la biometría del teléfono (ventana flotante del sistema). */
object Biometric {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    enum class Status { READY, NOT_ENROLLED, NO_HARDWARE }

    fun status(context: Context): Status = when (BiometricManager.from(context).canAuthenticate(AUTH)) {
        BiometricManager.BIOMETRIC_SUCCESS -> Status.READY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Status.NOT_ENROLLED
        else -> Status.NO_HARDWARE
    }

    fun available(context: Context): Boolean = status(context) == Status.READY

    /** Abre los ajustes del teléfono para registrar una huella. */
    fun enrollIntent(): Intent =
        if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, AUTH)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) onError(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(AUTH)
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info)
    }
}
