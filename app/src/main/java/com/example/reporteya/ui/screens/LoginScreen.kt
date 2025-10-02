package com.example.reporteya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.reporteya.R
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
                // Acceso fijo exclusivo para gerente
                val inputFixed = dni.trim()
                if (inputFixed.equals("empresa@gmail.com", ignoreCase = true) && password == "jjjc21$") {
                    withContext(Dispatchers.Main) {
                        cargando = false
                        SecureStorage.setSession(context, inputFixed, "fixed-manager-token", null)
                        onGerenteSuccess()
                    }
                    return@launch
                }

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
        // Logo de la empresa
        Image(painter = painterResource(id = R.drawable.logo_jjc), contentDescription = "Logo", modifier = Modifier.height(140.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Inicia sesión en tu cuenta",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        // Selector de rol con alto contraste
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (rol == Rol.Empleado) {
                Button(
                    onClick = { /* seleccionado */ },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.reporteya.ui.theme.BrandBluePrimary, contentColor = Color.White)
                ) { Text("Empleado") }
            } else {
                OutlinedButton(
                    onClick = { rol = Rol.Empleado },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Empleado") }
            }

            if (rol == Rol.Gerente) {
                Button(
                    onClick = { /* seleccionado */ },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.reporteya.ui.theme.BrandGreenSuccess, contentColor = Color.White)
                ) { Text("Gerente") }
            } else {
                OutlinedButton(
                    onClick = { rol = Rol.Gerente },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Gerente") }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (rol == Rol.Empleado) "Rol seleccionado: Empleado" else "Rol seleccionado: Gerente",
            color = if (rol == Rol.Empleado) com.example.reporteya.ui.theme.BrandBluePrimary else com.example.reporteya.ui.theme.BrandGreenSuccess
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it.trim() },
            label = { Text(if (BuildConfig.DNI_RESOLVER_URL.isNotBlank()) "DNI o email" else "Email") },
            isError = dniError != null,
            supportingText = { if (dniError != null) Text(dniError!!) else Text("") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                focusedPlaceholderColor = Color.Black,
                unfocusedPlaceholderColor = Color.Black,
                focusedContainerColor = com.example.reporteya.ui.theme.BrandBlueField,
                unfocusedContainerColor = com.example.reporteya.ui.theme.BrandBlueField,
                focusedIndicatorColor = com.example.reporteya.ui.theme.BrandBlueFocus,
                unfocusedIndicatorColor = com.example.reporteya.ui.theme.BrandGreyMuted
            )
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
            supportingText = { if (passError != null) Text(passError!!) else Text("No la compartas con nadie") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                focusedPlaceholderColor = Color.Black,
                unfocusedPlaceholderColor = Color.Black,
                focusedContainerColor = com.example.reporteya.ui.theme.BrandBlueField,
                unfocusedContainerColor = com.example.reporteya.ui.theme.BrandBlueField,
                focusedIndicatorColor = com.example.reporteya.ui.theme.BrandBlueFocus,
                unfocusedIndicatorColor = com.example.reporteya.ui.theme.BrandGreyMuted
            )
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { iniciar() },
            enabled = !cargando && dni.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.example.reporteya.ui.theme.BrandBluePrimary, contentColor = Color.White)
        ) { Text(if (cargando) "Cargando…" else "Iniciar sesión") }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error ?: "", color = Color.Red)
        }

        if (rol == Rol.Empleado) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onNavigateRegistroEmpleado,
                    colors = ButtonDefaults.textButtonColors(contentColor = com.example.reporteya.ui.theme.BrandBluePrimary)
                ) { Text("Registrarse como empleado") }
                TextButton(
                    onClick = onNavigateRecuperarContrasena,
                    colors = ButtonDefaults.textButtonColors(contentColor = com.example.reporteya.ui.theme.BrandBluePrimary)
                ) { Text("¿Olvidaste tu contraseña?") }
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

