package pe.condoapp

import java.time.LocalDate

/** Una sola app con tres roles. */
enum class Role(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Administrador"),
    TENANT("Inquilino")
}

enum class Template(val label: String) {
    CONDOMINIO("Condominio / edificio"),
    CASA_DEPAS("Casa por departamentos")
}

/** Funciones que el Super Admin puede activar o desactivar por cliente. */
enum class Feature(val label: String, val ready: Boolean) {
    COBROS("Cobros y fechas de pago", true),
    MORA("Recargo automático por mora", true),
    INCIDENCIAS("Reportes de servicios (internet, cable)", true),
    MEDIDORES("Fotos del medidor de luz", true),
    AVISOS("Avisos del administrador", true),
    EMERGENCIA("Botón de emergencia", true),
    AREAS_COMUNES("Áreas comunes y reservas", false),
    ASAMBLEAS("Asambleas y encuestas", false),
    VISITAS("Visitas y paquetes", false)
}

enum class IncidentStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    RESUELTO("Resuelto")
}

enum class EmergencyStatus(val label: String) {
    PENDIENTE("Esperando confirmación"),
    CONFIRMADA("Confirmada"),
    DESCARTADA("Falsa alarma"),
    CERRADA("Cerrada")
}

/** Plan de suscripción que el cliente le paga al Super Admin. */
enum class Plan(val label: String) {
    /** Plan mensual (de pago). */
    PAGO("Plan mensual"),
    ANUAL("Plan anual"),
    GRATIS("Gratis")
}

/** Plan del catálogo del Super Admin (Gratis, Plan mensual, Plan anual u otros que cree). */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: Double,
    /** Ciclo de cobro: GRATIS, PAGO (mensual) o ANUAL. */
    val cycle: Plan,
    /** Los clientes con este plan tienen todas las funciones activas. */
    val fullFeatures: Boolean = false,
    /** Plan de prueba: gratis por 30 días desde que se crea el cliente. */
    val trial: Boolean = false,
    /** El owner ve un recordatorio antes de la fecha de pago. */
    val reminder: Boolean = false
)

const val TRIAL_DAYS = 30L

/** Pedido de ayuda que un administrador envía al Super Admin. */
data class SupportRequest(
    val id: String,
    val fromUserId: String,
    val fromName: String,
    val clientId: String?,
    val clientName: String,
    val message: String,
    val createdAt: String,
    val read: Boolean = false
)

val PAYMENT_METHODS = listOf("Yape", "Plin", "Tarjeta de crédito/débito")

data class Client(
    val id: String,
    val name: String,
    val template: Template,
    /** Suscripción mensual que paga el owner al Super Admin. */
    val monthlyFee: Double,
    val active: Boolean,
    val modules: Set<Feature>,
    val lateFeePercent: Double = 5.0,
    val graceDays: Int = 3,
    /** Administrador responsable del pago de la suscripción. */
    val ownerId: String? = null,
    val plan: Plan = Plan.PAGO,
    /** Método de pago de la suscripción: Yape, Plin o tarjeta. */
    val paymentMethod: String? = null,
    /** Inicio de la suscripción (marca el ciclo del plan anual). */
    val since: LocalDate = LocalDate.now(),
    /** Plan del catálogo al que pertenece. */
    val planId: String? = null
)

/** Pago de la suscripción de un mes (period = "2026-10"). Solo lo registra el Super Admin. */
data class SubscriptionPayment(
    val id: String,
    val clientId: String,
    val period: String,
    val amount: Double,
    val paidAt: String
)

enum class SubStatus(val label: String) {
    PAGADO("Pagado"),
    PENDIENTE("Pendiente"),
    VENCIDO("Vencido"),
    GRATIS("Gratis"),
    PRUEBA("Prueba")
}

data class AppUser(
    val id: String,
    val name: String,
    val username: String,
    val password: String,
    val role: Role,
    val clientId: String?,
    val unit: String?,
    val dni: String? = null,
    val phone: String? = null,
    /** Foto de perfil guardada en el teléfono. */
    val photoPath: String? = null,
    /** Foto de portada de "Mi perfil" (si no hay, se usa la de Convive). */
    val coverPath: String? = null
)

/** Contraseña inicial de todas las cuentas nuevas. */
const val DEFAULT_PASSWORD = "1234"

data class UnitInfo(val clientId: String, val unit: String, val responsible: String)

data class Charge(
    val id: String,
    val clientId: String,
    val unit: String,
    val concept: String,
    val amount: Double,
    val due: LocalDate,
    val paid: Boolean = false
)

data class Incident(
    val id: String,
    val clientId: String,
    val userId: String,
    val userName: String,
    val unit: String,
    val service: String,
    val description: String,
    val status: IncidentStatus = IncidentStatus.PENDIENTE,
    val confirmed: Boolean? = null,
    val createdAt: String = ""
)

data class Notice(
    val id: String,
    val clientId: String,
    val category: String,
    val title: String,
    val body: String,
    val urgent: Boolean,
    val createdAt: String,
    val photoPath: String? = null
)

data class Emergency(
    val id: String,
    val clientId: String,
    val userId: String,
    val userName: String,
    val unit: String,
    val type: String,
    val status: EmergencyStatus,
    val createdAt: String
)

/** Pedido del administrador: los inquilinos deben enviar la foto de su medidor hasta [due]. */
data class MeterRequest(
    val id: String,
    val clientId: String,
    val title: String,
    val due: LocalDate,
    val createdAt: String,
    val open: Boolean = true
)

/** Foto del medidor enviada por un inquilino (la imagen se guarda en el teléfono). */
data class MeterReading(
    val id: String,
    val clientId: String,
    val requestId: String?,
    val userId: String,
    val userName: String,
    val unit: String,
    val photoPath: String,
    val reading: String,
    val sentAt: String,
    val reviewed: Boolean = false
)

/**
 * Contrato del inquilino en casas por departamentos.
 * Solo lo ven el administrador de la casa y el propio inquilino (nunca el Super Admin).
 */
data class Contract(
    val userId: String,
    val clientId: String,
    val dni: String,
    val start: LocalDate,
    val end: LocalDate,
    val guarantee: Boolean,
    val guaranteeAmount: Double,
    val monthlyRent: Double,
    val services: List<String>,
    /** El administrador le indicó al inquilino que debe firmar nuevamente. */
    val renewRequested: Boolean = false,
    val renewRequestedAt: String? = null
)

val CONTRACT_SERVICES = listOf("Agua", "Luz", "Internet", "Cable", "Gas", "Limpieza", "Cochera")

val SERVICES = listOf("Internet", "Cable", "Agua", "Luz", "Otro")
val NOTICE_CATEGORIES = listOf("Corte de agua", "Corte de luz", "Medidor de luz", "Internet / cable", "Mantenimiento", "Seguridad", "Otro")
val EMERGENCY_TYPES = listOf("Fuga de gas", "Incendio", "Robo", "Otra emergencia")
