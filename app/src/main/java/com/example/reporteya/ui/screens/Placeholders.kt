package com.example.reporteya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun RegistrationEmpleadoView(onRegisteredPendingId: (String) -> Unit = {}) {
    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var codigoInvitacion by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var dniErr by remember { mutableStateOf<String?>(null) }
    var nombreErr by remember { mutableStateOf<String?>(null) }
    var apellidoErr by remember { mutableStateOf<String?>(null) }
    var correoErr by remember { mutableStateOf<String?>(null) }
    var passwordErr by remember { mutableStateOf<String?>(null) }
    var confirmErr by remember { mutableStateOf<String?>(null) }
    var invitacionErr by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        dniErr = when {
            dni.isBlank() -> "Ingresa tu DNI"
            !dni.all { it.isDigit() } -> "El DNI solo debe tener números"
            dni.length != 8 -> "El DNI debe tener 8 dígitos"
            else -> null
        }
        nombreErr = if (nombre.isBlank()) "Ingresa tu nombre" else null
        apellidoErr = if (apellido.isBlank()) "Ingresa tu apellido" else null
        correoErr = when {
            correo.isBlank() -> "Ingresa tu correo"
            !correo.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) -> "Correo inválido"
            else -> null
        }
        passwordErr = when {
            password.isBlank() -> "Ingresa una contraseña"
            password.length < 8 -> "Mínimo 8 caracteres"
            !password.any { it.isUpperCase() } -> "Debe tener una mayúscula"
            !password.any { it.isDigit() } -> "Debe tener un número"
            else -> null
        }
        confirmErr = when {
            confirm.isBlank() -> "Confirma tu contraseña"
            confirm != password -> "Las contraseñas no coinciden"
            else -> null
        }
        invitacionErr = if (codigoInvitacion.isBlank()) "Ingresa el código de invitación" else null
        return listOf(dniErr, nombreErr, apellidoErr, correoErr, passwordErr, confirmErr, invitacionErr).all { it == null }
    }

    fun registrar() {
        if (!validate()) {
            error = "Completa todos los campos y asegúrate que las contraseñas coincidan"
            return
        }
        cargando = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "https://guillesilva04business.app.n8n.cloud/webhook/register"
                val body = """
                    {
                      "email":"${'$'}correo",
                      "dni":"${'$'}dni",
                      "nombre":"${'$'}nombre",
                      "apellido":"${'$'}apellido",
                      "password":"${'$'}password",
                      "codigoInvitacion":"${'$'}codigoInvitacion"
                    }
                """.trimIndent()
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
                    val msg = runCatching { JSONObject(responseText).optString("message") }.getOrNull().orEmpty()
                    error = if (msg.isNotBlank()) msg else if (responseText.isNotBlank()) responseText else "No se pudo registrar"
                } else {
                    val json = runCatching { JSONObject(responseText) }.getOrNull()
                    val status = json?.optString("status")
                    val pending = json?.optString("pending_id")
                    if (status == "ok" && !pending.isNullOrBlank()) {
                        cargando = false
                        onRegisteredPendingId(pending)
                    } else {
                        cargando = false
                        val msg = json?.optString("message").orEmpty()
                        error = if (msg.isNotBlank()) msg else "Registro no aceptado"
                    }
                }
            } catch (t: Throwable) {
                cargando = false
                error = t.message ?: "Error desconocido"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro de empleado", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("DNI") },
            isError = dniErr != null,
            supportingText = {
                if (dniErr != null) Text(dniErr!!, color = Color.Red) else Text("8 dígitos, solo números")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                isError = nombreErr != null,
                supportingText = { if (nombreErr != null) Text(nombreErr!!, color = Color.Red) else Text("Nombre legal") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                isError = apellidoErr != null,
                supportingText = { if (apellidoErr != null) Text(apellidoErr!!, color = Color.Red) else Text("Apellido legal") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            isError = correoErr != null,
            supportingText = { if (correoErr != null) Text(correoErr!!, color = Color.Red) else Text("Ej.: nombre@empresa.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Ocultar" else "Mostrar") } },
            isError = passwordErr != null,
            supportingText = {
                if (passwordErr != null) Text(passwordErr!!, color = Color.Red) else Text("Mínimo 8 caracteres, incluye 1 mayúscula y 1 número")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirmar contraseña") },
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { showConfirm = !showConfirm }) { Text(if (showConfirm) "Ocultar" else "Mostrar") } },
            isError = confirmErr != null,
            supportingText = { if (confirmErr != null) Text(confirmErr!!, color = Color.Red) else Text("Repite tu contraseña") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = codigoInvitacion,
            onValueChange = { codigoInvitacion = it },
            label = { Text("Código de invitación") },
            isError = invitacionErr != null,
            supportingText = { if (invitacionErr != null) Text(invitacionErr!!, color = Color.Red) else Text("Entregado por tu empresa") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!cargando) registrar() })
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { registrar() }, enabled = !cargando, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) {
            Text(if (cargando) "Cargando…" else "Crear Cuenta")
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
    }
}

