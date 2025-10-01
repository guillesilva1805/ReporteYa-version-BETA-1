package com.example.reporteya.ui.reporte.paso08_fotos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Paso08Fotos(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    val context = LocalContext.current

    val photoPickerAvailable = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)

    val launcherPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { result ->
        respuestas_reporte.actualizar { r -> r.copy(media = result) }
    }

    val launcherOpenMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        // Intentar persistir permisos de lectura
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
        respuestas_reporte.actualizar { r -> r.copy(media = uris) }
    }

    LaunchedEffect(estado.media) { onValidity(estado.media.isNotEmpty()) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (photoPickerAvailable) {
                    launcherPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                } else {
                    launcherOpenMultiple.launch(arrayOf("image/*", "video/*"))
                }
            }) { Text("Seleccionar fotos/vídeos") }
            if (estado.media.isNotEmpty()) TextButton(
                onClick = { respuestas_reporte.actualizar { r -> r.copy(media = emptyList()) }; onValidity(false) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) { Text("Borrar todo (${estado.media.size})") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = androidx.compose.ui.Modifier.height(140.dp)) {
            items(estado.media) { uri ->
                Box(modifier = Modifier.size(120.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = {
                        val nueva = estado.media.filterNot { it == uri }
                        respuestas_reporte.actualizar { r -> r.copy(media = nueva) }
                        onValidity(nueva.isNotEmpty())
                    }, modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar")
                    }
                }
            }
        }
    }
}


