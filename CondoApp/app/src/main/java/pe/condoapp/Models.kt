package pe.condoapp

import java.time.LocalDate

enum class Role(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Administrador"),
    USER("Inquilino")
}

enum class Template(val label: String) {
    CONDOMINIO("Condominio / edificio"),
    CASA_DEPAS("Casa por departamentos")
}

/** Funciones que el Super Admin puede activar o desactivar por cliente. */
enum class Feature(val label: String, val ready: Boolean) {
    COBROS("Cobros y fechas de pago", true),
    MORA("Recargo automático por mora", true),
    INCIDENCIAS("Servicios e incidencias (internet, cable)", true),
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

data class Client(
    val id: String,
    val name: String,
    val template: Template,
    val monthlyFee: Double,
    val active: Boolean,
    val modules: Set<Feature>,
    val lateFeePercent: Double = 5.0,
    val graceDays: Int = 3
)

data class AppUser(
    val id: String,
    val name: String,
    val role: Role,
    val clientId: String?,
    val unit: String?,
    val username: String = "",
    val password: String = ""
)

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
    val userName: String,
    val unit: String,
    val service: String,
    val description: String,
    val status: IncidentStatus = IncidentStatus.PENDIENTE,
    val confirmed: Boolean? = null
)

data class Notice(
    val id: String,
    val clientId: String,
    val category: String,
    val title: String,
    val body: String,
    val urgent: Boolean,
    val createdAt: String
)

data class Emergency(
    val id: String,
    val clientId: String,
    val userName: String,
    val unit: String,
    val type: String,
    val status: EmergencyStatus,
    val createdAt: String
)
