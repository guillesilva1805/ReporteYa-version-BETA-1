package com.example.reporteya.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reporteya.services.SupervisorService
import com.example.reporteya.services.Supervisor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorManagementScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val supervisores by SupervisorService.supervisores.collectAsState()
    val loading by SupervisorService.loading.collectAsState()
    val error by SupervisorService.error.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupervisor by remember { mutableStateOf<Supervisor?>(null) }
    var deletingSupervisor by remember { mutableStateOf<Supervisor?>(null) }
    
    LaunchedEffect(Unit) {
        SupervisorService.cargarSupervisores(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Supervisores",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón recargar
                IconButton(
                    onClick = { 
                        scope.launch { 
                            SupervisorService.cargarSupervisores(context, forzarRecarga = true) 
                        } 
                    },
                    enabled = !loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                }
                
                // Botón agregar
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Supervisor")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Estado de carga/error
        if (loading && supervisores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cargando supervisores...")
                }
            }
        } else if (error != null && supervisores.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error cargando supervisores",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            scope.launch { 
                                SupervisorService.cargarSupervisores(context, forzarRecarga = true) 
                            } 
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            // Lista de supervisores
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(supervisores) { supervisor ->
                    SupervisorItem(
                        supervisor = supervisor,
                        onEdit = { editingSupervisor = it },
                        onDelete = { deletingSupervisor = it },
                        enabled = !loading
                    )
                }
                
                if (supervisores.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay supervisores registrados",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo agregar supervisor
    if (showAddDialog) {
        AddSupervisorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre ->
                scope.launch {
                    val success = SupervisorService.agregarSupervisor(context, nombre)
                    if (success) {
                        showAddDialog = false
                    }
                }
            },
            loading = loading
        )
    }
    
    // Diálogo editar supervisor
    editingSupervisor?.let { supervisor ->
        EditSupervisorDialog(
            supervisor = supervisor,
            onDismiss = { editingSupervisor = null },
            onConfirm = { nuevoNombre ->
                scope.launch {
                    val success = SupervisorService.editarSupervisor(context, supervisor.id, nuevoNombre)
                    if (success) {
                        editingSupervisor = null
                    }
                }
            },
            loading = loading
        )
    }
    
    // Diálogo eliminar supervisor
    deletingSupervisor?.let { supervisor ->
        AlertDialog(
            onDismissRequest = { deletingSupervisor = null },
            title = { Text("Eliminar Supervisor") },
            text = { Text("¿Estás seguro de que quieres eliminar a '${supervisor.nombre}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = SupervisorService.eliminarSupervisor(context, supervisor.id)
                            if (success) {
                                deletingSupervisor = null
                            }
                        }
                    },
                    enabled = !loading
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSupervisor = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SupervisorItem(
    supervisor: Supervisor,
    onEdit: (Supervisor) -> Unit,
    onDelete: (Supervisor) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supervisor.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ID: ${supervisor.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onEdit(supervisor) },
                    enabled = enabled
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                
                IconButton(
                    onClick = { onDelete(supervisor) },
                    enabled = enabled
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSupervisorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    loading: Boolean
) {
    var nombre by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Agregar Supervisor") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del supervisor") },
                enabled = !loading,
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre) },
                enabled = !loading && nombre.isNotBlank()
            ) {
                Text(if (loading) "Agregando..." else "Agregar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !loading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EditSupervisorDialog(
    supervisor: Supervisor,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    loading: Boolean
) {
    var nombre by remember { mutableStateOf(supervisor.nombre) }
    
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Editar Supervisor") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del supervisor") },
                enabled = !loading,
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre) },
                enabled = !loading && nombre.isNotBlank() && nombre != supervisor.nombre
            ) {
                Text(if (loading) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !loading
            ) {
                Text("Cancelar")
            }
        }
    )
}
