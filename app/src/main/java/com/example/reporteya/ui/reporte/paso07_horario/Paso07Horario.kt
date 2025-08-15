package com.example.reporteya.ui.reporte.paso07_horario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Paso07Horario(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var inicio by remember(estado.value.horaInicio) { mutableStateOf(estado.value.horaInicio.orEmpty()) }
    var fin by remember(estado.value.horaFin) { mutableStateOf(estado.value.horaFin.orEmpty()) }
    var showPickerInicio by remember { mutableStateOf(false) }
    var showPickerFin by remember { mutableStateOf(false) }
    val pickerInicio = rememberTimePickerState()
    val pickerFin = rememberTimePickerState()
    Column {
        OutlinedTextField(value = inicio, onValueChange = {}, readOnly = true,
            label = { Text("Hora inicio (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { showPickerInicio = true }) { Text("Seleccionar inicio") }
        OutlinedTextField(value = fin, onValueChange = {}, readOnly = true,
            label = { Text("Hora fin (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { showPickerFin = true }) { Text("Seleccionar fin") }

        if (showPickerInicio) {
            AlertDialog(onDismissRequest = { showPickerInicio = false },
                confirmButton = {
                    Button(onClick = {
                        inicio = String.format("%02d:%02d", pickerInicio.hour, pickerInicio.minute)
                        respuestas_reporte.actualizar { r -> r.copy(horaInicio = inicio) }
                        showPickerInicio = false
                        onValidity(validar(inicio, fin))
                    }) { Text("OK") }
                },
                dismissButton = { Button(onClick = { showPickerInicio = false }) { Text("Cancelar") } },
                text = { TimePicker(state = pickerInicio) })
        }
        if (showPickerFin) {
            AlertDialog(onDismissRequest = { showPickerFin = false },
                confirmButton = {
                    Button(onClick = {
                        fin = String.format("%02d:%02d", pickerFin.hour, pickerFin.minute)
                        respuestas_reporte.actualizar { r -> r.copy(horaFin = fin) }
                        showPickerFin = false
                        onValidity(validar(inicio, fin))
                    }) { Text("OK") }
                },
                dismissButton = { Button(onClick = { showPickerFin = false }) { Text("Cancelar") } },
                text = { TimePicker(state = pickerFin) })
        }
    }
}

private fun validar(inicio: String, fin: String): Boolean {
    return try {
        val i = inicio.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val f = fin.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        f > i
    } catch (_: Exception) { false }
}


