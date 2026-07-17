package com.fronterait.saludfamiliar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.squareup.moshi.JsonClass

// Las entidades se serializan con Moshi para las copias de seguridad en JSON.

@JsonClass(generateAdapter = true)
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "fever_records",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")]
)
data class FeverRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val temperature: Double,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "mood_records",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")]
)
data class MoodRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val state: String, // e.g., "BIEN", "REGULAR", "MAL", "DECAIDO"
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "doctor_visits",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")]
)
data class DoctorVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val timestamp: Long,
    val doctorName: String,
    val notes: String
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "treatments",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")]
)
data class Treatment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val medication: String,
    val dose: String,
    val freqHours: Int,
    val durationDays: Int,
    val startTimestamp: Long,
    val active: Boolean
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "doses",
    foreignKeys = [ForeignKey(entity = Treatment::class, parentColumns = ["id"], childColumns = ["treatmentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("treatmentId")]
)
data class Dose(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treatmentId: Long,
    val scheduledTime: Long,
    val calendarEventId: Long?,
    val taken: Boolean = false
)
