package com.fronterait.saludfamiliar.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fronterait.saludfamiliar.data.DoctorVisit
import com.fronterait.saludfamiliar.data.FeverRecord
import com.fronterait.saludfamiliar.data.MoodRecord
import com.fronterait.saludfamiliar.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
val shortDateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

private enum class ListPhase { Loading, Empty, Content }

/**
 * Contenedor común para las listas de registros: hace un fundido entre
 * "cargando", "sin datos" y el contenido en vez de saltar entre estados,
 * que era lo que producía el parpadeo al entrar a cada pestaña.
 */
@Composable
fun <T> RecordListContainer(
    records: List<T>?,
    emptyIcon: ImageVector,
    emptyMessage: String,
    content: @Composable (List<T>) -> Unit
) {
    val phase = when {
        records == null -> ListPhase.Loading
        records.isEmpty() -> ListPhase.Empty
        else -> ListPhase.Content
    }
    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
        label = "recordList",
        modifier = Modifier.fillMaxSize()
    ) { target ->
        when (target) {
            ListPhase.Loading -> Box(modifier = Modifier.fillMaxSize())
            ListPhase.Empty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        emptyIcon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            ListPhase.Content -> records?.let { content(it) }
        }
    }
}

/** Botones compactos de editar/eliminar que se muestran en cada registro. */
@Composable
fun RecordActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row {
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** Confirmación genérica antes de eliminar un registro. */
@Composable
fun DeleteRecordDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar registro") },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun FeverTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getFeverRecords(personId) }.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FeverRecord?>(null) }
    var deletingRecord by remember { mutableStateOf<FeverRecord?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        RecordListContainer(
            records = records,
            emptyIcon = Icons.Outlined.Thermostat,
            emptyMessage = "No hay registros de fiebre."
        ) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${record.temperature}°C", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if(record.temperature > 37.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            RecordActions(onEdit = { editingRecord = record }, onDelete = { deletingRecord = record })
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar fiebre")
        }
    }

    if (showAddDialog || editingRecord != null) {
        val editing = editingRecord
        var temp by remember(editing) { mutableStateOf(editing?.temperature?.toString() ?: "") }
        val dismiss = { showAddDialog = false; editingRecord = null }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (editing == null) "Registrar Fiebre" else "Editar Fiebre") },
            text = {
                OutlinedTextField(
                    value = temp,
                    onValueChange = { temp = it },
                    label = { Text("Temperatura (°C)") },
                    modifier = Modifier.testTag("fever_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    temp.toDoubleOrNull()?.let {
                        if (editing == null) {
                            viewModel.insertFeverRecord(personId, it, System.currentTimeMillis())
                        } else {
                            viewModel.updateFeverRecord(editing.copy(temperature = it))
                        }
                        dismiss()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text("Cancelar") }
            }
        )
    }

    deletingRecord?.let { record ->
        DeleteRecordDialog(
            message = "¿Eliminar el registro de ${record.temperature}°C del ${dateFormat.format(Date(record.timestamp))}?",
            onConfirm = {
                viewModel.deleteFeverRecord(record.id)
                deletingRecord = null
            },
            onDismiss = { deletingRecord = null }
        )
    }
}

@Composable
fun MoodTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getMoodRecords(personId) }.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MoodRecord?>(null) }
    var deletingRecord by remember { mutableStateOf<MoodRecord?>(null) }
    val moods = listOf("😊 Bien", "😐 Regular", "😣 Mal", "😴 Decaído")

    Box(modifier = Modifier.fillMaxSize()) {
        RecordListContainer(
            records = records,
            emptyIcon = Icons.Outlined.Mood,
            emptyMessage = "No hay registros de humor."
        ) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(record.state, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            RecordActions(onEdit = { editingRecord = record }, onDelete = { deletingRecord = record })
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar humor")
        }
    }

    if (showAddDialog || editingRecord != null) {
        val editing = editingRecord
        val dismiss = { showAddDialog = false; editingRecord = null }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (editing == null) "Estado de ánimo" else "Editar estado de ánimo") },
            text = {
                Column {
                    moods.forEach { mood ->
                        TextButton(
                            onClick = {
                                if (editing == null) {
                                    viewModel.insertMoodRecord(personId, mood, System.currentTimeMillis())
                                } else {
                                    viewModel.updateMoodRecord(editing.copy(state = mood))
                                }
                                dismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mood, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } }
        )
    }

    deletingRecord?.let { record ->
        DeleteRecordDialog(
            message = "¿Eliminar el registro \"${record.state}\" del ${dateFormat.format(Date(record.timestamp))}?",
            onConfirm = {
                viewModel.deleteMoodRecord(record.id)
                deletingRecord = null
            },
            onDismiss = { deletingRecord = null }
        )
    }
}

@Composable
fun DoctorTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getDoctorVisits(personId) }.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DoctorVisit?>(null) }
    var deletingRecord by remember { mutableStateOf<DoctorVisit?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        RecordListContainer(
            records = records,
            emptyIcon = Icons.Outlined.MedicalServices,
            emptyMessage = "No hay visitas registradas."
        ) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(record.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                RecordActions(onEdit = { editingRecord = record }, onDelete = { deletingRecord = record })
                            }
                            if (record.notes.isNotBlank()) {
                                Text(record.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar visita")
        }
    }

    if (showAddDialog || editingRecord != null) {
        val editing = editingRecord
        var doctor by remember(editing) { mutableStateOf(editing?.doctorName ?: "") }
        var notes by remember(editing) { mutableStateOf(editing?.notes ?: "") }
        val dismiss = { showAddDialog = false; editingRecord = null }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (editing == null) "Registrar Visita" else "Editar Visita") },
            text = {
                Column {
                    OutlinedTextField(value = doctor, onValueChange = { doctor = it }, label = { Text("Doctor/Especialidad") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.height(100.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (doctor.isNotBlank()) {
                        if (editing == null) {
                            viewModel.insertDoctorVisit(personId, System.currentTimeMillis(), doctor, notes)
                        } else {
                            viewModel.updateDoctorVisit(editing.copy(doctorName = doctor, notes = notes))
                        }
                        dismiss()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text("Cancelar") }
            }
        )
    }

    deletingRecord?.let { record ->
        DeleteRecordDialog(
            message = "¿Eliminar la visita a ${record.doctorName} del ${dateFormat.format(Date(record.timestamp))}?",
            onConfirm = {
                viewModel.deleteDoctorVisit(record.id)
                deletingRecord = null
            },
            onDismiss = { deletingRecord = null }
        )
    }
}
