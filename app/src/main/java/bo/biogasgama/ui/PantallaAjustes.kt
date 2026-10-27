package bo.biogasgama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.biogasgama.AppViewModel
import bo.biogasgama.DESARROLLADOR
import bo.biogasgama.Formato
import bo.biogasgama.Metano
import bo.biogasgama.Umbrales
import bo.biogasgama.numero

@Composable
fun PantallaAjustes(vm: AppViewModel, modifier: Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditorUmbrales(vm)
        Seccion("Conversión de metano (verificación)") {
            listOf(1.0, 5.0, 10.0, 20.0, 100.0).forEach { lel ->
                Text("${Formato.num(lel)} % LEL → ${Formato.dos(Metano.volPct(lel))} % vol. = ${Formato.entero(Metano.ppm(lel))} ppm")
            }
        }
        AcercaDe()
    }
}

@Composable
private fun EditorUmbrales(vm: AppViewModel) {
    val u = vm.umbrales
    var ch4 by remember(u) { mutableStateOf(Formato.num(u.ch4MinLel)) }
    var o2 by remember(u) { mutableStateOf(Formato.num(u.o2MaxVol)) }
    var h2s by remember(u) { mutableStateOf(Formato.num(u.h2sMinPpm)) }
    var co by remember(u) { mutableStateOf(Formato.num(u.coMaxPpm)) }
    val nuevos = run {
        val a = numero(ch4); val b = numero(o2); val c = numero(h2s); val d = numero(co)
        if (a != null && b != null && c != null && d != null) Umbrales(a, b, c, d) else null
    }

    Seccion("Umbrales de evaluación") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(ch4, { ch4 = it }, "CH4 mínimo", "% LEL", Modifier.weight(1f))
            CampoNumero(o2, { o2 = it }, "O2 máximo", "% vol.", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(h2s, { h2s = it }, "H2S mínimo", "ppm", Modifier.weight(1f))
            CampoNumero(co, { co = it }, "CO máximo", "ppm", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { nuevos?.let { vm.guardarUmbrales(it) } }, enabled = nuevos != null && nuevos != u) {
                Text("Guardar umbrales")
            }
            TextButton(onClick = { vm.guardarUmbrales(Umbrales()) }) { Text("Restablecer") }
        }
    }
}

@Composable
private fun AcercaDe() {
    val contexto = LocalContext.current
    val version = remember {
        runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull() ?: ""
    }
    Seccion("Acerca de") {
        Text("Biogas $version", fontWeight = FontWeight.Bold)
        Text("Desarrollado por: $DESARROLLADOR", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Levantamiento georreferenciado de gases (BW GasAlertMax XT II), evaluación de potencial de biogás " +
                "y exportación OGC GeoPackage (UTM 20S, EPSG:32720) para QGIS/ArcGIS.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Mapa: teselas de Google (calles, satélite, híbrido, relieve), Esri y OpenStreetMap vía osmdroid.", style = MaterialTheme.typography.bodySmall)
        var verCreditos by remember { mutableStateOf(false) }
        TextButton(onClick = { verCreditos = true }) { Text("Propiedad intelectual y desarrollo") }
        if (verCreditos) DialogoPropiedad { verCreditos = false }
    }
}
