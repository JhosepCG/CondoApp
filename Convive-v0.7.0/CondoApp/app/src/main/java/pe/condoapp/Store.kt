package pe.condoapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
    val meterRequests = mutableStateListOf<MeterRequest>()
    val meterReadings = mutableStateListOf<MeterReading>()
    val contracts = mutableStateListOf<Contract>()
    val subscriptionPayments = mutableStateListOf<SubscriptionPayment>()
    val plans = mutableStateListOf<SubscriptionPlan>()
    val supportRequests = mutableStateListOf<SupportRequest>()

    /** Contexto de la app (para notificaciones). */
    var appContext: Context? = null
        private set
    /** Usuarios que ya vieron la introducción. */
    val onboarded = mutableStateListOf<String>()
    /** Descargas: cada persona que ingresó por primera vez a la app (id → fecha yyyy-MM-dd). */
    val activations = mutableStateMapOf<String, String>()

    var currentUser by mutableStateOf<AppUser?>(null)
        private set

    private var prefs: SharedPreferences? = null
    private var seq = 100
    private const val DATA_VERSION = 9

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
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
        Template.CONDOMINIO -> Feature.values().filter { it != Feature.MEDIDORES }.toSet()
        Template.CASA_DEPAS -> setOf(Feature.COBROS, Feature.INCIDENCIAS, Feature.MEDIDORES, Feature.AVISOS, Feature.EMERGENCIA)
    }

    /** La función de medidores solo existe para casas por departamentos. */
    fun featureOn(c: Client?, f: Feature): Boolean =
        c != null && f in c.modules && (f != Feature.MEDIDORES || c.template == Template.CASA_DEPAS)

    /** Busca inquilinos por nombre, usuario o departamento. clientId = null busca en todos. */
    fun searchTenants(query: String, clientId: String?): List<AppUser> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return users.filter { u ->
            u.role == Role.TENANT && (clientId == null || u.clientId == clientId) &&
                (u.name.lowercase().contains(q) || u.username.contains(q) || (u.unit?.lowercase()?.contains(q) == true))
        }
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
        val key = username.trim()
        // Se puede ingresar con el usuario o con el N° de DNI (inquilinos con contrato registrado).
        val u = users.firstOrNull { it.username.equals(key, ignoreCase = true) }
            ?: users.firstOrNull { it.dni == key }
            ?: contracts.firstOrNull { it.dni == key }?.let { c -> users.firstOrNull { it.id == c.userId } }
            ?: return "No encontramos ese usuario."
        if (u.password != password) return "La contraseña no es correcta."
        if (u.role != Role.SUPER_ADMIN && client(u.clientId)?.active != true) {
            return "Este condominio está desactivado. Comunícate con soporte."
        }
        currentUser = u
        markActivated(u.id)
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

    // ---------------- Tips de ayuda (se muestran una sola vez) ----------------
    private val seenTips = mutableStateListOf<String>()

    fun tipSeen(key: String): Boolean =
        key in seenTips || (prefs?.getStringSet("tips", emptySet())?.contains(key) == true)

    fun markTip(key: String) {
        if (key !in seenTips) seenTips.add(key)
        val set = (prefs?.getStringSet("tips", emptySet()) ?: emptySet()) + key
        prefs?.edit()?.putStringSet("tips", set)?.apply()
    }

    fun resetTips() {
        seenTips.clear()
        prefs?.edit()?.remove("tips")?.apply()
    }

    // ---------------- Huella dactilar ----------------
    /** Usuario que puede ingresar con su huella en este teléfono. */
    fun biometricUserId(): String? = prefs?.getString("bio_user", null)?.takeIf { id -> users.any { it.id == id } }

    fun setBiometricUser(id: String?) {
        prefs?.edit()?.apply { if (id == null) remove("bio_user") else putString("bio_user", id) }?.apply()
    }

    /** Ingreso después de validar la huella. */
    fun loginWithBiometric(): String? {
        val u = users.firstOrNull { it.id == biometricUserId() } ?: return "No encontramos tu cuenta."
        if (u.role != Role.SUPER_ADMIN && client(u.clientId)?.active != true) {
            return "Este condominio está desactivado. Comunícate con soporte."
        }
        currentUser = u
        markActivated(u.id)
        prefs?.edit()?.putString("session", u.id)?.apply()
        return null
    }

    // ---------------- Descargas (primer ingreso) ----------------
    private fun markActivated(userId: String) {
        if (userId in activations) return
        activations[userId] = LocalDate.now().toString()
        save()
    }

    /** Medio de pago → clientes que pagan con él y monto recaudado en el año. */
    data class MethodStat(val method: String, val clients: Int, val amount: Double)

    fun paymentMethodStats(year: String = LocalDate.now().year.toString()): List<MethodStat> =
        PAYMENT_METHODS.map { m ->
            val cs = clients.filter { it.active && it.plan != Plan.GRATIS && it.paymentMethod == m }
            val ids = cs.map { it.id }.toSet()
            MethodStat(m, cs.size, subscriptionPayments.filter { it.clientId in ids && it.period.contains(year) }.sumOf { it.amount })
        }

    // ---------------- Perfil ----------------
    fun updateUser(u: AppUser) {
        val i = users.indexOfFirst { it.id == u.id }
        if (i >= 0) {
            users[i] = u
            if (currentUser?.id == u.id) currentUser = u
            save()
        }
    }

    /** Nuevo Super Admin: ingresa con su DNI y la contraseña inicial es su DNI. */
    fun createSuperAdmin(name: String, dni: String, phone: String): String? {
        val n = name.trim()
        val d = dni.trim()
        val ph = phone.filter { it.isDigit() }
        if (n.isEmpty()) return "Escribe el nombre completo."
        if (d.length != 8 || !d.all { it.isDigit() }) return "El DNI debe tener 8 números."
        if (ph.length != 9) return "El celular debe tener 9 números."
        if (users.any { it.username == d || it.dni == d }) return "Ya existe un usuario con ese DNI."
        users.add(AppUser(nextId(), n, d, d, Role.SUPER_ADMIN, null, null, dni = d, phone = ph))
        save()
        return null
    }

    // ---------------- Ayuda para administradores ----------------
    fun addSupportRequest(from: AppUser, message: String): String? {
        val m = message.trim()
        if (m.length < 5) return "Cuéntanos un poco más (mínimo 5 letras)."
        val c = client(from.clientId)
        supportRequests.add(SupportRequest(nextId(), from.id, from.name, c?.id, c?.name ?: "-", m, now()))
        save()
        appContext?.let { Notifier.notifySupport(it, "${from.name} pide ayuda", "${c?.name ?: ""}: $m") }
        return null
    }

    fun unreadSupport(): Int = supportRequests.count { !it.read }

    fun markSupportRead(id: String? = null) {
        supportRequests.forEachIndexed { i, r ->
            if (!r.read && (id == null || r.id == id)) supportRequests[i] = r.copy(read = true)
        }
        save()
    }

    // ---------------- Catálogo de planes ----------------
    fun planOf(c: Client): SubscriptionPlan? =
        plans.firstOrNull { it.id == c.planId } ?: plans.firstOrNull { it.cycle == c.plan }

    fun clientsOnPlan(p: SubscriptionPlan): List<Client> = clients.filter { planOf(it)?.id == p.id }

    /** Cambia el plan de un cliente; toma el precio del plan. */
    fun assignPlan(clientId: String, p: SubscriptionPlan) {
        val i = clients.indexOfFirst { it.id == clientId }
        if (i >= 0) {
            clients[i] = clients[i].copy(planId = p.id, plan = p.cycle, monthlyFee = p.price)
            save()
        }
    }

    /** Crea o edita un plan. Si applyToClients, actualiza el monto de los clientes con ese plan. */
    fun savePlan(id: String?, name: String, price: Double?, cycle: Plan, applyToClients: Boolean): String? {
        val n = name.trim()
        if (n.isEmpty()) return "Escribe el nombre del plan."
        if (cycle != Plan.GRATIS && (price == null || price <= 0)) return "Indica el precio."
        if (plans.any { it.name.equals(n, ignoreCase = true) && it.id != id }) return "Ya existe un plan con ese nombre."
        val p = SubscriptionPlan(id ?: nextId(), n, if (cycle == Plan.GRATIS) 0.0 else price ?: 0.0, cycle)
        val i = plans.indexOfFirst { it.id == p.id }
        if (i >= 0) {
            val members = clientsOnPlan(plans[i]).map { it.id }.toSet()
            plans[i] = p
            clients.forEachIndexed { k, c ->
                if (c.id in members) {
                    clients[k] = c.copy(planId = p.id, plan = p.cycle, monthlyFee = if (applyToClients) p.price else c.monthlyFee)
                }
            }
        } else plans.add(p)
        save()
        return null
    }

    fun deletePlan(id: String): String? {
        val p = plans.firstOrNull { it.id == id } ?: return null
        if (clientsOnPlan(p).isNotEmpty()) return "Este plan tiene clientes. Cámbialos de plan antes de eliminarlo."
        plans.remove(p)
        save()
        return null
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
        amount: Double?,
        contract: Contract? = null
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
        if (contract != null) validateContract(contract)?.let { return it }

        val newId = nextId()
        users.add(AppUser(newId, n, un, password, role, c?.id, if (role == Role.TENANT) u else null))
        if (contract != null && role == Role.TENANT && c != null) {
            contracts.add(contract.copy(userId = newId, clientId = c.id))
        }
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
        contracts.removeAll { it.userId == id }
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
        meterRequests.removeAll { it.clientId == id }
        meterReadings.removeAll { it.clientId == id }
        contracts.removeAll { it.clientId == id }
        subscriptionPayments.removeAll { it.clientId == id }
        save()
    }

    // ---------------- Suscripciones (las cobra solo el Super Admin) ----------------
    /** La suscripción vence el día 5 de cada mes. */
    const val SUB_DUE_DAY = 5

    fun periodOf(d: LocalDate = LocalDate.now()): String = YearMonth.from(d).toString()

    /** Periodo de cobro: el mes (plan mensual) o el ciclo anual "A2026-10" (plan anual, desde su inicio). */
    fun periodFor(c: Client, d: LocalDate = LocalDate.now()): String {
        if (c.plan != Plan.ANUAL) return periodOf(d)
        var start = YearMonth.from(c.since)
        while (!start.plusYears(1).isAfter(YearMonth.from(d))) start = start.plusYears(1)
        return "A$start"
    }

    fun subDue(period: String): LocalDate =
        YearMonth.parse(period.removePrefix("A")).atDay(SUB_DUE_DAY)

    /** Siguiente fecha de pago después del periodo dado. */
    fun nextDue(period: String): LocalDate =
        if (period.startsWith("A")) subDue(period).plusYears(1) else subDue(period).plusMonths(1)

    fun feeLabel(c: Client): String = when (c.plan) {
        Plan.GRATIS -> "Gratis"
        Plan.ANUAL -> "${money(c.monthlyFee)}/año"
        Plan.PAGO -> "${money(c.monthlyFee)}/mes"
    }

    /** Lo que aporta al mes (el plan anual se divide entre 12). */
    fun monthlyValue(c: Client): Double = when (c.plan) {
        Plan.GRATIS -> 0.0
        Plan.ANUAL -> c.monthlyFee / 12
        Plan.PAGO -> c.monthlyFee
    }

    /** Owner: el administrador responsable de pagar. Si no hay uno marcado, el primer administrador. */
    fun ownerOf(c: Client?): AppUser? {
        if (c == null) return null
        return users.firstOrNull { it.id == c.ownerId && it.role == Role.ADMIN && it.clientId == c.id }
            ?: adminsOf(c.id).firstOrNull()
    }

    fun isOwner(u: AppUser): Boolean = u.role == Role.ADMIN && ownerOf(client(u.clientId))?.id == u.id

    fun setOwner(clientId: String, userId: String) {
        val i = clients.indexOfFirst { it.id == clientId }
        if (i >= 0) {
            clients[i] = clients[i].copy(ownerId = userId)
            save()
        }
    }

    fun setPlan(clientId: String, plan: Plan) {
        val i = clients.indexOfFirst { it.id == clientId }
        if (i >= 0) {
            clients[i] = clients[i].copy(plan = plan)
            save()
        }
    }

    fun subPayment(clientId: String, period: String): SubscriptionPayment? =
        subscriptionPayments.firstOrNull { it.clientId == clientId && it.period == period }

    fun subStatus(c: Client, period: String = periodFor(c)): SubStatus = when {
        c.plan == Plan.GRATIS -> SubStatus.GRATIS
        subPayment(c.id, period) != null -> SubStatus.PAGADO
        LocalDate.now().isAfter(subDue(period)) -> SubStatus.VENCIDO
        else -> SubStatus.PENDIENTE
    }

    /** Un solo botón: marca el mes como pagado o lo deshace. */
    fun toggleSubscriptionPaid(c: Client, period: String = periodFor(c)) {
        val existing = subPayment(c.id, period)
        if (existing != null) subscriptionPayments.remove(existing)
        else subscriptionPayments.add(SubscriptionPayment(nextId(), c.id, period, c.monthlyFee, now()))
        save()
    }

    /**
     * Botón + del Super Admin: crea el cliente y su administrador (owner).
     * El administrador ingresa con su DNI; la contraseña inicial también es su DNI.
     */
    fun createClientWithOwner(
        name: String,
        template: Template?,
        planChoice: SubscriptionPlan?,
        fee: Double?,
        paymentMethod: String?,
        modules: Set<Feature>,
        lateFee: Double,
        ownerName: String,
        dni: String,
        phone: String
    ): String? {
        val n = name.trim()
        val on = ownerName.trim()
        val d = dni.trim()
        val ph = phone.filter { it.isDigit() }
        if (n.isEmpty()) return "Escribe el nombre del condominio o casa."
        if (template == null) return "Elige la categoría."
        val pc = planChoice ?: return "Elige la suscripción."
        val plan = pc.cycle
        if (plan != Plan.GRATIS && (fee == null || fee <= 0)) return "Indica el monto de la suscripción."
        if (on.isEmpty()) return "Escribe el nombre completo."
        if (d.length != 8 || !d.all { it.isDigit() }) return "El DNI debe tener 8 números."
        if (ph.length != 9) return "El celular debe tener 9 números."
        if (users.any { it.username == d || it.dni == d }) return "Ya existe un usuario con ese DNI."
        val clientId = nextId()
        val userId = nextId()
        val mods = if (template == Template.CONDOMINIO) modules - Feature.MEDIDORES else modules
        clients.add(
            Client(
                clientId, n, template, if (plan == Plan.GRATIS) 0.0 else fee ?: 0.0, true, mods,
                lateFeePercent = lateFee, ownerId = userId, plan = plan, paymentMethod = paymentMethod,
                since = LocalDate.now(), planId = pc.id
            )
        )
        users.add(AppUser(userId, on, d, d, Role.ADMIN, clientId, null, dni = d, phone = ph))
        save()
        return null
    }

    // ---------------- Contratos (solo administrador de casa) ----------------
    fun contractOf(userId: String): Contract? = contracts.firstOrNull { it.userId == userId }

    fun contractExpired(c: Contract): Boolean = LocalDate.now().isAfter(c.end)

    /** Vence en 30 días o menos (y aún no venció). */
    fun contractEndingSoon(c: Contract): Boolean =
        !contractExpired(c) && !LocalDate.now().plusDays(30).isBefore(c.end)

    fun validateContract(c: Contract): String? = when {
        c.dni.length != 8 || !c.dni.all { it.isDigit() } -> "El DNI debe tener 8 números."
        !c.end.isAfter(c.start) -> "La fecha de fin debe ser posterior al inicio."
        c.monthlyRent <= 0 -> "Indica el monto mensual."
        c.guarantee && c.guaranteeAmount <= 0 -> "Indica el monto de la garantía."
        else -> null
    }

    fun saveContract(c: Contract): String? {
        validateContract(c)?.let { return it }
        val i = contracts.indexOfFirst { it.userId == c.userId }
        if (i >= 0) contracts[i] = c else contracts.add(c)
        save()
        return null
    }

    /** El administrador le indica al inquilino que debe firmar nuevamente el contrato. */
    fun requestRenewal(userId: String) {
        val i = contracts.indexOfFirst { it.userId == userId }
        if (i >= 0) {
            contracts[i] = contracts[i].copy(renewRequested = true, renewRequestedAt = now())
            save()
        }
    }

    /** Ya firmó: el contrato sigue desde la fecha de fin anterior por [months] meses. */
    fun renewContract(userId: String, months: Long) {
        val i = contracts.indexOfFirst { it.userId == userId }
        if (i >= 0) {
            val old = contracts[i]
            val from = if (old.end.isAfter(LocalDate.now())) old.end else LocalDate.now()
            contracts[i] = old.copy(start = from, end = from.plusMonths(months), renewRequested = false, renewRequestedAt = null)
            save()
        }
    }

    fun markOnboarded(userId: String) {
        if (userId !in onboarded) {
            onboarded.add(userId)
            save()
        }
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
    fun addNotice(clientId: String, category: String, title: String, body: String, urgent: Boolean, photoPath: String? = null) {
        notices.add(Notice(nextId(), clientId, category, title.trim(), body.trim(), urgent, now(), photoPath))
        save()
    }

    fun removeNotice(id: String) {
        notices.filter { it.id == id }.forEach { Photos.delete(it.photoPath) }
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

    // ---------------- Medidores de luz ----------------
    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** El pedido abierto más reciente del cliente (o null si no hay). */
    fun activeMeterRequest(clientId: String): MeterRequest? =
        meterRequests.lastOrNull { it.clientId == clientId && it.open }

    fun readingFor(requestId: String, userId: String): MeterReading? =
        meterReadings.lastOrNull { it.requestId == requestId && it.userId == userId }

    /** El administrador pide las fotos con fecha límite; los inquilinos reciben un aviso urgente. */
    fun addMeterRequest(clientId: String, title: String, due: LocalDate) {
        for (i in meterRequests.indices) {
            val r = meterRequests[i]
            if (r.clientId == clientId && r.open) meterRequests[i] = r.copy(open = false)
        }
        meterRequests.add(MeterRequest(nextId(), clientId, title.trim(), due, now()))
        notices.add(
            Notice(
                nextId(), clientId, "Medidor de luz", "Envía la foto de tu medidor de luz",
                "Tienes plazo hasta el ${due.format(dayFmt)}. Entra a Medidor y toma la foto o súbela desde tu galería.",
                true, now()
            )
        )
        save()
    }

    fun remindMeterRequest(r: MeterRequest) {
        notices.add(
            Notice(
                nextId(), r.clientId, "Medidor de luz", "Recordatorio: foto del medidor",
                "Si aún no la enviaste, tienes hasta el ${r.due.format(dayFmt)}.",
                true, now()
            )
        )
        save()
    }

    fun closeMeterRequest(id: String) {
        val i = meterRequests.indexOfFirst { it.id == id }
        if (i >= 0) {
            meterRequests[i] = meterRequests[i].copy(open = false)
            save()
        }
    }

    fun addMeterReading(user: AppUser, requestId: String?, photoPath: String, reading: String) {
        val cid = user.clientId ?: return
        meterReadings.add(
            MeterReading(nextId(), cid, requestId, user.id, user.name, user.unit ?: "-", photoPath, reading.trim(), now())
        )
        save()
    }

    fun setReadingReviewed(id: String) {
        val i = meterReadings.indexOfFirst { it.id == id }
        if (i >= 0) {
            meterReadings[i] = meterReadings[i].copy(reviewed = true)
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
        meterRequests.clear(); meterReadings.clear()
        contracts.clear(); onboarded.clear(); subscriptionPayments.clear(); activations.clear()
        plans.clear(); supportRequests.clear()
        plans.addAll(
            listOf(
                SubscriptionPlan("p_free", "Gratis", 0.0, Plan.GRATIS),
                SubscriptionPlan("p_mes", "Plan mensual", 30.0, Plan.PAGO),
                SubscriptionPlan("p_anual", "Plan anual", 300.0, Plan.ANUAL)
            )
        )
        seq = 100

        clients.addAll(
            listOf(
                Client("c1", "Condominio Los Álamos", Template.CONDOMINIO, 150.0, true, defaultsFor(Template.CONDOMINIO), ownerId = "u1", paymentMethod = "Tarjeta de crédito/débito", since = LocalDate.now().minusMonths(8), planId = "p_mes"),
                Client("c2", "Casa Quispe · 4 depas", Template.CASA_DEPAS, 40.0, true, defaultsFor(Template.CASA_DEPAS), ownerId = "u4", paymentMethod = "Yape", since = LocalDate.now().minusMonths(3), planId = "p_mes"),
                Client("c3", "Residencial Las Palmas", Template.CONDOMINIO, 30.0, true, defaultsFor(Template.CONDOMINIO), ownerId = "u9", paymentMethod = "Plin", since = LocalDate.now().minusMonths(2), planId = "p_mes")
            )
        )
        users.addAll(
            listOf(
                AppUser("u0", "Jhosep", "superadmin", "1234", Role.SUPER_ADMIN, null, null),
                AppUser("u1", "Ana Torres", "ana", "1234", Role.ADMIN, "c1", null, dni = "41234567", phone = "987111222"),
                AppUser("u2", "Carlos Ramos", "carlos", "1234", Role.TENANT, "c1", "Dpto 301"),
                AppUser("u3", "Lucía Vega", "lucia", "1234", Role.TENANT, "c1", "Dpto 302"),
                AppUser("u4", "Pedro Quispe", "pedro", "1234", Role.ADMIN, "c2", null, dni = "87654321", phone = "987654321"),
                AppUser("u5", "Marco Díaz", "marco", "1234", Role.TENANT, "c2", "Depa 2"),
                AppUser("u6", "Rosa Huamán", "rosa", "1234", Role.TENANT, "c2", "Depa 1"),
                AppUser("u7", "Diego Salas", "diego", "1234", Role.TENANT, "c2", "Depa 3"),
                AppUser("u8", "Sofía Quispe", "sofia", "1234", Role.ADMIN, "c2", null, dni = "76543210", phone = "912345678"),
                AppUser("u9", "Luis Rojas", "luis", "1234", Role.ADMIN, "c3", null, dni = "45678912", phone = "934567812")
            )
        )
        units.addAll(
            listOf(
                UnitInfo("c1", "Dpto 301", "Carlos Ramos"),
                UnitInfo("c1", "Dpto 302", "Carlos Ramos"),
                UnitInfo("c2", "Depa 1", "Pedro Quispe"),
                UnitInfo("c2", "Depa 2", "Pedro Quispe"),
                UnitInfo("c2", "Depa 3", "Pedro Quispe")
            )
        )
        val today = LocalDate.now()
        charges.addAll(
            listOf(
                Charge("ch1", "c1", "Dpto 301", "Cuota de mantenimiento", 180.0, today.plusDays(5)),
                Charge("ch2", "c1", "Dpto 302", "Cuota de mantenimiento", 180.0, today.minusDays(6)),
                Charge("ch3", "c2", "Depa 2", "Renta mensual", 650.0, today.plusDays(3)),
                Charge("ch4", "c2", "Depa 1", "Renta mensual", 600.0, today.minusDays(2), paid = true),
                Charge("ch5", "c2", "Depa 3", "Renta mensual", 700.0, today.minusDays(4)),
                Charge("ch6", "c1", "Dpto 301", "Cuota de mantenimiento", 180.0, today.minusDays(30), paid = true),
                Charge("ch7", "c1", "Dpto 302", "Cuota de mantenimiento", 180.0, today.minusDays(36), paid = true)
            )
        )
        incidents.add(
            Incident("i1", "c1", "u3", "Lucía Vega", "Dpto 302", "Internet", "Sin señal desde la mañana.", createdAt = now())
        )
        incidents.add(
            Incident("i2", "c2", "u7", "Diego Salas", "Depa 3", "Agua", "Poca presión en la ducha.", createdAt = now())
        )
        notices.addAll(
            listOf(
                Notice("n1", "c1", "Corte de agua", "Corte de agua programado", "Mañana de 9:00 a 13:00 por mantenimiento de la red.", true, now()),
                Notice("n2", "c1", "Mantenimiento", "Fumigación de áreas comunes", "El sábado desde las 8:00. Mantén a tus mascotas dentro.", false, now())
            )
        )
        contracts.addAll(
            listOf(
                Contract("u5", "c2", "70123456", today.minusMonths(11), today.plusDays(20), true, 650.0, 650.0, listOf("Agua", "Luz", "Internet")),
                Contract("u6", "c2", "45678912", today.minusMonths(13), today.minusDays(15), true, 600.0, 600.0, listOf("Agua", "Internet", "Cable")),
                Contract("u7", "c2", "72345678", today.minusMonths(3), today.plusMonths(9), false, 0.0, 700.0, listOf("Agua", "Luz"))
            )
        )
        // Suscripciones: Los Álamos pagó los últimos 3 meses; Casa Quispe debe el mes actual.
        val ym = YearMonth.from(today)
        (1L..3L).forEach { k ->
            val p = ym.minusMonths(k - 1).toString()
            subscriptionPayments.add(SubscriptionPayment("sp1$k", "c1", p, 150.0, "05/" + ym.minusMonths(k - 1).monthValue.toString().padStart(2, '0')))
        }
        (2L..3L).forEach { k ->
            val p = ym.minusMonths(k - 1).toString()
            subscriptionPayments.add(SubscriptionPayment("sp2$k", "c2", p, 40.0, "04/" + ym.minusMonths(k - 1).monthValue.toString().padStart(2, '0')))
        }
        (1L..2L).forEach { k ->
            val p = ym.minusMonths(k - 1).toString()
            subscriptionPayments.add(SubscriptionPayment("sp3$k", "c3", p, 30.0, "03/" + ym.minusMonths(k - 1).monthValue.toString().padStart(2, '0')))
        }
        // Descargas de ejemplo: quién ya ingresó a la app y cuándo (Diego aún no la descarga).
        mapOf("u1" to 21L, "u2" to 13L, "u3" to 6L, "u4" to 4L, "u5" to 2L, "u6" to 1L, "u8" to 1L, "u9" to 0L)
            .forEach { (id, d) -> activations[id] = today.minusDays(d).toString() }
        supportRequests.add(
            SupportRequest("sr1", "u4", "Pedro Quispe", "c2", "Casa Quispe · 4 depas",
                "¿Cómo registro a un nuevo inquilino con su contrato?", now())
        )
        val month = today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("es", "PE"))
        addMeterRequest("c2", "Medidor de luz · $month", today.plusDays(3))
        notices.add(Notice("n3", "c2", "Corte de luz", "Corte de luz el jueves", "Luz del Sur cortará el servicio de 8:00 a 12:00. Adjunto el comunicado.", false, now()))
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
            j.putNullable("ownerId", c.ownerId); j.put("plan", c.plan.name)
            j.putNullable("payMethod", c.paymentMethod); j.put("since", c.since.toString())
            j.putNullable("planId", c.planId)
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
            j.putNullable("dni", u.dni); j.putNullable("phone", u.phone); j.putNullable("photo", u.photoPath)
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
            j.put("createdAt", n.createdAt); j.putNullable("photoPath", n.photoPath)
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

        val mra = JSONArray()
        meterRequests.forEach { r ->
            val j = JSONObject()
            j.put("id", r.id); j.put("clientId", r.clientId); j.put("title", r.title)
            j.put("due", r.due.toString()); j.put("createdAt", r.createdAt); j.put("open", r.open)
            mra.put(j)
        }
        o.put("meterRequests", mra)

        val mla = JSONArray()
        meterReadings.forEach { m ->
            val j = JSONObject()
            j.put("id", m.id); j.put("clientId", m.clientId); j.putNullable("requestId", m.requestId)
            j.put("userId", m.userId); j.put("userName", m.userName); j.put("unit", m.unit)
            j.put("photoPath", m.photoPath); j.put("reading", m.reading); j.put("sentAt", m.sentAt)
            j.put("reviewed", m.reviewed)
            mla.put(j)
        }
        o.put("meterReadings", mla)

        val cta = JSONArray()
        contracts.forEach { c ->
            val j = JSONObject()
            j.put("userId", c.userId); j.put("clientId", c.clientId); j.put("dni", c.dni)
            j.put("start", c.start.toString()); j.put("end", c.end.toString())
            j.put("guarantee", c.guarantee); j.put("guaranteeAmount", c.guaranteeAmount)
            j.put("rent", c.monthlyRent)
            val sv = JSONArray()
            c.services.forEach { sv.put(it) }
            j.put("services", sv)
            j.put("renewRequested", c.renewRequested); j.putNullable("renewRequestedAt", c.renewRequestedAt)
            cta.put(j)
        }
        o.put("contracts", cta)

        val spa = JSONArray()
        subscriptionPayments.forEach { sp ->
            val j = JSONObject()
            j.put("id", sp.id); j.put("clientId", sp.clientId); j.put("period", sp.period)
            j.put("amount", sp.amount); j.put("paidAt", sp.paidAt)
            spa.put(j)
        }
        o.put("subscriptionPayments", spa)

        val oba = JSONArray()
        onboarded.forEach { oba.put(it) }
        o.put("onboarded", oba)

        val act = JSONObject()
        activations.forEach { (k, v) -> act.put(k, v) }
        o.put("activations", act)

        val pla = JSONArray()
        plans.forEach { p ->
            val j = JSONObject()
            j.put("id", p.id); j.put("name", p.name); j.put("price", p.price); j.put("cycle", p.cycle.name)
            pla.put(j)
        }
        o.put("plans", pla)

        val sra = JSONArray()
        supportRequests.forEach { r ->
            val j = JSONObject()
            j.put("id", r.id); j.put("from", r.fromUserId); j.put("fromName", r.fromName)
            j.putNullable("clientId", r.clientId); j.put("clientName", r.clientName)
            j.put("message", r.message); j.put("createdAt", r.createdAt); j.put("read", r.read)
            sra.put(j)
        }
        o.put("support", sra)

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
                graceDays = j.getInt("grace"),
                ownerId = j.str("ownerId"),
                plan = runCatching { Plan.valueOf(j.getString("plan")) }.getOrDefault(Plan.PAGO),
                paymentMethod = if (j.has("payMethod")) j.str("payMethod") else null,
                planId = if (j.has("planId")) j.str("planId") else null,
                since = runCatching { LocalDate.parse(j.getString("since")) }.getOrDefault(LocalDate.now())
            )
        }
        val us = o.getJSONArray("users").objects().map { j ->
            AppUser(
                j.getString("id"), j.getString("name"), j.getString("username"), j.getString("password"),
                Role.valueOf(j.getString("role")), j.str("clientId"), j.str("unit"),
                if (j.has("dni")) j.str("dni") else null, if (j.has("phone")) j.str("phone") else null,
                if (j.has("photo")) j.str("photo") else null
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
                j.getString("body"), j.getBoolean("urgent"), j.getString("createdAt"), j.str("photoPath")
            )
        }
        val em = o.getJSONArray("emergencies").objects().map { j ->
            Emergency(
                j.getString("id"), j.getString("clientId"), j.getString("userId"), j.getString("userName"),
                j.getString("unit"), j.getString("type"), EmergencyStatus.valueOf(j.getString("status")),
                j.getString("createdAt")
            )
        }
        val mr = (o.optJSONArray("meterRequests") ?: JSONArray()).objects().map { j ->
            MeterRequest(
                j.getString("id"), j.getString("clientId"), j.getString("title"),
                LocalDate.parse(j.getString("due")), j.getString("createdAt"), j.getBoolean("open")
            )
        }
        val ml = (o.optJSONArray("meterReadings") ?: JSONArray()).objects().map { j ->
            MeterReading(
                id = j.getString("id"),
                clientId = j.getString("clientId"),
                requestId = j.str("requestId"),
                userId = j.getString("userId"),
                userName = j.getString("userName"),
                unit = j.getString("unit"),
                photoPath = j.getString("photoPath"),
                reading = j.optString("reading"),
                sentAt = j.getString("sentAt"),
                reviewed = j.optBoolean("reviewed")
            )
        }

        val ct = (o.optJSONArray("contracts") ?: JSONArray()).objects().map { j ->
            val sv = j.getJSONArray("services")
            Contract(
                userId = j.getString("userId"),
                clientId = j.getString("clientId"),
                dni = j.getString("dni"),
                start = LocalDate.parse(j.getString("start")),
                end = LocalDate.parse(j.getString("end")),
                guarantee = j.getBoolean("guarantee"),
                guaranteeAmount = j.getDouble("guaranteeAmount"),
                monthlyRent = j.getDouble("rent"),
                services = (0 until sv.length()).map { sv.getString(it) },
                renewRequested = j.optBoolean("renewRequested"),
                renewRequestedAt = j.str("renewRequestedAt")
            )
        }
        val ob = o.optJSONArray("onboarded") ?: JSONArray()
        val pl = (o.optJSONArray("plans") ?: JSONArray()).objects().map { j ->
            SubscriptionPlan(j.getString("id"), j.getString("name"), j.getDouble("price"), Plan.valueOf(j.getString("cycle")))
        }
        val sr = (o.optJSONArray("support") ?: JSONArray()).objects().map { j ->
            SupportRequest(
                j.getString("id"), j.getString("from"), j.getString("fromName"), j.str("clientId"),
                j.getString("clientName"), j.getString("message"), j.getString("createdAt"), j.optBoolean("read")
            )
        }
        val sps = (o.optJSONArray("subscriptionPayments") ?: JSONArray()).objects().map { j ->
            SubscriptionPayment(j.getString("id"), j.getString("clientId"), j.getString("period"), j.getDouble("amount"), j.getString("paidAt"))
        }

        clients.clear(); clients.addAll(cl)
        users.clear(); users.addAll(us)
        units.clear(); units.addAll(un)
        charges.clear(); charges.addAll(ch)
        incidents.clear(); incidents.addAll(inc)
        notices.clear(); notices.addAll(no)
        emergencies.clear(); emergencies.addAll(em)
        meterRequests.clear(); meterRequests.addAll(mr)
        meterReadings.clear(); meterReadings.addAll(ml)
        contracts.clear(); contracts.addAll(ct)
        subscriptionPayments.clear(); subscriptionPayments.addAll(sps)
        onboarded.clear(); onboarded.addAll((0 until ob.length()).map { ob.getString(it) })
        activations.clear()
        o.optJSONObject("activations")?.let { a -> a.keys().forEach { k -> activations[k] = a.getString(k) } }
        plans.clear(); plans.addAll(pl)
        supportRequests.clear(); supportRequests.addAll(sr)
        seq = o.getInt("seq")
        true
    } catch (e: Exception) {
        false
    }
}
