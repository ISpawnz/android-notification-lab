package com.spawn.capture.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spawn.capture.data.AppDatabase
import com.spawn.capture.net.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(applicationContext).eventDao()
        val events = dao.pending()
        if (events.isEmpty()) return@withContext Result.success()
        try {
            val code = ApiClient.post(events)
            if (code in 200..299) {
                dao.delete(events.map { it.id })
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
