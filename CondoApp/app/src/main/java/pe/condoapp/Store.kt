package pe.condoapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Almacén de datos EN MEMORIA (modo demostración).
 * Toda la lógica de negocio pasa por aquí, así que más adelante se puede
 * reemplazar por un backend (por ejemplo Supabase) sin tocar las pantallas.
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

    private var seq = 100
    fun nextId(): String = "id" + (seq++)

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))

    // ---------- Consultas ----------
    fun client(id: String?): Client? = clients.firstOrNull { it.id == id }
    fun clientOf(user: AppUser): Client? = client(user.clientId)

    fun defaultsFor(t: Template): Set<Feature> = when (t) {
        Template.CONDOMINIO -> Feature.values().toSet()
        Template.CASA_DEPAS -> setOf(
            Feature.COBROS, Feature.INCIDENCIAS, Feature.AVISOS, Feature.EMERGENCIA
        )
    }

    fun isOverdue(c: Charge): Boolean = !c.paid && LocalDate.now().isAfter(c.due)

    /** Monto a pagar; suma el recargo por mora solo si el módulo está activo. */
    fun amountDue(c: Charge): Double {
        val cl = client(c.clientId) ?: return c.amount
        val lateStart = c.due.plusDays(cl.graceDays.toLong())
        return if (!c.paid && Feature.MORA in cl.modules && LocalDate.now().isAfter(lateStart)) {
            c.amount * (1 + cl.lateFeePercent / 100.0)
        } else c.amount
    }

    fun responsibleOf(clientId: String, unit: String?): String? =
        units.firstOrNull { it.clientId == clientId && it.unit == unit }?.responsible

    // ---------- Clientes (Super Admin) ----------
    fun updateClient(c: Client) {
        val i = clients.indexOfFirst { it.id == c.id }
        if (i >= 0) clients[i] = c
    }

    fun addClient(t: Template) {
        val n = clients.size + 1
        val name = if (t == Template.CONDOMINIO) "Nuevo condominio $n" else "Nueva casa $n"
        clients.add(
            Client(
                id = nextId(), name = name, template = t,
                monthlyFee = if (t == Template.CONDOMINIO) 150.0 else 40.0,
                active = true, modules = defaultsFor(t)
            )
        )
    }

    // ---------- Cobros ----------
    fun markPaid(id: String) {
        val i = charges.indexOfFirst { it.id == id }
        if (i >= 0) charges[i] = charges[i].copy(paid = true)
    }

    fun addResident(client: Client, name: String, role: Role, unit: String, amount: Double) {
        users.add(AppUser(nextId(), name, role, client.id, unit))
        if (units.none { it.clientId == client.id && it.unit == unit }) {
            units.add(UnitInfo(client.id, unit, name))
        }
        val concept = if (client.template == Template.CONDOMINIO) "Cuota de mantenimiento" else "Renta mensual"
        charges.add(
            Charge(nextId(), client.id, unit, concept, amount, LocalDate.now().plusMonths(1).withDayOfMonth(5))
        )
    }

    // ---------- Incidencias ----------
    fun addIncident(user: AppUser, service: String, description: String) {
        val cid = user.clientId ?: return
        incidents.add(Incident(nextId(), cid, user.name, user.unit ?: "-", service, description))
    }

    fun setIncidentStatus(id: String, s: IncidentStatus) {
        val i = incidents.indexOfFirst { it.id == id }
        if (i >= 0) incidents[i] = incidents[i].copy(status = s, confirmed = null)
    }

    /** El inquilino confirma si de verdad se resolvió. Si no, se reabre. */
    fun confirmIncident(id: String, solved: Boolean) {
        val i = incidents.indexOfFirst { it.id == id }
        if (i < 0) return
        incidents[i] = if (solved) incidents[i].copy(confirmed = true)
        else incidents[i].copy(status = IncidentStatus.PENDIENTE, confirmed = null)
    }

    // ---------- Avisos ----------
    fun addNotice(clientId: String, category: String, title: String, body: String, urgent: Boolean) {
        notices.add(Notice(nextId(), clientId, category, title, body, urgent, now()))
    }

    fun removeNotice(id: String) {
        notices.removeAll { it.id == id }
    }

    // ---------- Emergencias ----------
    fun addEmergency(user: AppUser, type: String) {
        val cid = user.clientId ?: return
        emergencies.add(
            Emergency(nextId(), cid, user.name, user.unit ?: "-", type, EmergencyStatus.PENDIENTE, now())
        )
    }

    fun setEmergency(id: String, s: EmergencyStatus) {
        val i = emergencies.indexOfFirst { it.id == id }
        if (i >= 0) emergencies[i] = emergencies[i].copy(status = s)
    }

    // ---------- Datos de demostración ----------
    init {
        val c1 = Client("c1", "Condominio Los Álamos", Template.CONDOMINIO, 150.0, true, defaultsFor(Template.CONDOMINIO))
        val c2 = Client("c2", "Casa Familia Quispe (4 depas)", Template.CASA_DEPAS, 40.0, true, defaultsFor(Template.CASA_DEPAS))
        clients.addAll(listOf(c1, c2))

        users.addAll(
            listOf(
                AppUser("u0", "Jhosep (Super Admin)", Role.SUPER_ADMIN, null, null),
                AppUser("u1", "Ana Torres", Role.ADMIN, "c1", null),
                AppUser("u2", "Carlos Ramos", Role.OWNER, "c1", "Dpto 301"),
                AppUser("u3", "Lucía Vega", Role.TENANT, "c1", "Dpto 302"),
                AppUser("u4", "Pedro Quispe", Role.ADMIN, "c2", null),
                AppUser("u5", "Marco Díaz", Role.TENANT, "c2", "Depa 2")
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

        notices.add(
            Notice("n1", "c1", "Corte de agua", "Corte programado", "Mañana de 9:00 a 13:00 por mantenimiento de la red.", true, now())
        )
    }
}
