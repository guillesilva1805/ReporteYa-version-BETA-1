package com.example.reporteya.ui.reporte.paso09_equipos

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
fun Paso09Equipos(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var equipos by remember(estado.value.equipos) { mutableStateOf(estado.value.equipos.orEmpty()) }
    Column {
        OutlinedTextField(
            value = equipos,
            onValueChange = {
                equipos = it
                respuestas_reporte.actualizar { r -> r.copy(equipos = it) }
                onValidity(it.isNotBlank())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Equipos utilizados") }
        )
    }
}


