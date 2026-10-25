package bo.biogasgama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bo.biogasgama.AppViewModel
import bo.biogasgama.DIRECCIONES
import bo.biogasgama.Evaluacion
import bo.biogasgama.Formato
import bo.biogasgama.Metano
import bo.biogasgama.Posicion
import bo.biogasgama.numero
import java.util.Locale

@Composable
fun PantallaCaptura(vm: AppViewModel, pos: Posicion?, modifier: Modifier) {
    val ev = vm.evaluacion()
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TarjetaGnss(pos)

        Seccion("Metano (CH4) · BW GasAlertMax XT II") {
            val lel = numero(vm.ch4Lel)
            CampoNumero(
                vm.ch4Lel, { vm.ch4Lel = it }, "Lectura del detector", "% LEL",
                Modifier.fillMaxWidth(),
                error = if (lel != null && (lel < 0 || lel > 100)) "Fuera del rango del detector (0–100 % LEL)" else null,
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Valor(if (lel != null) Formato.dos(Metano.volPct(lel)) else "—", "% vol.")
                Valor(if (lel != null) Formato.entero(Metano.ppm(lel)) else "—", "ppm")
            }
            Text(
                "100 % LEL = 5,00 % vol. = 50.000 ppm",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        Seccion("Otros gases") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero(vm.h2sPpm, { vm.h2sPpm = it }, "H2S", "ppm", Modifier.weight(1f))
                CampoNumero(vm.coPpm, { vm.coPpm = it }, "CO", "ppm", Modifier.weight(1f))
            }
            CampoNumero(vm.o2Vol, { vm.o2Vol = it }, "O2", "% vol.", Modifier.fillMaxWidth())
        }

        TarjetaVeredicto(ev)

        Seccion("Condiciones ambientales") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val textoGnss = pos?.altitudMsnm?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                CampoNumero(
                    vm.altitudManual ?: textoGnss, { vm.altitudManual = it }, "Altitud", "m s. n. m.",
                    Modifier.weight(1f),
                )
                TextButton(onClick = { vm.altitudManual = null }, enabled = vm.altitudManual != null) { Text("GNSS") }
            }
            Text(
                if (vm.altitudManual != null) "Ajuste manual" else "Fuente: ${pos?.fuenteAltitud ?: "esperando GNSS"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
            CampoNumero(vm.tempC, { vm.tempC = it }, "Temperatura ambiente", "°C", Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CampoNumero(
                    vm.viento, { vm.viento = it }, "Viento", if (vm.vientoKmh) "km/h" else "m/s",
                    Modifier.weight(1f),
                )
                FilterChip(selected = !vm.vientoKmh, onClick = { vm.vientoKmh = false }, label = { Text("m/s") })
                FilterChip(selected = vm.vientoKmh, onClick = { vm.vientoKmh = true }, label = { Text("km/h") })
            }
            SelectorDireccion(vm.vientoDir) { vm.vientoDir = it }
        }

        OutlinedTextField(
            value = vm.observaciones,
            onValueChange = { vm.observaciones = it },
            label = { Text("Observaciones") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        val lel = numero(vm.ch4Lel)
        val listo = pos != null && ev != null && lel != null && lel in 0.0..100.0
        Button(onClick = { pos?.let { vm.guardar(it) } }, enabled = listo, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(
                when {
                    pos == null -> "Esperando posición GNSS…"
                    ev == null -> "Complete CH4, H2S, CO y O2"
                    else -> "Guardar punto"
                }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TarjetaGnss(pos: Posicion?) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1EC)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (pos == null) {
                Text("Buscando señal GNSS…", fontWeight = FontWeight.Bold)
                Text("Active la ubicación y salga a cielo abierto.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Lat ${Formato.coord(pos.latitud)}   Lon ${Formato.coord(pos.longitud)}", fontWeight = FontWeight.Bold)
                val precision = pos.precisionM?.let { "± ${Formato.entero(Math.round(it * 10) / 10.0)} m" } ?: "sin dato"
                val altitud = pos.altitudMsnm?.let { "${Formato.entero(Math.round(it).toDouble())} m s. n. m." } ?: "sin dato"
                Text("Precisión: $precision   ·   Altitud: $altitud", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Valor(numero: String, unidad: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(numero, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VerdeOscuro)
        Text(unidad, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun TarjetaVeredicto(ev: Evaluacion?) {
    val fondo = when {
        ev == null -> Color(0xFFF1F1F1)
        ev.potencial -> Color(0xFFE3F3E4)
        else -> Color(0xFFFBE4E4)
    }
    Card(colors = CardDefaults.cardColors(containerColor = fondo), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Potencial de biogás", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (ev != null) Insignia(ev.potencial)
            }
            if (ev == null) {
                Text("Ingrese CH4, H2S, CO y O2 para evaluar.", style = MaterialTheme.typography.bodySmall)
            } else {
                Spacer(Modifier.height(6.dp))
                ev.criterios.forEach { c ->
                    Text(
                        (if (c.cumple) "✓ " else "✗ ") + c.texto,
                        color = if (c.cumple) VerdeSi else RojoNo,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(ev.resumen, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SelectorDireccion(actual: String, alElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Dirección del viento:")
        Spacer(Modifier.width(8.dp))
        Box {
            OutlinedButton(onClick = { abierto = true }) { Text(actual.ifBlank { "Elegir" }) }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                DropdownMenuItem(text = { Text("Sin dato") }, onClick = { alElegir(""); abierto = false })
                DIRECCIONES.forEach { d ->
                    DropdownMenuItem(text = { Text(d) }, onClick = { alElegir(d); abierto = false })
                }
            }
        }
    }
}
