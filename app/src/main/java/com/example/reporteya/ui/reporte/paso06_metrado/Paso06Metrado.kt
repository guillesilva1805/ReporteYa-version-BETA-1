package com.example.reporteya.ui.reporte.paso06_metrado

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.example.reporteya.ui.reporte.common.respuestas_reporte

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso06Metrado(onValidity: (Boolean) -> Unit) {
    val estado by respuestas_reporte.estado.collectAsState()

    // Unidades sugeridas
    val unidades = listOf("m", "km", "cm", "kg", "t", "unid", "m2", "m3", "l")

    // Parsear metrado guardado (formato: "5 m; 2 kg") a lista de pares
    val initialItems = remember(estado.metrado) {
        val raw = estado.metrado.orEmpty()
        val parts = raw.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val parsed = parts.mapNotNull { p ->
            val seg = p.split(" ")
            if (seg.size >= 2) seg[0] to seg[1] else null
        }
        if (parsed.isNotEmpty()) parsed.toMutableList() else mutableListOf("" to "")
    }
    var items by remember { mutableStateOf(initialItems) }

    fun recalcAndStore() {
        val ok = items.isNotEmpty() && items.all { (q, u) -> q.toDoubleOrNull() != null && u.isNotBlank() }
        val joined = items.filter { (q, u) -> q.isNotBlank() && u.isNotBlank() }
            .joinToString("; ") { (q, u) -> "$q $u" }
        respuestas_reporte.actualizar { r -> r.copy(metrado = joined) }
        onValidity(ok)
    }

    LaunchedEffect(Unit) { recalcAndStore() }

    Column {
        items.forEachIndexed { index, (cantidad, unidad) ->
            var qty by remember(index, cantidad) { mutableStateOf(cantidad) }
            var unit by remember(index, unidad) { mutableStateOf(unidad) }
            var expanded by remember(index) { mutableStateOf(false) }

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = {
                        qty = it.filter { ch -> ch.isDigit() || ch == '.' }
                        items[index] = qty to unit
                        recalcAndStore()
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Cantidad") }
                )

                Spacer(Modifier.width(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = unit,
                        onValueChange = {},
                        label = { Text("Unidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        unidades.forEach { u ->
                            DropdownMenuItem(text = { Text(u) }, onClick = {
                                unit = u
                                items[index] = qty to unit
                                expanded = false
                                recalcAndStore()
                            })
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                if (items.size > 1) {
                    TextButton(onClick = {
                        items = items.toMutableList().also { it.removeAt(index) }
                        recalcAndStore()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)) { Text("Eliminar") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                items = items.toMutableList().also { it.add("" to "") }
                recalcAndStore()
            }) { Text("Agregar fila") }
        }

        Spacer(Modifier.height(8.dp))
        Text("Ingresa cantidad y elige unidad. Puedes agregar o eliminar filas.")
    }
}


