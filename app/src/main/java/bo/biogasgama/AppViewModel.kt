package bo.biogasgama

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DIRECCIONES = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val gnss = MonitorGnss(app)
    private val gpkg = GeoPackage(File(app.filesDir, "biogasgama.gpkg"))
    private val prefs = app.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    // Formulario de captura (texto tal como lo escribe el usuario)
    var ch4Lel by mutableStateOf("")
    var h2sPpm by mutableStateOf("")
    var coPpm by mutableStateOf("")
    var o2Vol by mutableStateOf("")
    /** null = usar la altitud del GNSS; texto = ajuste manual. */
    var altitudManual by mutableStateOf<String?>(null)
    var tempC by mutableStateOf("")
    var viento by mutableStateOf("")
    var vientoKmh by mutableStateOf(false)
    var vientoDir by mutableStateOf("")
    var observaciones by mutableStateOf("")

    var umbrales by mutableStateOf(cargarUmbrales())
        private set
    var registros by mutableStateOf<List<Registro>>(emptyList())
        private set
    var aviso by mutableStateOf<String?>(null)

    init {
        refrescar()
    }

    fun evaluacion(): Evaluacion? = evaluar(numero(ch4Lel), numero(o2Vol), numero(h2sPpm), numero(coPpm), umbrales)

    fun refrescar() {
        viewModelScope.launch {
            registros = withContext(Dispatchers.IO) { gpkg.listar() }
        }
    }

    fun guardar(pos: Posicion) {
        val ev = evaluacion() ?: return
        val vel = numero(viento)?.let { if (vientoKmh) it / 3.6 else it }
        val registro = Registro(
            fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            latitud = pos.latitud,
            longitud = pos.longitud,
            altitudMsnm = altitudManual?.let { numero(it) } ?: pos.altitudMsnm,
            precisionM = pos.precisionM,
            tempC = numero(tempC),
            vientoVelMs = vel,
            vientoDir = vientoDir.ifBlank { null },
            ch4Lel = numero(ch4Lel)!!,
            h2sPpm = numero(h2sPpm)!!,
            coPpm = numero(coPpm)!!,
            o2VolPct = numero(o2Vol)!!,
            potencialBiogas = ev.potencial,
            observaciones = observaciones.trim(),
        )
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { gpkg.insertar(registro) }
            aviso = "Punto $id guardado: potencial ${if (ev.potencial) "SÍ" else "NO"}"
            ch4Lel = ""; h2sPpm = ""; coPpm = ""; o2Vol = ""; observaciones = ""
            refrescar()
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { gpkg.borrar(id) }
            refrescar()
        }
    }

    fun nombreExportacion(): String =
        "BiogasGama_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".gpkg"

    /** Guarda el .gpkg en la ubicación elegida con el selector del sistema (SAF). */
    fun exportarA(uri: Uri) {
        viewModelScope.launch {
            aviso = try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { gpkg.copiarA(it) }
                }
                "GeoPackage guardado (${registros.size} puntos)"
            } catch (e: Exception) {
                "No se pudo guardar: ${e.message}"
            }
        }
    }

    /** Copia en caché para compartir con Intent.ACTION_SEND. */
    suspend fun archivoParaCompartir(): File = withContext(Dispatchers.IO) {
        val dir = File(getApplication<Application>().cacheDir, "compartir").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        File(dir, nombreExportacion()).also { f -> f.outputStream().use { gpkg.copiarA(it) } }
    }

    fun guardarUmbrales(u: Umbrales) {
        umbrales = u
        prefs.edit()
            .putFloat("ch4MinLel", u.ch4MinLel.toFloat())
            .putFloat("o2MaxVol", u.o2MaxVol.toFloat())
            .putFloat("h2sMinPpm", u.h2sMinPpm.toFloat())
            .putFloat("coMaxPpm", u.coMaxPpm.toFloat())
            .apply()
    }

    private fun cargarUmbrales(): Umbrales {
        val d = Umbrales()
        return Umbrales(
            ch4MinLel = prefs.getFloat("ch4MinLel", d.ch4MinLel.toFloat()).toDouble(),
            o2MaxVol = prefs.getFloat("o2MaxVol", d.o2MaxVol.toFloat()).toDouble(),
            h2sMinPpm = prefs.getFloat("h2sMinPpm", d.h2sMinPpm.toFloat()).toDouble(),
            coMaxPpm = prefs.getFloat("coMaxPpm", d.coMaxPpm.toFloat()).toDouble(),
        )
    }

    override fun onCleared() {
        gnss.detener()
    }
}
