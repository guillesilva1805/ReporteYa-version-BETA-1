package com.example.reporteya.ui.reporte.paso05_disciplina_actividad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
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
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.reporteya.services.DisciplinaActividadService
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso05DisciplinaActividad(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    val disciplinas by DisciplinaActividadService.disciplinas.collectAsState()
    val actividadesAll by DisciplinaActividadService.actividadesAll.collectAsState() // (disciplina, actividad)
    val cargando by DisciplinaActividadService.loading.collectAsState()
    val error by DisciplinaActividadService.error.collectAsState()
    var expandedDisc by remember { mutableStateOf(false) }
    var expandedAct by remember { mutableStateOf(false) }
    var disciplina by remember(estado.disciplina) { mutableStateOf(estado.disciplina.orEmpty()) }
    var actividad by remember(estado.actividad) { mutableStateOf(estado.actividad.orEmpty()) }
    // Guardar también el id de la disciplina seleccionada para filtrar por id
    var disciplinaIdSeleccionada by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        // Forzar recarga desde Supabase al entrar
        DisciplinaActividadService.cargar(context, forzarRecarga = true)
        onValidity(disciplina.isNotBlank() && actividad.isNotBlank())
    }

    Column {
        when {
            cargando -> CircularProgressIndicator()
            false -> Text("") // NUNCA mostrar error
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(expanded = expandedDisc, onExpandedChange = { expandedDisc = !expandedDisc }, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    TextField(
                        readOnly = true,
                        value = disciplina,
                        onValueChange = {},
                        label = { Text("Disciplina") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDisc) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    DropdownMenu(expanded = expandedDisc, onDismissRequest = { expandedDisc = false }) {
                        // Mostrar nombres de disciplinas
                        disciplinas.forEach { d ->
                            DropdownMenuItem(text = { Text(d) }, onClick = {
                                disciplina = d; actividad = "";
                                // Resolver id por nombre actual de forma explícita desde el servicio
                                val id = com.example.reporteya.services.DisciplinaActividadService.findDisciplinaIdByNombre(d)
                                disciplinaIdSeleccionada = id
                                respuestas_reporte.actualizar { r -> r.copy(disciplina = d, actividad = "") }
                                expandedDisc = false
                                onValidity(false)
                            })
                        }
                    }
                }
                FilledIconButton(
                    onClick = { scope.launch { DisciplinaActividadService.cargar(context, forzarRecarga = true) } },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = com.example.reporteya.ui.theme.BrandApprove)
                ) { Icon(Icons.Filled.Refresh, contentDescription = "Recargar", tint = Color.White) }
                }
                // Filtrar por id si lo conocemos; si no, caer a filtrar por nombre (back-compat)
                val actividades = if (!disciplina.isNullOrBlank()) {
                    val id = com.example.reporteya.services.DisciplinaActividadService.findDisciplinaIdByNombre(disciplina)
                    disciplinaIdSeleccionada = id
                    if (id != null) actividadesAll.filter { it.first == id }.map { it.second } else emptyList()
                } else emptyList()
                if (!cargando && disciplinas.isEmpty()) {
                    Text("Sin disciplinas")
                }
                ExposedDropdownMenuBox(expanded = expandedAct, onExpandedChange = { expandedAct = !expandedAct }) {
                    TextField(
                        readOnly = true,
                        value = actividad,
                        onValueChange = {},
                        label = { Text("Actividad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAct) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    DropdownMenu(expanded = expandedAct, onDismissRequest = { expandedAct = false }) {
                        actividades.forEach { a ->
                            DropdownMenuItem(text = { Text(a) }, onClick = {
                                actividad = a; respuestas_reporte.actualizar { r -> r.copy(actividad = a) }
                                expandedAct = false
                                onValidity(disciplina.isNotBlank() && actividad.isNotBlank())
                            })
                        }
                    }
                }
                if (!cargando && disciplinaIdSeleccionada != null && actividades.isEmpty()) {
                    Text("Sin actividades para la disciplina seleccionada")
                }
                // Botón Recargar (texto en negro)
                TextButton(
                    onClick = { scope.launch { DisciplinaActividadService.cargar(context, forzarRecarga = true) } },
                    enabled = !cargando,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) { androidx.compose.material3.Text(if (cargando) "Recargando..." else "Recargar") }
            }
        }
    }
}
// Lógica local eliminada: ahora se usa DisciplinaActividadService


