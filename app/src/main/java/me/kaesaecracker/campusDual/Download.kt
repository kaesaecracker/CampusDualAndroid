package me.kaesaecracker.campusDual

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.d
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

const val ScheduleSettingsKey: String = "pref_schedule_data"
const val WidgetDataSettingsKey: String = "pref_widget_data"

private class ScheduleDeserializer : ResponseDeserializable<List<JsonLesson>> {
    override fun deserialize(content: String) = Gson().fromJson<List<JsonLesson>>(content, object : TypeToken<List<JsonLesson>>() {}.type)!!
}

fun downloadAndSaveToSettings(context: Context): Boolean {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val userId = prefs.getString("pref_userId", "") ?: ""
    val password = prefs.getString("pref_password", "") ?: ""

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
            "hash" to password,
            "start" to today.getUnixTimestamp(),
            "end" to inWeeks.getUnixTimestamp()
    )).responseObject(ScheduleDeserializer())
    Log.d("download", "got response")

    val (jsonSchedule, err) = result
    if (err != null || jsonSchedule == null) {
        Log.w("download", err)
        return false
    }

    d("download", "got schedule")

    val schedule = mutableListOf<Schoolday>()
    for (jsonLesson in jsonSchedule) {
        val lesson = Lesson(
                jsonLesson.title,
                jsonLesson.start.toLong(),
                jsonLesson.end.toLong(),
                jsonLesson.room,
                jsonLesson.instructor
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

    d("download", "items: ${schedule.size}, first: $gsonFirstDay")

    return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(WidgetDataSettingsKey, gsonFirstDay)
            .putString(ScheduleSettingsKey, gsonSchedule)
            .commit()
}