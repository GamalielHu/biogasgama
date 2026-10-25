package bo.biogasgama

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Codificación de geometrías GeoPackage (cabecera GP + WKB), OGC 12-128r18 §2.1.3. */
object GeometriaGpkg {
    const val SRS_WGS84 = 4326

    fun punto(lon: Double, lat: Double, srsId: Int = SRS_WGS84): ByteArray =
        ByteBuffer.allocate(8 + 21).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('G'.code.toByte())
            put('P'.code.toByte())
            put(0)          // versión 1 del formato
            put(0x01)       // banderas: estándar, no vacía, sin envolvente, little-endian
            putInt(srsId)
            put(0x01)       // WKB little-endian
            putInt(1)       // wkbPoint
            putDouble(lon)  // X
            putDouble(lat)  // Y
        }.array()

    /** Devuelve (lon, lat) de un punto GeoPackage sin envolvente. */
    fun leerPunto(blob: ByteArray): Pair<Double, Double> {
        val b = ByteBuffer.wrap(blob)
        val banderas = blob[3].toInt()
        b.order(if (banderas and 1 == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val envolvente = when ((banderas shr 1) and 0x07) { 0 -> 0; 1 -> 32; 2, 3 -> 48; else -> 64 }
        val inicioWkb = 8 + envolvente
        b.order(if (blob[inicioWkb].toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        return b.getDouble(inicioWkb + 5) to b.getDouble(inicioWkb + 13)
    }
}
