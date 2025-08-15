package com.example.reporteya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun LoginScreen(
    onEmpleadoSuccess: () -> Unit,
    onGerenteSuccess: () -> Unit,
    onNavigateRegistroEmpleado: () -> Unit,
    onNavigateRecuperarContrasena: () -> Unit
) {
    enum class Rol { Empleado, Gerente }
    var rol by remember { mutableStateOf(Rol.Empleado) }
    var dni by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dniError by remember { mutableStateOf<String?>(null) }
    var passError by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun validate(): Boolean {
        dniError = when {
            dni.isBlank() -> "Ingresa tu DNI"
            dni.any { !it.isDigit() } -> "El DNI solo debe tener números"
            dni.length < 8 || dni.length > 10 -> "El DNI debe tener entre 8 y 10 dígitos"
            else -> null
        }
        passError = if (password.isBlank()) "Ingresa tu contraseña" else null
        return dniError == null && passError == null
    }

    fun iniciar() {
        if (!validate()) return
        cargando = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (urlStr, body) = if (rol == Rol.Empleado) {
                    "https://guillesilva04business.app.n8n.cloud/webhook/login" to (
                        """{"dni":"${'$'}dni","contraseña":"${'$'}password","correo":"","codigoInvitacion":""}"""
                    )
                } else {
                    "https://guillesilva04business.app.n8n.cloud/webhook/iniciarsesiongerente" to (
                        """{"dni":"${'$'}dni","contraseña":"${'$'}password"}"""
                    )
                }
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 20000
                }
                conn.outputStream.use { it.write(body.encodeToByteArray()) }
                val ok = conn.responseCode in 200..299
                val responseText = try { (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText() } catch (_: Throwable) { "" }
                conn.disconnect()
                if (!ok) {
                    cargando = false
                    error = "Error de inicio de sesión (código ${'$'}{conn.responseCode})"
                } else {
                    if (rol == Rol.Empleado) {
                        // Para empleado: éxito por 2xx, persistir sesión y navegar
                        runCatching {
                            val prefs = context.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("dniEmpleado", dni).apply()
                        }
                        cargando = false
                        onEmpleadoSuccess()
                    } else {
                        // Gerente: parsear respuesta según formatos aceptados
                        var exito = false
                        var errorMsg: String? = null
                        runCatching {
                            if (responseText.trim().startsWith("[")) {
                                val arr = JSONArray(responseText)
                                val bodyObj = arr.optJSONObject(0)?.optJSONObject("response")?.optJSONObject("body")
                                exito = bodyObj?.optBoolean("éxito") == true
                            } else {
                                val obj = JSONObject(responseText)
                                when {
                                    obj.has("éxito") -> exito = obj.optBoolean("éxito")
                                    obj.has("success") && obj.optBoolean("success") == false -> {
                                        errorMsg = obj.optString("message").ifBlank { null }
                                        exito = false
                                    }
                                }
                            }
                        }.onFailure {
                            errorMsg = "Formato de respuesta inválido."
                        }
                        if (exito) {
                            // Persistir sesión de gerente
                            runCatching {
                                val prefs = context.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putString("dniGerente", dni).apply()
                            }
                            cargando = false
                            onGerenteSuccess()
                        } else {
                            cargando = false
                            error = errorMsg ?: "Formato de respuesta inválido."
                        }
                    }
                }
            } catch (t: Throwable) {
                cargando = false
                error = "Error de red: ${'$'}{t.message ?: "desconocido"}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo (placeholder)
        Text("ReporteYa", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Inicia sesión en tu cuenta", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        // Segmented control (Empleado / Gerente)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { rol = Rol.Empleado }, enabled = rol != Rol.Empleado) { Text("Empleado") }
            OutlinedButton(onClick = { rol = Rol.Gerente }, enabled = rol != Rol.Gerente) { Text("Gerente") }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = {
                val digits = it.filter { ch -> ch.isDigit() }
                dni = digits.take(10)
            },
            label = { Text("DNI") },
            isError = dniError != null,
            supportingText = { if (dniError != null) Text(dniError!!) else Text("Solo números") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    val label = if (showPassword) "Ocultar" else "Mostrar"
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Visibility, contentDescription = label)
                }
            },
            isError = passError != null,
            supportingText = { if (passError != null) Text(passError!!) else Text("No la compartas con nadie") }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { iniciar() },
            enabled = !cargando && dni.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
        ) {
            Text(if (cargando) "Cargando…" else "Iniciar sesión")
        }

        if (rol == Rol.Empleado) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onNavigateRegistroEmpleado) { Text("Registrarse como empleado") }
                TextButton(onClick = onNavigateRecuperarContrasena) { Text("¿Olvidaste tu contraseña?") }
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(error!!) }
        )
    }
}

