package bo.biogasgama.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import bo.biogasgama.AppViewModel
import bo.biogasgama.Formato
import bo.biogasgama.Registro
import kotlinx.coroutines.launch

private const val TIPO_GPKG = "application/geopackage+sqlite3"

@Composable
fun PantallaRegistros(vm: AppViewModel, modifier: Modifier) {
    val contexto = LocalContext.current
    val ambito = rememberCoroutineScope()
    var aBorrar by remember { mutableStateOf<Registro?>(null) }

    // application/octet-stream: los proveedores de documentos respetan el nombre .gpkg tal cual.
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { vm.exportarA(it) }
    }

    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${vm.registros.size} puntos · capa puntos_biogas (Point, EPSG:32720)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { guardar.launch(vm.nombreExportacion()) }, modifier = Modifier.weight(1f)) {
                Text("Exportar .gpkg")
            }
            OutlinedButton(
                onClick = {
                    ambito.launch {
                        val archivo = vm.archivoParaCompartir()
                        val uri = FileProvider.getUriForFile(contexto, contexto.packageName + ".archivos", archivo)
                        val envio = Intent(Intent.ACTION_SEND).apply {
                            type = TIPO_GPKG
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, archivo.name)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        contexto.startActivity(Intent.createChooser(envio, "Compartir GeoPackage"))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(" Compartir")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.registros, key = { it.id }) { r ->
                FilaRegistro(r) { aBorrar = r }
            }
        }
    }

    aBorrar?.let { r ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("Borrar punto ${r.id}") },
            text = { Text("Se eliminará del GeoPackage el punto del ${r.fechaHora}.") },
            confirmButton = {
                TextButton(onClick = { vm.borrar(r.id); aBorrar = null }) { Text("Borrar", color = RojoNo) }
            },
            dismissButton = { TextButton(onClick = { aBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaRegistro(r: Registro, alBorrar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${r.id}  ${r.fechaHora}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Insignia(r.potencialBiogas)
                }
                Text(
                    "CH4 ${Formato.num(r.ch4Lel)} % LEL = ${Formato.dos(r.ch4VolPct)} % vol. = ${Formato.entero(r.ch4Ppm)} ppm",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "H2S ${Formato.num(r.h2sPpm)} ppm · CO ${Formato.num(r.coPpm)} ppm · O2 ${Formato.num(r.o2VolPct)} %",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${Formato.coord(r.latitud)}, ${Formato.coord(r.longitud)}" +
                        (r.altitudMsnm?.let { " · ${Formato.entero(Math.round(it).toDouble())} m s. n. m." } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                if (r.observaciones.isNotBlank()) Text(r.observaciones, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = alBorrar) { Icon(Icons.Filled.Delete, contentDescription = "Borrar") }
        }
    }
}
