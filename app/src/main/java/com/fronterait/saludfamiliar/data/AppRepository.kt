package com.fronterait.saludfamiliar.data

class AppRepository(private val appDao: AppDao) {
    fun getAllPersons() = appDao.getAllPersons()
    fun getPersonById(id: Long) = appDao.getPersonById(id)
    suspend fun insertPerson(person: Person) = appDao.insertPerson(person)
    suspend fun deletePerson(id: Long) = appDao.deletePerson(id)

    fun getFeverRecords(personId: Long) = appDao.getFeverRecordsForPerson(personId)
    suspend fun insertFeverRecord(record: FeverRecord) = appDao.insertFeverRecord(record)

    fun getMoodRecords(personId: Long) = appDao.getMoodRecordsForPerson(personId)
    suspend fun insertMoodRecord(record: MoodRecord) = appDao.insertMoodRecord(record)

    fun getDoctorVisits(personId: Long) = appDao.getDoctorVisitsForPerson(personId)
    suspend fun insertDoctorVisit(visit: DoctorVisit) = appDao.insertDoctorVisit(visit)

    fun getTreatments(personId: Long) = appDao.getTreatmentsForPerson(personId)
    fun getTreatmentById(id: Long) = appDao.getTreatmentById(id)
    suspend fun insertTreatment(treatment: Treatment): Long = appDao.insertTreatment(treatment)
    suspend fun updateTreatment(treatment: Treatment) = appDao.updateTreatment(treatment)

    fun getDoses(treatmentId: Long) = appDao.getDosesForTreatment(treatmentId)
    fun getUpcomingDose(personId: Long, currentTime: Long) = appDao.getUpcomingDoseForPerson(personId, currentTime)
    suspend fun insertDoses(doses: List<Dose>): List<Long> = appDao.insertDoses(doses)
    suspend fun updateDose(dose: Dose) = appDao.updateDose(dose)
    suspend fun getDosesWithCalendarEvents(treatmentId: Long) = appDao.getDosesWithCalendarEventsForTreatment(treatmentId)
    suspend fun getDosesForTreatmentOnce(treatmentId: Long) = appDao.getDosesForTreatmentOnce(treatmentId)
    suspend fun markDoseTakenById(doseId: Long) = appDao.markDoseTakenById(doseId)
}
