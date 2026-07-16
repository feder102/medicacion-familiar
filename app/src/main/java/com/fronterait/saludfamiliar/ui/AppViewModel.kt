package com.fronterait.saludfamiliar.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fronterait.saludfamiliar.data.*
import com.fronterait.saludfamiliar.notifications.AlarmScheduler
import com.fronterait.saludfamiliar.util.CalendarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
    }

    val allPersons: StateFlow<List<Person>> = repository.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertPerson(name: String, emoji: String) {
        viewModelScope.launch {
            repository.insertPerson(Person(name = name, emoji = emoji))
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            repository.deletePerson(id)
        }
    }

    fun getPersonById(id: Long): StateFlow<Person?> {
        return repository.getPersonById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getFeverRecords(personId: Long) = repository.getFeverRecords(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertFeverRecord(personId: Long, temperature: Double, timestamp: Long) {
        viewModelScope.launch {
            repository.insertFeverRecord(FeverRecord(personId = personId, temperature = temperature, timestamp = timestamp))
        }
    }

    fun getMoodRecords(personId: Long) = repository.getMoodRecords(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertMoodRecord(personId: Long, state: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertMoodRecord(MoodRecord(personId = personId, state = state, timestamp = timestamp))
        }
    }

    fun getDoctorVisits(personId: Long) = repository.getDoctorVisits(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertDoctorVisit(personId: Long, timestamp: Long, doctorName: String, notes: String) {
        viewModelScope.launch {
            repository.insertDoctorVisit(DoctorVisit(personId = personId, timestamp = timestamp, doctorName = doctorName, notes = notes))
        }
    }

    fun getTreatments(personId: Long) = repository.getTreatments(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTreatmentById(id: Long) = repository.getTreatmentById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun getDoses(treatmentId: Long) = repository.getDoses(treatmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getUpcomingDose(personId: Long) = repository.getUpcomingDose(personId, System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
