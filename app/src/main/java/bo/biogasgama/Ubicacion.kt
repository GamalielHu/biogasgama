package bo.biogasgama

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.location.altitude.AltitudeConverter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Posicion(
    val latitud: Double,
    val longitud: Double,
    val precisionM: Double?,
    val altitudMsnm: Double?,
    val fuenteAltitud: String,
)

/**
 * Posición en tiempo real con FusedLocationProviderClient. La altitud sobre el nivel
 * del mar se toma de la sentencia NMEA GGA del receptor GNSS; si no llega, en Android 14+
 * se convierte la altura elipsoidal con el modelo de geoide del sistema.
 */
class MonitorGnss(private val contexto: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(contexto)
    private val gestor = contexto.getSystemService(LocationManager::class.java)
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _posicion = MutableStateFlow<Posicion?>(null)
    val posicion: StateFlow<Posicion?> = _posicion

    @Volatile private var altitudNmea: Double? = null
    @Volatile private var horaNmea = 0L
    private var activo = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            resultado.lastLocation?.let { procesar(it) }
        }
    }

    private val oyenteNmea = OnNmeaMessageListener { mensaje, _ -> leerGga(mensaje) }

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (activo || !tienePermiso()) return
        activo = true
        val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        fused.requestLocationUpdates(pedido, callback, Looper.getMainLooper())
        runCatching { gestor?.addNmeaListener(oyenteNmea, Handler(Looper.getMainLooper())) }
    }

    fun detener() {
        if (!activo) return
        activo = false
        fused.removeLocationUpdates(callback)
        runCatching { gestor?.removeNmeaListener(oyenteNmea) }
    }

    private fun procesar(loc: Location) {
        ambito.launch {
            var altitud: Double? = null
            var fuente = "sin dato"
            val nmea = altitudNmea
            if (nmea != null && System.currentTimeMillis() - horaNmea < 10_000) {
                altitud = nmea
                fuente = "GNSS (geoide)"
            } else {
                val msl = if (Build.VERSION.SDK_INT >= 34) altitudGeoide(loc) else null
                if (msl != null) {
                    altitud = msl
                    fuente = "GNSS (modelo geoide)"
                } else if (loc.hasAltitude()) {
                    altitud = loc.altitude
                    fuente = "GNSS elipsoidal, ajuste manual"
                }
            }
            _posicion.value = Posicion(
                latitud = loc.latitude,
                longitud = loc.longitude,
                precisionM = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                altitudMsnm = altitud,
                fuenteAltitud = fuente,
            )
        }
    }

    @RequiresApi(34)
    private fun altitudGeoide(loc: Location): Double? = try {
        if (!loc.hasAltitude()) null
        else {
            AltitudeConverter().addMslAltitudeToLocation(contexto, loc)
            if (loc.hasMslAltitude()) loc.mslAltitudeMeters else null
        }
    } catch (e: Exception) {
        null
    }

    /** $GPGGA / $GNGGA: campo 9 = altitud de la antena sobre el nivel medio del mar. */
    private fun leerGga(mensaje: String) {
        if (mensaje.length < 6 || mensaje.substring(3, 6) != "GGA") return
        val c = mensaje.substringBefore('*').split(',')
        if (c.size < 10 || (c[6].toIntOrNull() ?: 0) == 0) return
        c[9].toDoubleOrNull()?.let {
            altitudNmea = it
            horaNmea = System.currentTimeMillis()
        }
    }
}
