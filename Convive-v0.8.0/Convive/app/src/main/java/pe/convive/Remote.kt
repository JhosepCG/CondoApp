package pe.convive

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo de Supabase (REST + Auth) sin dependencias nuevas.
 *
 * Se activa solo si el build trae SUPABASE_URL y SUPABASE_KEY (clave publishable).
 * Si no, [enabled] es false y la app sigue funcionando con [Store] en modo local.
 *
 * Todas las funciones son bloqueantes: llámalas fuera del hilo principal
 * (por ejemplo con `withContext(Dispatchers.IO)`).
 */
object Remote {
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey: String = BuildConfig.SUPABASE_KEY

    val enabled: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    /** Sesión actual de Supabase Auth (en memoria). */
    data class Session(val userId: String, val accessToken: String, val refreshToken: String)

    @Volatile
    var session: Session? = null
        private set

    class RemoteException(val code: Int, message: String) : IOException(message)

    // ---------------- Auth ----------------

    /**
     * Ingreso con usuario o DNI. Primero se resuelve el email interno con la función
     * `login_email` (no revela si la cuenta existe) y luego se usa Supabase Auth.
     */
    fun signIn(identifier: String, password: String): Session {
        check(enabled) { "Supabase no está configurado" }
        val email = JSONArray("[" + request("POST", RemoteUrls.rpc(baseUrl, "login_email"),
            JSONObject().put("identifier", identifier.trim()).toString(), auth = false) + "]").getString(0)
        val body = JSONObject().put("email", email).put("password", password).toString()
        val res = try {
            JSONObject(request("POST", RemoteUrls.token(baseUrl, "password"), body, auth = false))
        } catch (e: RemoteException) {
            // Mismo mensaje para usuario inexistente y contraseña incorrecta.
            if (e.code == 400) throw RemoteException(400, "Usuario o contraseña incorrectos.") else throw e
        }
        val s = Session(
            userId = res.getJSONObject("user").getString("id"),
            accessToken = res.getString("access_token"),
            refreshToken = res.getString("refresh_token")
        )
        session = s
        return s
    }

    fun refresh(): Session {
        val old = session ?: throw RemoteException(401, "No hay sesión")
        val res = JSONObject(request("POST", RemoteUrls.token(baseUrl, "refresh_token"),
            JSONObject().put("refresh_token", old.refreshToken).toString(), auth = false))
        val s = old.copy(accessToken = res.getString("access_token"), refreshToken = res.getString("refresh_token"))
        session = s
        return s
    }

    fun signOut() {
        if (session != null) runCatching { request("POST", "$baseUrl/auth/v1/logout", null) }
        session = null
    }

    // ---------------- Datos (PostgREST) ----------------

    /** `select("notices", mapOf("client_id" to "eq.<id>", "order" to "created_at.desc"))` */
    fun select(table: String, params: Map<String, String> = emptyMap()): JSONArray =
        JSONArray(request("GET", RemoteUrls.table(baseUrl, table, params), null))

    fun insert(table: String, row: JSONObject): JSONArray =
        JSONArray(request("POST", RemoteUrls.table(baseUrl, table), row.toString(), prefer = "return=representation"))

    fun update(table: String, filters: Map<String, String>, changes: JSONObject): JSONArray {
        require(filters.isNotEmpty()) { "update sin filtros no está permitido" }
        return JSONArray(request("PATCH", RemoteUrls.table(baseUrl, table, filters), changes.toString(), prefer = "return=representation"))
    }

    fun delete(table: String, filters: Map<String, String>) {
        require(filters.isNotEmpty()) { "delete sin filtros no está permitido" }
        request("DELETE", RemoteUrls.table(baseUrl, table, filters), null)
    }

    // ---------------- HTTP ----------------

    private fun request(method: String, url: String, body: String?, auth: Boolean = true, prefer: String? = null): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("apikey", apiKey)
            setRequestProperty("Accept", "application/json")
            if (auth) session?.let { setRequestProperty("Authorization", "Bearer ${it.accessToken}") }
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw RemoteException(code, RemoteUrls.errorMessage(code))
            return text.ifBlank { "[]" }
        } finally {
            conn.disconnect()
        }
    }
}

/** Construcción de URLs y mensajes (sin Android ni red, para poder probarlo). */
object RemoteUrls {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun table(base: String, table: String, params: Map<String, String> = emptyMap()): String {
        require(table.matches(Regex("[a-z_]+"))) { "Nombre de tabla inválido: $table" }
        val q = params.entries.joinToString("&") { (k, v) -> enc(k) + "=" + enc(v) }
        return "$base/rest/v1/$table" + if (q.isEmpty()) "" else "?$q"
    }

    fun rpc(base: String, fn: String): String {
        require(fn.matches(Regex("[a-z_]+"))) { "Nombre de función inválido: $fn" }
        return "$base/rest/v1/rpc/$fn"
    }

    fun token(base: String, grant: String): String = "$base/auth/v1/token?grant_type=${enc(grant)}"

    /** Email interno de Supabase Auth para un usuario de Convive. */
    fun authEmail(username: String): String = username.trim().lowercase() + "@users.convive.app"

    /** Mensaje para el usuario según el código HTTP (sin filtrar detalles del servidor). */
    fun errorMessage(code: Int): String = when (code) {
        400 -> "Revisa los datos ingresados."
        401 -> "Tu sesión venció. Vuelve a ingresar."
        403 -> "No tienes permiso para hacer esto."
        404 -> "No encontramos lo que buscas."
        409 -> "Ese dato ya existe."
        429 -> "Demasiados intentos. Espera un momento."
        in 500..599 -> "El servidor no responde. Intenta más tarde."
        else -> "No pudimos conectar (error $code)."
    }
}
