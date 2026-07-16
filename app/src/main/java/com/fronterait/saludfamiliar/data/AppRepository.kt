package com.fronterait.saludfamiliar.data

class AppRepository(private val appDao: AppDao) {
    fun getAllPersons() = appDao.getAllPersons()
    fun getPersonById(id: Long) = appDao.getPersonById(id)
    suspend fun insertPerson(person: Person) = appDao.insertPerson(person)
    suspend fun updatePerson(person: Person) = appDao.updatePerson(person)
    suspend fun deletePerson(id: Long) = appDao.deletePerson(id)

    fun getFeverRecords(personId: Long) = appDao.getFeverRecordsForPerson(personId)
    suspend fun insertFeverRecord(record: FeverRecord) = appDao.insertFeverRecord(record)
    suspend fun updateFeverRecord(record: FeverRecord) = appDao.updateFeverRecord(record)
    suspend fun deleteFeverRecord(id: Long) = appDao.deleteFeverRecord(id)

    fun getMoodRecords(personId: Long) = appDao.getMoodRecordsForPerson(personId)
    suspend fun insertMoodRecord(record: MoodRecord) = appDao.insertMoodRecord(record)
    suspend fun updateMoodRecord(record: MoodRecord) = appDao.updateMoodRecord(record)
    suspend fun deleteMoodRecord(id: Long) = appDao.deleteMoodRecord(id)

    fun getDoctorVisits(personId: Long) = appDao.getDoctorVisitsForPerson(personId)
    suspend fun insertDoctorVisit(visit: DoctorVisit) = appDao.insertDoctorVisit(visit)
    suspend fun updateDoctorVisit(visit: DoctorVisit) = appDao.updateDoctorVisit(visit)
    suspend fun deleteDoctorVisit(id: Long) = appDao.deleteDoctorVisit(id)

    fun getTreatments(personId: Long) = appDao.getTreatmentsForPerson(personId)
    fun getTreatmentById(id: Long) = appDao.getTreatmentById(id)
    suspend fun insertTreatment(treatment: Treatment): Long = appDao.insertTreatment(treatment)
    suspend fun updateTreatment(treatment: Treatment) = appDao.updateTreatment(treatment)
    suspend fun deleteTreatment(id: Long) = appDao.deleteTreatment(id)

    fun getDoses(treatmentId: Long) = appDao.getDosesForTreatment(treatmentId)
    fun getUpcomingDose(personId: Long, currentTime: Long) = appDao.getUpcomingDoseForPerson(personId, currentTime)
    suspend fun insertDoses(doses: List<Dose>): List<Long> = appDao.insertDoses(doses)
    suspend fun updateDose(dose: Dose) = appDao.updateDose(dose)
    suspend fun getDosesWithCalendarEvents(treatmentId: Long) = appDao.getDosesWithCalendarEventsForTreatment(treatmentId)
    suspend fun getDosesForTreatmentOnce(treatmentId: Long) = appDao.getDosesForTreatmentOnce(treatmentId)
    suspend fun getDosesForPersonOnce(personId: Long) = appDao.getDosesForPersonOnce(personId)
    suspend fun markDoseTakenById(doseId: Long) = appDao.markDoseTakenById(doseId)
    suspend fun getPendingDoseReminders(currentTime: Long) = appDao.getPendingDoseReminders(currentTime)

    // Copias de seguridad
    suspend fun getAllPersonsOnce() = appDao.getAllPersonsOnce()
    suspend fun getAllFeverRecordsOnce() = appDao.getAllFeverRecordsOnce()
    suspend fun getAllMoodRecordsOnce() = appDao.getAllMoodRecordsOnce()
    suspend fun getAllDoctorVisitsOnce() = appDao.getAllDoctorVisitsOnce()
    suspend fun getAllTreatmentsOnce() = appDao.getAllTreatmentsOnce()
    suspend fun getAllDosesOnce() = appDao.getAllDosesOnce()

    suspend fun replaceAllData(backup: BackupData) = appDao.replaceAllData(
        persons = backup.persons,
        feverRecords = backup.feverRecords,
        moodRecords = backup.moodRecords,
        doctorVisits = backup.doctorVisits,
        treatments = backup.treatments,
        // Los ids de eventos de calendario del backup pueden pertenecer a otro
        // dispositivo: se descartan para no borrar eventos ajenos por error.
        doses = backup.doses.map { it.copy(calendarEventId = null) }
    )
}
