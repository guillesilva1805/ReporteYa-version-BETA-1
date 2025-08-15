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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@Composable
fun Paso08Fotos(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { result ->
        respuestas_reporte.actualizar { r -> r.copy(media = result) }
        onValidity(result.isNotEmpty())
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Text("Seleccionar fotos/vídeos") }
            if (estado.media.isNotEmpty()) TextButton(onClick = { respuestas_reporte.actualizar { r -> r.copy(media = emptyList()) }; onValidity(false) }) { Text("Borrar todo") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.media) { uri ->
                Column {
                    AsyncImage(model = uri, contentDescription = null)
                    TextButton(onClick = {
                        val nueva = estado.media.filterNot { it == uri }
                        respuestas_reporte.actualizar { r -> r.copy(media = nueva) }
                        onValidity(nueva.isNotEmpty())
                    }) { Text("Quitar") }
                }
            }
        }
    }
}


