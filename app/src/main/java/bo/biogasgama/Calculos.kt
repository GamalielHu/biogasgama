package bo.biogasgama

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Conversión del canal LEL del BW GasAlertMax XT II calibrado con metano.
 * Referencia: 100 % LEL = 5,00 % vol. = 50 000 ppm.
 */
object Metano {
    fun volPct(lel: Double): Double = lel / 20.0
    fun ppm(lel: Double): Double = lel * 500.0
}

/** Umbrales configurables de la evaluación de potencial de biogás. */
data class Umbrales(
    val ch4MinLel: Double = 1.0,
    val o2MaxVol: Double = 18.0,
    val h2sMinPpm: Double = 1.0,
    val coMaxPpm: Double = 25.0,
)

data class Criterio(val texto: String, val cumple: Boolean)

data class Evaluacion(val potencial: Boolean, val criterios: List<Criterio>, val resumen: String)

/** Devuelve null mientras falte alguna de las cuatro lecturas de gas. */
fun evaluar(ch4Lel: Double?, o2Vol: Double?, h2sPpm: Double?, coPpm: Double?, u: Umbrales): Evaluacion? {
    if (ch4Lel == null || o2Vol == null || h2sPpm == null || coPpm == null) return null

    val hayCh4 = ch4Lel > 0 && ch4Lel >= u.ch4MinLel
    val anoxico = o2Vol < u.o2MaxVol
    val hayH2s = h2sPpm > 0 && h2sPpm >= u.h2sMinPpm
    val sinCombustion = coPpm <= u.coMaxPpm

    val criterios = listOf(
        Criterio("CH4 ≥ ${Formato.num(u.ch4MinLel)} % LEL (${Formato.entero(Metano.ppm(u.ch4MinLel))} ppm)", hayCh4),
        Criterio("O2 < ${Formato.num(u.o2MaxVol)} % vol. (condición anóxica)", anoxico),
        Criterio("H2S detectable (≥ ${Formato.num(u.h2sMinPpm)} ppm)", hayH2s),
        Criterio("CO ≤ ${Formato.num(u.coMaxPpm)} ppm (sin combustión)", sinCombustion),
    )
    val potencial = hayCh4 && anoxico && hayH2s && sinCombustion

    val resumen = if (potencial) {
        "Digestión anaerobia activa: CH4 en ambiente anóxico con H2S y sin CO de combustión."
    } else {
        buildList {
            if (!hayCh4) add("CH4 nulo o bajo el umbral")
            if (ch4Lel > 0 && !sinCombustion) add("CH4 atribuible a gases de combustión (CO alto)")
            else if (!sinCombustion) add("CO alto: fuente de combustión vehicular/industrial")
            if (!anoxico) add("sin condición anóxica (O2 alto)")
            if (!hayH2s) add("H2S no detectable")
        }.joinToString("; ").replaceFirstChar { it.uppercase() } + "."
    }
    return Evaluacion(potencial, criterios, resumen)
}

/** Lee números escritos con coma o punto decimal. */
fun numero(texto: String?): Double? = texto?.trim()?.replace(',', '.')?.toDoubleOrNull()

object Formato {
    private val simbolos = DecimalFormatSymbols().apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }

    private fun patron(p: String) = DecimalFormat(p, simbolos)

    /** Hasta 2 decimales, sin ceros sobrantes: 1 -> "1", 18,5 -> "18,5". */
    fun num(v: Double): String = patron("#,##0.##").format(v)

    /** Siempre 2 decimales: 0,05 / 5,00. */
    fun dos(v: Double): String = patron("#,##0.00").format(v)

    fun entero(v: Double): String = patron("#,##0.#").format(v)

    fun coord(v: Double): String = patron("0.000000").format(v)
}
