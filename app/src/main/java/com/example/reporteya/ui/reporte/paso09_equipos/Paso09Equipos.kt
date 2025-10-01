package com.example.reporteya.ui.reporte.paso09_equipos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
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
fun Paso09Equipos(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    var equipos by remember(estado.equipos) { mutableStateOf(estado.equipos.orEmpty()) }
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
        Spacer(Modifier.height(8.dp))
    }
}


