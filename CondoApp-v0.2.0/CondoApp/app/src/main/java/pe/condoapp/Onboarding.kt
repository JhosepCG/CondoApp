package pe.condoapp

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// Introducción de 3 pasos (distinta para administrador e inquilino)
// ------------------------------------------------------------------

class OnboardingStep(val icon: ImageVector, val title: String, val body: String, val accent: Color)

fun onboardingSteps(user: AppUser): List<OnboardingStep> {
    val c = Store.clientOf(user)
    val casa = c?.template == Template.CASA_DEPAS
    return if (user.role == Role.ADMIN) {
        listOf(
            if (casa) OnboardingStep(
                Icons.Rounded.PersonAdd,
                "Registra a tus inquilinos",
                "Guarda su nombre, DNI, fecha de inicio, garantía, pago mensual y los servicios que le das.",
                Palette.Teal
            ) else OnboardingStep(
                Icons.Rounded.PersonAdd,
                "Registra a los residentes",
                "Crea las cuentas de inquilinos y de otros administradores de tu condominio.",
                Palette.Teal
            ),
            if (casa) OnboardingStep(
                Icons.Rounded.Payments,
                "Cobra y pide los medidores",
                "Registra los pagos y pide la foto del medidor de luz con fecha límite. Busca a cualquier inquilino y mira todo en su perfil.",
                Palette.Amber
            ) else OnboardingStep(
                Icons.Rounded.Payments,
                "Controla la cobranza",
                "Genera cuotas, registra pagos y deja que la mora se calcule sola. Busca a cualquier inquilino y mira todo en su perfil.",
                Palette.Amber
            ),
            OnboardingStep(
                Icons.Rounded.Campaign,
                "Avisa y mantén el orden",
                if (casa) "Publica avisos con foto, atiende reportes y emergencias, y avisa cuándo toca firmar de nuevo el contrato."
                else "Publica avisos con foto, atiende los reportes de servicios y confirma las emergencias.",
                Palette.Navy
            )
        )
    } else {
        listOf(
            OnboardingStep(
                Icons.Rounded.Home,
                "Tu hogar en un solo lugar",
                if (casa) "Mira tu próximo pago, tu contrato y los servicios incluidos."
                else "Mira tu próximo pago y quién es el responsable de tu domicilio.",
                Palette.Teal
            ),
            if (casa) OnboardingStep(
                Icons.Rounded.PhotoCamera,
                "Envía la foto de tu medidor",
                "Cuando la administración lo pida, toma la foto o súbela de tu galería. También puedes reportar fallas.",
                Palette.Amber
            ) else OnboardingStep(
                Icons.Rounded.Build,
                "Reporta fallas",
                "Avisa si falla el internet, el cable o el agua, y confirma cuando quede resuelto.",
                Palette.Amber
            ),
            OnboardingStep(
                Icons.Rounded.NotificationsActive,
                "Avisos y emergencias",
                "Entérate de cortes y novedades, y usa el botón de emergencia si pasa algo.",
                Palette.Navy
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(user: AppUser, onDone: () -> Unit) {
    val steps = remember(user.id) { onboardingSteps(user) }
    val pager = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == steps.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Convive.", style = MaterialTheme.typography.titleLarge, color = Palette.Navy, modifier = Modifier.weight(1f))
            if (!last) {
                Text(
                    "Saltar",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onDone).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val s = steps[page]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StepIllustration(s, page + 1)
                Spacer(Modifier.height(36.dp))
                Text(
                    "PASO ${page + 1} DE ${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Teal
                )
                Spacer(Modifier.height(8.dp))
                Text(s.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    s.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            repeat(steps.size) { i ->
                val sel = pager.currentPage == i
                Box(
                    Modifier
                        .height(8.dp)
                        .width(if (sel) 26.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (sel) Palette.Navy else Palette.Line)
                        .animateContentSize()
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pager.currentPage > 0) {
                SecondaryButton(
                    "Atrás",
                    { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    Modifier.weight(1f)
                )
            }
            PrimaryButton(
                if (last) "Empezar" else "Siguiente",
                {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    Unit
                },
                Modifier.weight(if (pager.currentPage > 0) 1.6f else 1f),
                color = Palette.Navy,
                icon = if (last) Icons.Rounded.Check else null
            )
        }
    }
}

@Composable
private fun StepIllustration(s: OnboardingStep, number: Int) {
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Palette.TealSoft)
        )
        Box(
            Modifier
                .size(178.dp)
                .clip(CircleShape)
                .border(2.dp, Palette.Surface, CircleShape)
                .background(Brush.linearGradient(listOf(Palette.Navy, Palette.Teal)))
        )
        Icon(s.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(76.dp))
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-18).dp, y = 18.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(s.accent)
                .border(4.dp, Palette.Bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.titleLarge,
                color = if (s.accent == Palette.Amber) Palette.Navy else Color.White
            )
        }
    }
}
