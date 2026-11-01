package pe.convive

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteTest {
    private val base = "https://demo.supabase.co"

    @Test fun table_sinParametros() {
        assertEquals("$base/rest/v1/notices", RemoteUrls.table(base, "notices"))
    }

    @Test fun table_codificaFiltros() {
        assertEquals(
            "$base/rest/v1/charges?client_id=eq.abc&unit=eq.2%C2%B0%20Piso%20A&order=due.asc",
            RemoteUrls.table(base, "charges", linkedMapOf("client_id" to "eq.abc", "unit" to "eq.2° Piso A", "order" to "due.asc"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun table_rechazaNombresRaros() {
        RemoteUrls.table(base, "notices?select=*")
    }

    @Test fun rpc_y_token() {
        assertEquals("$base/rest/v1/rpc/login_email", RemoteUrls.rpc(base, "login_email"))
        assertEquals("$base/auth/v1/token?grant_type=password", RemoteUrls.token(base, "password"))
    }

    @Test fun authEmail() {
        assertEquals("marco@users.convive.app", RemoteUrls.authEmail(" Marco "))
    }

    @Test fun errorMessage_noFiltraDetalles() {
        assertEquals("No tienes permiso para hacer esto.", RemoteUrls.errorMessage(403))
        assertEquals("El servidor no responde. Intenta más tarde.", RemoteUrls.errorMessage(503))
        assertEquals("No pudimos conectar (error 418).", RemoteUrls.errorMessage(418))
    }
}
