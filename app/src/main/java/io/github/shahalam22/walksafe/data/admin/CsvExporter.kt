package io.github.shahalam22.walksafe.data.admin

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Saves a file in the phone's Downloads folder. */
@Singleton
class CsvExporter @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun saveToDownloads(name: String, text: String) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the file")
        resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Could not write the file")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
