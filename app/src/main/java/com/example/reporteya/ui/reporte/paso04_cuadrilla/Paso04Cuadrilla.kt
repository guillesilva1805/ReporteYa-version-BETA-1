package com.example.reporteya.ui.reporte.paso04_cuadrilla

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
import androidx.compose.runtime.collectAsState
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
fun Paso04Cuadrilla(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    var cuadrilla by remember(estado.cuadrilla) { mutableStateOf(estado.cuadrilla.orEmpty()) }
    Column {
        OutlinedTextField(
            value = cuadrilla,
            onValueChange = {
                cuadrilla = it
                respuestas_reporte.actualizar { r -> r.copy(cuadrilla = it) }
                onValidity(it.isNotBlank())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Datos de la cuadrilla") }
        )
    }
}


