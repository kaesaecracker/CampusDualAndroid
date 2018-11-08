package me.kaesaecracker.campusDual

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


val AppTimeZone: DateTimeZone = DateTimeZone.forOffsetHours(+1)

fun DateTime.getUnixTimestamp() = this.millis / 1000

private inline fun <reified T> Gson.myJson(o: Any) = this.toJson(o, object : TypeToken<T>() {}.type)
private inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun List<Schoolday>.toLessonList(): MutableList<Lesson> {
    d("statics", "toLessonsList: in ${this.size} elements")

    val list = mutableListOf<Lesson>()
    for (day in this)
        for (lesson in day.lessons)
            list.add(lesson)

    d("statics", "toLessonsList: out ${list.size} elements")
    return list
}

private val gson = GsonBuilder().create()

fun dayToString(day: Schoolday): String? = gson.myJson<Schoolday>(day)
fun scheduleToString(schedule: List<Schoolday>): String? = gson.myJson<List<Schoolday>>(schedule)

fun stringToSchedule(str: String): List<Schoolday>? = gson.fromJson<List<Schoolday>>(str)
fun stringToDay(str: String): Schoolday? = gson.fromJson<Schoolday>(str)


fun List<Schoolday>?.getCurrentDay(): Schoolday? {
    if (this == null) return null

    val today = DateTime(AppTimeZone)
    for (day in this) {
        if (day.date!!.dayOfYear == today.dayOfYear || day.date!!.isAfterNow)
            return day
    }

    return null
}

fun List<Lesson>.getNonPassedLessons(): List<Lesson> {
    val list = mutableListOf<Lesson>()

    for (lesson in this) {
        if (lesson.end.isAfterNow) list.add(lesson)
    }

    return list
}