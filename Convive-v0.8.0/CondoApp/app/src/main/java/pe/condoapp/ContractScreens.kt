package pe.condoapp

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ------------------------------------------------------------------
// Contrato del inquilino (solo administrador de casa e inquilino)
// ------------------------------------------------------------------

/** Estado del formulario de contrato. */
class ContractFormState(initial: Contract?) {
    var dni by mutableStateOf(initial?.dni ?: "")
    var start by mutableStateOf(initial?.start ?: LocalDate.now())
    var end by mutableStateOf(initial?.end ?: LocalDate.now().plusMonths(12))
    var rent by mutableStateOf(initial?.monthlyRent?.let { fmtNum(it) } ?: "")
    var guarantee by mutableStateOf(initial?.guarantee ?: false)
    var guaranteeAmount by mutableStateOf(initial?.guaranteeAmount?.takeIf { it > 0 }?.let { fmtNum(it) } ?: "")
    var services by mutableStateOf(initial?.services?.toSet() ?: setOf("Agua", "Luz"))
    private val keepRenew = initial?.renewRequested ?: false
    private val keepRenewAt = initial?.renewRequestedAt

    fun toContract(userId: String, clientId: String): Contract = Contract(
        userId = userId,
        clientId = clientId,
        dni = dni.trim(),
        start = start,
        end = end,
        guarantee = guarantee,
        guaranteeAmount = if (guarantee) guaranteeAmount.toDoubleOrNull() ?: 0.0 else 0.0,
        monthlyRent = rent.toDoubleOrNull() ?: 0.0,
        services = CONTRACT_SERVICES.filter { it in services } + services.filter { it !in CONTRACT_SERVICES },
        renewRequested = keepRenew,
        renewRequestedAt = keepRenewAt
    )
}

private fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

@Composable
fun rememberContractFormState(initial: Contract?): ContractFormState = remember { ContractFormState(initial) }

