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

data class Frente(
    val id: String,
    val nombre: String
)

object FrenteService {
    private const val BASE_URL = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/frentes"
    private const val CACHE_KEY = "frentes_cache"

    private val _frentes = MutableStateFlow<List<Frente>>(emptyList())
    val frentes: StateFlow<List<Frente>> = _frentes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun cargarFrentes(context: Context, forzarRecarga: Boolean = false) {
        _loading.value = true
        _error.value = null
        try {
            if (!forzarRecarga) {
                val cached = cargarDesdeCache(context)
                if (cached.isNotEmpty()) {
                    _frentes.value = cached
                    println("DEBUG FrenteService: Cargados ${cached.size} frentes desde cache")
                }
            }

            val desdeServidor = cargarDesdeServidor()
            if (desdeServidor.isNotEmpty()) {
                _frentes.value = desdeServidor
                guardarEnCache(context, desdeServidor)
                println("DEBUG FrenteService: Cargados ${desdeServidor.size} frentes desde servidor y guardados en cache")
            }
        } catch (e: Exception) {
            _error.value = e.message
            println("DEBUG FrenteService: Error: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    private suspend fun cargarDesdeServidor(): List<Frente> {
        return try {
            val url = "$BASE_URL?select=*"
            println("DEBUG FrenteService: Conectando a $url")
            val (code, body) = withContext(Dispatchers.IO) {
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
                val c = conn.responseCode
                val ok = c in 200..299
                val b = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
                conn.disconnect()
                Pair(c, b)
            }
            println("DEBUG FrenteService: Código de respuesta: $code")
            println("DEBUG FrenteService: Body (preview): ${body.take(200)}...")
            if (code in 200..299) parseFrentes(body) else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun parseFrentes(json: String): List<Frente> {
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<Frente>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = when {
                    obj.has("id") -> obj.optString("id")
                    obj.has("uuid") -> obj.optString("uuid")
                    obj.has("uid") -> obj.optString("uid")
                    obj.has("pk") -> obj.optString("pk")
                    else -> (i + 1).toString()
                }
                val nombre = when {
                    obj.has("nombre") -> obj.optString("nombre")
                    obj.has("nombre_frente") -> obj.optString("nombre_frente")
                    obj.has("frente") -> obj.optString("frente")
                    obj.has("titulo") -> obj.optString("titulo")
                    obj.has("name") -> obj.optString("name")
                    obj.has("title") -> obj.optString("title")
                    else -> "Frente ${i + 1}"
                }
                out.add(Frente(id, nombre))
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun cargarDesdeCache(context: Context): List<Frente> {
        return try {
            val cached = FileCache.read(context, CACHE_KEY) ?: return emptyList()
            parseFrentes(cached)
        } catch (_: Exception) { emptyList() }
    }

    private fun guardarEnCache(context: Context, frentes: List<Frente>) {
        try {
            val arr = JSONArray().apply {
                frentes.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("nombre", f.nombre)
                    })
                }
            }
            FileCache.save(context, CACHE_KEY, arr.toString())
        } catch (_: Exception) { }
    }
}


