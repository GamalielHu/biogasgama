package bo.biogasgama.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import bo.biogasgama.AppViewModel
import bo.biogasgama.BuildConfig
import bo.biogasgama.FiltroVeredicto
import bo.biogasgama.Formato
import bo.biogasgama.MapaBase
import bo.biogasgama.Metano
import bo.biogasgama.Posicion
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.ByteArrayOutputStream

private val AzulYo = Color(0xFF1E88E5)

private fun MapaBase.tipo(): MapType = when (this) {
    MapaBase.NORMAL -> MapType.NORMAL
    MapaBase.SATELITE -> MapType.SATELLITE
    MapaBase.HIBRIDO -> MapType.HYBRID
    MapaBase.RELIEVE -> MapType.TERRAIN
}

/** Teselas de un color sólido: velo que atenúa el mapa base y queda bajo los marcadores. */
private fun veloSolido(color: Int): TileProvider {
    val png = ByteArrayOutputStream().also { salida ->
        Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            .compress(Bitmap.CompressFormat.PNG, 100, salida)
    }.toByteArray()
    val tesela = Tile(256, 256, png)
    return TileProvider { _, _, _ -> tesela }
}

@Composable
fun PantallaMapa(vm: AppViewModel, pos: Posicion?, modifier: Modifier) {
    val contexto = LocalContext.current
    val conPermiso = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-16.5, -64.7), 5f) // Bolivia, hasta tener posición
    }
    var centrado by rememberSaveable { mutableStateOf(false) }
    var panelAbierto by rememberSaveable { mutableStateOf(false) }
    val visibles = vm.registrosVisibles()

    LaunchedEffect(pos != null) {
        if (pos != null && !centrado) {
            camara.position = CameraPosition.fromLatLngZoom(LatLng(pos.latitud, pos.longitud), 17f)
            centrado = true
        }
    }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camara,
            properties = MapProperties(mapType = vm.mapaBase.tipo(), isMyLocationEnabled = conPermiso),
            uiSettings = MapUiSettings(myLocationButtonEnabled = conPermiso, zoomControlsEnabled = false, mapToolbarEnabled = false),
        ) {
            key(vm.veloOscuro) {
                val proveedor = remember { veloSolido(if (vm.veloOscuro) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
                TileOverlay(
                    tileProvider = proveedor,
                    transparency = vm.opacidadMapa.coerceIn(0f, 1f),
                    visible = vm.opacidadMapa < 1f,
                    fadeIn = false,
                )
            }
            visibles.forEach { r ->
                key(r.id) {
                    Marker(
                        state = MarkerState(position = LatLng(r.latitud, r.longitud)),
                        title = "Punto ${r.id} · Potencial ${if (r.potencialBiogas) "SÍ" else "NO"}",
                        snippet = "CH4 ${Formato.num(r.ch4Lel)} % LEL (${Formato.entero(r.ch4Ppm)} ppm) · O2 ${Formato.num(r.o2VolPct)} % · " +
                            "H2S ${Formato.num(r.h2sPpm)} ppm · CO ${Formato.num(r.coPpm)} ppm",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (r.potencialBiogas) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                        ),
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.TopStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!BuildConfig.TIENE_CLAVE_MAPAS) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
                    Text(
                        "Falta la clave de Google Maps (secreto MAPS_API_KEY): el mapa base no se mostrará.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp).widthIn(max = 260.dp),
                    )
                }
            }
            if (panelAbierto) PanelCapas(vm, visibles.size) else Leyenda(pos, visibles.size, vm.registros.size)
        }

        ExtendedFloatingActionButton(
            onClick = { panelAbierto = !panelAbierto },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) { Text(if (panelAbierto) "Cerrar capas" else "Capas y filtros") }
    }
}

@Composable
private fun PanelCapas(vm: AppViewModel, visibles: Int) {
    Card(Modifier.widthIn(max = 340.dp)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Mapa base", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapaBase.entries.forEach { m ->
                    FilterChip(selected = vm.mapaBase == m, onClick = { vm.mapaBase = m }, label = { Text(m.etiqueta) })
                }
            }

            Text("Veredicto de biogás", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FiltroVeredicto.entries.forEach { f ->
                    FilterChip(selected = vm.filtroVeredicto == f, onClick = { vm.filtroVeredicto = f }, label = { Text(f.etiqueta) })
                }
            }

            val lel = vm.filtroCh4MinLel.toDouble()
            Text(
                "CH4 mínimo: ${Formato.num(lel)} % LEL (${Formato.entero(Metano.ppm(lel))} ppm)",
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = vm.filtroCh4MinLel,
                onValueChange = { vm.filtroCh4MinLel = Math.round(it * 2) / 2f },
                valueRange = 0f..100f,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0f, 1f, 5f, 10f, 20f, 50f).forEach { v ->
                    FilterChip(
                        selected = vm.filtroCh4MinLel == v,
                        onClick = { vm.filtroCh4MinLel = v },
                        label = { Text(Formato.num(v.toDouble())) },
                    )
                }
            }

            Text("Opacidad del mapa base: ${Math.round(vm.opacidadMapa * 100)} %", fontWeight = FontWeight.Bold)
            Slider(value = vm.opacidadMapa, onValueChange = { vm.opacidadMapa = it }, valueRange = 0f..1f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = vm.veloOscuro, onClick = { vm.veloOscuro = true }, label = { Text("Oscurecer") })
                FilterChip(selected = !vm.veloOscuro, onClick = { vm.veloOscuro = false }, label = { Text("Aclarar") })
            }

            Text("$visibles de ${vm.registros.size} puntos visibles", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Leyenda(pos: Posicion?, visibles: Int, total: Int) {
    Card {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Punto(VerdeSi, "Potencial SÍ")
            Punto(RojoNo, "Potencial NO")
            Punto(AzulYo, pos?.precisionM?.let { "Yo · ± ${Formato.entero(Math.round(it).toDouble())} m" } ?: "Yo · sin señal")
            Text("$visibles de $total puntos", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Punto(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}
