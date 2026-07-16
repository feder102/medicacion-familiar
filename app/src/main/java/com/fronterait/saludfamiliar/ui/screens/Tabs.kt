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

@Composable
fun FeverTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getFeverRecords(personId) }.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RecordListContainer(
            records = records,
            emptyIcon = Icons.Outlined.Thermostat,
            emptyMessage = "No hay registros de fiebre."
        ) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${record.temperature}°C", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if(record.temperature > 37.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar fiebre")
        }
    }

    if (showDialog) {
        var temp by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar Fiebre") },
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
                        viewModel.insertFeverRecord(personId, it, System.currentTimeMillis())
                        showDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun MoodTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getMoodRecords(personId) }.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
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
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(record.state, style = MaterialTheme.typography.titleMedium)
                            Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar humor")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Estado de ánimo") },
            text = {
                Column {
                    moods.forEach { mood ->
                        TextButton(
                            onClick = {
                                viewModel.insertMoodRecord(personId, mood, System.currentTimeMillis())
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mood, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun DoctorTab(personId: Long, viewModel: AppViewModel) {
    val records by remember(personId) { viewModel.getDoctorVisits(personId) }.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RecordListContainer(
            records = records,
            emptyIcon = Icons.Outlined.MedicalServices,
            emptyMessage = "No hay visitas registradas."
        ) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(record.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(dateFormat.format(Date(record.timestamp)), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (record.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(record.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Agregar visita")
        }
    }

    if (showDialog) {
        var doctor by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar Visita") },
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
                        viewModel.insertDoctorVisit(personId, System.currentTimeMillis(), doctor, notes)
                        showDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
