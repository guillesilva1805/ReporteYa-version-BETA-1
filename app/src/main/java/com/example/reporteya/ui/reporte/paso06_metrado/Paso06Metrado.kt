package com.example.reporteya.ui.reporte.paso06_metrado

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
fun Paso06Metrado(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var metrado by remember(estado.value.metrado) { mutableStateOf(estado.value.metrado.orEmpty()) }
    Column {
        OutlinedTextField(
            value = metrado,
            onValueChange = {
                metrado = it
                respuestas_reporte.actualizar { r -> r.copy(metrado = it) }
                val ok = it.any { ch -> ch.isDigit() } && it.any { ch -> !ch.isDigit() }
                onValidity(ok)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Metrado (cantidad + unidad)") }
        )
        Spacer(Modifier.height(4.dp))
        Text("Debe incluir número y unidad")
    }
}


