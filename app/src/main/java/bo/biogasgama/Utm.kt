package bo.biogasgama

import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

/**
 * WGS 84 / UTM zona 20 Sur (EPSG:32720), serie de Krüger (precisión submilimétrica).
 * La zona es fija: puntos fuera de 66°–60° O se proyectan igual en la zona 20, como hace EPSG:32720.
 */
object Utm {
    const val SRS_UTM20S = 32720
    private const val MERIDIANO_CENTRAL = -63.0
    private const val K0 = 0.9996
    private const val FALSO_ESTE = 500_000.0
    private const val FALSO_NORTE = 10_000_000.0

    private const val A_ELIPSOIDE = 6378137.0
    private const val F = 1 / 298.257223563
    private val N = F / (2 - F)
    private val A = A_ELIPSOIDE / (1 + N) * (1 + N * N / 4 + N * N * N * N / 64)
    private val ALFA = doubleArrayOf(
        N / 2 - 2 * N * N / 3 + 5 * N * N * N / 16,
        13 * N * N / 48 - 3 * N * N * N / 5,
        61 * N * N * N / 240,
    )

    /** Devuelve (este, norte) en metros. */
    fun aUtm20S(lat: Double, lon: Double): Pair<Double, Double> {
        val phi = Math.toRadians(lat)
        val dLambda = Math.toRadians(lon - MERIDIANO_CENTRAL)
        val c = 2 * sqrt(N) / (1 + N)
        val t = sinh(atanh(sin(phi)) - c * atanh(c * sin(phi)))
        val xi = atan(t / cos(dLambda))
        val eta = atanh(sin(dLambda) / sqrt(1 + t * t))
        var e = eta
        var n = xi
        for (j in 1..3) {
            e += ALFA[j - 1] * cos(2 * j * xi) * sinh(2 * j * eta)
            n += ALFA[j - 1] * sin(2 * j * xi) * cosh(2 * j * eta)
        }
        return (FALSO_ESTE + K0 * A * e) to (FALSO_NORTE + K0 * A * n)
    }
}
