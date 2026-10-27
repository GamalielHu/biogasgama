package bo.biogasgama

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculosTest {

    @Test
    fun conversionMetanoSegunReferencia() {
        // % LEL -> (% vol., ppm)
        val casos = mapOf(
            1.0 to (0.05 to 500.0),
            5.0 to (0.25 to 2500.0),
            10.0 to (0.50 to 5000.0),
            20.0 to (1.00 to 10000.0),
            100.0 to (5.00 to 50000.0),
        )
        casos.forEach { (lel, esperado) ->
            assertEquals(esperado.first, Metano.volPct(lel), 1e-9)
            assertEquals(esperado.second, Metano.ppm(lel), 1e-9)
        }
    }

    @Test
    fun formatoEspanol() {
        assertEquals("0,05", Formato.dos(Metano.volPct(1.0)))
        assertEquals("2.500", Formato.entero(Metano.ppm(5.0)))
        assertEquals("50.000", Formato.entero(Metano.ppm(100.0)))
        assertEquals(12.5, numero("12,5")!!, 1e-9)
    }

    @Test
    fun evaluacionPotencial() {
        val u = Umbrales()
        assertNull(evaluar(5.0, null, 3.0, 0.0, u))
        assertTrue(evaluar(5.0, 12.0, 3.0, 0.0, u)!!.potencial)
        assertFalse("sin CH4", evaluar(0.0, 12.0, 3.0, 0.0, u)!!.potencial)
        assertFalse("O2 normal", evaluar(5.0, 20.9, 3.0, 0.0, u)!!.potencial)
        assertFalse("sin H2S", evaluar(5.0, 12.0, 0.0, 0.0, u)!!.potencial)
        assertFalse("combustión", evaluar(5.0, 12.0, 3.0, 150.0, u)!!.potencial)
    }

    @Test
    fun geometriaPuntoGpkg() {
        val blob = GeometriaGpkg.punto(-68.15, -16.5)
        assertEquals(29, blob.size)
        assertEquals('G'.code.toByte(), blob[0])
        assertEquals('P'.code.toByte(), blob[1])
        val (lon, lat) = GeometriaGpkg.leerPunto(blob)
        assertEquals(-68.15, lon, 1e-12)
        assertEquals(-16.5, lat, 1e-12)
    }

    @Test
    fun utm20SComoPyproj() {
        // Valores de referencia de pyproj (EPSG:4326 -> EPSG:32720)
        val casos = listOf(
            Triple(-17.78, -63.18, 480921.2131 to 8034144.9771),
            Triple(0.0, -63.0, 500000.0 to 10000000.0),
            Triple(-22.1, -60.5, 757929.8795 to 7553986.3790),
            Triple(-16.5, -68.15, -50241.2410 to 8168719.0326),
        )
        casos.forEach { (lat, lon, esperado) ->
            val (este, norte) = Utm.aUtm20S(lat, lon)
            assertEquals(esperado.first, este, 0.001)
            assertEquals(esperado.second, norte, 0.001)
        }
    }
}
