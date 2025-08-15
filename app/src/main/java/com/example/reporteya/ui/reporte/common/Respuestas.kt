package com.example.reporteya.ui.reporte.common

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RespuestasReporte(
	val supervisor: String? = null,
	val frente: String? = null,
	val ubicacion: String? = null,
	val cuadrilla: String? = null,
	val disciplina: String? = null,
	val actividad: String? = null,
	val metrado: String? = null,
	val horaInicio: String? = null,
	val horaFin: String? = null,
	val media: List<Uri> = emptyList(),
	val equipos: String? = null,
	val materiales: String? = null,
	val clima: String? = null,
	val recursos: String? = null,
	val epp: String? = null,
)

object respuestas_reporte {
    private val _estado = MutableStateFlow(RespuestasReporte())
    val estado: StateFlow<RespuestasReporte> = _estado.asStateFlow()

    fun actualizar(transform: (RespuestasReporte) -> RespuestasReporte) {
        _estado.value = transform(_estado.value)
    }

    fun limpiar() {
        _estado.value = RespuestasReporte()
    }
}


