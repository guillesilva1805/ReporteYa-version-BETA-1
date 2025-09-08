package com.example.reporteya.ui.reporte.paso01_supervisor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.reporteya.ui.reporte.common.respuestas_reporte
import com.example.reporteya.services.SupervisorService
import com.example.reporteya.services.Supervisor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso01Supervisor(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()
    val supervisores by SupervisorService.supervisores.collectAsState()
    val loading by SupervisorService.loading.collectAsState()
    val error by SupervisorService.error.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    var seleccionado by remember(estado.supervisor) { mutableStateOf(estado.supervisor.orEmpty()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        println("DEBUG Paso01: Iniciando carga de supervisores...")
        SupervisorService.cargarSupervisores(context)
    }
    
    LaunchedEffect(seleccionado) {
        onValidity(seleccionado.isNotBlank())
    }

    Column {
        // Debug info
        println("DEBUG Paso01: supervisores.size = ${supervisores.size}")
        println("DEBUG Paso01: loading = $loading")
        println("DEBUG Paso01: error = $error")
        supervisores.forEach { println("DEBUG Paso01: - ${it.nombre} (${it.id})") }
        
        if (loading && supervisores.isEmpty()) {
            Text("Cargando supervisores...")
            CircularProgressIndicator()
        } else {
            // Botón recargar si hay error
            if (error != null && supervisores.isEmpty()) {
                Text("No se pudieron cargar los supervisores")
                TextButton(
                    onClick = { 
                        scope.launch { 
                            SupervisorService.cargarSupervisores(context, forzarRecarga = true) 
                        } 
                    }
                ) {
                    Text("Recargar")
                }
            }
            
            // Dropdown siempre disponible
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    readOnly = true,
                    value = seleccionado,
                    onValueChange = {},
                    label = { Text("Seleccionar Supervisor") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    supervisores.forEach { supervisor ->
                        DropdownMenuItem(
                            text = { Text(supervisor.nombre) },
                            onClick = {
                                seleccionado = supervisor.nombre
                                respuestas_reporte.actualizar { r -> r.copy(supervisor = supervisor.nombre) }
                                expanded = false
                                onValidity(true)
                            }
                        )
                    }
                }
            }
            
            // Botón recargar siempre disponible
            if (supervisores.isNotEmpty()) {
                TextButton(
                    onClick = { 
                        scope.launch { 
                            SupervisorService.cargarSupervisores(context, forzarRecarga = true) 
                        } 
                    },
                    enabled = !loading
                ) {
                    Text(if (loading) "Recargando..." else "Recargar")
                }
            }
        }
    }
}




