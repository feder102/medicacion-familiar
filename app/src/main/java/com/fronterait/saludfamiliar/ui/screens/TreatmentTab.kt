package com.fronterait.saludfamiliar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fronterait.saludfamiliar.data.Treatment
import com.fronterait.saludfamiliar.ui.AppViewModel
import java.util.Date

@Composable
fun TreatmentTab(personId: Long, personName: String, viewModel: AppViewModel) {
    val treatments by viewModel.getTreatments(personId).collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (treatments.isEmpty()) {
            Text("No hay tratamientos activos.", modifier = Modifier.align(Alignment.Center).padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(treatments) { treatment ->
                    TreatmentItem(treatment, viewModel, personName)
                }
            }
        }
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar tratamiento")
        }
    }

    if (showDialog) {
        TreatmentDialog(personId, personName, viewModel, onDismiss = { showDialog = false })
    }
}

@Composable
fun TreatmentItem(treatment: Treatment, viewModel: AppViewModel, personName: String) {
    val doses by viewModel.getDoses(treatment.id).collectAsStateWithLifecycle()
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (treatment.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, contentColor = if(treatment.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(treatment.medication, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (treatment.active) {
                    TextButton(onClick = { viewModel.cancelTreatment(context, treatment) }) {
                        Text("Suspender", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text("Suspendido", color = MaterialTheme.colorScheme.error)
                }
            }
            Text("${treatment.dose} cada ${treatment.freqHours}hs por ${treatment.durationDays} días", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tomas:", style = MaterialTheme.typography.titleMedium)
            
            // Limit to showing the next few or active ones to save space, but let's show all for simplicity
            doses.forEach { dose ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(shortDateFormat.format(Date(dose.scheduledTime)), style = MaterialTheme.typography.bodyMedium, color = if(dose.taken) MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f) else MaterialTheme.colorScheme.onSurface)
                    Row {
                        if (dose.taken) {
                            Icon(Icons.Default.Check, contentDescription = "Tomada", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            if (treatment.active) {
                                IconButton(onClick = { viewModel.markDoseTaken(dose, true) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Marcar tomada", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreatmentDialog(personId: Long, personName: String, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var medication by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var freqHours by remember { mutableStateOf("8") }
    var durationDays by remember { mutableStateOf("5") }
    val calendar = remember { java.util.Calendar.getInstance() }
    var startTimestamp by remember { mutableStateOf(calendar.timeInMillis) }
    val context = LocalContext.current

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(java.util.Calendar.MINUTE, minute)
            startTimestamp = calendar.timeInMillis
        },
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE),
        true
    )

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Tratamiento") },
        text = {
            Column {
                OutlinedTextField(value = medication, onValueChange = { medication = it }, label = { Text("Medicamento (Ej: Paracetamol)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dosis (Ej: 500mg)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = freqHours, onValueChange = { freqHours = it.filter { c -> c.isDigit() } }, label = { Text("Cada (hs)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = durationDays, onValueChange = { durationDays = it.filter { c -> c.isDigit() } }, label = { Text("Días") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Inicio: ${shortDateFormat.format(Date(startTimestamp))}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Nota: Se crearán eventos con recordatorio en tu calendario de Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Button(onClick = {
                val fH = freqHours.toIntOrNull() ?: 0
                val dD = durationDays.toIntOrNull() ?: 0
                if (medication.isNotBlank() && dose.isNotBlank() && fH > 0 && dD > 0) {
                    viewModel.createTreatment(
                        context = context,
                        personName = personName,
                        personId = personId,
                        medication = medication,
                        doseDesc = dose,
                        freqHours = fH,
                        durationDays = dD,
                        startTimestamp = startTimestamp
                    )
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
