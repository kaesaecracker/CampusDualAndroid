package me.kaesaecracker.campusDual

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log.*
import android.view.View
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import android.content.ComponentName
import me.kaesaecracker.campusDual.R.layout.widget
import android.app.PendingIntent
import android.content.Intent



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
            if (day.isEmpty()) return

            val view = RemoteViews(context.packageName, R.layout.widget)

            val weekDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())
            val timeFormat = SimpleDateFormat(context.getString(R.string.time_format), Locale.getDefault())

            val last = day.last()
            val first = day.first()
            // put dayheader data from bundle
            view.setTextViewText(R.id.widget_weekday, weekDayFormat.format(day[0].startDate))
            view.setTextViewText(R.id.widget_date, dateFormat.format(day[0].startDate))
            view.setTextViewText(R.id.widget_lessonCount, "${context.getString(R.string.widget_lessonCount)} ${day.size}")
            view.setTextViewText(R.id.widget_fromTo, timeFormat.format(first.startDate) + "-" + timeFormat.format(last.endDate))


            // find cuurent and next lesson
            var current: ScheduleFragment.Lesson? = null
            var next: ScheduleFragment.Lesson? = null
            val nowCal = Calendar.getInstance()
            nowCal.time = Date()

            d("log", "testing calendar: ${dateFormat.format(nowCal.time)} ${timeFormat.format(nowCal.time)}")

            for (lesson in day) {
                d("log", "testing lesson ${lesson.title}, ${dateFormat.format(lesson.startDate)} ${timeFormat.format(lesson.startDate)}")

                val startCalendar = Calendar.getInstance()
                startCalendar.time = lesson.startDate
                val endCalendar = Calendar.getInstance()
                endCalendar.time = lesson.endDate


                if (current == null && nowCal.after(startCalendar) && nowCal.before(endCalendar))
                    current = lesson

                if (next == null && nowCal.before(startCalendar) && lesson != current)
                    next = lesson
            }

            // set current data
            if (current != null) {
                d("log", "current lesson: ${current.title}")
                view.setTextViewText(R.id.widget_currentLesson_title, current.title)
                view.setTextViewText(R.id.widget_currentLesson_room, current.room)
                view.setTextViewText(R.id.widget_currentLesson_time, timeFormat.format(current.startDate))
            } else {
                view.setViewVisibility(R.id.widget_currentLesson, View.GONE)
                view.setViewVisibility(R.id.widget_lineBottom, View.GONE)
            }

            // set next data
            if (next != null) {
                d("log", "next lesson: ${next.title}")
                view.setTextViewText(R.id.widget_nextLesson_title, next.title)
                view.setTextViewText(R.id.widget_nextLesson_room, next.room)
                view.setTextViewText(R.id.widget_nextLesson_time, timeFormat.format(next.startDate))
            } else {
                view.setViewVisibility(R.id.widget_lineBottom, View.GONE)
                view.setViewVisibility(R.id.widget_nextLesson, View.GONE)
            }

            // set onclick
            val launchActivity = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0)
            view.setOnClickPendingIntent(R.id.widget_header, pendingIntent)
            view.setOnClickPendingIntent(R.id.widget_currentLesson, pendingIntent)
            view.setOnClickPendingIntent(R.id.widget_nextLesson, pendingIntent)

            // send update
            appWidgetManager.updateAppWidget(appWidgetId, view)
        }
    }
}

