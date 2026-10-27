package bo.biogasgama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val CONDICIONES = listOf(
    "CH4 mínimo (1 % LEL = 500 ppm): la lectura de metano debe ser igual o mayor.",
    "O2 máximo (18 % vol.): el oxígeno debe ser menor; indica ambiente sin aire (anóxico).",
    "H2S mínimo (1 ppm): el sulfuro de hidrógeno debe ser igual o mayor; indica descomposición.",
    "CO máximo (25 ppm): el monóxido debe ser igual o menor; descarta motores o quemas.",
)

private val CRITERIOS = listOf(
    "Lo que pida el estudio o el cliente. Si los términos de referencia o la norma del proyecto fijan límites, use esos. " +
        "Referencia común en rellenos sanitarios (EPA, EE. UU.): metano menor a 25 % LEL dentro de construcciones " +
        "y menor a 100 % LEL (5 % vol.) en el límite del predio.",
    "Las alarmas del detector. El GasAlertMax XT II trae de fábrica alarmas de CH4 10 / 20 % LEL, H2S 10 / 15 ppm, " +
        "CO 35 / 200 ppm y O2 19,5 %. Verifique en su equipo si fueron cambiadas.",
    "Mediciones de fondo (lo más práctico). Mida en 3 a 5 puntos limpios cerca del sitio, lejos de residuos y carreteras, " +
        "y ponga cada umbral un poco por encima de lo leído. Ej.: si el fondo da CO 5 ppm, un CO máximo de 15–25 ppm está bien.",
    "Puntos conocidos. Mida en un venteo que sabe que tiene biogás y en otro que no, y ajuste hasta que la app " +
        "dé SÍ en el primero y NO en el segundo.",
)

@Composable
fun DialogoAyudaUmbrales(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("¿Cómo usar los umbrales?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Los umbrales son los límites con que la app decide el potencial de biogás. " +
                        "Para dar SÍ deben cumplirse las cuatro condiciones a la vez:",
                )
                CONDICIONES.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Los valores indicados son los iniciales de la app (Restablecer vuelve a ellos). Cámbielos, toque " +
                        "Guardar umbrales y la evaluación en Captura se actualiza al instante.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text("¿En qué basarse para cambiarlos?", fontWeight = FontWeight.Bold)
                CRITERIOS.forEachIndexed { i, c -> Text("${i + 1}. $c", style = MaterialTheme.typography.bodySmall) }

                Text("Ojo con el O2", fontWeight = FontWeight.Bold)
                Text(
                    "Al aire libre el O2 casi siempre marca 20,9 % aunque haya fuga de metano, así que con O2 < 18 % " +
                        "el resultado sería NO. Ese criterio sirve en pozos, sondeos o venteos; si mide en superficie, " +
                        "súbalo a 20,5 % o cerca.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text("Importante", fontWeight = FontWeight.Bold)
                Text(
                    "Cada punto guarda su veredicto al capturarlo. Cambiar los umbrales después solo afecta a los puntos nuevos.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Entendido") } },
    )
}
