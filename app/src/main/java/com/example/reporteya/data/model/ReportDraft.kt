package com.example.reporteya.data.model

import android.net.Uri
import java.util.UUID

data class MaterialEntry(
    val name: String,
    val quantity: String? = null
)

data class ReportDraft(
    val supervisorName: String = "",
    val dni: String = "",
    val position: String = "",
    val discipline: String = "",
    val activity: String = "",
    val quantityValue: String = "",
    val quantityUnit: String = "",
    val materials: List<MaterialEntry> = emptyList(),
    val equipment: List<String> = emptyList(),
    val weather: String = "",
    val comments: String = "",
    val photos: List<Uri> = emptyList(),
    val code6: String? = null,
    val reportId: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
)


