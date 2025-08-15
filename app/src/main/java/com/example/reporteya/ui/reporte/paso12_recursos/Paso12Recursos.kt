package com.example.reporteya.ui.reporte.paso12_recursos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
fun Paso12Recursos(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var recursos by remember(estado.value.recursos) { mutableStateOf(estado.value.recursos.orEmpty()) }
    Column {
        OutlinedTextField(
            value = recursos,
            onValueChange = {
                recursos = it
                respuestas_reporte.actualizar { r -> r.copy(recursos = it) }
                onValidity(it.isNotBlank())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Restricciones de recursos") }
        )
    }
}


