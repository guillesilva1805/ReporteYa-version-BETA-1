package com.example.reporteya.ui.reporte.paso14_revision_enviar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import com.example.reporteya.services.SecureStorage
import coil.compose.AsyncImage
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.compose.ui.platform.LocalContext
import com.example.reporteya.BuildConfig

@Composable
fun Paso14RevisionEnviar(onEnviado: () -> Unit, onValidity: (Boolean) -> Unit, onEditStep: (Int) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    val context = LocalContext.current
    onValidity(true)

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
        Text("Revisión final: verifica tus respuestas")
        Spacer(Modifier.height(8.dp))
        Resumen(estado, onEditStep)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { showConfirm = true }, enabled = !enviando) { Text(if (enviando) "Cargando…" else "Enviar reporte") }
        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirm = false
                        val validationError = validateAll(estado)
                        if (validationError != null) {
                            error = validationError
                        } else {
                            enviando = true
                            enviarConUpload(context, estado,
                                onOk = {
                                    enviando = false
                                    respuestas_reporte.limpiar()
                                    onEnviado()
                                },
                                onFail = { err ->
                                    enviando = false
                                    error = err
                                }
                            )
                        }
                    }) { Text("Sí, enviar") }
                },
                dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancelar") } },
                title = { Text("Confirmar envío") },
                text = { Text("¿Estás seguro de enviar el reporte?") }
            )
        }
        if (error != null) {
            Text(error!!)
            TextButton(onClick = {
                enviando = true
                enviarConUpload(context, estado,
                    onOk = { enviando = false; respuestas_reporte.limpiar(); onEnviado() },
                    onFail = { err -> enviando = false; error = err }
                )
            }) { Text("Reintentar") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Resumen(estado: com.example.reporteya.ui.reporte.common.RespuestasReporte, onEditStep: (Int) -> Unit) {
    @Composable
    fun Linea(titulo: String, valor: String?, paso: Int) {
        val mostrado = if (valor.isNullOrBlank()) "—" else valor
        Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text("$titulo: $mostrado")
            TextButton(onClick = { onEditStep(paso) }) { Text("Editar") }
        }
    }
    Linea("1) Supervisor", estado.supervisor, 1)
    Linea("2) Frente de trabajo", estado.frente, 2)
    Linea("3) Ubicación específica", estado.ubicacion, 3)
    Linea("4) Datos de la cuadrilla", estado.cuadrilla, 4)
    Linea("5) Disciplina", estado.disciplina, 5)
    Linea("5) Actividad", estado.actividad, 5)
    Linea("6) Metrado", estado.metrado, 6)
    Linea("7) Hora inicio", estado.horaInicio, 7)
    Linea("7) Hora fin", estado.horaFin, 7)
    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text("8) Fotos/Vídeos: ${estado.media.size} archivo(s)")
        TextButton(onClick = { onEditStep(8) }) { Text("Editar") }
    }
    if (estado.media.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        LazyRow {
            items(estado.media) { uri ->
                AsyncImage(model = uri, contentDescription = null)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
    Linea("9) Equipos utilizados", estado.equipos, 9)
    Linea("10) Materiales utilizados", estado.materiales, 10)
    Linea("11) Restricciones de clima", estado.clima, 11)
    Linea("12) Restricciones de recursos", estado.recursos, 12)
    Linea("13) Restricciones de EPP", estado.epp, 13)
}

private fun enviarConUpload(
    context: Context,
    estado: com.example.reporteya.ui.reporte.common.RespuestasReporte,
    onOk: () -> Unit,
    onFail: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1) Subir medios a Supabase y obtener URLs públicas
            val publicLinks = mutableListOf<String>()
            for (uri in estado.media) {
                val link = uploadUriToSupabase(context, uri)
                if (link == null) {
                    onFail("Fallo al subir un archivo a Supabase")
                    return@launch
                } else {
                    publicLinks.add(link)
                }
            }
            // 2) Construir JSON con los links públicos + dni/email del usuario si existe
            val cuerpo = buildJsonExtended(estado, publicLinks, SecureStorage.getDni(context))
            // 3) Enviar al webhook fijo de n8n
            val urlFinal = BuildConfig.N8N_WEBHOOK_URL
            val conn = (URL(urlFinal).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                // Adjuntar JWT si existe (desde SecureStorage)
                val jwt = SecureStorage.getJwt(context)
                if (!jwt.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $jwt")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(cuerpo.encodeToByteArray()) }
            val ok = conn.responseCode in 200..299
            val code = conn.responseCode
            conn.disconnect()
            if (ok) onOk() else onFail(if (code == 401 || code == 403) "Sesión expirada. Inicia sesión de nuevo." else "No se pudo enviar (código $code)")
        } catch (t: Throwable) {
            onFail(t.message ?: "Error desconocido")
        }
    }
}

private fun buildJson(
    estado: com.example.reporteya.ui.reporte.common.RespuestasReporte,
    mediaLinks: List<String> = estado.media.map { it.toString() }
): String {
    fun esc(s: String?) = (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
    val media = mediaLinks.joinToString(prefix = "[", postfix = "]") { '"' + esc(it) + '"' }
    return """
      {
        "supervisor": "${esc(estado.supervisor)}",
        "frente": "${esc(estado.frente)}",
        "ubicacion": "${esc(estado.ubicacion)}",
        "cuadrilla": "${esc(estado.cuadrilla)}",
        "disciplina": "${esc(estado.disciplina)}",
        "actividad": "${esc(estado.actividad)}",
        "metrado": "${esc(estado.metrado)}",
        "horaInicio": "${esc(estado.horaInicio)}",
        "horaFin": "${esc(estado.horaFin)}",
        "media": $media,
        "equipos": "${esc(estado.equipos)}",
        "materiales": "${esc(estado.materiales)}",
        "clima": "${esc(estado.clima)}",
        "recursos": "${esc(estado.recursos)}",
        "epp": "${esc(estado.epp)}"
      }
    """.trimIndent()
}

// Versión extendida con claves alineadas a tu n8n
private fun buildJsonExtended(
    estado: com.example.reporteya.ui.reporte.common.RespuestasReporte,
    mediaLinks: List<String>,
    dniUsuario: String?
): String {
    fun esc(s: String?) = (s ?: "").replace("\\", "\\\\").replace("\"", "\\\")
    val media = mediaLinks.joinToString(prefix = "[", postfix = "]") { '"' + esc(it) + '"' }
    val primeraFoto = mediaLinks.firstOrNull() ?: ""
    return """
      {
        "dni": "${esc(dniUsuario)}",
        "dni_usuario": "${esc(dniUsuario)}",
        "01_nombre_supervisor": "${esc(estado.supervisor)}",
        "02_frente_trabajo": "${esc(estado.frente)}",
        "03_ubicacion": "${esc(estado.ubicacion)}",
        "04_nombre_frente": "${esc(estado.frente)}",
        "05_disciplina": "${esc(estado.disciplina)}",
        "06_actividad": "${esc(estado.actividad)}",
        "07_hora_inicio": "${esc(estado.horaInicio)}",
        "07_hora_fin": "${esc(estado.horaFin)}",
        "08_url_foto": "${esc(primeraFoto)}",
        "media": $media,
        "media_urls": $media,
        "09_equipos": "${esc(estado.equipos)}",
        "10_materiales": "${esc(estado.materiales)}",
        "11_clima": "${esc(estado.clima)}",
        "12_recursos": "${esc(estado.recursos)}",
        "13_epp": "${esc(estado.epp)}",
        "equipos": "${esc(estado.equipos)}",
        "materiales": "${esc(estado.materiales)}",
        "clima": "${esc(estado.clima)}",
        "recursos": "${esc(estado.recursos)}",
        "epp": "${esc(estado.epp)}",
        "supervisor": "${esc(estado.supervisor)}",
        "frente": "${esc(estado.frente)}",
        "ubicacion": "${esc(estado.ubicacion)}",
        "cuadrilla": "${esc(estado.cuadrilla)}",
        "disciplina": "${esc(estado.disciplina)}",
        "actividad": "${esc(estado.actividad)}",
        "metrado": "${esc(estado.metrado)}",
        "horaInicio": "${esc(estado.horaInicio)}",
        "horaFin": "${esc(estado.horaFin)}"
      }
    """.trimIndent()
}

private fun validateAll(estado: com.example.reporteya.ui.reporte.common.RespuestasReporte): String? {
    if (estado.supervisor.isNullOrBlank()) return "Completa el campo Supervisor"
    if (estado.frente.isNullOrBlank()) return "Completa el campo Frente de trabajo"
    if (estado.ubicacion.isNullOrBlank()) return "Completa la Ubicación específica"
    if (estado.cuadrilla.isNullOrBlank()) return "Completa los datos de la Cuadrilla"
    if (estado.disciplina.isNullOrBlank()) return "Selecciona la Disciplina"
    if (estado.actividad.isNullOrBlank()) return "Selecciona la Actividad"
    if (estado.metrado.isNullOrBlank()) return "Completa el Metrado"
    if (estado.horaInicio.isNullOrBlank() || estado.horaFin.isNullOrBlank()) return "Completa el Horario"
    if (estado.media.isEmpty()) return "Agrega al menos una foto o video"
    if (estado.equipos.isNullOrBlank()) return "Completa los Equipos utilizados"
    if (estado.materiales.isNullOrBlank()) return "Completa los Materiales utilizados"
    if (estado.clima.isNullOrBlank()) return "Completa las Restricciones de clima"
    if (estado.recursos.isNullOrBlank()) return "Completa las Restricciones de recursos"
    if (estado.epp.isNullOrBlank()) return "Completa las Restricciones de EPP"
    return null
}

private fun uploadUriToSupabase(context: Context, uri: android.net.Uri): String? {
    val supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { return null }
    val supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { return null }
    val bucket = BuildConfig.SUPABASE_BUCKET.ifBlank { return null }

    // Derivar nombre de archivo y content-type
    val resolver = context.contentResolver
    val type = resolver.getType(uri) ?: "application/octet-stream"
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)?.let { ".$it" } ?: ""
    val filename = "${System.currentTimeMillis()}_${(1000..9999).random()}$ext"
    val uploadUrl = "$supabaseUrl/storage/v1/object/$bucket/$filename"
    return try {
        val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            // Preferir JWT de sesión para cumplir políticas de Storage; caer a anon si no hay sesión
            val jwt = SecureStorage.getJwt(context)
            val bearer = if (!jwt.isNullOrBlank()) jwt else supabaseKey
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("apikey", supabaseKey)
            setRequestProperty("Content-Type", type)
            // Permitir sobrescritura si se repite nombre aleatorio (raro)
            setRequestProperty("x-upsert", "true")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 30000
        }
        resolver.openInputStream(uri)?.use { input ->
            conn.outputStream.use { out ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                }
            }
        } ?: run { conn.disconnect(); return null }
        val ok = conn.responseCode in 200..299
        conn.disconnect()
        if (!ok) return null
        // Construir URL pública (requiere bucket público)
        "$supabaseUrl/storage/v1/object/public/$bucket/$filename"
    } catch (_: Throwable) { null }
}


