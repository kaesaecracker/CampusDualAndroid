package xyz.mattishub.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log.d
import androidx.work.*
import java.util.concurrent.TimeUnit

const val setting_last_background_state = "setting_last_background_state"

class RefreshScheduleWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        d("log", "doWork")
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        val result = if (downloadAndSaveToSettings(context)) {
            prefEditor.putString(setting_last_background_state, "SUCCESS")
            Result.success()
        } else{
            prefEditor.putString(setting_last_background_state, "FAILED")
            Result.retry()
        }

        prefEditor.apply()
        return result
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