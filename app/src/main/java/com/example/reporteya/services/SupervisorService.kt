package com.example.reporteya.services

import android.content.Context
import com.example.reporteya.BuildConfig
import com.example.reporteya.ui.reporte.common.FileCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class Supervisor(
    val id: String,
    val nombre: String
)

sealed class SupervisorResult {
    object Loading : SupervisorResult()
    data class Success(val supervisores: List<Supervisor>) : SupervisorResult()
    data class Error(val message: String) : SupervisorResult()
}

object SupervisorService {
    private const val CACHE_KEY = "supervisores_cache"
    private const val BASE_URL = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/supervisores"
    
    private val _supervisores = MutableStateFlow<List<Supervisor>>(emptyList())
    val supervisores: StateFlow<List<Supervisor>> = _supervisores.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun cargarSupervisores(context: Context, forzarRecarga: Boolean = false) {
        _loading.value = true
        _error.value = null
        
        try {
            // Si no es recarga forzada, intentar cargar desde cache primero
            if (!forzarRecarga) {
                val cached = cargarDesdeCache(context)
                if (cached.isNotEmpty()) {
                    _supervisores.value = cached
                    println("DEBUG SupervisorService: Cargados ${cached.size} supervisores desde cache")
                }
            }
            
            // SIEMPRE intentar cargar desde servidor (datos frescos)
            println("DEBUG SupervisorService: Intentando cargar desde servidor...")
            val fromServer = cargarDesdeServidor()
            println("DEBUG SupervisorService: Respuesta del servidor: ${fromServer.size} supervisores")
            
            if (fromServer.isNotEmpty()) {
                _supervisores.value = fromServer
                guardarEnCache(context, fromServer)
                println("DEBUG SupervisorService: Guardados ${fromServer.size} supervisores en cache")
            } else {
                println("DEBUG SupervisorService: Servidor no devolvió datos")
                // Mantener lo que haya en memoria/cache; no forzar valores
            }
        } catch (e: Exception) {
            val errorMsg = "Error cargando supervisores: ${e.message}"
            println("DEBUG SupervisorService: $errorMsg")
            _error.value = errorMsg
            // No forzar datos; dejar vacío si falla
        } finally {
            _loading.value = false
        }
    }
    
    suspend fun agregarSupervisor(context: Context, nombre: String): Boolean {
        if (nombre.isBlank()) return false
        
        _loading.value = true
        _error.value = null
        
        return try {
            val id = UUID.randomUUID().toString()
            val body = JSONObject().apply {
                put("id", id)
                put("nombre_y_apellido", nombre)
            }.toString()
            
            val conn = (URL(BASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val success = conn.responseCode in 200..299
            conn.disconnect()
            
            if (success) {
                // Recargar lista desde servidor
                cargarSupervisores(context, forzarRecarga = true)
                true
            } else {
                _error.value = "Error agregando supervisor"
                false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error agregando supervisor"
            false
        } finally {
            _loading.value = false
        }
    }
    
    suspend fun editarSupervisor(context: Context, id: String, nuevoNombre: String): Boolean {
        if (nuevoNombre.isBlank()) return false
        
        _loading.value = true
        _error.value = null
        
        return try {
            val body = JSONObject().apply {
                put("nombre_y_apellido", nuevoNombre)
            }.toString()
            
            val conn = (URL("$BASE_URL?id=eq.$id").openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val success = conn.responseCode in 200..299
            conn.disconnect()
            
            if (success) {
                // Recargar lista desde servidor
                cargarSupervisores(context, forzarRecarga = true)
                true
            } else {
                _error.value = "Error editando supervisor"
                false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error editando supervisor"
            false
        } finally {
            _loading.value = false
        }
    }
    
    suspend fun eliminarSupervisor(context: Context, id: String): Boolean {
        _loading.value = true
        _error.value = null
        
        return try {
            val conn = (URL("$BASE_URL?id=eq.$id").openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            }
            
            val success = conn.responseCode in 200..299
            conn.disconnect()
            
            if (success) {
                // Recargar lista desde servidor
                cargarSupervisores(context, forzarRecarga = true)
                true
            } else {
                _error.value = "Error eliminando supervisor"
                false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error eliminando supervisor"
            false
        } finally {
            _loading.value = false
        }
    }
    
    private suspend fun cargarDesdeServidor(): List<Supervisor> {
        return try {
            val url = "$BASE_URL?select=*"
            println("DEBUG SupervisorService: Conectando a $url")
            val (responseCode, body) = withContext(Dispatchers.IO) {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "ReporteYa-Android/1.0")
                    connectTimeout = 30000
                    readTimeout = 30000
                    doInput = true
                    useCaches = false
                }
                val code = conn.responseCode
                val success = code in 200..299
                val text = (if (success) conn.inputStream else conn.errorStream).bufferedReader().readText()
                conn.disconnect()
                Pair(code, text)
            }

            println("DEBUG SupervisorService: Código de respuesta: $responseCode")
            println("DEBUG SupervisorService: Cuerpo de respuesta: ${body.take(200)}...")
            
            if (responseCode in 200..299) {
                val supervisores = parsearSupervisores(body)
                println("DEBUG SupervisorService: Parseados ${supervisores.size} supervisores")
                supervisores.forEach { println("DEBUG SupervisorService: - ${it.nombre} (${it.id})") }
                supervisores
            } else {
                println("DEBUG SupervisorService: Error HTTP $responseCode: $body")
                emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG SupervisorService: Excepción en cargarDesdeServidor: ${e.javaClass.simpleName}: ${e.message}")
            println("DEBUG SupervisorService: StackTrace: ${e.stackTrace.take(3).joinToString()}")
            when (e) {
                is java.net.UnknownHostException -> println("DEBUG SupervisorService: No se puede resolver el host")
                is java.net.SocketTimeoutException -> println("DEBUG SupervisorService: Timeout de conexión")
                is java.net.ConnectException -> println("DEBUG SupervisorService: No se puede conectar")
                is javax.net.ssl.SSLException -> println("DEBUG SupervisorService: Error SSL/TLS")
                else -> println("DEBUG SupervisorService: Error desconocido: ${e.javaClass.simpleName}")
            }
            emptyList()
        }
    }
    
    private fun cargarDesdeCache(context: Context): List<Supervisor> {
        return try {
            val cached = FileCache.read(context, CACHE_KEY)
            if (cached != null) {
                parsearSupervisores(cached)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun guardarEnCache(context: Context, supervisores: List<Supervisor>) {
        try {
            val json = JSONArray().apply {
                supervisores.forEach { supervisor ->
                    put(JSONObject().apply {
                        put("id", supervisor.id)
                        put("nombre_y_apellido", supervisor.nombre)
                    })
                }
            }.toString()
            
            FileCache.save(context, CACHE_KEY, json)
        } catch (e: Exception) {
            // Ignorar errores de cache
        }
    }
    
    private fun parsearSupervisores(json: String): List<Supervisor> {
        return try {
            val array = JSONArray(json)
            val supervisores = mutableListOf<Supervisor>()
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val nombre = when {
                    obj.has("nombre_y_apellido") -> obj.getString("nombre_y_apellido")
                    obj.has("nombre") -> obj.getString("nombre")
                    obj.has("name") -> obj.getString("name")
                    else -> "Supervisor ${i + 1}"
                }
                supervisores.add(Supervisor(id, nombre))
            }
            
            supervisores
        } catch (e: Exception) {
            emptyList()
        }
    }
}
