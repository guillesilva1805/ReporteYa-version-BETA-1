package com.example.reporteya.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// import removed: rememberTopAppBarState not used
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.reporteya.data.model.MaterialEntry
import com.example.reporteya.viewmodel.ReportViewModel

@Composable
fun EmployeeFlowScreen(
    onFinish: (Boolean) -> Unit,
    reportViewModel: ReportViewModel = viewModel()
) {
    val draft by reportViewModel.draft.collectAsState()
    val step by reportViewModel.currentStep.collectAsState()
    val saved by reportViewModel.saved.collectAsState()
    val sendInProgress by reportViewModel.sendInProgress.collectAsState()
    val sendError by reportViewModel.sendError.collectAsState()

    val progress = step / 15f
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        when (step) {
            1 -> StepTextField("1) Nombre del supervisor", draft.supervisorName, reportViewModel::updateSupervisorName)
            2 -> StepNumericField("2) DNI", draft.dni, reportViewModel::updateDni)
            3 -> StepTextField("3) Cargo", draft.position, reportViewModel::updatePosition)
            4 -> StepDiscipline(draft.discipline, onSelect = reportViewModel::updateDiscipline)
            5 -> StepActivity(draft.discipline, draft.activity, onSelect = reportViewModel::updateActivity)
            6 -> StepQuantity(draft.quantityValue, draft.quantityUnit, reportViewModel::updateQuantity, reportViewModel::updateQuantityUnit)
            7 -> StepMaterials(draft.materials, reportViewModel::updateMaterials)
            8 -> StepEquipment(draft.equipment, reportViewModel::updateEquipment)
            9 -> StepWeather(draft.weather, reportViewModel::updateWeather)
            10 -> StepMultilineText("10) Comentarios/observaciones", draft.comments, reportViewModel::updateComments)
            11 -> StepPhotos(draft.photos, reportViewModel::updatePhotos)
            12 -> StepCode(draft.code6, onGenerate = reportViewModel::generateCodeIfNeeded)
            13 -> StepSaved()
            14 -> StepSend(sendInProgress, sendError) { url ->
                reportViewModel.sendReport(url) { reportViewModel.nextStep() }
            }
            15 -> StepSuccess(draft.reportId, draft.code6.orEmpty(), draft.timestampMs) { onFinish(true) }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { reportViewModel.prevStep() }, enabled = step > 1) { Text("Anterior") }
            TextButton(
                onClick = { reportViewModel.nextStep() },
                enabled = reportViewModel.validateCurrentStep() && step < 15
            ) { Text("Siguiente") }
        }
        if (saved) { Text("Guardado ✓") }
    }
}

@Composable
private fun StepTextField(label: String, value: String, onChange: (String) -> Unit) {
    Column { OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
}

@Composable
private fun StepMultilineText(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
    }
}

@Composable
private fun StepNumericField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepDiscipline(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("MDT", "TOPOGRAFIA", "CIVIL", "MONTAJE", "TENDIDO", "ELECTRICIDAD")
    Column {
        Text("4) Seleccionar Disciplina")
        options.forEach { label ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = selected == label, onCheckedChange = { if (it) onSelect(label) })
                Text(label)
            }
        }
    }
}

@Composable
private fun StepActivity(discipline: String, selected: String, onSelect: (String) -> Unit) {
    val map = mapOf(
        "MDT" to listOf(
            "Desbroce manual",
            "Desbroce con equipo",
            "Construccion de acceso",
            "Excavacion puntual con equipo",
            "Excavacion manual",
            "Excavacion masiva",
            "Perfilado",
            "Relleno controlado puntual",
            "Relleno controlado masivo",
            "Relleno no controlado",
            "Instalacion de cable de tierra p/contrapeso"
        ),
        "TOPOGRAFIA" to listOf(
            "Replanteo topografico de trazo",
            "Replanteo topografico de acceso",
            "Replanteo topografico de torre",
            "Replanteo topografico en Subestacion",
            "Liberacion topografica"
        ),
        "CIVIL" to listOf(
            "Colocacion de solado",
            "Colocacion de acero de refuerzo",
            "Colocacion encofrado",
            "Retiro de encofrado",
            "Colocacion de anclajes",
            "Colocacion de concreto 240 manual",
            "Colocacion de concreto 240 con mixer",
            "Colocacion de muro block"
        ),
        "MONTAJE" to listOf(
            "Seleccion de elementos",
            "Transporde de estructuras",
            "Prearmado",
            "Izaje de estructuras",
            "Revision y Verticalizacion",
            "Torqueo",
            "Instalacion de señaletica"
        ),
        "TENDIDO" to listOf(
            "Instalacion de poleas manual",
            "Instalacion de poleas con winche",
            "Riega de cordina",
            "Tendido de conductor",
            "Tendido de OPGW",
            "Tendido de cable de guarda",
            "Tendido de ADSS",
            "Flechado, amarre y engrapado",
            "Instalacion de amortiguadores",
            "Instalacion de cajas de empalme"
        ),
        "ELECTRICIDAD" to listOf(
            "Tendido de barras",
            "Montaje de tableros",
            "Cableado y conexionado MD",
            "Cableado y conexionado BT",
            "Instalacion de malla a tierra",
            "Soldadura exotermica",
            "Instalacion de luminaria",
            "Instalacion de tomacorriente, interruptor, pararrayos",
            "Instalacion de tuberia PVC bancoducto"
        )
    )
    val options = map[discipline].orEmpty()
    Column {
        Text("5) Seleccionar Actividad")
        options.forEach { label ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = selected == label, onCheckedChange = { if (it) onSelect(label) })
                Text(label)
            }
        }
    }
}