@Composable
fun OtpEmpleadoView(pendingId: String, onOtpSuccessGoLogin: () -> Unit = {}) {
    var code by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun validarLocal(): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }

    fun confirmar() {
        if (!validarLocal()) { error = "Ingresa el código de 6 dígitos"; return }
        cargando = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "https://guillesilva04business.app.n8n.cloud/webhook/verify"
                val body = """{"pending_id":"${'$'}pendingId","code":"${'$'}code"}"""
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
                    error = if (responseText.isNotBlank()) responseText else "No se pudo verificar"
                } else {
                    val json = runCatching { JSONObject(responseText) }.getOrNull()
                    val status = json?.optString("status")
                    if (status == "registered") {
                        cargando = false
                        success = true
                    } else {
                        cargando = false
                        val msg = json?.optString("message").orEmpty()
                        error = if (msg.isNotBlank()) msg else "Código inválido o expirado"
                    }
                }
            } catch (t: Throwable) {
                cargando = false
                error = t.message ?: "Error desconocido"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!success) {
            Text("Confirma tu cuenta", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    val v = it.filter { ch -> ch.isDigit() }.take(6)
                    code = v
                    if (v.length == 6 && !cargando) confirmar()
                },
                label = { Text("Código OTP (6 dígitos)") },
                singleLine = true,
                supportingText = { Text("Revisa tu correo o canal indicado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!cargando) confirmar() }),
                modifier = Modifier.focusRequester(focusRequester)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { confirmar() }, enabled = !cargando) { Text(if (cargando) "Cargando…" else "Confirmar") }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Color.Red)
            }
        } else {
            Text("Tu cuenta ha sido creada", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onOtpSuccessGoLogin() }) { Text("Ir a Iniciar Sesión") }
        }
    }
}

