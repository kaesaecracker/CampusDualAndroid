package xyz.mattishub.campusDual

import android.content.Context
import android.util.Log.d
import androidx.work.*
import java.util.concurrent.TimeUnit

class RefreshScheduleWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        d("log", "doWork")
        return if (downloadAndSaveToSettings(context)) Result.success() else Result.retry()
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