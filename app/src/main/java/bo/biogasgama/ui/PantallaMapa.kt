package bo.biogasgama.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import bo.biogasgama.Formato
import bo.biogasgama.Posicion
import bo.biogasgama.Registro
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private val AzulYo = Color(0xFF1E88E5)

/** Esri World Imagery: las teselas se piden como /tile/{z}/{y}/{x}. */
private object TeselasSatelitales : OnlineTileSourceBase(
    "EsriWorldImagery", 0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Imágenes © Esri, Maxar, Earthstar Geographics",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
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
fun PantallaMapa(pos: Posicion?, registros: List<Registro>, modifier: Modifier) {
    val contexto = LocalContext.current
    var centrado by remember { mutableStateOf(false) }
    val mapa = remember {
        MapView(contexto).apply {
            setTileSource(TeselasSatelitales)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
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
            m.overlays.clear()
            m.overlays.add(CopyrightOverlay(contexto))
            registros.forEach { r ->
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

        Leyenda(pos, Modifier.align(Alignment.TopStart).padding(8.dp))

        FloatingActionButton(
            onClick = { pos?.let { mapa.controller.animateTo(GeoPoint(it.latitud, it.longitud)) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Centrar en mi posición")
        }
    }
}

@Composable
private fun Leyenda(pos: Posicion?, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Punto(VerdeSi, "Potencial SÍ")
            Punto(RojoNo, "Potencial NO")
            Punto(AzulYo, pos?.precisionM?.let { "Yo · ± ${Formato.entero(Math.round(it).toDouble())} m" } ?: "Yo · sin señal")
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
