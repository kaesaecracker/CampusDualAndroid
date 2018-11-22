package xyz.mattishub.campusDual

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Menu
import androidx.annotation.ColorRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.kaesaecracker.campusDual.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


val AppTimeZone: DateTimeZone = DateTimeZone.forOffsetHours(+1)

fun DateTime.getUnixTimestamp() = this.millis / 1000

private inline fun <reified T> Gson.myJson(o: Any) = this.toJson(o, object : TypeToken<T>() {}.type)
private inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun List<Schoolday>.toLessonList(): MutableList<Lesson> {
    val list = mutableListOf<Lesson>()
    for (day in this)
        for (lesson in day.lessons)
            list.add(lesson)

    return list
}

private val gson = GsonBuilder().create()

fun dayToString(day: Schoolday): String? = gson.myJson<Schoolday>(day)
fun scheduleToString(schedule: List<Schoolday>): String? = gson.myJson<List<Schoolday>>(schedule)

fun stringToSchedule(str: String): List<Schoolday>? = gson.fromJson(str)
fun stringToDay(str: String): Schoolday? = gson.fromJson<Schoolday>(str)

fun parseJsonSchedule(str: String): List<JsonLesson>? = gson.fromJson(str)

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

fun tintMenuIcon(context: Context, menu: Menu, id: Int, @ColorRes color: Int) {
    val item = menu.findItem(id)
    if (item != null) {
        val normalDrawable = item.icon
        val wrapDrawable = DrawableCompat.wrap(normalDrawable)
        if (Build.VERSION.SDK_INT >= 23) {
            DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color, context.theme))
        } else {
            @Suppress("DEPRECATION")
            DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))
        }

        item.icon = wrapDrawable
    }
}

fun openChromeCustomTab(url: String, context: Context) {
    Log.d("log", "chrome custom tab $url")
    val builder = CustomTabsIntent.Builder()

    builder.setToolbarColor(context.resources.getColor(R.color.colorPrimary))
    builder.setSecondaryToolbarColor(context.resources.getColor(R.color.colorAccent))
    builder.setShowTitle(true)

    val customTabsIntent = builder.build()
    customTabsIntent.launchUrl(context, Uri.parse(url))
}

fun forceRefreshWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ScheduleWidgetProvider::class.java))
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_lessonList)
}

val Fragment.mainActivity: MainActivity
    get() = this.activity as MainActivity

fun SharedPreferences.Editor.put(pair: Pair<String, Any>): SharedPreferences.Editor {
    val key = pair.first
    val value = pair.second
    when (value) {
        is String -> putString(key, value)
        is Int -> putInt(key, value)
        is Boolean -> putBoolean(key, value)
        is Long -> putLong(key, value)
        is Float -> putFloat(key, value)
        else -> error("Only primitive types can be stored in SharedPreferences")
    }

    return this
}