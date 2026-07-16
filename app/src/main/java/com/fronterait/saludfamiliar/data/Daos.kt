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

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePerson(id: Long)

    // Fever
    @Query("SELECT * FROM fever_records WHERE personId = :personId ORDER BY timestamp DESC")
    fun getFeverRecordsForPerson(personId: Long): Flow<List<FeverRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeverRecord(feverRecord: FeverRecord)

    // Mood
    @Query("SELECT * FROM mood_records WHERE personId = :personId ORDER BY timestamp DESC")
    fun getMoodRecordsForPerson(personId: Long): Flow<List<MoodRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodRecord(moodRecord: MoodRecord)

    // Doctor Visit
    @Query("SELECT * FROM doctor_visits WHERE personId = :personId ORDER BY timestamp DESC")
    fun getDoctorVisitsForPerson(personId: Long): Flow<List<DoctorVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorVisit(visit: DoctorVisit)

    // Treatment
    @Query("SELECT * FROM treatments WHERE personId = :personId ORDER BY startTimestamp DESC")
    fun getTreatmentsForPerson(personId: Long): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    fun getTreatmentById(id: Long): Flow<Treatment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatment(treatment: Treatment): Long

    @Update
    suspend fun updateTreatment(treatment: Treatment)

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
}
