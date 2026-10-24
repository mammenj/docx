package com.jsmammen.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object BackupStorage {
    const val AUTO_BACKUP_NAME = "OutpatientAutoBackup.json"

    fun save(context: Context, fileName: String, content: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, content)
        } else {
            saveToPublicDownloads(context, fileName, content)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(context: Context, fileName: String, content: String): Uri? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // Remove previous backups (including older timestamped files) so Downloads stays clean.
        context.contentResolver.delete(
            collection,
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("OutpatientBackup%"),
        )
        context.contentResolver.delete(
            collection,
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("OutpatientAutoBackup%"),
        )
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(collection, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        return uri
    }

    private fun saveToPublicDownloads(context: Context, fileName: String, content: String): Uri? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("OutpatientBackup") || file.name.startsWith("OutpatientAutoBackup")) {
                file.delete()
            }
        }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }
        return Uri.fromFile(file)
    }
}
