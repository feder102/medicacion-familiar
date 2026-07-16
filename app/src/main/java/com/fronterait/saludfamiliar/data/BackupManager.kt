package com.fronterait.saludfamiliar.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException

/**
 * Contenido completo de la base de datos local, serializable a JSON
 * para exportar/importar copias de seguridad.
 */
@JsonClass(generateAdapter = true)
data class BackupData(
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val persons: List<Person>,
    val feverRecords: List<FeverRecord>,
    val moodRecords: List<MoodRecord>,
    val doctorVisits: List<DoctorVisit>,
    val treatments: List<Treatment>,
    val doses: List<Dose>
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

object BackupManager {
    private val adapter = Moshi.Builder().build()
        .adapter(BackupData::class.java)
        .indent("  ")

    suspend fun createBackup(repository: AppRepository): BackupData = BackupData(
        exportedAt = System.currentTimeMillis(),
        persons = repository.getAllPersonsOnce(),
        feverRecords = repository.getAllFeverRecordsOnce(),
        moodRecords = repository.getAllMoodRecordsOnce(),
        doctorVisits = repository.getAllDoctorVisitsOnce(),
        treatments = repository.getAllTreatmentsOnce(),
        doses = repository.getAllDosesOnce()
    )

    fun writeBackup(context: Context, uri: Uri, backup: BackupData) {
        val json = adapter.toJson(backup)
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("No se pudo abrir el archivo de destino")
    }

    fun readBackup(context: Context, uri: Uri): BackupData {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IOException("No se pudo abrir el archivo")
        val backup = adapter.fromJson(json) ?: throw IOException("Archivo de backup vacío o inválido")
        if (backup.formatVersion > BackupData.FORMAT_VERSION) {
            throw IOException("El backup fue creado con una versión más nueva de la app")
        }
        return backup
    }
}
