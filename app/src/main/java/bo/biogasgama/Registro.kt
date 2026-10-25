package bo.biogasgama

const val DESARROLLADOR = "Gamaliel Huasco"

/** Un punto de levantamiento de campo; viento siempre en m/s. */
data class Registro(
    val id: Long = 0,
    val fechaHora: String,
    val latitud: Double,
    val longitud: Double,
    val altitudMsnm: Double?,
    val precisionM: Double?,
    val tempC: Double?,
    val vientoVelMs: Double?,
    val vientoDir: String?,
    val ch4Lel: Double,
    val h2sPpm: Double,
    val coPpm: Double,
    val o2VolPct: Double,
    val potencialBiogas: Boolean,
    val observaciones: String,
) {
    val ch4VolPct: Double get() = Metano.volPct(ch4Lel)
    val ch4Ppm: Double get() = Metano.ppm(ch4Lel)
}