@Composable
fun RecuperarContrasenaView(onSuccessBackToLogin: () -> Unit = {}) {
    // Paso 1
    var correo by remember { mutableStateOf("") }
    var loadingPaso1 by remember { mutableStateOf(false) }
    var errorPaso1 by remember { mutableStateOf<String?>(null) }
    var paso2Habilitado by remember { mutableStateOf(false) }

    // Paso 2
    var nuevaPass by remember { mutableStateOf("") }
    var repetirPass by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var loadingPaso2 by remember { mutableStateOf(false) }
    var errorPaso2 by remember { mutableStateOf<String?>(null) }
    var showPass1 by remember { mutableStateOf(false) }
    var showPass2 by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    fun enviarCodigo() {
        if (correo.isBlank()) { errorPaso1 = "Ingresa tu correo"; return }
        loadingPaso1 = true
        errorPaso1 = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "https://guillesilva04business.app.n8n.cloud/webhook/cambiarcontra?correo=" + java.net.URLEncoder.encode(correo, "UTF-8")
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                }
                val ok = conn.responseCode in 200..299
                val responseText = try { (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText() } catch (_: Throwable) { "" }
                conn.disconnect()
                if (!ok) {
                    loadingPaso1 = false
                    errorPaso1 = if (responseText.isNotBlank()) responseText else "No se pudo enviar el código"
                } else {
                    val json = runCatching { JSONObject(responseText) }.getOrNull()
                    val all = json?.optString("all")
                    if (all == "succes") {
                        loadingPaso1 = false
                        paso2Habilitado = true
                    } else {
                        loadingPaso1 = false
                        errorPaso1 = "No se pudo enviar el código"
                    }
                }
            } catch (t: Throwable) {
                loadingPaso1 = false
                errorPaso1 = t.message ?: "Error desconocido"
            }
        }
    }

    fun confirmarNueva() {
        if (nuevaPass.isBlank() || repetirPass.isBlank()) { errorPaso2 = "Completa ambas contraseñas"; return }
        if (nuevaPass != repetirPass) { errorPaso2 = "Las contraseñas no coinciden"; return }
        if (codigo.isBlank()) { errorPaso2 = "Ingresa el código de verificación"; return }
        loadingPaso2 = true
        errorPaso2 = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "https://guillesilva04business.app.n8n.cloud/webhook/otp2?nueva_contrasena=" +
                    java.net.URLEncoder.encode(nuevaPass, "UTF-8") +
                    "&codigo=" + java.net.URLEncoder.encode(codigo, "UTF-8")
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                }
                val ok = conn.responseCode in 200..299
                val responseText = try { (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText() } catch (_: Throwable) { "" }
                conn.disconnect()
                if (!ok) {
                    loadingPaso2 = false
                    errorPaso2 = if (responseText.isNotBlank()) responseText else "Código o contraseña incorrectos."
                } else {
                    val json = runCatching { JSONObject(responseText) }.getOrNull()
                    val all = json?.optString("all")
                    if (all == "succes") {
                        loadingPaso2 = false
                        showSuccess = true
                    } else {
                        loadingPaso2 = false
                        errorPaso2 = "Código o contraseña incorrectos."
                    }
                }
            } catch (t: Throwable) {
                loadingPaso2 = false
                errorPaso2 = t.message ?: "Error desconocido"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // Paso 1: correo
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            supportingText = { Text("Ingresa tu correo para recibir un código") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (!loadingPaso1) enviarCodigo() })
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { enviarCodigo() }, enabled = !loadingPaso1) { Text(if (loadingPaso1) "Enviando…" else "Enviar") }
        if (errorPaso1 != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorPaso1!!, color = Color.Red)
        }

        // Paso 2: nueva contraseña + código
        if (paso2Habilitado) {
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = nuevaPass,
                onValueChange = { nuevaPass = it },
                label = { Text("Nueva contraseña") },
                visualTransformation = if (showPass1) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showPass1 = !showPass1 }) { Text(if (showPass1) "Ocultar" else "Mostrar") } },
                supportingText = { Text("Mínimo 8 caracteres, incluye 1 mayúscula y 1 número") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = repetirPass,
                onValueChange = { repetirPass = it },
                label = { Text("Repite tu nueva contraseña") },
                visualTransformation = if (showPass2) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showPass2 = !showPass2 }) { Text(if (showPass2) "Ocultar" else "Mostrar") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("Código de verificación") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!loadingPaso2) confirmarNueva() })
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { confirmarNueva() }, enabled = !loadingPaso2) { Text(if (loadingPaso2) "Confirmando…" else "Confirmar") }
            if (errorPaso2 != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorPaso2!!, color = Color.Red)
            }
        }
    }

    if (showSuccess) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSuccess = false; onSuccessBackToLogin() },
            confirmButton = { TextButton(onClick = { showSuccess = false; onSuccessBackToLogin() }) { Text("OK") } },
            title = { Text("Contraseña actualizada") },
            text = { Text("Tu contraseña ha sido cambiada correctamente") }
        )
    }
}


