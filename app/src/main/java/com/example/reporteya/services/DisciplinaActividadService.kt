package com.example.reporteya.services

import android.content.Context
import com.example.reporteya.BuildConfig
import com.example.reporteya.ui.reporte.common.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DisciplinaActividadService {
    private const val DISCIPLINAS_URL = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/disciplinas?select=*"
    private const val ACTIVIDADES_URL = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/actividades?select=*"
    private const val CACHE_DISC = "disciplinas_cache"
    private const val CACHE_ACT = "actividades_cache"

    private val _disciplinas = MutableStateFlow<List<String>>(emptyList())
    val disciplinas: StateFlow<List<String>> = _disciplinas.asStateFlow()

    // Mapa interno id -> nombre de disciplina (para unir con actividades)
    private var disciplinaIdToNombre: Map<String, String> = emptyMap()

    // Lista de pares (disciplina, actividad)
    // Par: (disciplinaId, actividadNombre)
    private val _actividadesAll = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val actividadesAll: StateFlow<List<Pair<String, String>>> = _actividadesAll.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun findDisciplinaIdByNombre(nombre: String): String? {
        return disciplinaIdToNombre.entries.firstOrNull { it.value == nombre }?.key
    }

    suspend fun cargar(context: Context, forzarRecarga: Boolean = false) {
        _loading.value = true
        _error.value = null
        try {
            if (!forzarRecarga) {
                FileCache.read(context, CACHE_DISC)?.let {
                    val det = parseDisciplinasDetalladas(it)
                    disciplinaIdToNombre = det.associate { p -> p.first to p.second }
                    _disciplinas.value = det.map { p -> p.second }
                }
                FileCache.read(context, CACHE_ACT)?.let {
                    val actividadesPorId = parseActividadesPorDisciplinaId(it)
                    _actividadesAll.value = actividadesPorId
                }
            }

            val dBody = request(DISCIPLINAS_URL)
            val aBody = request(ACTIVIDADES_URL)

            if (dBody != null) {
                val disciplinasDetalladas = parseDisciplinasDetalladas(dBody)
                disciplinaIdToNombre = disciplinasDetalladas.associate { it.first to it.second }
                val nombres = disciplinasDetalladas.map { it.second }
                if (nombres.isNotEmpty()) {
                    _disciplinas.value = nombres
                    FileCache.save(context, CACHE_DISC, dBody)
                }
            }

            if (aBody != null) {
                val actividadesPorId = parseActividadesPorDisciplinaId(aBody)
                if (actividadesPorId.isNotEmpty()) {
                    _actividadesAll.value = actividadesPorId
                    FileCache.save(context, CACHE_ACT, aBody)
                }
            }
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }

    private suspend fun request(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 30000
                    readTimeout = 30000
                    doInput = true
                    useCaches = false
                }
                val code = conn.responseCode
                val ok = code in 200..299
                val body = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
                conn.disconnect()
                if (ok) body else null
            } catch (_: Exception) { null }
        }
    }

    private fun parseDisciplinasDetalladas(json: String): List<Pair<String, String>> {
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val nombre = when {
                    obj.has("disciplina") -> obj.optString("disciplina")
                    obj.has("nombre") -> obj.optString("nombre")
                    obj.has("name") -> obj.optString("name")
                    else -> "Disciplina ${i + 1}"
                }
                if (id.isNotBlank()) out.add(id to nombre)
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun parseActividadesPorDisciplinaId(json: String): List<Pair<String, String>> {
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val disciplinaId = obj.optString("disciplina_id")
                val actividadNombre = when {
                    obj.has("nombre") -> obj.optString("nombre")
                    obj.has("actividad") -> obj.optString("actividad")
                    obj.has("activity") -> obj.optString("activity")
                    else -> "Actividad ${i + 1}"
                }
                if (disciplinaId.isNotBlank() && actividadNombre.isNotBlank()) out.add(disciplinaId to actividadNombre)
            }
            out
        } catch (_: Exception) { emptyList() }
    }
}


