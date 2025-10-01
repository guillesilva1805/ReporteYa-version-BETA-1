package com.example.reporteya.ui.reporte.paso02_frente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
// removed unused Spacer/width imports
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reporteya.ui.reporte.common.respuestas_reporte
// removed unused FileCache
import com.example.reporteya.services.FrenteService
// removed unused TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
// removed unused networking imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso02Frente(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    val frentes by FrenteService.frentes.collectAsState()
    val cargando by FrenteService.loading.collectAsState()
    val error by FrenteService.error.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var seleccionado by remember(estado.frente) { mutableStateOf(estado.frente.orEmpty()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        FrenteService.cargarFrentes(context)
        onValidity(seleccionado.isNotBlank())
    }

    Column {
        if (cargando) {
            Text("Cargando frentes de trabajo...")
            CircularProgressIndicator()
        } else {
            // SIEMPRE mostrar el dropdown, nunca error
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        readOnly = true,
                        value = seleccionado,
                        onValueChange = {},
                        label = { Text("Frente de trabajo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frentes.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f.nombre) },
                                onClick = {
                                    seleccionado = f.nombre
                                    respuestas_reporte.actualizar { r -> r.copy(frente = f.nombre) }
                                    expanded = false
                                    onValidity(true)
                                }
                            )
                        }
                    }
                }
                FilledIconButton(
                    onClick = { scope.launch { FrenteService.cargarFrentes(context, forzarRecarga = true) } },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = com.example.reporteya.ui.theme.BrandApprove)
                ) { Icon(Icons.Filled.Refresh, contentDescription = "Recargar", tint = Color.White) }
            }

            // Botón textual opcional se mantiene si lo necesitas

            // Mensaje de error simple si no hay datos
            if (error != null && frentes.isEmpty()) {
                Text("No se pudieron cargar los frentes")
            }
        }
    }
}

// Resultado y fetch locales eliminados: ahora se usa FrenteService


