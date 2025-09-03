package com.example.reporteya.services

import android.content.Context
import com.example.reporteya.BuildConfig
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object RoleService {
    fun fetchRole(context: Context, jwt: String, userId: String?): Result<String> {
        return runCatching {
            val id = userId ?: throw IllegalArgumentException("userId requerido")
            val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/verified_employees?select=role&user_id=eq.$id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $jwt")
                connectTimeout = 15000
                readTimeout = 20000
            }
            val ok = conn.responseCode in 200..299
            val body = (if (ok) conn.inputStream else conn.errorStream).bufferedReader().readText()
            conn.disconnect()
            if (!ok) throw IllegalStateException("HTTP ${'$'}{conn.responseCode}: ${'$'}body")
            val arr = JSONArray(body)
            if (arr.length() == 0) return@runCatching "employee"
            val role = arr.getJSONObject(0).optString("role").ifBlank { "employee" }
            role
        }
    }
}