/** Campos del contrato: DNI, inicio, fin, renta, garantía y servicios. */
@Composable
fun ContractFields(f: ContractFormState) {
    val context = LocalContext.current
    val pickDate: (LocalDate, (LocalDate) -> Unit) -> Unit = { current, onPick ->
        DatePickerDialog(
            context,
            { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
            current.year, current.monthValue - 1, current.dayOfMonth
        ).show()
        Unit
    }

    AppTextField(
        f.dni,
        { v -> f.dni = v.filter { it.isDigit() }.take(8) },
        "DNI",
        icon = Icons.Rounded.Badge,
        keyboardType = KeyboardType.Number
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateBox("Inicio", f.start, Modifier.weight(1f)) {
            pickDate(f.start) { picked ->
                val months = ChronoUnit.MONTHS.between(f.start, f.end).coerceAtLeast(1)
                f.start = picked
                f.end = picked.plusMonths(months)
            }
        }
        DateBox("Fin del contrato", f.end, Modifier.weight(1f)) {
            pickDate(f.end) { picked -> f.end = picked }
        }
    }
    val months = ChronoUnit.MONTHS.between(f.start, f.end)
    ChoiceChips(
        listOf("6 meses", "12 meses", "24 meses"),
        when (months) {
            6L -> "6 meses"
            12L -> "12 meses"
            24L -> "24 meses"
            else -> ""
        }
    ) { label ->
        f.end = f.start.plusMonths(label.substringBefore(" ").toLong())
    }

    AppTextField(
        f.rent,
        { v -> f.rent = v.filter { it.isDigit() || it == '.' } },
        "Pago mensual (S/)",
        icon = Icons.Rounded.Payments,
        keyboardType = KeyboardType.Decimal
    )

    ToggleRow("Dejó garantía", "Monto entregado al firmar el contrato.", f.guarantee) { f.guarantee = it }
    if (f.guarantee) {
        AppTextField(
            f.guaranteeAmount,
            { v -> f.guaranteeAmount = v.filter { it.isDigit() || it == '.' } },
            "Monto de la garantía (S/)",
            icon = Icons.Rounded.Savings,
            keyboardType = KeyboardType.Decimal
        )
    }

    Text("Servicios incluidos", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
    MultiChoiceChips(CONTRACT_SERVICES, f.services) { s ->
        f.services = if (s in f.services) f.services - s else f.services + s
    }
}

@Composable
private fun DateBox(label: String, date: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .clip(shape)
            .background(Palette.Field)
            .border(1.dp, Palette.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            Text(date.format(dateFmt), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Selección múltiple con chips. */
@Composable
fun MultiChoiceChips(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { o ->
            val sel = o in selected
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) Palette.TealSoft else Palette.Surface)
                    .border(1.dp, if (sel) Palette.Teal else Palette.Line, CircleShape)
                    .clickable { onToggle(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sel) Icon(Icons.Rounded.Check, contentDescription = null, tint = Palette.Teal, modifier = Modifier.size(16.dp))
                Text(o, color = if (sel) Palette.Teal else Palette.Ink, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

class ContractBadge(val label: String, val fg: Color, val bg: Color, val icon: ImageVector)

fun contractBadge(c: Contract): ContractBadge {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), c.end)
    return when {
        Store.contractExpired(c) -> ContractBadge("Vencido", Palette.Red, Palette.RedSoft, Icons.Rounded.ErrorOutline)
        Store.contractEndingSoon(c) -> ContractBadge(
            if (days == 0L) "Vence hoy" else "Vence en $days días",
            Palette.AmberDark,
            Palette.AmberSoft,
            Icons.Rounded.Schedule
        )
        else -> ContractBadge("Vigente", Palette.Green, Palette.GreenSoft, Icons.Rounded.CheckCircle)
    }
}

/** Resumen del contrato (lo ven el administrador de la casa y el inquilino). */
@Composable
fun ContractSummary(c: Contract, showDni: Boolean) {
    val b = contractBadge(c)
    AppCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Description, Palette.Navy, Palette.NavySoft)
            Column(Modifier.weight(1f)) {
                Text("Contrato", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${c.start.format(dateFmt)} al ${c.end.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
            Pill(b.label, b.fg, b.bg, b.icon)
        }
        LineDivider()
        Row(Modifier.fillMaxWidth()) {
            MiniStat("Pago mensual", money(c.monthlyRent), Modifier.weight(1f))
            MiniStat("Garantía", if (c.guarantee) money(c.guaranteeAmount) else "No dejó", Modifier.weight(1f))
        }
        if (showDni) MiniStat("DNI", c.dni)
        if (c.services.isNotEmpty()) {
            Text("Servicios incluidos", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                c.services.forEach { s -> Pill(s, Palette.Teal, Palette.TealSoft, serviceIcon(s)) }
            }
        }
    }
}

/** Sección de contrato en el perfil del inquilino (solo administrador de casa). */
@Composable
fun ContractAdminSection(u: AppUser, c: Client) {
    val contract = Store.contractOf(u.id)
    var editing by remember { mutableStateOf(false) }
    val title = if (contract == null) null else contractBadge(contract).label
    SectionTitle("Contrato", trailing = title)

    if (editing || contract == null) {
        if (!editing) {
            AppCard {
                Text(
                    "Aún no registraste el contrato de ${u.name.substringBefore(" ")}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Muted
                )
                PrimaryButton("Registrar contrato", { editing = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
        } else {
            ContractEditor(u, c, contract) { editing = false }
        }
        return
    }

    ContractSummary(contract, showDni = true)
    RenewalBox(u, contract)
    SecondaryButton("Editar contrato", { editing = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
}

@Composable
private fun ContractEditor(u: AppUser, c: Client, contract: Contract?, onDone: () -> Unit) {
    val f = rememberContractFormState(contract)
    var error by remember { mutableStateOf<String?>(null) }
    AppCard(padding = 18.dp) {
        Text(if (contract == null) "Registrar contrato" else "Editar contrato", style = MaterialTheme.typography.titleLarge)
        Text(u.name, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        ContractFields(f)
        error?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancelar", onDone, Modifier.weight(1f))
            PrimaryButton(
                "Guardar",
                {
                    val r = Store.saveContract(f.toContract(u.id, c.id))
                    error = r
                    if (r == null) onDone()
                },
                Modifier.weight(1f)
            )
        }
    }
}

/** Indicador de renovación: checkbox + botón Enviar. El inquilino solo se entera cuando el administrador lo envía. */
@Composable
private fun RenewalBox(u: AppUser, contract: Contract) {
    val expired = Store.contractExpired(contract)
    val soon = Store.contractEndingSoon(contract)
    var mustSign by remember(contract.userId) { mutableStateOf(expired) }
    val bg = when {
        contract.renewRequested -> Palette.TealSoft
        expired -> Palette.RedSoft
        soon -> Palette.AmberSoft
        else -> Palette.Surface
    }
    AppCard(color = bg, borderColor = if (bg == Palette.Surface) Palette.Line else bg) {
        if (contract.renewRequested) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Rounded.MarkEmailRead, Palette.Teal, Palette.Surface, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Aviso de renovación enviado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${u.name.substringBefore(" ")} ya sabe que debe firmar nuevamente · ${contract.renewRequestedAt ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted
                    )
                }
            }
            Text("¿Ya firmó el nuevo contrato? Renuévalo por:", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("6 meses", { Store.renewContract(u.id, 6) }, Modifier.weight(1f))
                PrimaryButton("12 meses", { Store.renewContract(u.id, 12) }, Modifier.weight(1f), icon = Icons.Rounded.Check)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(
                    Icons.Rounded.EditNote,
                    if (expired) Palette.Red else if (soon) Palette.AmberDark else Palette.Navy,
                    if (bg == Palette.Surface) Palette.NavySoft else Palette.Surface,
                    40.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            expired -> "El contrato terminó el ${contract.end.format(dateFmt)}"
                            soon -> "El contrato termina el ${contract.end.format(dateFmt)}"
                            else -> "Renovación del contrato"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Indícale al inquilino si debe firmar nuevamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { mustSign = !mustSign },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = mustSign,
                    onCheckedChange = { mustSign = it },
                    colors = CheckboxDefaults.colors(checkedColor = Palette.Teal, uncheckedColor = Palette.Muted)
                )
                Text("Debe firmar nuevamente el contrato", style = MaterialTheme.typography.bodyLarge)
            }
            PrimaryButton(
                "Enviar al inquilino",
                { Store.requestRenewal(u.id) },
                Modifier.fillMaxWidth(),
                enabled = mustSign,
                color = Palette.Navy,
                icon = Icons.Rounded.Campaign
            )
        }
    }
}

/** Banner del inquilino cuando la administración le pide firmar nuevamente. */
@Composable
fun RenewalBanner(c: Contract) {
    AppCard(color = Palette.AmberSoft, borderColor = Palette.Amber.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.EditNote, Palette.AmberDark, Palette.Surface)
            Column(Modifier.weight(1f)) {
                Text("Debes firmar nuevamente tu contrato", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (Store.contractExpired(c)) "Tu contrato terminó el ${c.end.format(dateFmt)}. Coordina con tu administración para firmarlo."
                    else "Tu contrato termina el ${c.end.format(dateFmt)}. Coordina con tu administración para firmarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.AmberDark,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Inquilinos con contrato vencido o por vencer (para el panel del administrador de casa). */
fun contractsToReview(clientId: String): List<Pair<AppUser, Contract>> =
    Store.tenantsOf(clientId).mapNotNull { u ->
        val c = Store.contractOf(u.id) ?: return@mapNotNull null
        if (!c.renewRequested && (Store.contractExpired(c) || Store.contractEndingSoon(c))) u to c else null
    }.sortedBy { it.second.end }
