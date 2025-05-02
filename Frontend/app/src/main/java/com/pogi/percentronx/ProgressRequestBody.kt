@file:Suppress("SpellCheckingInspection", "PrivatePropertyName")

package com.pogi.percentronx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong


class ProgressRequestBody(
    context: Context,
    private val uri: Uri,
    private val contentType: String,
    private val onProgressUpdate: (Float) -> Unit
) : RequestBody() {

    private val contentResolver: ContentResolver = context.contentResolver
    private var fileSize: Long = -1


    private val bytesUploaded = AtomicLong(0)


    private var lastProgressUpdateTime = 0L
    private val MIN_PROGRESS_UPDATE_INTERVAL = 250L 

    init {

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }


            if (fileSize <= 0) {
                contentResolver.openFileDescriptor(uri, "r")?.use {
                    fileSize = it.statSize
                }
            }

            Log.d("ProgressRequestBody", "File size: $fileSize bytes (${fileSize / (1024 * 1024)} MB)")
        } catch (e: Exception) {
            Log.e("ProgressRequestBody", "Error determining file size: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return fileSize
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        bytesUploaded.set(0)
        lastProgressUpdateTime = System.currentTimeMillis()

        try {

            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream == null) {
                Log.e("ProgressRequestBody", "Failed to open input stream for URI: $uri")
                throw IOException("Could not open input stream for URI")
            }


            val source = inputStream.source()


            val buffer = Buffer()
            val bufferSize = 16384L 

            var read: Long
            var lastPercentReported = -1 

            while (source.read(buffer, bufferSize).also { read = it } != -1L) {

                sink.write(buffer, read)


                val totalUploaded = bytesUploaded.addAndGet(read)


                val now = System.currentTimeMillis()
                if (now - lastProgressUpdateTime >= MIN_PROGRESS_UPDATE_INTERVAL) {

                    val progress = if (fileSize > 0) {
                        totalUploaded.toFloat() / fileSize.toFloat()
                    } else {
                        -1f
                    }


                    val percent = (progress * 100).toInt()


                    if (percent != lastPercentReported && (percent % 5 == 0 || totalUploaded == fileSize)) {
                        lastPercentReported = percent
                        Log.d("ProgressRequestBody", "Upload progress: $percent% ($totalUploaded/$fileSize bytes)")
                    }


                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onProgressUpdate(progress)
                    }

                    lastProgressUpdateTime = now
                }
            }


            source.close()
            inputStream.close()


            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onProgressUpdate(1.0f)
            }

            Log.d("ProgressRequestBody", "Upload completed. Total size: $fileSize bytes")

        } catch (e: Exception) {
            Log.e("ProgressRequestBody", "Error during file upload: ${e.message}")
            e.printStackTrace()
            throw IOException("Error uploading file: ${e.message}", e)
        }
    }
}


fun formatSubmissionDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString
        outputFormat.format(date)
    } catch (e: Exception) {
        try {

            val altInputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            altInputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val date = altInputFormat.parse(dateString) ?: return dateString
            outputFormat.format(date)
        } catch (e2: Exception) {
            Log.e("DateFormatter", "Error formatting date: ${e2.message}")
            dateString
        }
    }
}