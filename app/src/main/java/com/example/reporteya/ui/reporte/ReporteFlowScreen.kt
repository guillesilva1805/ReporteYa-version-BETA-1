@file:Suppress("SpellCheckingInspection")

package com.example.reporteya.ui.reporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import com.example.reporteya.ui.reporte.paso01_supervisor.Paso01Supervisor
import com.example.reporteya.ui.reporte.paso02_frente.Paso02Frente
import com.example.reporteya.ui.reporte.paso03_ubicacion.Paso03Ubicacion
import com.example.reporteya.ui.reporte.paso04_cuadrilla.Paso04Cuadrilla
import com.example.reporteya.ui.reporte.paso05_disciplina_actividad.Paso05DisciplinaActividad
import com.example.reporteya.ui.reporte.paso06_metrado.Paso06Metrado
import com.example.reporteya.ui.reporte.paso07_horario.Paso07Horario
import com.example.reporteya.ui.reporte.paso08_fotos.Paso08Fotos
import com.example.reporteya.ui.reporte.paso09_equipos.Paso09Equipos
import com.example.reporteya.ui.reporte.paso10_materiales.Paso10Materiales
import com.example.reporteya.ui.reporte.paso11_clima.Paso11Clima
import com.example.reporteya.ui.reporte.paso12_recursos.Paso12Recursos
import com.example.reporteya.ui.reporte.paso13_epp.Paso13Epp
import com.example.reporteya.ui.reporte.paso14_revision_enviar.Paso14RevisionEnviar

@Composable
fun ReporteFlowScreen(onFinish: () -> Unit, onLogout: () -> Unit) {
    var paso by remember { mutableIntStateOf(1) }
    var valido by remember { mutableStateOf(false) }
    val progreso = paso / 14f
    val respuestas by respuestas_reporte.estado.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun isStepValid(step: Int): Boolean {
        return when (step) {
            1 -> !respuestas.supervisor.isNullOrBlank()
            2 -> !respuestas.frente.isNullOrBlank()
            3 -> !respuestas.ubicacion.isNullOrBlank()
            4 -> !respuestas.cuadrilla.isNullOrBlank()
            5 -> !respuestas.disciplina.isNullOrBlank() && !respuestas.actividad.isNullOrBlank()
            6 -> !respuestas.metrado.isNullOrBlank() && respuestas.metrado!!.any { it.isDigit() } && respuestas.metrado!!.any { !it.isDigit() }
            7 -> {
                val inicio = respuestas.horaInicio
                val fin = respuestas.horaFin
                if (inicio.isNullOrBlank() || fin.isNullOrBlank()) return false
                val i = parseMinutesAmPm(inicio) ?: return false
                val f = parseMinutesAmPm(fin) ?: return false
                f > i
            }
            8 -> respuestas.media.isNotEmpty()
            9 -> !respuestas.equipos.isNullOrBlank()
            10 -> !respuestas.materiales.isNullOrBlank()
            11 -> !respuestas.clima.isNullOrBlank()
            12 -> !respuestas.recursos.isNullOrBlank()
            13 -> !respuestas.epp.isNullOrBlank()
            14 -> (
                !respuestas.supervisor.isNullOrBlank() &&
                !respuestas.frente.isNullOrBlank() &&
                !respuestas.ubicacion.isNullOrBlank() &&
                !respuestas.cuadrilla.isNullOrBlank() &&
                !respuestas.disciplina.isNullOrBlank() &&
                !respuestas.actividad.isNullOrBlank() &&
                !respuestas.metrado.isNullOrBlank() &&
                !respuestas.horaInicio.isNullOrBlank() &&
                !respuestas.horaFin.isNullOrBlank() &&
                respuestas.media.isNotEmpty() &&
                !respuestas.equipos.isNullOrBlank() &&
                !respuestas.materiales.isNullOrBlank() &&
                !respuestas.clima.isNullOrBlank() &&
                !respuestas.recursos.isNullOrBlank() &&
                !respuestas.epp.isNullOrBlank()
            )
            else -> false
        }
    }

    LaunchedEffect(paso, respuestas) {
        // Recalcular validez al entrar/cambiar de paso con los datos actuales
        valido = isStepValid(paso)
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        when (paso) {
            1 -> Paso01Supervisor(onValidity = { valido = isStepValid(1) })
            2 -> Paso02Frente(onValidity = { valido = isStepValid(2) })
            3 -> Paso03Ubicacion(onValidity = { valido = isStepValid(3) })
            4 -> Paso04Cuadrilla(onValidity = { valido = isStepValid(4) })
            5 -> Paso05DisciplinaActividad(onValidity = { valido = isStepValid(5) })
            6 -> Paso06Metrado(onValidity = { valido = isStepValid(6) })
            7 -> Paso07Horario(onValidity = { valido = isStepValid(7) })
            8 -> Paso08Fotos(onValidity = { valido = isStepValid(8) })
            9 -> Paso09Equipos(onValidity = { valido = isStepValid(9) })
            10 -> Paso10Materiales(onValidity = { valido = isStepValid(10) })
            11 -> Paso11Clima(onValidity = { valido = isStepValid(11) })
            12 -> Paso12Recursos(onValidity = { valido = isStepValid(12) })
            13 -> Paso13Epp(onValidity = { valido = isStepValid(13) })
            14 -> Paso14RevisionEnviar(onEnviado = {
                // Envío exitoso → volver al paso 1 automáticamente
                paso = 1
                onFinish()
            }, onValidity = { valido = isStepValid(14) })
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { if (paso > 1) paso -= 1 }, enabled = paso > 1) { Text("Anterior") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { if (paso < 14 && valido) paso += 1 }, enabled = (paso < 14 && valido)) { Text("Siguiente") }
                TextButton(onClick = { showLogoutConfirm = true }) { Text("Cerrar sesión") }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    // Borrar sesión y volver al login
                    val prefs = context.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
                    prefs.edit { remove("dniEmpleado") }
                    showLogoutConfirm = false
                    onLogout()
                }) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("No") } },
            title = { Text("¿Estás seguro?") },
            text = { Text("Se cerrará tu sesión") }
        )
    }
}

private fun parseMinutesAmPm(value: String): Int? {
    return try {
        val parts = value.trim().split(" ")
        val hm = parts[0].split(":")
        val h = hm[0].toInt()
        val m = hm[1].toInt()
        when (parts.getOrNull(1)?.uppercase()) {
            "AM" -> if (h == 12) 0 * 60 + m else h * 60 + m
            "PM" -> if (h == 12) 12 * 60 + m else (h + 12) * 60 + m
            else -> h * 60 + m
        }
    } catch (_: Exception) { null }
}


