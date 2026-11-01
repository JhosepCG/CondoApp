package pe.condoapp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas de la lógica de [Store] en la JVM.
 * Sin llamar a Store.init(), save() no escribe nada: cada prueba arma sus propios datos.
 */
class StoreLogicTest {

    private val condo = Client("c1", "Condominio Los Álamos", Template.CONDOMINIO, 150.0, true,
        setOf(Feature.COBROS, Feature.MORA, Feature.AVISOS), lateFeePercent = 5.0, graceDays = 3)
    private val casa = Client("c2", "Casa Quispe", Template.CASA_DEPAS, 40.0, true,
        setOf(Feature.COBROS, Feature.MEDIDORES))
    private val apagado = Client("c3", "Edificio Cerrado", Template.CONDOMINIO, 0.0, false, emptySet())

    @Before
    fun setUp() {
        clearStore()
        Store.clients.addAll(listOf(condo, casa, apagado))
        Store.users.addAll(
            listOf(
                AppUser("u0", "Jhosep", "jhosep", "1234", Role.SUPER_ADMIN, null, null),
                AppUser("u1", "Ana Torres", "ana", "clave", Role.ADMIN, "c1", null, dni = "41234567"),
                AppUser("u2", "Carlos Ramos", "carlos", "1234", Role.TENANT, "c1", "Dpto 301"),
                AppUser("u3", "Lucía Vega", "lucia", "1234", Role.TENANT, "c1", "Dpto 302", phone = "987111222"),
                AppUser("u5", "Marco Díaz", "marco", "1234", Role.TENANT, "c2", "2° Piso B"),
                AppUser("u6", "Rosa Huamán", "rosa", "1234", Role.TENANT, "c2", "2° Piso A"),
                AppUser("u7", "Diego Salas", "diego", "1234", Role.TENANT, "c2", "3° Piso A"),
                AppUser("u9", "Iván Pérez", "ivan", "1234", Role.TENANT, "c3", "Dpto 101")
            )
        )
        Store.contracts.add(contract("u5", dni = "70123456"))
    }

    @After
    fun tearDown() {
        Store.logout()
        clearStore()
    }

    private fun clearStore() {
        Store.clients.clear(); Store.users.clear(); Store.units.clear(); Store.charges.clear()
        Store.contracts.clear(); Store.activations.clear()
    }

    private fun contract(
        userId: String,
        dni: String = "70123456",
        start: LocalDate = LocalDate.now().minusMonths(6),
        end: LocalDate = LocalDate.now().plusMonths(6),
        rent: Double = 600.0,
        guarantee: Boolean = false,
        guaranteeAmount: Double = 0.0
    ) = Contract(userId, "c2", dni, start, end, guarantee, guaranteeAmount, rent, listOf("Agua"))

    // ---------------- Ingreso ----------------

    @Test fun login_conUsuario_ignoraMayusculasYEspacios() {
        assertNull(Store.login("  CARLOS ", "1234"))
        assertEquals("u2", Store.currentUser?.id)
    }

    @Test fun login_conDniDelPerfil() {
        assertNull(Store.login("41234567", "clave"))
        assertEquals("u1", Store.currentUser?.id)
    }

    @Test fun login_conDniDelContrato() {
        assertNull(Store.login("70123456", "1234"))
        assertEquals("u5", Store.currentUser?.id)
    }

    @Test fun login_contrasenaIncorrecta() {
        assertEquals("La contraseña no es correcta.", Store.login("ana", "1234"))
        assertNull(Store.currentUser)
    }

    @Test fun login_usuarioInexistente() {
        assertEquals("No encontramos ese usuario.", Store.login("nadie", "1234"))
    }

    @Test fun login_clienteDesactivado_bloqueaAlInquilino() {
        assertEquals("Este condominio está desactivado. Comunícate con soporte.", Store.login("ivan", "1234"))
        assertNull(Store.currentUser)
    }

    @Test fun login_superAdmin_noDependeDeUnCliente() {
        assertNull(Store.login("jhosep", "1234"))
        assertEquals(Role.SUPER_ADMIN, Store.currentUser?.role)
    }

    @Test fun login_registraLaPrimeraActivacion() {
        Store.login("carlos", "1234")
        assertEquals(LocalDate.now().toString(), Store.activations["u2"])
    }

    // ---------------- Usuario sugerido ----------------

    @Test fun suggestUsername_quitaTildesYUsaElPrimerNombre() {
        assertEquals("angel", Store.suggestUsername("Ángel Núñez"))
    }

    @Test fun suggestUsername_numeraSiYaExiste() {
        assertEquals("carlos2", Store.suggestUsername("Carlos Mendoza"))
    }

