package xyz.mattishub.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log.d
import android.util.Log.w
import com.github.kittinunf.fuel.httpGet
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import xyz.mattishub.campusDual.fragments.SettingsFragment

const val ScheduleSettingsKey: String = "pref_schedule_data_v2"
const val LastRefreshSettingsKey: String = "pref_last_download"

fun downloadAndSaveToSettings(context: Context): Boolean {
    fun jsonToInternal(jsonLessons: List<JsonLesson>): LessonList {
        val schedule = mutableListOf<Lesson>()
        for (jsonLesson in jsonLessons) {
            val lesson = Lesson(
                    jsonLesson.title.trim(),
                    jsonLesson.start.toLong(),
                    jsonLesson.end.toLong(),
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
    val urlBase = prefs.getString(SettingsFragment.setting_backend, context.getString(R.string.default_backend_url))
            ?: ""

    val today = DateTime(DateTimeZone.UTC)
            .withHourOfDay(0)
            .withMinuteOfHour(0)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
            .plusDays(12)
    val inWeeks = today.plusWeeks(4)

    val (_, _, result) = urlBase.httpGet(listOf(
            "userid" to userId,
            "hash" to hash,
            "start" to today.getUnixTimestamp(),
            "end" to inWeeks.getUnixTimestamp()
    )).responseString()

    val (jsonScheduleString, err) = result
    if (err != null || jsonScheduleString == null) {
        w("download", "result: err='$err', str='$jsonScheduleString'")
        return false
    }

    val jsonSchedule = parseJsonSchedule(jsonScheduleString)
    if (jsonSchedule == null) {
        w("download", "could not deserialize json schedule string '$jsonScheduleString'")
        return false
    }

    val schedule = jsonToInternal(jsonSchedule)

    val gsonSchedule = scheduleToString(schedule) ?: return false
    val lastRefreshMillis = DateTime(AppTimeZone).millis

    d("download", "got ${schedule.size} items")
    return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .put(ScheduleSettingsKey to gsonSchedule)
            .put(LastRefreshSettingsKey to lastRefreshMillis)
            .commit()
}

