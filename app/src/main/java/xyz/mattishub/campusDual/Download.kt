package xyz.mattishub.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log.d
import android.util.Log.w
import com.github.kittinunf.fuel.httpGet
import org.joda.time.DateTime
import xyz.mattishub.campusDual.fragments.SettingsFragment
import java.net.MalformedURLException
import java.net.URL

const val ScheduleSettingsKey: String = "pref_schedule_data_v2"
const val LastRefreshSettingsKey: String = "pref_last_download_v2"

private const val LogTag: String = "download"

fun downloadAndSaveToSettings(context: Context): Boolean {
    fun jsonToInternal(jsonLessons: List<JsonLesson>): LessonList {
        val schedule = mutableListOf<Lesson>()
        for (jsonLesson in jsonLessons) {
            val lesson = Lesson(
                    jsonLesson.title.trim(),
                    jsonLesson.start.toLongOrNull() ?: 0L,
                    jsonLesson.end.toLongOrNull() ?: 0L,
                    jsonLesson.room.trim(),
                    jsonLesson.instructor.trim()
            )

            lesson.isFirstOfDay = schedule.isEmpty()
                    || schedule.last().start.dayOfYear != lesson.start.dayOfYear
            schedule.add(lesson)
        }

        return LessonList(schedule)
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val userId = prefs.getString(SettingsFragment.setting_matric, "") ?: ""
    val hash = prefs.getString(SettingsFragment.setting_hash, "") ?: ""
    var urlBase = prefs.getString(SettingsFragment.setting_backend, context.getString(R.string.url_default_backend))
            ?: ""
    try {
        val parsedUrl = URL(urlBase)
    } catch (ex: MalformedURLException) {
        w(LogTag, "URL in settings not valid, using default one")
        val defaultBackend = context.getString(R.string.url_default_backend)
        prefs.edit().putString(SettingsFragment.setting_backend, defaultBackend).apply()
        urlBase = defaultBackend
    }

    val now = DateTime(AppTimeZone)
    val today = now
            .withHourOfDay(0)
            .withMinuteOfHour(0)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
    val inWeeks = today.plusWeeks(20)

    val (_, _, result) = urlBase.httpGet(listOf(
            "userid" to userId,
            "hash" to hash,
            "start" to today.getUnixTimestamp(),
            "end" to inWeeks.getUnixTimestamp()
    )).responseString()

    val (jsonScheduleString, err) = result
    if (err != null || jsonScheduleString == null) {
        w(LogTag, "result: err='$err', str='$jsonScheduleString'")
        return false
    }

    val jsonSchedule = parseJsonSchedule(jsonScheduleString)
    if (jsonSchedule == null) {
        w(LogTag, "could not deserialize json schedule string '$jsonScheduleString'")
        return false
    }
    if (jsonSchedule.isEmpty()) {
        w(LogTag, "got empty list! String: '$jsonScheduleString'")
        return false
    }

    var schedule = jsonToInternal(jsonSchedule)
    if (schedule.size == 0) {
        w(LogTag, "json was not empty, but lesson list is")
        return false
    }

    schedule = schedule.whereDayNotInPast()

    val gsonSchedule = scheduleToString(schedule) ?: return false
    val lastRefreshMillis = DateTime(AppTimeZone).millis

    d(LogTag, "got ${schedule.size} items")
    return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .put(ScheduleSettingsKey to gsonSchedule)
            .put(LastRefreshSettingsKey to lastRefreshMillis)
            .commit()
}