    @Test fun suggestUsername_ignoraLaPropiaCuenta() {
        assertEquals("carlos", Store.suggestUsername("Carlos Ramos", exceptId = "u2"))
    }

    @Test fun suggestUsername_nombreVacio() {
        assertEquals("usuario", Store.suggestUsername("   "))
    }

    // ---------------- Búsqueda inteligente ----------------

    private fun search(q: String, clientId: String? = null) = Store.searchTenants(q, clientId).map { it.username }

    @Test fun search_ignoraTildes() {
        assertEquals(listOf("lucia"), search("lucia"))
    }

    @Test fun search_entiendePisosEscritosDeVariasFormas() {
        val esperado = setOf("marco", "rosa")
        assertEquals(esperado, search("segundo piso").toSet())
        assertEquals(esperado, search("2do piso").toSet())
        assertEquals(esperado, search("2° piso").toSet())
    }

    @Test fun search_porDepartamento() {
        assertEquals(listOf("carlos"), search("dpto 301"))
        assertEquals(listOf("carlos"), search("departamento 301"))
    }

    @Test fun search_porDniDelContratoYCelular() {
        assertEquals(listOf("marco"), search("70123456"))
        assertEquals(listOf("lucia"), search("987111"))
    }

    @Test fun search_toleraUnErrorDeTipeo() {
        assertEquals(listOf("rosa"), search("rossa"))
    }

    @Test fun search_filtraPorCliente() {
        assertTrue(search("piso", clientId = "c1").isEmpty())
        assertEquals(3, search("piso", clientId = "c2").size)
    }

    @Test fun search_noIncluyeAdministradores() {
        assertTrue(search("ana").isEmpty())
    }

    @Test fun search_vacio() {
        assertTrue(search("   ").isEmpty())
    }

    // ---------------- Cobros y mora ----------------

    private fun charge(clientId: String, due: LocalDate, paid: Boolean = false) =
        Charge("ch", clientId, "Dpto 301", "Cuota", 100.0, due, paid)

    @Test fun amountDue_sumaMoraDespuesDeLosDiasDeGracia() {
        assertEquals(105.0, Store.amountDue(charge("c1", LocalDate.now().minusDays(10))), 0.001)
    }

    @Test fun amountDue_sinMoraDentroDeLosDiasDeGracia() {
        assertEquals(100.0, Store.amountDue(charge("c1", LocalDate.now().minusDays(2))), 0.001)
    }

    @Test fun amountDue_sinMoraSiLaFuncionEstaApagada() {
        assertEquals(100.0, Store.amountDue(charge("c2", LocalDate.now().minusDays(30))), 0.001)
    }

    @Test fun amountDue_pagadoNoTieneMora() {
        assertEquals(100.0, Store.amountDue(charge("c1", LocalDate.now().minusDays(30), paid = true)), 0.001)
    }

    @Test fun isOverdue() {
        assertTrue(Store.isOverdue(charge("c1", LocalDate.now().minusDays(1))))
        assertTrue(!Store.isOverdue(charge("c1", LocalDate.now())))
    }

    // ---------------- Funciones por cliente ----------------

    @Test fun featureOn_medidoresSoloEnCasas() {
        assertTrue(Store.featureOn(casa, Feature.MEDIDORES))
        assertTrue(!Store.featureOn(condo.copy(modules = condo.modules + Feature.MEDIDORES), Feature.MEDIDORES))
        assertTrue(!Store.featureOn(null, Feature.AVISOS))
    }

    // ---------------- Contratos ----------------

    @Test fun validateContract_valido() {
        assertNull(Store.validateContract(contract("u5")))
    }

    @Test fun validateContract_errores() {
        assertEquals("El DNI debe tener 8 números.", Store.validateContract(contract("u5", dni = "1234")))
        assertEquals("El DNI debe tener 8 números.", Store.validateContract(contract("u5", dni = "1234567a")))
        val hoy = LocalDate.now()
        assertEquals("La fecha de fin debe ser posterior al inicio.", Store.validateContract(contract("u5", start = hoy, end = hoy)))
        assertEquals("Indica el monto mensual.", Store.validateContract(contract("u5", rent = 0.0)))
        assertEquals("Indica el monto de la garantía.", Store.validateContract(contract("u5", guarantee = true)))
    }

    @Test fun contractEndingSoon_y_expired() {
        val pronto = contract("u5", end = LocalDate.now().plusDays(10))
        val vencido = contract("u5", end = LocalDate.now().minusDays(1))
        assertTrue(Store.contractEndingSoon(pronto))
        assertTrue(Store.contractExpired(vencido))
        assertTrue(!Store.contractEndingSoon(vencido))
        assertNotNull(Store.contractOf("u5"))
    }
}
