package com.example.reporteya.ui.reporte.paso05_disciplina_actividad

import androidx.compose.foundation.layout.Column
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
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import com.example.reporteya.ui.reporte.common.FileCache
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso05DisciplinaActividad(onValidity: (Boolean) -> Unit) {
    val estado = respuestas_reporte.estado
    var disciplinas by remember { mutableStateOf(listOf<String>()) }
    var actividadesAll by remember { mutableStateOf(listOf<Pair<String,String>>()) } // (disciplina, actividad)
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedDisc by remember { mutableStateOf(false) }
    var expandedAct by remember { mutableStateOf(false) }
    var disciplina by remember(estado.value.disciplina) { mutableStateOf(estado.value.disciplina.orEmpty()) }
    var actividad by remember(estado.value.actividad) { mutableStateOf(estado.value.actividad.orEmpty()) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        cargando = true; error = null
        val urlDisc = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/disciplinas?select=*"
        val urlAct = "https://uppdkjfjxtjnukftgwhz.supabase.co/rest/v1/actividades?select=*"
        FileCache.read(context, urlDisc)?.let { cached ->
            runCatching { JSONArray(cached) }.onSuccess { arr ->
                disciplinas = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i); if (obj.has("nombre")) obj.getString("nombre") else obj.optString("name", obj.toString())
                }
            }
        }
        FileCache.read(context, urlAct)?.let { cached ->
            runCatching { JSONArray(cached) }.onSuccess { arr ->
                val out = mutableListOf<Pair<String,String>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val d = obj.optString("disciplina").ifBlank { obj.optString("discipline") }
                    val a = obj.optString("actividad").ifBlank { obj.optString("activity") }
                    if (d.isNotBlank() && a.isNotBlank()) out.add(d to a)
                }
                actividadesAll = out
            }
        }
        val r1 = fetchLista(urlDisc)
        val r2 = fetchActividades(urlAct)
        disciplinas = (r1 as? Resultado.Ok)?.datos ?: emptyList()
        actividadesAll = (r2 as? ResultadoAct.Ok)?.datos ?: emptyList()
        if (r1 is Resultado.Error) error = r1.mensaje
        if (r2 is ResultadoAct.Error) error = (error ?: r2.mensaje)
        runCatching {
            val c1 = (URL(urlDisc).openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
            if (c1.responseCode in 200..299) FileCache.save(context, urlDisc, c1.inputStream.bufferedReader().readText()); c1.disconnect()
            val c2 = (URL(urlAct).openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
            if (c2.responseCode in 200..299) FileCache.save(context, urlAct, c2.inputStream.bufferedReader().readText()); c2.disconnect()
        }
        cargando = false
        onValidity(disciplina.isNotBlank() && actividad.isNotBlank() && error == null)
    }

    Column {
        when {
            cargando -> CircularProgressIndicator()
            error != null -> Text("Error: ${'$'}error")
            else -> {
                ExposedDropdownMenuBox(expanded = expandedDisc, onExpandedChange = { expandedDisc = !expandedDisc }) {
                    TextField(readOnly = true, value = disciplina, onValueChange = {}, label = { Text("Disciplina") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDisc) })
                    DropdownMenu(expanded = expandedDisc, onDismissRequest = { expandedDisc = false }) {
                        disciplinas.forEach { d ->
                            DropdownMenuItem(text = { Text(d) }, onClick = {
                                disciplina = d; actividad = ""; respuestas_reporte.actualizar { r -> r.copy(disciplina = d, actividad = "") }
                                expandedDisc = false
                                onValidity(false)
                            })
                        }
                    }
                }
                val actividades = actividadesAll.filter { it.first == disciplina }.map { it.second }
                ExposedDropdownMenuBox(expanded = expandedAct, onExpandedChange = { expandedAct = !expandedAct }) {
                    TextField(readOnly = true, value = actividad, onValueChange = {}, label = { Text("Actividad") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAct) })
                    DropdownMenu(expanded = expandedAct, onDismissRequest = { expandedAct = false }) {
                        actividades.forEach { a ->
                            DropdownMenuItem(text = { Text(a) }, onClick = {
                                actividad = a; respuestas_reporte.actualizar { r -> r.copy(actividad = a) }
                                expandedAct = false
                                onValidity(disciplina.isNotBlank() && actividad.isNotBlank())
                            })
                        }
                    }
                }
            }
        }
    }
}

private sealed class Resultado { data class Ok(val datos: List<String>) : Resultado(); data class Error(val mensaje: String) : Resultado() }
private sealed class ResultadoAct { data class Ok(val datos: List<Pair<String,String>>) : ResultadoAct(); data class Error(val mensaje: String) : ResultadoAct() }

private fun fetchLista(urlStr: String): Resultado {
    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply { requestMethod = "GET"; connectTimeout = 15000; readTimeout = 20000 }
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

private fun fetchActividades(urlStr: String): ResultadoAct {
    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply { requestMethod = "GET"; connectTimeout = 15000; readTimeout = 20000 }
        val ok = conn.responseCode in 200..299
        val body = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
        conn.disconnect()
        if (!ok) return ResultadoAct.Error("HTTP ${'$'}{conn.responseCode}")
        val arr = JSONArray(body)
        val out = mutableListOf<Pair<String,String>>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val d = obj.optString("disciplina").ifBlank { obj.optString("discipline") }
            val a = obj.optString("actividad").ifBlank { obj.optString("activity") }
            if (d.isNotBlank() && a.isNotBlank()) out.add(d to a)
        }
        ResultadoAct.Ok(out)
    } catch (t: Throwable) { ResultadoAct.Error(t.message ?: "Error") }
}


