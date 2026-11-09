package pe.condoapp

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/**
 * «Continuar con Google» con la cuenta del teléfono (Credential Manager).
 * Necesita el ID de cliente web de Google Cloud en res/values/strings.xml (google_web_client_id);
 * mientras esté vacío, la app muestra la cuenta de prueba.
 */
object GoogleAuth {
    sealed interface Result {
        data class Ok(val idToken: String, val rawNonce: String, val name: String?, val email: String) : Result
        data object Cancelled : Result
        data object NoAccounts : Result
        data class Failed(val reason: String) : Result
    }

    fun clientId(context: Context): String = context.getString(R.string.google_web_client_id).trim()

    fun configured(context: Context): Boolean = clientId(context).isNotEmpty()

    suspend fun signIn(activity: Activity): Result {
        val raw = UUID.randomUUID().toString()
        val hashed = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        val option = GetSignInWithGoogleOption.Builder(clientId(activity)).setNonce(hashed).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val c = CredentialManager.create(activity).getCredential(activity, request).credential
            if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val g = GoogleIdTokenCredential.createFrom(c.data)
                Result.Ok(g.idToken, raw, g.displayName, g.id)
            } else Result.Failed("credential ${c.type}")
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            Result.NoAccounts
        } catch (e: GetCredentialException) {
            Result.Failed(e.type)
        }
    }
}
