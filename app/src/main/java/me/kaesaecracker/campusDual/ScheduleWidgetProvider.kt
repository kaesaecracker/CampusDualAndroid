package me.kaesaecracker.campusDual

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log.*
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class ScheduleWidgetProvider : AppWidgetProvider() {

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        i("log", "onUpdate in AppWidgetProvider")

        // There may be multiple widgets active, so update all of them
        // build bundle
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dataString = prefs.getString("widget_data", "") ?: ""
        if (dataString.isEmpty()) {
            w("log", "returning because no data is availiable")
            return
        }

        val gson = GsonBuilder().create()!!
        val day = gson.fromJson<List<ScheduleFragment.Lesson>>(dataString)
        if (day == null) {
            w("log", "returning because day is null")
            return
        }

        for (appWidgetId in appWidgetIds) {
            // send bundle
            updateAppWidget(context, appWidgetManager, appWidgetId, day)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int, day: List<ScheduleFragment.Lesson>) {
            i("log", "updating widget $appWidgetId with ${day.size} elements")

            val view = RemoteViews(context.packageName, R.layout.widget_schedule)

            val weekDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())

            // put dayheader data from bundle
            view.setTextViewText(R.id.widget_weekday, weekDayFormat.format(day[0].startDate))
            view.setTextViewText(R.id.widget_date, dateFormat.format(day[0].startDate))

            // for each lesson create new widget_lesson
            for (lesson in day){
                d("log", "add lesson")
                val lessonView = RemoteViews(context.packageName, R.layout.widget_lesson)
                lessonView.setTextViewText(R.id.lesson_title, lesson.title)
                lessonView.setTextViewText(R.id.lesson_time, dateFormat.format(lesson.startDate))
                lessonView.setTextViewText(R.id.lesson_room, lesson.room)

                d("log", "lessonView layout id: " + lessonView.toString())
                //view.addView(R.id.widget_lessonList, lessonView)
            }

            d("log", "before update: " + view.toString())
            appWidgetManager.updateAppWidget(appWidgetId, view)
            d("log", "updateAppWidget")
        }
    }
}

