package com.example.reporteya.ui.reporte.paso05_disciplina_actividad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
                ExposedDropdownMenuBox(expanded = expandedDisc, onExpandedChange = { expandedDisc = !expandedDisc }) {
                    TextField(
                        readOnly = true,
                        value = disciplina,
                        onValueChange = {},
                        label = { Text("Disciplina") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDisc) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().menuAnchor()
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
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().menuAnchor()
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
                // Botón Recargar
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch { DisciplinaActividadService.cargar(context, forzarRecarga = true) }
                }, enabled = !cargando) {
                    androidx.compose.material3.Text(if (cargando) "Recargando..." else "Recargar")
                }
            }
        }
    }
}
// Lógica local eliminada: ahora se usa DisciplinaActividadService


