package com.example.reporteya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
// import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.reporteya.services.AuthService
import com.example.reporteya.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun RegistrationEmpleadoView(onRegisteredPendingId: (String) -> Unit = {}) {
    var codigo by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var aceptoPrivacidad by remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    // No se piden contraseñas en este flujo

    var dniErr by remember { mutableStateOf<String?>(null) }
    var nombreErr by remember { mutableStateOf<String?>(null) }
    var apellidoErr by remember { mutableStateOf<String?>(null) }
    var correoErr by remember { mutableStateOf<String?>(null) }
    var privacidadErr by remember { mutableStateOf<String?>(null) }
    var codigoErr by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        codigoErr = if (codigo.isBlank()) "Ingresa el código de obra" else null
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
        privacidadErr = if (!aceptoPrivacidad) "Debes aceptar la Política de Privacidad" else null
        return listOf(codigoErr, dniErr, correoErr, privacidadErr).all { it == null }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    fun registrar() {
        if (!validate()) {
            error = "Datos inválidos o cuenta no habilitada. Inténtalo más tarde."
            return
        }
        cargando = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base = BuildConfig.INVITE_API_BASE
                if (base.isBlank()) throw IllegalStateException("INVITE_API_BASE no configurado")
                val url = URL("$base/api/invite")
                val body = JSONObject().apply {
                    put("code", codigo)
                    put("dni", dni)
                    put("email", correo)
                    if (nombre.isNotBlank()) put("first_name", nombre)
                    if (apellido.isNotBlank()) put("last_name", apellido)
                }.toString()
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 20000
                    readTimeout = 20000
                }
                conn.outputStream.use { it.write(body.encodeToByteArray()) }
                val codeResp = conn.responseCode
                conn.disconnect()
                withContext(Dispatchers.Main) {
                    cargando = false
                    if (codeResp in 200..299) {
                        // Navegar a CheckEmailView: marcador vacío
                        onRegisteredPendingId("")
                    } else if (codeResp == 429) {
                        error = "Demasiados intentos. Intenta en unos minutos."
                    } else {
                        error = "Datos inválidos o cuenta no habilitada. Inténtalo más tarde."
                    }
                }
            } catch (_: Throwable) {
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
        Text("Registro de empleado", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it.trim().take(32) },
            label = { Text("Código de obra") },
            isError = codigoErr != null,
            supportingText = { if (codigoErr != null) Text(codigoErr!!, color = Color.Red) else Text("") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("DNI") },
            isError = dniErr != null,
            supportingText = { if (dniErr != null) Text(dniErr!!, color = Color.Red) else Text("8 dígitos, solo números") },
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = aceptoPrivacidad, onCheckedChange = { aceptoPrivacidad = it })
            Text("Acepto la Política de Privacidad")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { registrar() }, enabled = !cargando && aceptoPrivacidad && codigo.isNotBlank() && dni.isNotBlank() && correo.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) {
            Text(if (cargando) "Cargando…" else "Crear Cuenta")
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
    }
}

@Composable
fun CheckEmailView(onBackToLogin: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Revisa tu correo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Te enviamos un enlace para crear tu contraseña y completar tu acceso.")
        Spacer(Modifier.height(8.dp))
        Text("Si no lo ves, revisa Spam o solicita una nueva invitación.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBackToLogin) { Text("Volver al inicio de sesión") }
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
            cargando = false
            success = true
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
        // Simulación local: habilitar paso 2 sin red
        loadingPaso1 = false
        paso2Habilitado = true
    }

    fun confirmarNueva() {
        if (nuevaPass.isBlank() || repetirPass.isBlank()) { errorPaso2 = "Completa ambas contraseñas"; return }
        if (nuevaPass != repetirPass) { errorPaso2 = "Las contraseñas no coinciden"; return }
        if (codigo.isBlank()) { errorPaso2 = "Ingresa el código de verificación"; return }
        loadingPaso2 = true
        errorPaso2 = null
        CoroutineScope(Dispatchers.IO).launch {
            loadingPaso2 = false
            showSuccess = true
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


