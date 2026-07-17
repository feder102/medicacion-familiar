package com.fronterait.saludfamiliar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class DoseReminderInfo(
    val id: Long,
    val scheduledTime: Long,
    val medication: String,
    val doseDesc: String,
    val personId: Long,
    val personName: String
)

@Dao
interface AppDao {
    // Person
    @Query("SELECT * FROM persons")
    fun getAllPersons(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonById(id: Long): Flow<Person?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePerson(id: Long)

    // Fever
    @Query("SELECT * FROM fever_records WHERE personId = :personId ORDER BY timestamp DESC")
    fun getFeverRecordsForPerson(personId: Long): Flow<List<FeverRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeverRecord(feverRecord: FeverRecord)

    @Update
    suspend fun updateFeverRecord(feverRecord: FeverRecord)

    @Query("DELETE FROM fever_records WHERE id = :id")
    suspend fun deleteFeverRecord(id: Long)

    // Mood
    @Query("SELECT * FROM mood_records WHERE personId = :personId ORDER BY timestamp DESC")
    fun getMoodRecordsForPerson(personId: Long): Flow<List<MoodRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodRecord(moodRecord: MoodRecord)

    @Update
    suspend fun updateMoodRecord(moodRecord: MoodRecord)

    @Query("DELETE FROM mood_records WHERE id = :id")
    suspend fun deleteMoodRecord(id: Long)

    // Doctor Visit
    @Query("SELECT * FROM doctor_visits WHERE personId = :personId ORDER BY timestamp DESC")
    fun getDoctorVisitsForPerson(personId: Long): Flow<List<DoctorVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorVisit(visit: DoctorVisit)

    @Update
    suspend fun updateDoctorVisit(visit: DoctorVisit)

    @Query("DELETE FROM doctor_visits WHERE id = :id")
    suspend fun deleteDoctorVisit(id: Long)

    // Treatment
    @Query("SELECT * FROM treatments WHERE personId = :personId ORDER BY startTimestamp DESC")
    fun getTreatmentsForPerson(personId: Long): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    fun getTreatmentById(id: Long): Flow<Treatment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatment(treatment: Treatment): Long

    @Update
    suspend fun updateTreatment(treatment: Treatment)

    @Query("DELETE FROM treatments WHERE id = :id")
    suspend fun deleteTreatment(id: Long)

    // Dose
    @Query("SELECT * FROM doses WHERE treatmentId = :treatmentId ORDER BY scheduledTime ASC")
    fun getDosesForTreatment(treatmentId: Long): Flow<List<Dose>>

    @Query("SELECT d.* FROM doses d INNER JOIN treatments t ON d.treatmentId = t.id WHERE t.personId = :personId AND t.active = 1 AND d.taken = 0 AND d.scheduledTime > :currentTime ORDER BY d.scheduledTime ASC LIMIT 1")
    fun getUpcomingDoseForPerson(personId: Long, currentTime: Long): Flow<Dose?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoses(doses: List<Dose>): List<Long>

    @Update
    suspend fun updateDose(dose: Dose)

    @Query("SELECT * FROM doses WHERE calendarEventId IS NOT NULL AND treatmentId = :treatmentId")
    suspend fun getDosesWithCalendarEventsForTreatment(treatmentId: Long): List<Dose>

    @Query("SELECT * FROM doses WHERE treatmentId = :treatmentId")
    suspend fun getDosesForTreatmentOnce(treatmentId: Long): List<Dose>

    @Query("UPDATE doses SET taken = 1 WHERE id = :doseId")
    suspend fun markDoseTakenById(doseId: Long)

    @Query("""
        SELECT d.id AS id, d.scheduledTime AS scheduledTime, t.medication AS medication, t.dose AS doseDesc, p.id AS personId, p.name AS personName
        FROM doses d
        INNER JOIN treatments t ON d.treatmentId = t.id
        INNER JOIN persons p ON t.personId = p.id
        WHERE t.active = 1 AND d.taken = 0 AND d.scheduledTime > :currentTime
    """)
    suspend fun getPendingDoseReminders(currentTime: Long): List<DoseReminderInfo>

    @Query("SELECT d.* FROM doses d INNER JOIN treatments t ON d.treatmentId = t.id WHERE t.personId = :personId")
    suspend fun getDosesForPersonOnce(personId: Long): List<Dose>

    // Copias de seguridad: lecturas puntuales de todas las tablas
    @Query("SELECT * FROM persons")
    suspend fun getAllPersonsOnce(): List<Person>

    @Query("SELECT * FROM fever_records")
    suspend fun getAllFeverRecordsOnce(): List<FeverRecord>

    @Query("SELECT * FROM mood_records")
    suspend fun getAllMoodRecordsOnce(): List<MoodRecord>

    @Query("SELECT * FROM doctor_visits")
    suspend fun getAllDoctorVisitsOnce(): List<DoctorVisit>

    @Query("SELECT * FROM treatments")
    suspend fun getAllTreatmentsOnce(): List<Treatment>

    @Query("SELECT * FROM doses")
    suspend fun getAllDosesOnce(): List<Dose>

    // Restauración: se borra todo (las tablas hijas caen en cascada desde persons)
    // y se reinserta el contenido del backup preservando los ids originales.
    @Query("DELETE FROM persons")
    suspend fun deleteAllPersons()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersons(persons: List<Person>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeverRecords(records: List<FeverRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodRecords(records: List<MoodRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorVisits(visits: List<DoctorVisit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatments(treatments: List<Treatment>)

    @Transaction
    suspend fun replaceAllData(
        persons: List<Person>,
        feverRecords: List<FeverRecord>,
        moodRecords: List<MoodRecord>,
        doctorVisits: List<DoctorVisit>,
        treatments: List<Treatment>,
        doses: List<Dose>
    ) {
        deleteAllPersons()
        insertPersons(persons)
        insertFeverRecords(feverRecords)
        insertMoodRecords(moodRecords)
        insertDoctorVisits(doctorVisits)
        insertTreatments(treatments)
        insertDoses(doses)
    }
}
