package me.kaesaecracker.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.d
import androidx.work.*
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit

class RefreshScheduleWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        d("log", "doWork")
        return if (downloadAndSaveToSettings(context)) Result.SUCCESS else Result.RETRY
    }

}

fun setupBackgroundWorker() {
    val request = PeriodicWorkRequestBuilder<RefreshScheduleWorker>(
            12, TimeUnit.HOURS,
            1, TimeUnit.HOURS
    ).setConstraints(
            Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
    ).keepResultsForAtLeast(1, TimeUnit.HOURS)
            .build()

    WorkManager.getInstance().enqueueUniquePeriodicWork(
            "background_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
    )
}