@Composable
private fun StepQuantity(value: String, unit: String, onValue: (String) -> Unit, onUnit: (String) -> Unit) {
    Column {
        Text("6) Cargar metrado")
        OutlinedTextField(value = value, onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) }, label = { Text("Cantidad") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = unit, onValueChange = onUnit, label = { Text("Unidad (ej: m²)") })
    }
}

@Composable
private fun StepMaterials(value: List<MaterialEntry>, onChange: (List<MaterialEntry>) -> Unit) {
    val names = listOf("Cemento", "Arena", "Acero", "Cable", "Otro")
    val local = remember(value) { mutableStateListOf<MaterialEntry>().apply { addAll(value) } }
    Column {
        Text("7) Materiales usados")
        names.forEach { name ->
            val entry = local.find { it.name == name }
            val checked = entry != null
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = checked, onCheckedChange = { isChecked ->
                    if (isChecked && entry == null) {
                        local.add(MaterialEntry(name))
                    } else if (!isChecked && entry != null) {
                        local.remove(entry)
                    }
                    onChange(local.toList())
                })
                Text(name, modifier = Modifier.weight(1f))
                if (checked) {
                    OutlinedTextField(
                        value = entry?.quantity.orEmpty(),
                        onValueChange = { q ->
                            val idx = local.indexOfFirst { it.name == name }
                            if (idx >= 0) local[idx] = local[idx].copy(quantity = q)
                            onChange(local.toList())
                        },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepEquipment(value: List<String>, onChange: (List<String>) -> Unit) {
    val options = listOf("Excavadora", "Grúa", "Soldadora", "Generador")
    val selected = remember(value) { mutableStateListOf<String>().apply { addAll(value) } }
    Column {
        Text("8) Equipos usados")
        options.forEach { name ->
            val checked = selected.contains(name)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { isChecked ->
                    if (isChecked) selected.add(name) else selected.remove(name)
                    onChange(selected.toList())
                })
                Text(name)
            }
        }
    }
}

@Composable
private fun StepWeather(value: String, onChange: (String) -> Unit) {
    val options = listOf("Soleado", "Nublado", "Lluvia", "Otro")
    Column {
        Text("9) Clima")
        options.forEach { label ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = value == label, onCheckedChange = { if (it) onChange(label) })
                Text(label)
            }
        }
    }
}

@Composable
private fun StepPhotos(uris: List<Uri>, onChange: (List<Uri>) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { result ->
        onChange(result)
    }
    Column {
        Text("11) Subir fotos (1 a 10)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Seleccionar fotos") }
            if (uris.isNotEmpty()) TextButton(onClick = { onChange(emptyList()) }) { Text("Borrar todas") }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uris) { uri ->
                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.height(80.dp))
                TextButton(onClick = { onChange(uris.filterNot { it == uri }) }) { Text("Borrar") }
            }
        }
    }
}

@Composable
private fun StepCode(code: String?, onGenerate: () -> Unit) {
    Column {
        Text("12) Código único de 6 dígitos")
        if (code.isNullOrBlank()) {
            Button(onClick = onGenerate) { Text("Generar código") }
        } else {
            Text("Código: ${'$'}code")
        }
    }
}

@Composable
private fun StepSaved() { Text("13) Guardado automático ✓") }

@Composable
private fun StepSend(loading: Boolean, error: String?, onSend: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    Column {
        Text("14) Enviar reporte")
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL de destino") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (loading) CircularProgressIndicator() else Button(onClick = { onSend(url) }) { Text("Enviar") }
        if (error != null) Text(error)
    }
}

@Composable
private fun StepSuccess(reportId: String, code6: String, timestampMs: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("15) Éxito")
        Text("ID de reporte: ${'$'}reportId")
        Text("Código: ${'$'}code6")
        Text("Fecha y hora: ${'$'}{java.text.SimpleDateFormat(\"yyyy-MM-dd HH:mm\").format(java.util.Date(timestampMs))}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Reporte ${'$'}reportId (código ${'$'}code6)")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
            }) { Text("Compartir") }
            Button(onClick = { /* Pendiente: generación de PDF */ }) { Text("Ver/Descargar PDF") }
        }
        TextButton(onClick = onClose) { Text("Finalizar") }
    }
}


