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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.reporteya.services.AuthService
import com.example.reporteya.services.SecureStorage
import com.example.reporteya.services.RoleService
import com.example.reporteya.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private enum class Rol { Empleado, Gerente }

@Composable
fun LoginScreen(
    onEmpleadoSuccess: () -> Unit,
    onGerenteSuccess: () -> Unit,
    onNavigateRegistroEmpleado: () -> Unit,
    onNavigateRecuperarContrasena: () -> Unit
) {
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
        dniError = if (dni.isBlank()) "Ingresa tu email" else null
        passError = if (password.isBlank()) "Ingresa tu contraseña" else null
        return dniError == null && passError == null
    }

    fun iniciar() {
        if (!validate()) return
        cargando = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Bypass temporal para pruebas: solo en debug
                val inputByUser = dni.trim()
                if (BuildConfig.DEBUG && inputByUser.equals("gasa132004@gmail.com", ignoreCase = true) && password == "60023") {
                    withContext(Dispatchers.Main) {
                        cargando = false
                        SecureStorage.setSession(context, inputByUser, "debug-token", null)
                        onEmpleadoSuccess()
                    }
                    return@launch
                }

                val input = dni.trim()
                val maybeResolver = BuildConfig.DNI_RESOLVER_URL
                val emailToUse = if (maybeResolver.isNotBlank() && input.all { it.isDigit() } && input.length in 8..10) {
                    try {
                        val url = URL(maybeResolver)
                        val body = JSONObject().apply { put("dni", input) }.toString()
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json")
                            doOutput = true
                            connectTimeout = 15000
                            readTimeout = 15000
                        }
                        conn.outputStream.use { it.write(body.encodeToByteArray()) }
                        val ok = conn.responseCode in 200..299
                        val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
                        conn.disconnect()
                        val json = JSONObject(resp)
                        if (ok && json.optBoolean("ok") && json.optString("email").isNotBlank()) json.optString("email") else input
                    } catch (_: Throwable) { input }
                } else input

                val signIn = AuthService.signInWithEmail(context, emailToUse, password)
                signIn.fold(onSuccess = { auth ->
                    // Guardar sesión local segura
                    SecureStorage.setSession(context, emailToUse, auth.accessToken, auth.refreshToken)
                    // Decidir navegación por rol real
                    val role = RoleService.fetchRole(context, auth.accessToken, auth.userId).getOrElse { "employee" }
                    withContext(Dispatchers.Main) {
                        cargando = false
                        if (role.equals("manager", true) || role.equals("approver", true) || role.equals("admin", true)) onGerenteSuccess() else onEmpleadoSuccess()
                    }
                }, onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        cargando = false
                        error = "Credenciales inválidas o cuenta no habilitada."
                    }
                })
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    cargando = false
                    error = "Error de red. Intenta nuevamente."
                }
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

        // Selector de rol con alto contraste
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (rol == Rol.Empleado) {
                Button(
                    onClick = { /* seleccionado */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0), contentColor = Color.White)
                ) { Text("Empleado") }
            } else {
                OutlinedButton(onClick = { rol = Rol.Empleado }) { Text("Empleado") }
            }

            if (rol == Rol.Gerente) {
                Button(
                    onClick = { /* seleccionado */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) { Text("Gerente") }
            } else {
                OutlinedButton(onClick = { rol = Rol.Gerente }) { Text("Gerente") }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (rol == Rol.Empleado) "Rol seleccionado: Empleado" else "Rol seleccionado: Gerente",
            color = if (rol == Rol.Empleado) Color(0xFF1565C0) else Color(0xFF2E7D32)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it.trim() },
            label = { Text(if (BuildConfig.DNI_RESOLVER_URL.isNotBlank()) "DNI o email" else "Email") },
            isError = dniError != null,
            supportingText = { if (dniError != null) Text(dniError!!) else Text("") }
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
                    Icon(imageVector = Icons.Filled.Visibility, contentDescription = label)
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
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error ?: "", color = Color.Red)
        }

        if (rol == Rol.Empleado) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onNavigateRegistroEmpleado) { Text("Registrarse como empleado") }
                TextButton(onClick = onNavigateRecuperarContrasena) { Text("¿Olvidaste tu contraseña?") }
            }
        }
    }

    // El diálogo de error sigue disponible si decides mantenerlo; comentado para evitar duplicidad visual
    // if (error != null) {
    //     AlertDialog(
    //         onDismissRequest = { error = null },
    //         confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
    //         title = { Text("Error") },
    //         text = { Text(error!!) }
    //     )
    // }
}

