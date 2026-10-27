package bo.biogasgama.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bo.biogasgama.AppViewModel
import bo.biogasgama.BuildConfig
import bo.biogasgama.DESARROLLADOR

val VerdeSi = Color(0xFF2E7D32)
val RojoNo = Color(0xFFC62828)
val VerdeOscuro = Color(0xFF1F4E3D)

private data class Pestana(val titulo: String, val icono: ImageVector)

private val PESTANAS = listOf(
    Pestana("Captura", Icons.Filled.Edit),
    Pestana("Mapa", Icons.Filled.Place),
    Pestana("Registros", Icons.Filled.List),
    Pestana("Ajustes", Icons.Filled.Settings),
)

@Composable
fun AppBiogas(vm: AppViewModel) {
    MaterialTheme(colorScheme = lightColorScheme(primary = VerdeOscuro, secondary = VerdeSi)) {
        var pestana by rememberSaveable { mutableIntStateOf(0) }
        val posicion by vm.gnss.posicion.collectAsState()
        val avisos = remember { SnackbarHostState() }

        LaunchedEffect(vm.aviso) {
            vm.aviso?.let {
                avisos.showSnackbar(it)
                vm.aviso = null
            }
        }

        Scaffold(
            topBar = { Encabezado() },
            snackbarHost = { SnackbarHost(avisos) },
            bottomBar = {
                NavigationBar {
                    PESTANAS.forEachIndexed { i, p ->
                        NavigationBarItem(
                            selected = pestana == i,
                            onClick = { pestana = i },
                            icon = { Icon(p.icono, contentDescription = p.titulo) },
                            label = { Text(p.titulo) },
                        )
                    }
                }
            },
        ) { relleno ->
            val m = Modifier.padding(relleno).fillMaxSize()
            when (pestana) {
                0 -> PantallaCaptura(vm, posicion, m)
                1 -> PantallaMapa(vm, posicion, m)
                2 -> PantallaRegistros(vm, m)
                else -> PantallaAjustes(vm, m)
            }
        }
    }
}

@Composable
private fun Encabezado() {
    var verCreditos by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(VerdeOscuro)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Biogas",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            // Un toque en el título abre los créditos.
            modifier = Modifier.clickable { verCreditos = true },
        )
        Text("Desarrollado por: $DESARROLLADOR", color = Color(0xFFCDE6D8), fontSize = 12.sp)
    }
    if (verCreditos) DialogoPropiedad { verCreditos = false }
}

@Composable
fun DialogoPropiedad(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Propiedad Intelectual y Desarrollo") },
        text = {
            Column {
                Text("Desarrollador: $DESARROLLADOR", fontWeight = FontWeight.Bold)
                Text("Versión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Text("Compilación: ${BuildConfig.BUILD_TIME}")
                Text(
                    "© $DESARROLLADOR. Todos los derechos reservados. El diseño, código fuente y la lógica de cálculo " +
                        "de esta aplicación son autoría técnica de $DESARROLLADOR; se prohíbe su copia, modificación " +
                        "o distribución sin autorización.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
}
