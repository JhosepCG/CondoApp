package pe.condoapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Conexión con la base de datos de Convive (Supabase): cuentas, registro, llaves y suscripción.
 * En el teléfono solo va la clave pública; todo lo sensible pasa por funciones del servidor.
 */
object Cloud {
    private const val BASE = "https://nbisetgrrbawczcnyvwg.supabase.co"
    private const val KEY = "sb_publishable_2mp2ivuN9whPxX0rMKcAfw_LN755qM6"

    /** El servidor respondió con un error (code: invalid_credentials, usada, vencida…). */
    class Failure(val code: String, val status: Int, message: String = code) : Exception(message)

    /** Sin internet o el servidor no respondió. */
    class Offline(cause: Throwable) : Exception("offline", cause)

    data class Session(val access: String, val refresh: String, val expiresAt: Long, val userId: String, val email: String?)

    /** Suscripción de la propiedad del propietario (Google Play). */
    data class Subscription(
        val status: String,
        val store: String,
        val planId: String,
        val trialEndsAt: String?,
        val periodEnd: String?,
        val autoRenewing: Boolean?
    )

    private var prefs: SharedPreferences? = null
    private val refreshLock = Mutex()

    var session: Session? = null
        private set

    var subscription by mutableStateOf<Subscription?>(null)

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("convive_cloud", Context.MODE_PRIVATE)
        prefs = p
        session = p.getString("session", null)?.let { runCatching { fromStored(JSONObject(it)) }.getOrNull() }
        subscription = p.getString("subscription", null)?.let { runCatching { parseSubscription(JSONObject(it)) }.getOrNull() }
    }

    val signedIn: Boolean get() = session != null
    val userId: String? get() = session?.userId

    /** Mensaje para mostrar cuando algo falla con la red o el servidor. */
    fun message(e: Throwable): String = when (e) {
        is Offline -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        is Failure -> when (e.code) {
            "session_expired", "no_session" -> "Tu sesión terminó. Vuelve a ingresar."
            "rate_limited", "over_email_send_rate_limit", "over_request_rate_limit" -> "Demasiados intentos. Espera unos minutos."
            else -> "No pudimos completar la acción. Inténtalo de nuevo."
        }
        else -> "Ocurrió un error. Inténtalo de nuevo."
    }

    // ---------------- Sesión ----------------

    private fun fromStored(j: JSONObject) = Session(
        j.getString("access"), j.getString("refresh"), j.getLong("exp"), j.getString("uid"), j.optString("email").ifEmpty { null }
    )

    private fun parseSession(j: JSONObject): Session {
        val user = j.optJSONObject("user")
        val exp = j.optLong("expires_at").takeIf { it > 0 } ?: (System.currentTimeMillis() / 1000 + j.optLong("expires_in", 3600))
        return Session(
            j.getString("access_token"), j.getString("refresh_token"), exp,
            user?.optString("id").orEmpty(), user?.optString("email")?.ifEmpty { null }
        )
    }

    private fun keep(s: Session?, persist: Boolean = true) {
        session = s
        prefs?.edit()?.apply {
            if (s == null || !persist) remove("session")
            else putString("session", JSONObject().put("access", s.access).put("refresh", s.refresh).put("exp", s.expiresAt)
                .put("uid", s.userId).put("email", s.email ?: "").toString())
        }?.apply()
    }

    /** Guarda (o no, si no marcó «Recordar») la sesión actual en el teléfono. */
    fun remember(persist: Boolean) = keep(session, persist)

    fun saveSubscription(j: JSONObject?) {
        subscription = j?.let { parseSubscription(it) }
        prefs?.edit()?.apply { if (j == null) remove("subscription") else putString("subscription", j.toString()) }?.apply()
    }

    private fun parseSubscription(j: JSONObject) = Subscription(
        status = j.optString("status", "prueba"),
        store = j.optString("store", "ninguna"),
        planId = j.optString("plan_id", "mensual"),
        trialEndsAt = j.optString("trial_ends_at").takeIf { it.isNotEmpty() && it != "null" },
        periodEnd = j.optString("current_period_end").takeIf { it.isNotEmpty() && it != "null" },
        autoRenewing = if (j.has("auto_renewing") && !j.isNull("auto_renewing")) j.optBoolean("auto_renewing") else null
    )

    private suspend fun accessToken(): String {
        val s = session ?: throw Failure("no_session", 401)
        val now = System.currentTimeMillis() / 1000
        if (s.expiresAt - 60 > now) return s.access
        return refreshLock.withLock {
            val cur = session ?: throw Failure("no_session", 401)
            if (cur.expiresAt - 60 > System.currentTimeMillis() / 1000) return@withLock cur.access
            try {
                val r = request("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", cur.refresh)) as JSONObject
                val next = parseSession(r)
                keep(next, prefs?.contains("session") == true)
                next.access
            } catch (e: Failure) {
                keep(null)
                throw Failure("session_expired", 401)
            }
        }
    }

    // ---------------- HTTP ----------------

    private suspend fun request(method: String, path: String, body: Any? = null, bearer: String? = null): Any? =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(BASE + path).openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 25_000
                conn.setRequestProperty("apikey", KEY)
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-Type", "application/json")
                if (bearer != null) conn.setRequestProperty("Authorization", "Bearer $bearer")
                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                conn.disconnect()
                if (code !in 200..299) throw failureOf(text, code)
                if (text.isBlank()) null else JSONTokener(text).nextValue()
            } catch (e: IOException) {
                throw Offline(e)
            }
        }

    /** Código de error de Supabase Auth, de las funciones o de la base de datos (raise exception). */
    private fun failureOf(text: String, status: Int): Failure {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return Failure("http_$status", status, text)
        val code = listOf("error_code", "error", "code").map { j.optString(it) }.firstOrNull { it.isNotEmpty() && it != "null" }
        val msg = j.optString("message").ifEmpty { j.optString("msg") }.ifEmpty { j.optString("error_description") }
        // Errores lanzados por las funciones SQL (P0001): el mensaje es el motivo («usada», «vencida»…).
        return if (code == "P0001" && msg.isNotEmpty()) Failure(msg.trim(), status, msg) else Failure(code ?: "http_$status", status, msg.ifEmpty { text })
    }

    private suspend fun rpc(name: String, args: JSONObject = JSONObject(), anon: Boolean = false): Any? =
        request("POST", "/rest/v1/rpc/$name", args, if (anon) null else accessToken())

    private suspend fun select(path: String): JSONArray =
        request("GET", "/rest/v1/$path", null, accessToken()) as? JSONArray ?: JSONArray()

    private suspend fun function(name: String, body: JSONObject, auth: Boolean): JSONObject =
        request("POST", "/functions/v1/$name", body, if (auth) accessToken() else null) as? JSONObject ?: JSONObject()

    // ---------------- Cuentas ----------------

    /** Crea la cuenta (nombre y celular van al perfil). false si el proyecto pide confirmar el correo. */
    suspend fun signUp(email: String, password: String, name: String, phone: String?): Boolean {
        val data = JSONObject().put("full_name", name)
        if (phone != null) data.put("phone", phone)
        val r = request("POST", "/auth/v1/signup", JSONObject().put("email", email).put("password", password).put("data", data)) as JSONObject
        return if (r.has("access_token")) { keep(parseSession(r)); true } else false
    }

    /** Ingreso con DNI, celular, usuario o correo. */
    suspend fun login(identifier: String, password: String, persist: Boolean) {
        val r = function("login", JSONObject().put("identifier", identifier).put("password", password), auth = false)
        keep(parseSession(r), persist)
    }

    /** Ingreso con la cuenta de Google del teléfono (token de Credential Manager). */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        val body = JSONObject().put("provider", "google").put("id_token", idToken).put("nonce", rawNonce)
        val r = request("POST", "/auth/v1/token?grant_type=id_token", body) as JSONObject
        keep(parseSession(r))
    }

    suspend fun signOut() {
        val s = session ?: return
        runCatching { request("POST", "/auth/v1/logout", null, s.access) }
        keep(null)
        saveSubscription(null)
    }

    fun forget() { keep(null); saveSubscription(null) }

    // ---------------- Recuperar contraseña (código al correo) ----------------

    /** Envía el código. Devuelve a dónde se envió (el correo, o enmascarado si se pidió por celular). */
    suspend fun recoverSend(email: String?, phone: String?): String {
        val b = JSONObject().put("action", "send")
        if (email != null) b.put("email", email) else b.put("phone", phone)
        return function("recover", b, auth = false).optString("destination", email ?: "")
    }

    /** Valida el código y devuelve una sesión temporal solo para cambiar la contraseña. */
    suspend fun recoverVerify(email: String?, phone: String?, code: String): Session {
        val b = JSONObject().put("action", "verify").put("token", code)
        if (email != null) b.put("email", email) else b.put("phone", phone)
        return parseSession(function("recover", b, auth = false))
    }

    /** Guarda la nueva contraseña y cierra las sesiones en otros teléfonos. */
    suspend fun setNewPassword(temp: Session, password: String) {
        request("PUT", "/auth/v1/user", JSONObject().put("password", password), temp.access)
        runCatching { request("POST", "/auth/v1/logout?scope=others", null, temp.access) }
        runCatching { request("POST", "/auth/v1/logout?scope=local", null, temp.access) }
    }

    // ---------------- Registro ----------------

    /** null si el correo y el celular están libres; si no, el mensaje para el campo. */
    suspend fun signupCheck(email: String, phone: String?): String? {
        val args = JSONObject().put("p_email", email).put("p_phone", phone ?: JSONObject.NULL)
        val r = rpc("signup_check", args, anon = true)
        fun taken(o: JSONObject, vararg keys: String) = keys.any { k -> o.optBoolean(k, false) }
        return when (r) {
            is JSONObject -> when {
                taken(r, "email_taken", "email_exists", "email") -> "Este correo ya tiene una cuenta."
                taken(r, "phone_taken", "phone_exists", "phone") -> "Este celular ya tiene una cuenta."
                else -> null
            }
            is String -> when {
                r.contains("email", true) || r.contains("correo", true) -> "Este correo ya tiene una cuenta."
                r.contains("phone", true) || r.contains("celular", true) -> "Este celular ya tiene una cuenta."
                else -> null
            }
            else -> null
        }
    }

    /** Perfil, propiedad y suscripción de quien ingresó. */
    suspend fun myAccount(): JSONObject = when (val r = rpc("my_account")) {
        is JSONObject -> r
        is JSONArray -> r.optJSONObject(0) ?: JSONObject()
        else -> JSONObject()
    }

    /** Propietario: crea la propiedad con sus unidades y la prueba de 2 meses. Devuelve el id de la propiedad. */
    suspend fun registerOwner(kind: String, name: String, address: String, reference: String, specs: List<UnitSpec>, annual: Boolean): String {
        val units = JSONArray()
        specs.forEach { s ->
            units.put(
                JSONObject().put("unit", s.unit).put("name", s.unit).put("rent", s.rent)
                    .put("available", s.available).put("status", if (s.available) "disponible" else "ocupado")
                    .put("services", JSONArray(s.services))
            )
        }
        val args = JSONObject().put("p_kind", kind).put("p_name", name).put("p_address", address)
            .put("p_reference", reference.ifBlank { JSONObject.NULL }).put("p_units", units).put("p_plan", if (annual) "anual" else "mensual")
        val r = rpc("register_owner", args)
        return when (r) {
            is String -> r
            is JSONObject -> r.optString("client_id").ifEmpty { r.optString("id") }
            else -> ""
        }
    }

    suspend fun units(clientId: String): JSONArray = select("units?client_id=eq.$clientId&select=*")

    /** Inquilinos vinculados a la propiedad (solo los ve su propietario). */
    suspend fun tenants(clientId: String): JSONArray = select("profiles?client_id=eq.$clientId&role=eq.TENANT&select=*")

    // ---------------- Llaves ----------------

    suspend fun previewInvitation(code: String): JSONObject = rpc("preview_invitation", JSONObject().put("p_code", code)) as? JSONObject ?: JSONObject()

    suspend fun redeemInvitation(code: String): JSONObject = rpc("redeem_invitation", JSONObject().put("p_code", code)) as? JSONObject ?: JSONObject()

    /** Propietario: genera la llave de 6 dígitos para una unidad (dura 48 horas). */
    suspend fun createInvitation(unit: String, rent: Double, payDay: Int, start: String, end: String, services: List<String>): JSONObject {
        val args = JSONObject().put("p_unit", unit).put("p_rent", rent).put("p_pay_day", payDay)
            .put("p_start", start).put("p_end", end).put("p_services", JSONArray(services))
        return rpc("create_invitation", args) as? JSONObject ?: JSONObject()
    }

    // ---------------- Google Play ----------------

    /** Envía la compra al servidor, que la confirma con Google. false si el servidor aún no está configurado. */
    suspend fun verifyPlay(purchaseToken: String, productId: String, basePlanId: String): Boolean {
        return try {
            val r = function(
                "verify-play-subscription",
                JSONObject().put("purchaseToken", purchaseToken).put("productId", productId).put("basePlanId", basePlanId),
                auth = true
            )
            saveSubscription(JSONObject(subscriptionJson()).apply {
                listOf("status", "plan_id", "current_period_end", "trial_ends_at").forEach { k -> if (r.has(k)) put(k, r.get(k)) }
                put("store", "google_play")
            })
            true
        } catch (e: Failure) {
            if (e.code == "not_configured") false else throw e
        }
    }

    private fun subscriptionJson(): String = prefs?.getString("subscription", null) ?: "{}"
}
