package bo.biogasgama.ui

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import bo.biogasgama.AppViewModel
import bo.biogasgama.FiltroVeredicto
import bo.biogasgama.Formato
import bo.biogasgama.MapaBase
import bo.biogasgama.Metano
import bo.biogasgama.Posicion
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private val AzulYo = Color(0xFF1E88E5)

/** Teselas de Google sin clave: lyrs = m (calles), s (satélite), y (híbrido), p (relieve). */
private class TeselasGoogle(nombre: String, capa: String) : OnlineTileSourceBase(
    nombre, 0, 20, 256, ".png",
    Array(4) { "https://mt$it.google.com/vt/lyrs=$capa" },
    "© Google",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + "&x=" + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex)
}

/** Esri World Imagery: las teselas se piden como /tile/{z}/{y}/{x}. */
private object TeselasEsri : OnlineTileSourceBase(
    "EsriWorldImagery", 0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Imágenes © Esri, Maxar, Earthstar Geographics",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

private val FUENTES: Map<MapaBase, ITileSource> = mapOf(
    MapaBase.CALLES to TeselasGoogle("GoogleCalles", "m"),
    MapaBase.SATELITE to TeselasGoogle("GoogleSatelite", "s"),
    MapaBase.HIBRIDO to TeselasGoogle("GoogleHibrido", "y"),
    MapaBase.RELIEVE to TeselasGoogle("GoogleRelieve", "p"),
    MapaBase.ESRI to TeselasEsri,
    MapaBase.OSM to TileSourceFactory.MAPNIK,
)

/** Atenúa solo las teselas (no los marcadores) hacia negro o blanco según la opacidad. */
private fun filtroOpacidad(opacidad: Float, oscuro: Boolean): ColorMatrixColorFilter? {
    if (opacidad >= 1f) return null
    val a = opacidad.coerceIn(0f, 1f)
    val desplazamiento = if (oscuro) 0f else (1f - a) * 255f
    return ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                a, 0f, 0f, 0f, desplazamiento,
                0f, a, 0f, 0f, desplazamiento,
                0f, 0f, a, 0f, desplazamiento,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
}

private fun icono(contexto: Context, color: Color, dp: Int): Drawable {
    val densidad = contexto.resources.displayMetrics.density
    val px = (dp * densidad).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color.toArgb())
        setStroke((2 * densidad).toInt(), android.graphics.Color.WHITE)
        setSize(px, px)
    }
}

@Composable
fun PantallaMapa(vm: AppViewModel, pos: Posicion?, modifier: Modifier) {
    val contexto = LocalContext.current
    var centrado by remember { mutableStateOf(false) }
    var panelAbierto by rememberSaveable { mutableStateOf(false) }
    val visibles = vm.registrosVisibles()
    val mapa = remember {
        MapView(contexto).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(-16.5, -64.7)) // Bolivia, hasta tener posición
        }
    }
    DisposableEffect(mapa) {
        mapa.onResume()
        onDispose {
            mapa.onPause()
            mapa.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize(), update = { m ->
            val fuente = FUENTES.getValue(vm.mapaBase)
            if (m.tileProvider.tileSource.name() != fuente.name()) {
                m.setTileSource(fuente)
                m.maxZoomLevel = fuente.maximumZoomLevel.toDouble()
            }
            m.overlayManager.tilesOverlay.setColorFilter(filtroOpacidad(vm.opacidadMapa, vm.veloOscuro))

            m.overlays.clear()
            m.overlays.add(CopyrightOverlay(contexto))
            visibles.forEach { r ->
                m.overlays.add(Marker(m).apply {
                    position = GeoPoint(r.latitud, r.longitud)
                    setIcon(icono(contexto, if (r.potencialBiogas) VerdeSi else RojoNo, 16))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Punto ${r.id} · Potencial ${if (r.potencialBiogas) "SÍ" else "NO"}"
                    snippet = "CH4 ${Formato.num(r.ch4Lel)} % LEL (${Formato.entero(r.ch4Ppm)} ppm)<br>" +
                        "O2 ${Formato.num(r.o2VolPct)} %  ·  H2S ${Formato.num(r.h2sPpm)} ppm  ·  CO ${Formato.num(r.coPpm)} ppm"
                    subDescription = r.fechaHora
                })
            }
            pos?.let { p ->
                val yo = GeoPoint(p.latitud, p.longitud)
                p.precisionM?.let { radio ->
                    m.overlays.add(Polygon(m).apply {
                        setPoints(Polygon.pointsAsCircle(yo, radio))
                        fillPaint.color = AzulYo.copy(alpha = 0.18f).toArgb()
                        outlinePaint.color = AzulYo.toArgb()
                        outlinePaint.strokeWidth = 2f
                        setOnClickListener { _, _, _ -> false }
                    })
                }
                m.overlays.add(Marker(m).apply {
                    position = yo
                    setIcon(icono(contexto, AzulYo, 18))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Mi posición"
                    snippet = p.precisionM?.let { "Precisión ± ${Formato.entero(Math.round(it).toDouble())} m" } ?: ""
                })
                if (!centrado) {
                    m.controller.setZoom(17.0)
                    m.controller.setCenter(yo)
                    centrado = true
                }
            }
            m.invalidate()
        })

        Column(Modifier.align(Alignment.TopStart).padding(8.dp)) {
            if (panelAbierto) PanelCapas(vm, visibles.size) else Leyenda(pos, visibles.size, vm.registros.size)
        }

        ExtendedFloatingActionButton(
            onClick = { panelAbierto = !panelAbierto },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) { Text(if (panelAbierto) "Cerrar capas" else "Capas y filtros") }

        FloatingActionButton(
            onClick = { pos?.let { mapa.controller.animateTo(GeoPoint(it.latitud, it.longitud)) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Centrar en mi posición")
        }
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
