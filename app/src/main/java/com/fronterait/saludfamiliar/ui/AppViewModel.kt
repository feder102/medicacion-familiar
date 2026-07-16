package com.fronterait.saludfamiliar.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fronterait.saludfamiliar.data.*
import com.fronterait.saludfamiliar.notifications.AlarmScheduler
import com.fronterait.saludfamiliar.util.CalendarHelper
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la próxima medicación de una persona para el dashboard. */
sealed interface UpcomingMedication {
    data object Loading : UpcomingMedication
    data object None : UpcomingMedication
    data class Scheduled(val dose: Dose, val treatment: Treatment) : UpcomingMedication
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
    }

    // Los flows por id se cachean: crear un StateFlow nuevo en cada llamada desde
    // composición reinicia la colección en su valor inicial y hace que la UI
    // parpadee entre el estado vacío y los datos en cada recomposición.
    // Un valor `null` en los flows de listas significa "cargando".
    private val personFlows = ConcurrentHashMap<Long, StateFlow<Person?>>()
    private val feverFlows = ConcurrentHashMap<Long, StateFlow<List<FeverRecord>?>>()
    private val moodFlows = ConcurrentHashMap<Long, StateFlow<List<MoodRecord>?>>()
    private val doctorVisitFlows = ConcurrentHashMap<Long, StateFlow<List<DoctorVisit>?>>()
    private val treatmentFlows = ConcurrentHashMap<Long, StateFlow<List<Treatment>?>>()
    private val doseFlows = ConcurrentHashMap<Long, StateFlow<List<Dose>?>>()
    private val upcomingMedicationFlows = ConcurrentHashMap<Long, StateFlow<UpcomingMedication>>()

    val allPersons: StateFlow<List<Person>> = repository.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertPerson(name: String, emoji: String) {
        viewModelScope.launch {
            repository.insertPerson(Person(name = name, emoji = emoji))
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    /**
     * Elimina el perfil y todos sus registros. Antes de borrar, cancela los
     * recordatorios pendientes y los eventos de calendario de sus tratamientos.
     */
    fun deletePerson(context: Context, personId: Long) {
        viewModelScope.launch {
            val doses = repository.getDosesForPersonOnce(personId)
            doses.forEach { dose ->
                AlarmScheduler.cancelDoseReminder(context, dose.id)
                dose.calendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            }
            repository.deletePerson(personId)
        }
    }

    fun getPersonById(id: Long): StateFlow<Person?> = personFlows.getOrPut(id) {
        repository.getPersonById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getFeverRecords(personId: Long): StateFlow<List<FeverRecord>?> = feverFlows.getOrPut(personId) {
        repository.getFeverRecords(personId)
            .map<List<FeverRecord>, List<FeverRecord>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun insertFeverRecord(personId: Long, temperature: Double, timestamp: Long) {
        viewModelScope.launch {
            repository.insertFeverRecord(FeverRecord(personId = personId, temperature = temperature, timestamp = timestamp))
        }
    }

    fun updateFeverRecord(record: FeverRecord) {
        viewModelScope.launch { repository.updateFeverRecord(record) }
    }

    fun deleteFeverRecord(id: Long) {
        viewModelScope.launch { repository.deleteFeverRecord(id) }
    }

    fun getMoodRecords(personId: Long): StateFlow<List<MoodRecord>?> = moodFlows.getOrPut(personId) {
        repository.getMoodRecords(personId)
            .map<List<MoodRecord>, List<MoodRecord>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun insertMoodRecord(personId: Long, state: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertMoodRecord(MoodRecord(personId = personId, state = state, timestamp = timestamp))
        }
    }

    fun updateMoodRecord(record: MoodRecord) {
        viewModelScope.launch { repository.updateMoodRecord(record) }
    }

    fun deleteMoodRecord(id: Long) {
        viewModelScope.launch { repository.deleteMoodRecord(id) }
    }

    fun getDoctorVisits(personId: Long): StateFlow<List<DoctorVisit>?> = doctorVisitFlows.getOrPut(personId) {
        repository.getDoctorVisits(personId)
            .map<List<DoctorVisit>, List<DoctorVisit>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun insertDoctorVisit(personId: Long, timestamp: Long, doctorName: String, notes: String) {
        viewModelScope.launch {
            repository.insertDoctorVisit(DoctorVisit(personId = personId, timestamp = timestamp, doctorName = doctorName, notes = notes))
        }
    }

    fun updateDoctorVisit(visit: DoctorVisit) {
        viewModelScope.launch { repository.updateDoctorVisit(visit) }
    }

    fun deleteDoctorVisit(id: Long) {
        viewModelScope.launch { repository.deleteDoctorVisit(id) }
    }

    fun getTreatments(personId: Long): StateFlow<List<Treatment>?> = treatmentFlows.getOrPut(personId) {
        repository.getTreatments(personId)
            .map<List<Treatment>, List<Treatment>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getDoses(treatmentId: Long): StateFlow<List<Dose>?> = doseFlows.getOrPut(treatmentId) {
        repository.getDoses(treatmentId)
            .map<List<Dose>, List<Dose>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getUpcomingMedication(personId: Long): StateFlow<UpcomingMedication> =
        upcomingMedicationFlows.getOrPut(personId) {
            minuteTicker()
                .flatMapLatest { now -> repository.getUpcomingDose(personId, now) }
                .distinctUntilChanged()
                .flatMapLatest { dose ->
                    if (dose == null) {
                        flowOf(UpcomingMedication.None)
                    } else {
                        repository.getTreatmentById(dose.treatmentId).map { treatment ->
                            if (treatment == null) UpcomingMedication.None
                            else UpcomingMedication.Scheduled(dose, treatment)
                        }
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpcomingMedication.Loading)
        }

    // Refresca "ahora" cada minuto para que la próxima dosis avance sola sin
    // depender de recomposiciones.
    private fun minuteTicker(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    fun createTreatment(
        context: Context,
        personName: String,
        personId: Long,
        medication: String,
        doseDesc: String,
        freqHours: Int,
        durationDays: Int,
        startTimestamp: Long
    ) {
        viewModelScope.launch {
            val treatment = Treatment(
                personId = personId,
                medication = medication,
                dose = doseDesc,
                freqHours = freqHours,
                durationDays = durationDays,
                startTimestamp = startTimestamp,
                active = true
            )
            val treatmentId = repository.insertTreatment(treatment)

            val calendarId = CalendarHelper.getPrimaryCalendarId(context)

            val doses = mutableListOf<Dose>()
            var currentDoseTime = startTimestamp
            val endTimestamp = startTimestamp + (durationDays * 24L * 60L * 60L * 1000L)

            while (currentDoseTime <= endTimestamp) {
                var eventId: Long? = null
                if (calendarId != null) {
                    val title = "Medicación: $medication - $personName"
                    val desc = "Dosis: $doseDesc. Registrado en Salud Familiar."
                    eventId = CalendarHelper.addEvent(context, calendarId, title, desc, currentDoseTime)
                }

                doses.add(Dose(
                    treatmentId = treatmentId,
                    scheduledTime = currentDoseTime,
                    calendarEventId = eventId
                ))

                currentDoseTime += (freqHours * 60L * 60L * 1000L)
            }

            val doseIds = repository.insertDoses(doses)
            doses.zip(doseIds).forEach { (dose, doseId) ->
                AlarmScheduler.scheduleDoseReminder(
                    context = context,
                    doseId = doseId,
                    scheduledTime = dose.scheduledTime,
                    personId = personId,
                    personName = personName,
                    medication = medication,
                    doseDesc = doseDesc
                )
            }
        }
    }

    fun cancelTreatment(context: Context, treatment: Treatment) {
        viewModelScope.launch {
            repository.updateTreatment(treatment.copy(active = false))

            val doses = repository.getDosesForTreatmentOnce(treatment.id)
            val currentTime = System.currentTimeMillis()
            doses.forEach { dose ->
                if (dose.scheduledTime > currentTime) {
                    dose.calendarEventId?.let { eventId ->
                        CalendarHelper.deleteEvent(context, eventId)
                    }
                    AlarmScheduler.cancelDoseReminder(context, dose.id)
                    repository.updateDose(dose.copy(calendarEventId = null)) // clear the event id so we don't try to delete it again
                }
            }
        }
    }

    fun markDoseTaken(context: Context, dose: Dose, taken: Boolean) {
        viewModelScope.launch {
            repository.updateDose(dose.copy(taken = taken))
            if (taken) {
                AlarmScheduler.cancelDoseReminder(context, dose.id)
            }
        }
    }

    /**
     * Actualiza el nombre del medicamento y la descripción de la dosis.
     * La frecuencia y duración no se modifican porque cambiarían las tomas
     * ya generadas. Los recordatorios futuros se reprograman con el texto nuevo.
     */
    fun updateTreatmentDetails(
        context: Context,
        treatment: Treatment,
        personName: String,
        medication: String,
        doseDesc: String
    ) {
        viewModelScope.launch {
            repository.updateTreatment(treatment.copy(medication = medication, dose = doseDesc))
            if (treatment.active) {
                val now = System.currentTimeMillis()
                repository.getDosesForTreatmentOnce(treatment.id)
                    .filter { !it.taken && it.scheduledTime > now }
                    .forEach { dose ->
                        AlarmScheduler.scheduleDoseReminder(
                            context = context,
                            doseId = dose.id,
                            scheduledTime = dose.scheduledTime,
                            personId = treatment.personId,
                            personName = personName,
                            medication = medication,
                            doseDesc = doseDesc
                        )
                    }
            }
        }
    }

    /** Elimina el tratamiento con sus tomas, recordatorios y eventos de calendario. */
    fun deleteTreatment(context: Context, treatment: Treatment) {
        viewModelScope.launch {
            val doses = repository.getDosesForTreatmentOnce(treatment.id)
            doses.forEach { dose ->
                AlarmScheduler.cancelDoseReminder(context, dose.id)
                dose.calendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            }
            repository.deleteTreatment(treatment.id)
        }
    }

    // ---- Copias de seguridad ----

    /** Exporta toda la base de datos como JSON al archivo elegido por el usuario. */
    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = BackupManager.createBackup(repository)
                withContext(Dispatchers.IO) {
                    BackupManager.writeBackup(context, uri, backup)
                }
                onResult(true, "Copia de seguridad exportada correctamente")
            } catch (e: Exception) {
                onResult(false, "No se pudo exportar la copia de seguridad")
            }
        }
    }

    /**
     * Restaura una copia de seguridad reemplazando todos los datos actuales.
     * Cancela los recordatorios existentes y reprograma los del backup.
     */
    fun importBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = withContext(Dispatchers.IO) {
                    BackupManager.readBackup(context, uri)
                }

                val now = System.currentTimeMillis()
                repository.getPendingDoseReminders(now).forEach {
                    AlarmScheduler.cancelDoseReminder(context, it.id)
                }

                repository.replaceAllData(backup)

                repository.getPendingDoseReminders(now).forEach { reminder ->
                    AlarmScheduler.scheduleDoseReminder(
                        context = context,
                        doseId = reminder.id,
                        scheduledTime = reminder.scheduledTime,
                        personId = reminder.personId,
                        personName = reminder.personName,
                        medication = reminder.medication,
                        doseDesc = reminder.doseDesc
                    )
                }
                onResult(true, "Datos restaurados correctamente")
            } catch (e: Exception) {
                onResult(false, "No se pudo importar: el archivo no es un backup válido")
            }
        }
    }
}
