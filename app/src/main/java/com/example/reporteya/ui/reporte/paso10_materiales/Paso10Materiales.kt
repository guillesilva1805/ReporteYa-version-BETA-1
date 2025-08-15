package com.example.reporteya.ui.reporte.paso10_materiales

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
fun Paso10Materiales(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var materiales by remember(estado.value.materiales) { mutableStateOf(estado.value.materiales.orEmpty()) }
    Column {
        OutlinedTextField(
            value = materiales,
            onValueChange = {
                materiales = it
                respuestas_reporte.actualizar { r -> r.copy(materiales = it) }
                onValidity(it.isNotBlank())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Materiales utilizados") }
        )
    }
}


