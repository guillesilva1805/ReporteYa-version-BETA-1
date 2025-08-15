package com.example.reporteya.ui.reporte.common

import android.content.Context
import java.io.File

object FileCache {
    fun save(context: Context, key: String, content: String) {
        try {
            val file = File(context.filesDir, safeName(key))
            file.writeText(content)
        } catch (_: Throwable) { }
    }

    fun read(context: Context, key: String): String? {
        return try {
            val file = File(context.filesDir, safeName(key))
            if (file.exists()) file.readText() else null
        } catch (_: Throwable) { null }
    }

    private fun safeName(key: String): String {
        val sanitized = key.replace(Regex("[^a-zA-Z0-9_]+"), "_")
        return "cache_${'$'}sanitized.json"
    }
}


