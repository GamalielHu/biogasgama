package bo.biogasgama.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CampoNumero(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    unidad: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { t -> alCambiar(t.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }) },
        label = { Text(etiqueta) },
        suffix = { Text(unidad) },
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

@Composable
fun Insignia(potencial: Boolean, modifier: Modifier = Modifier) {
    Surface(color = if (potencial) VerdeSi else RojoNo, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            if (potencial) "SÍ" else "NO",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
