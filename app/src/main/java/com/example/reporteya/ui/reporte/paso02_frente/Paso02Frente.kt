package com.example.reporteya.ui.reporte.paso02_frente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import com.example.reporteya.ui.reporte.common.FileCache
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso02Frente(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var opciones by remember { mutableStateOf(listOf<String>()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var seleccionado by remember(estado.value.frente) { mutableStateOf(estado.value.frente.orEmpty()) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        cargando = true; error = null
        val url = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/frentes?select=*"
        FileCache.read(context, url)?.let { cached ->
            runCatching { JSONArray(cached) }.onSuccess { arr ->
                opciones = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i); if (obj.has("nombre")) obj.getString("nombre") else obj.optString("name", obj.toString())
                }
            }
        }
        val r = fetchLista(url)
        when (r) {
            is Resultado.Ok -> opciones = r.datos
            is Resultado.Error -> error = r.mensaje
        }
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().readText()
                FileCache.save(context, url, body)
            }
            conn.disconnect()
        }
        cargando = false
        onValidity(seleccionado.isNotBlank() && error == null && !cargando)
    }

    Column {
        when {
            cargando -> CircularProgressIndicator()
            error != null -> Text("Error: ${'$'}error")
            else -> {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        readOnly = true,
                        value = seleccionado,
                        onValueChange = {},
                        label = { Text("Frente de trabajo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        opciones.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = {
                                seleccionado = item
                                respuestas_reporte.actualizar { r -> r.copy(frente = item) }
                                expanded = false
                                onValidity(true)
                            })
                        }
                    }
                }
            }
        }
    }
}

private sealed class Resultado { data class Ok(val datos: List<String>) : Resultado(); data class Error(val mensaje: String) : Resultado() }

private fun fetchLista(urlStr: String): Resultado {
    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 15000; readTimeout = 20000
        }
        val ok = conn.responseCode in 200..299
        val body = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
        conn.disconnect()
        if (!ok) return Resultado.Error("HTTP ${'$'}{conn.responseCode}")
        val arr = JSONArray(body)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val label = when {
                obj.has("nombre") -> obj.getString("nombre")
                obj.has("name") -> obj.getString("name")
                else -> obj.toString()
            }
            out.add(label)
        }
        Resultado.Ok(out)
    } catch (t: Throwable) { Resultado.Error(t.message ?: "Error") }
}


