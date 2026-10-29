package pe.condoapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Almacén de datos local. Los datos se guardan en el teléfono (SharedPreferences),
 * así que ya no se borran al cerrar la app.
 * Toda la lógica pasa por aquí: para conectar un backend (por ejemplo Supabase)
 * basta con reemplazar este archivo, sin tocar las pantallas.
 */
object Store {
    val clients = mutableStateListOf<Client>()
    val users = mutableStateListOf<AppUser>()
    val units = mutableStateListOf<UnitInfo>()
    val charges = mutableStateListOf<Charge>()
    val incidents = mutableStateListOf<Incident>()
    val notices = mutableStateListOf<Notice>()
    val emergencies = mutableStateListOf<Emergency>()

    var currentUser by mutableStateOf<AppUser?>(null)
        private set

    private var prefs: SharedPreferences? = null
    private var seq = 100
    private const val DATA_VERSION = 2

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("condoapp", Context.MODE_PRIVATE)
        prefs = p
        val raw = p.getString("data", null)
        if (raw == null || !load(raw)) {
            seed()
            save()
        }
        val sid = p.getString("session", null)
        currentUser = users.firstOrNull { it.id == sid }
    }

    private fun nextId(): String = "id" + (seq++)
    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))

    // ---------------- Consultas ----------------
    fun client(id: String?): Client? = clients.firstOrNull { it.id == id }
    fun clientOf(user: AppUser): Client? = client(user.clientId)
    fun adminsOf(clientId: String): List<AppUser> = users.filter { it.clientId == clientId && it.role == Role.ADMIN }
    fun tenantsOf(clientId: String): List<AppUser> = users.filter { it.clientId == clientId && it.role == Role.TENANT }
    fun unitsOf(clientId: String): List<UnitInfo> = units.filter { it.clientId == clientId }

    fun defaultsFor(t: Template): Set<Feature> = when (t) {
        Template.CONDOMINIO -> Feature.values().toSet()
        Template.CASA_DEPAS -> setOf(Feature.COBROS, Feature.INCIDENCIAS, Feature.AVISOS, Feature.EMERGENCIA)
    }

    fun isOverdue(c: Charge): Boolean = !c.paid && LocalDate.now().isAfter(c.due)

    /** Monto a pagar; suma el recargo por mora solo si esa función está activa. */
    fun amountDue(c: Charge): Double {
        val cl = client(c.clientId) ?: return c.amount
        val lateStart = c.due.plusDays(cl.graceDays.toLong())
        return if (!c.paid && Feature.MORA in cl.modules && LocalDate.now().isAfter(lateStart)) {
            c.amount * (1 + cl.lateFeePercent / 100.0)
        } else c.amount
    }

    fun responsibleOf(clientId: String, unit: String?): String? =
        units.firstOrNull { it.clientId == clientId && it.unit == unit }?.responsible

    private fun nextDueDate(): LocalDate {
        val d = LocalDate.now().withDayOfMonth(5)
        return if (d.isAfter(LocalDate.now())) d else d.plusMonths(1)
    }

    // ---------------- Sesión ----------------
    /** Devuelve null si todo salió bien, o el mensaje de error. */
    /** Con remember = false la sesión no se guarda y se pedirá ingresar al volver a abrir la app. */
    fun login(username: String, password: String, remember: Boolean = true): String? {
        val u = users.firstOrNull { it.username.equals(username.trim(), ignoreCase = true) }
            ?: return "No encontramos ese usuario."
        if (u.password != password) return "La contraseña no es correcta."
        if (u.role != Role.SUPER_ADMIN && client(u.clientId)?.active != true) {
            return "Este condominio está desactivado. Comunícate con soporte."
        }
        currentUser = u
        if (remember) prefs?.edit()?.putString("session", u.id)?.apply()
        else prefs?.edit()?.remove("session")?.apply()
        return null
    }

    /** La administración (o el Super Admin) asigna una contraseña nueva. */
    fun resetPassword(id: String, newPassword: String) {
        if (newPassword.length < 4) return
        val i = users.indexOfFirst { it.id == id }
        if (i >= 0) {
            users[i] = users[i].copy(password = newPassword)
            save()
        }
    }

    fun logout() {
        currentUser = null
        prefs?.edit()?.remove("session")?.apply()
    }

    // ---------------- Cuentas ----------------
    fun createAccount(
        name: String,
        username: String,
        password: String,
        role: Role,
        clientId: String?,
        unit: String?,
        responsible: String?,
        amount: Double?
    ): String? {
        val n = name.trim()
        val un = username.trim().lowercase()
        if (n.isEmpty()) return "Escribe el nombre."
        if (un.length < 3 || un.contains(" ")) return "El usuario debe tener al menos 3 letras y sin espacios."
        if (users.any { it.username.equals(un, ignoreCase = true) }) return "Ese usuario ya existe."
        if (password.length < 4) return "La contraseña debe tener al menos 4 caracteres."
        val c = client(clientId)
        if (role != Role.SUPER_ADMIN && c == null) return "Elige un cliente."
        val u = unit?.trim()?.takeIf { it.isNotEmpty() }
        if (role == Role.TENANT && u == null) return "Indica el departamento o casa."

        users.add(AppUser(nextId(), n, un, password, role, c?.id, if (role == Role.TENANT) u else null))
        if (role == Role.TENANT && c != null && u != null) {
            if (units.none { it.clientId == c.id && it.unit == u }) {
                val resp = responsible?.trim()?.takeIf { it.isNotEmpty() } ?: n
                units.add(UnitInfo(c.id, u, resp))
            }
            if (amount != null && amount > 0 && Feature.COBROS in c.modules) {
                val concept = if (c.template == Template.CONDOMINIO) "Cuota de mantenimiento" else "Renta mensual"
                charges.add(Charge(nextId(), c.id, u, concept, amount, nextDueDate()))
            }
        }
        save()
        return null
    }

    fun deleteAccount(id: String) {
        if (id == currentUser?.id) return
        users.removeAll { it.id == id }
        save()
    }

    // ---------------- Clientes (Super Admin) ----------------
    fun addClient(name: String, t: Template, fee: Double) {
        clients.add(Client(nextId(), name.trim(), t, fee, true, defaultsFor(t)))
        save()
    }

    fun updateClient(c: Client) {
        val i = clients.indexOfFirst { it.id == c.id }
        if (i >= 0) {
            clients[i] = c
            save()
        }
    }

    fun deleteClient(id: String) {
        clients.removeAll { it.id == id }
        users.removeAll { it.clientId == id }
        units.removeAll { it.clientId == id }
        charges.removeAll { it.clientId == id }
        incidents.removeAll { it.clientId == id }
        notices.removeAll { it.clientId == id }
        emergencies.removeAll { it.clientId == id }
        save()
    }

    // ---------------- Cobros ----------------
    fun addCharge(clientId: String, unit: String, concept: String, amount: Double) {
        charges.add(Charge(nextId(), clientId, unit, concept.trim(), amount, nextDueDate()))
        save()
    }

    fun markPaid(id: String) {
        val i = charges.indexOfFirst { it.id == id }
        if (i >= 0) {
            charges[i] = charges[i].copy(paid = true)
            save()
        }
    }

    // ---------------- Reportes de servicios ----------------
    fun addIncident(user: AppUser, service: String, description: String) {
        val cid = user.clientId ?: return
        incidents.add(
            Incident(nextId(), cid, user.id, user.name, user.unit ?: "-", service, description.trim(), createdAt = now())
        )
        save()
    }

    fun setIncidentStatus(id: String, s: IncidentStatus) {
        val i = incidents.indexOfFirst { it.id == id }
        if (i >= 0) {
            incidents[i] = incidents[i].copy(status = s, confirmed = null)
            save()
        }
    }

    /** El inquilino confirma si de verdad se resolvió. Si no, se reabre. */
    fun confirmIncident(id: String, solved: Boolean) {
        val i = incidents.indexOfFirst { it.id == id }
        if (i < 0) return
        incidents[i] = if (solved) incidents[i].copy(confirmed = true)
        else incidents[i].copy(status = IncidentStatus.PENDIENTE, confirmed = null)
        save()
    }

    // ---------------- Avisos ----------------
    fun addNotice(clientId: String, category: String, title: String, body: String, urgent: Boolean) {
        notices.add(Notice(nextId(), clientId, category, title.trim(), body.trim(), urgent, now()))
        save()
    }

    fun removeNotice(id: String) {
        notices.removeAll { it.id == id }
        save()
    }

    // ---------------- Emergencias ----------------
    fun addEmergency(user: AppUser, type: String) {
        val cid = user.clientId ?: return
        emergencies.add(
            Emergency(nextId(), cid, user.id, user.name, user.unit ?: "-", type, EmergencyStatus.PENDIENTE, now())
        )
        save()
    }

    fun setEmergency(id: String, s: EmergencyStatus) {
        val i = emergencies.indexOfFirst { it.id == id }
        if (i >= 0) {
            emergencies[i] = emergencies[i].copy(status = s)
            save()
        }
    }

    fun resetDemo() {
        seed()
        save()
        logout()
    }

    // ---------------- Datos de prueba ----------------
    private fun seed() {
        clients.clear(); users.clear(); units.clear(); charges.clear()
        incidents.clear(); notices.clear(); emergencies.clear()
        seq = 100

        clients.addAll(
            listOf(
                Client("c1", "Condominio Los Álamos", Template.CONDOMINIO, 150.0, true, defaultsFor(Template.CONDOMINIO)),
                Client("c2", "Casa Quispe · 4 depas", Template.CASA_DEPAS, 40.0, true, defaultsFor(Template.CASA_DEPAS))
            )
        )
        users.addAll(
            listOf(
                AppUser("u0", "Jhosep", "superadmin", "1234", Role.SUPER_ADMIN, null, null),
                AppUser("u1", "Ana Torres", "ana", "1234", Role.ADMIN, "c1", null),
                AppUser("u2", "Carlos Ramos", "carlos", "1234", Role.TENANT, "c1", "Dpto 301"),
                AppUser("u3", "Lucía Vega", "lucia", "1234", Role.TENANT, "c1", "Dpto 302"),
                AppUser("u4", "Pedro Quispe", "pedro", "1234", Role.ADMIN, "c2", null),
                AppUser("u5", "Marco Díaz", "marco", "1234", Role.TENANT, "c2", "Depa 2")
            )
        )
        units.addAll(
            listOf(
                UnitInfo("c1", "Dpto 301", "Carlos Ramos"),
                UnitInfo("c1", "Dpto 302", "Carlos Ramos"),
                UnitInfo("c2", "Depa 2", "Pedro Quispe")
            )
        )
        val today = LocalDate.now()
        charges.addAll(
            listOf(
                Charge("ch1", "c1", "Dpto 301", "Cuota de mantenimiento", 180.0, today.plusDays(5)),
                Charge("ch2", "c1", "Dpto 302", "Cuota de mantenimiento", 180.0, today.minusDays(6)),
                Charge("ch3", "c2", "Depa 2", "Renta mensual", 650.0, today.plusDays(3))
            )
        )
        incidents.add(
            Incident("i1", "c1", "u3", "Lucía Vega", "Dpto 302", "Internet", "Sin señal desde la mañana.", createdAt = now())
        )
        notices.addAll(
            listOf(
                Notice("n1", "c1", "Corte de agua", "Corte de agua programado", "Mañana de 9:00 a 13:00 por mantenimiento de la red.", true, now()),
                Notice("n2", "c1", "Mantenimiento", "Fumigación de áreas comunes", "El sábado desde las 8:00. Mantén a tus mascotas dentro.", false, now())
            )
        )
    }

    // ---------------- Guardado local (JSON) ----------------
    private fun JSONObject.putNullable(k: String, v: Any?) {
        put(k, v ?: JSONObject.NULL)
    }

    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun save() {
        val p = prefs ?: return
        val o = JSONObject()
        o.put("v", DATA_VERSION)
        o.put("seq", seq)

        val ca = JSONArray()
        clients.forEach { c ->
            val j = JSONObject()
            j.put("id", c.id); j.put("name", c.name); j.put("template", c.template.name)
            j.put("fee", c.monthlyFee); j.put("active", c.active)
            j.put("late", c.lateFeePercent); j.put("grace", c.graceDays)
            val m = JSONArray()
            c.modules.forEach { f -> m.put(f.name) }
            j.put("modules", m)
            ca.put(j)
        }
        o.put("clients", ca)

        val ua = JSONArray()
        users.forEach { u ->
            val j = JSONObject()
            j.put("id", u.id); j.put("name", u.name); j.put("username", u.username)
            j.put("password", u.password); j.put("role", u.role.name)
            j.putNullable("clientId", u.clientId); j.putNullable("unit", u.unit)
            ua.put(j)
        }
        o.put("users", ua)

        val una = JSONArray()
        units.forEach { u ->
            val j = JSONObject()
            j.put("clientId", u.clientId); j.put("unit", u.unit); j.put("responsible", u.responsible)
            una.put(j)
        }
        o.put("units", una)

        val cha = JSONArray()
        charges.forEach { c ->
            val j = JSONObject()
            j.put("id", c.id); j.put("clientId", c.clientId); j.put("unit", c.unit)
            j.put("concept", c.concept); j.put("amount", c.amount)
            j.put("due", c.due.toString()); j.put("paid", c.paid)
            cha.put(j)
        }
        o.put("charges", cha)

        val ia = JSONArray()
        incidents.forEach { i ->
            val j = JSONObject()
            j.put("id", i.id); j.put("clientId", i.clientId); j.put("userId", i.userId)
            j.put("userName", i.userName); j.put("unit", i.unit); j.put("service", i.service)
            j.put("description", i.description); j.put("status", i.status.name)
            j.putNullable("confirmed", i.confirmed); j.put("createdAt", i.createdAt)
            ia.put(j)
        }
        o.put("incidents", ia)

        val na = JSONArray()
        notices.forEach { n ->
            val j = JSONObject()
            j.put("id", n.id); j.put("clientId", n.clientId); j.put("category", n.category)
            j.put("title", n.title); j.put("body", n.body); j.put("urgent", n.urgent)
            j.put("createdAt", n.createdAt)
            na.put(j)
        }
        o.put("notices", na)

        val ea = JSONArray()
        emergencies.forEach { e ->
            val j = JSONObject()
            j.put("id", e.id); j.put("clientId", e.clientId); j.put("userId", e.userId)
            j.put("userName", e.userName); j.put("unit", e.unit); j.put("type", e.type)
            j.put("status", e.status.name); j.put("createdAt", e.createdAt)
            ea.put(j)
        }
        o.put("emergencies", ea)

        p.edit().putString("data", o.toString()).apply()
    }

    private fun load(raw: String): Boolean = try {
        val o = JSONObject(raw)
        if (o.optInt("v") != DATA_VERSION) throw IllegalStateException("Versión de datos antigua")

        val cl = o.getJSONArray("clients").objects().map { j ->
            val mods = j.getJSONArray("modules")
            val names = (0 until mods.length()).map { mods.getString(it) }
            Client(
                id = j.getString("id"),
                name = j.getString("name"),
                template = Template.valueOf(j.getString("template")),
                monthlyFee = j.getDouble("fee"),
                active = j.getBoolean("active"),
                modules = Feature.values().filter { it.name in names }.toSet(),
                lateFeePercent = j.getDouble("late"),
                graceDays = j.getInt("grace")
            )
        }
        val us = o.getJSONArray("users").objects().map { j ->
            AppUser(
                j.getString("id"), j.getString("name"), j.getString("username"), j.getString("password"),
                Role.valueOf(j.getString("role")), j.str("clientId"), j.str("unit")
            )
        }
        val un = o.getJSONArray("units").objects().map { j ->
            UnitInfo(j.getString("clientId"), j.getString("unit"), j.getString("responsible"))
        }
        val ch = o.getJSONArray("charges").objects().map { j ->
            Charge(
                j.getString("id"), j.getString("clientId"), j.getString("unit"), j.getString("concept"),
                j.getDouble("amount"), LocalDate.parse(j.getString("due")), j.getBoolean("paid")
            )
        }
        val inc = o.getJSONArray("incidents").objects().map { j ->
            Incident(
                id = j.getString("id"),
                clientId = j.getString("clientId"),
                userId = j.getString("userId"),
                userName = j.getString("userName"),
                unit = j.getString("unit"),
                service = j.getString("service"),
                description = j.getString("description"),
                status = IncidentStatus.valueOf(j.getString("status")),
                confirmed = if (j.isNull("confirmed")) null else j.getBoolean("confirmed"),
                createdAt = j.optString("createdAt")
            )
        }
        val no = o.getJSONArray("notices").objects().map { j ->
            Notice(
                j.getString("id"), j.getString("clientId"), j.getString("category"), j.getString("title"),
                j.getString("body"), j.getBoolean("urgent"), j.getString("createdAt")
            )
        }
        val em = o.getJSONArray("emergencies").objects().map { j ->
            Emergency(
                j.getString("id"), j.getString("clientId"), j.getString("userId"), j.getString("userName"),
                j.getString("unit"), j.getString("type"), EmergencyStatus.valueOf(j.getString("status")),
                j.getString("createdAt")
            )
        }

        clients.clear(); clients.addAll(cl)
        users.clear(); users.addAll(us)
        units.clear(); units.addAll(un)
        charges.clear(); charges.addAll(ch)
        incidents.clear(); incidents.addAll(inc)
        notices.clear(); notices.addAll(no)
        emergencies.clear(); emergencies.addAll(em)
        seq = o.getInt("seq")
        true
    } catch (e: Exception) {
        false
    }
}
