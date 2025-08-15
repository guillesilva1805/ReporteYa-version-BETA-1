package com.example.reporteya.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporteya.data.model.MaterialEntry
import com.example.reporteya.data.model.ReportDraft
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val _draft = MutableStateFlow(ReportDraft())
    val draft: StateFlow<ReportDraft> = _draft.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _sendInProgress = MutableStateFlow(false)
    val sendInProgress: StateFlow<Boolean> = _sendInProgress.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    fun updateSupervisorName(value: String) = update { it.copy(supervisorName = value) }
    fun updateDni(value: String) = update { it.copy(dni = value.filter { c -> c.isDigit() }) }
    fun updatePosition(value: String) = update { it.copy(position = value) }
    fun updateDiscipline(value: String) = update { it.copy(discipline = value, activity = "") }
    fun updateActivity(value: String) = update { it.copy(activity = value) }
    fun updateQuantity(value: String) = update { it.copy(quantityValue = value) }
    fun updateQuantityUnit(value: String) = update { it.copy(quantityUnit = value) }
    fun updateMaterials(value: List<MaterialEntry>) = update { it.copy(materials = value) }
    fun updateEquipment(value: List<String>) = update { it.copy(equipment = value) }
    fun updateWeather(value: String) = update { it.copy(weather = value) }
    fun updateComments(value: String) = update { it.copy(comments = value) }
    fun updatePhotos(value: List<Uri>) = update { it.copy(photos = value) }

    fun nextStep() { if (_currentStep.value < 15) _currentStep.value = _currentStep.value + 1 }
    fun prevStep() { if (_currentStep.value > 1) _currentStep.value = _currentStep.value - 1 }

    fun validateCurrentStep(): Boolean = when (_currentStep.value) {
        1 -> _draft.value.supervisorName.isNotBlank()
        2 -> _draft.value.dni.isNotBlank()
        3 -> _draft.value.position.isNotBlank()
        4 -> _draft.value.discipline.isNotBlank()
        5 -> _draft.value.activity.isNotBlank()
        6 -> _draft.value.quantityValue.isNotBlank() && _draft.value.quantityUnit.isNotBlank()
        7 -> _draft.value.materials.isNotEmpty()
        8 -> _draft.value.equipment.isNotEmpty()
        9 -> _draft.value.weather.isNotBlank()
        10 -> _draft.value.comments.isNotBlank()
        11 -> _draft.value.photos.isNotEmpty()
        12 -> _draft.value.code6?.length == 6
        13 -> true
        14 -> true
        15 -> true
        else -> false
    }

    fun markSaved() { _saved.value = true }

    private fun update(block: (ReportDraft) -> ReportDraft) {
        _draft.value = block(_draft.value)
        _saved.value = true
    }

    fun generateCodeIfNeeded() {
        if (_draft.value.code6.isNullOrBlank()) {
            val code = (100000 + Random().nextInt(900000)).toString()
            _draft.value = _draft.value.copy(code6 = code)
            _saved.value = true
        }
    }

    fun sendReport(destinationUrl: String, onSuccess: () -> Unit) {
        if (destinationUrl.isBlank()) {
            _sendError.value = "Ingresa la URL de destino"
            return
        }
        _sendInProgress.value = true
        _sendError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = buildJson(_draft.value)
                val url = URL(destinationUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 20000
                }
                conn.outputStream.use { it.write(json.encodeToByteArray()) }
                val ok = conn.responseCode in 200..299
                conn.disconnect()
                if (ok) {
                    _sendInProgress.value = false
                    // Limpia las respuestas del usuario tras enviar
                    val summary = _draft.value
                    _draft.value = ReportDraft() // resetear
                    _currentStep.value = 15
                    onSuccess()
                } else {
                    _sendInProgress.value = false
                    _sendError.value = "No se pudo enviar (código ${'$'}{conn.responseCode})"
                }
            } catch (t: Throwable) {
                _sendInProgress.value = false
                _sendError.value = t.message ?: "Error desconocido"
            }
        }
    }

    private fun buildJson(d: ReportDraft): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val photos = d.photos.joinToString(prefix = "[", postfix = "]") { '"' + esc(it.toString()) + '"' }
        val materials = d.materials.joinToString(prefix = "[", postfix = "]") {
            val q = it.quantity?.let { qv -> ",\"quantity\":\"${'$'}{esc(qv)}\"" } ?: ""
            "{\"name\":\"${'$'}{esc(it.name)}\"${'$'}q}"
        }
        val equipment = d.equipment.joinToString(prefix = "[", postfix = "]") { '"' + esc(it) + '"' }
        return """
            {
              "reportId": "${'$'}{esc(d.reportId)}",
              "timestampMs": ${d.timestampMs},
              "supervisorName": "${'$'}{esc(d.supervisorName)}",
              "dni": "${'$'}{esc(d.dni)}",
              "position": "${'$'}{esc(d.position)}",
              "discipline": "${'$'}{esc(d.discipline)}",
              "activity": "${'$'}{esc(d.activity)}",
              "quantity": { "value": "${'$'}{esc(d.quantityValue)}", "unit": "${'$'}{esc(d.quantityUnit)}" },
              "materials": ${'$'}materials,
              "equipment": ${'$'}equipment,
              "weather": "${'$'}{esc(d.weather)}",
              "comments": "${'$'}{esc(d.comments)}",
              "photos": ${'$'}photos,
              "code6": "${'$'}{esc(d.code6 ?: "")}" 
            }
        """.trimIndent()
    }
}


