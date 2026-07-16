package com.fronterait.saludfamiliar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fronterait.saludfamiliar.data.Person
import com.fronterait.saludfamiliar.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToPerson: (Long) -> Unit
) {
    val persons by viewModel.allPersons.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPersonId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(persons) {
        if (persons.isNotEmpty() && selectedPersonId == null) {
            selectedPersonId = persons.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Salud Familiar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal list of persons
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(persons) { person ->
                    val isSelected = selectedPersonId == person.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedPersonId = person.id }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = person.emoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = person.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (persons.size < 5) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showAddDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Añadir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (persons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay personas registradas. Toca Añadir.", modifier = Modifier.padding(16.dp))
                }
            } else if (selectedPersonId != null) {
                DashboardContent(personId = selectedPersonId!!, viewModel = viewModel, onNavigateToPerson = onNavigateToPerson)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("😊") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Persona") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth().testTag("person_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth().testTag("person_emoji_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && emoji.isNotBlank()) {
                            viewModel.insertPerson(name, emoji)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_person_button")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DashboardContent(personId: Long, viewModel: AppViewModel, onNavigateToPerson: (Long) -> Unit) {
    val context = LocalContext.current
    val upcomingDose by viewModel.getUpcomingDose(personId).collectAsStateWithLifecycle()
    val upcomingTreatment by produceState<com.fronterait.saludfamiliar.data.Treatment?>(initialValue = null, upcomingDose) {
        if (upcomingDose != null) {
            viewModel.getTreatmentById(upcomingDose!!.treatmentId).collect {
                value = it
            }
        } else {
            value = null
        }
    }
    
    val feverRecords by viewModel.getFeverRecords(personId).collectAsStateWithLifecycle()
    val moodRecords by viewModel.getMoodRecords(personId).collectAsStateWithLifecycle()

    val latestFever = feverRecords.firstOrNull()
    val latestMood = moodRecords.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .let { m ->
                val scrollState = rememberScrollState()
                m.verticalScroll(scrollState)
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Próxima Medicación
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToPerson(personId) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (upcomingDose != null && upcomingTreatment != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text("Próxima Medicación", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${upcomingTreatment!!.medication} ${upcomingTreatment!!.dose}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        // Tag for "En X min" could go here
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(upcomingDose!!.scheduledTime))
                        Text(formattedTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Button(onClick = { viewModel.markDoseTaken(context, upcomingDose!!, true) }) {
                            Text("Registrar Toma")
                        }
                    }
                } else {
                    Text("Próxima Medicación", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay medicación programada", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Fiebre
            Card(
                modifier = Modifier.weight(1f).clickable { onNavigateToPerson(personId) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FIEBRE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (latestFever != null) {
                        Text("${latestFever.temperature}°C", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val status = if (latestFever.temperature > 38.0) "Fiebre alta" else if (latestFever.temperature > 37.5) "Febrícula" else "Normal"
                        val color = if (latestFever.temperature > 37.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
                    } else {
                        Text("--", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Sin datos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Humor
            Card(
                modifier = Modifier.weight(1f).clickable { onNavigateToPerson(personId) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("HUMOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (latestMood != null) {
                        val emoji = latestMood.state.substringBefore(" ")
                        val text = latestMood.state.substringAfter(" ")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Text("--", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Sin datos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        // Let's add a button to enter the full details
        OutlinedButton(onClick = { onNavigateToPerson(personId) }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver Detalles y Cargar Datos")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
