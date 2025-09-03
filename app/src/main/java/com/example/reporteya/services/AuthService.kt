package com.example.reporteya.services

import android.content.Context
import com.example.reporteya.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthService {
    private const val EMAIL_ALIAS_DOMAIN = "interna.tuapp"

    data class AuthResult(val accessToken: String, val refreshToken: String?, val userId: String?)

    fun buildAliasEmailFromDni(dni: String): String = "$dni@$EMAIL_ALIAS_DOMAIN"

    fun signUp(context: Context, dni: String, password: String): Result<Unit> {
        return runCatching {
            val email = buildAliasEmailFromDni(dni)
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/signup")
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val ok = conn.responseCode in 200..299
            val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException(parseError(resp))
        }
    }

    fun signIn(context: Context, dni: String, password: String): Result<AuthResult> {
        return runCatching {
            val email = buildAliasEmailFromDni(dni)
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val ok = conn.responseCode in 200..299
            val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException(parseError(resp))
            val json = JSONObject(resp)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token").ifBlank { null }
            val userId = json.optJSONObject("user")?.optString("id")
            if (access.isBlank()) throw IllegalStateException("Respuesta inválida de Auth")
            AuthResult(access, refresh, userId)
        }
    }

    fun signInWithEmail(context: Context, email: String, password: String): Result<AuthResult> {
        return runCatching {
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val ok = conn.responseCode in 200..299
            val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException(parseError(resp))
            val json = JSONObject(resp)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token").ifBlank { null }
            val userId = json.optJSONObject("user")?.optString("id")
            if (access.isBlank()) throw IllegalStateException("Respuesta inválida de Auth")
            AuthResult(access, refresh, userId)
        }
    }

    fun getUser(context: Context, jwt: String): Result<JSONObject> {
        return runCatching {
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $jwt")
                connectTimeout = 20000
                readTimeout = 20000
            }
            val ok = conn.responseCode in 200..299
            val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException(parseError(resp))
            JSONObject(resp)
        }
    }

    fun refresh(context: Context, refreshToken: String): Result<AuthResult> {
        return runCatching {
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            val body = JSONObject().apply { put("refresh_token", refreshToken) }.toString()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.encodeToByteArray()) }
            val ok = conn.responseCode in 200..299
            val resp = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException(parseError(resp))
            val json = JSONObject(resp)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token").ifBlank { null }
            val userId = json.optJSONObject("user")?.optString("id")
            if (access.isBlank()) throw IllegalStateException("Respuesta inválida de Auth")
            AuthResult(access, refresh, userId)
        }
    }

    private fun parseError(response: String): String {
        return try {
            val obj = JSONObject(response)
            obj.optString("error_description").ifBlank { obj.optString("msg").ifBlank { obj.optString("message", "Error de autenticación") } }
        } catch (_: Throwable) { if (response.isNotBlank()) response else "Error de autenticación" }
    }
}


