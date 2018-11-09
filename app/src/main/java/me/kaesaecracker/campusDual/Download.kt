package me.kaesaecracker.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.d
import android.util.Log.w
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

const val ScheduleSettingsKey: String = "pref_schedule_data"
const val WidgetDataSettingsKey: String = "pref_widget_data"

private class ScheduleDeserializer : ResponseDeserializable<List<JsonLesson>> {
    override fun deserialize(content: String): List<JsonLesson> {
        return Gson().fromJson<List<JsonLesson>>(content, object : TypeToken<List<JsonLesson>>() {}.type)
    }
}

fun downloadAndSaveToSettings(context: Context): Boolean {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val userId = prefs.getString(SettingsFragment.setting_matric, "") ?: ""
    val hash = prefs.getString(SettingsFragment.setting_hash, "") ?: ""

    Log.d("download", "refresh")
    val urlBase = context.resources.getString(R.string.backend_url)

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

    val schedule = mutableListOf<Schoolday>()
    for (jsonLesson in jsonSchedule) {
        val lesson = Lesson(
                jsonLesson.title.trim(),
                jsonLesson.start.toLong(),
                jsonLesson.end.toLong(),
                jsonLesson.room.trim(),
                jsonLesson.instructor.trim()
        )

        if (schedule.isEmpty() || schedule.last().date!!.dayOfYear != lesson.start.dayOfYear) {
            val newSchoolday = Schoolday(jsonLesson.start.toLong(), mutableListOf(lesson))
            schedule.add(newSchoolday)
        } else {
            schedule.last().lessons.add(lesson)
        }
    }

    val gsonFirstDay = dayToString(schedule.first())
    val gsonSchedule = scheduleToString(schedule) ?: return false

    d("download", "got ${schedule.size} items")
    return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(WidgetDataSettingsKey, gsonFirstDay)
            .putString(ScheduleSettingsKey, gsonSchedule)
            .commit()
}