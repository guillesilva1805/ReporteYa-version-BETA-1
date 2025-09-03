package com.example.reporteya.ui.reporte.paso11_clima

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
fun Paso11Clima(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    var clima by remember(estado.clima) { mutableStateOf(estado.clima.orEmpty()) }
    Column {
        OutlinedTextField(
            value = clima,
            onValueChange = {
                clima = it
                respuestas_reporte.actualizar { r -> r.copy(clima = it) }
                onValidity(it.isNotBlank())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Restricciones de clima") }
        )
    }
}


