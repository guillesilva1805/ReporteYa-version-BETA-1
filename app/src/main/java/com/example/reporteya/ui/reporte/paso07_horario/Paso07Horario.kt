package com.example.reporteya.ui.reporte.paso07_horario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Paso07Horario(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    var inicio by remember(estado.horaInicio) { mutableStateOf(estado.horaInicio.orEmpty()) }
    var fin by remember(estado.horaFin) { mutableStateOf(estado.horaFin.orEmpty()) }
    var showPickerInicio by remember { mutableStateOf(false) }
    var showPickerFin by remember { mutableStateOf(false) }
    val pickerInicio = rememberTimePickerState(is24Hour = false)
    val pickerFin = rememberTimePickerState(is24Hour = false)
    Column {
        OutlinedTextField(value = inicio, onValueChange = { v ->
            inicio = v
            onValidity(validar(inicio, fin))
        }, readOnly = true,
            label = { Text("Hora inicio (hh:mm AM/PM)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { showPickerInicio = true }) { Text("Seleccionar inicio") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = fin, onValueChange = { v ->
            fin = v
            onValidity(validar(inicio, fin))
        }, readOnly = true,
            label = { Text("Hora fin (hh:mm AM/PM)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { showPickerFin = true }) { Text("Seleccionar fin") }

        if (showPickerInicio) {
            AlertDialog(onDismissRequest = { showPickerInicio = false },
                confirmButton = {
                    Button(onClick = {
                        inicio = formatAmPm(pickerInicio.hour, pickerInicio.minute)
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
                        fin = formatAmPm(pickerFin.hour, pickerFin.minute)
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
    val i = parseMinutes(inicio)
    val f = parseMinutes(fin)
    return i != null && f != null && f > i
}

private fun formatAmPm(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val h12 = ((hour24 + 11) % 12) + 1
    return String.format("%02d:%02d %s", h12, minute, period)
}

private fun parseMinutes(value: String): Int? {
    return try {
        val parts = value.trim().split(" ")
        val hm = parts[0].split(":")
        val h = hm[0].toInt()
        val m = hm[1].toInt()
        val total = when (parts.getOrNull(1)?.uppercase()) {
            "AM" -> if (h == 12) 0 * 60 + m else h * 60 + m
            "PM" -> if (h == 12) 12 * 60 + m else (h + 12) * 60 + m
            else -> h * 60 + m
        }
        total
    } catch (_: Exception) { null }
}


