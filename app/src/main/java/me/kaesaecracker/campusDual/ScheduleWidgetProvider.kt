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

            val last = day.last()
            val first = day.first()

            // put dayheader data from bundle
            view.setTextViewText(R.id.widget_weekday, first.startDate.toString(context.getString(R.string.weekday_format)))
            view.setTextViewText(R.id.widget_date, first.startDate.toString(context.getString(R.string.date_format)))
            view.setTextViewText(R.id.widget_lessonCount, "${day.size} ${context.getString(R.string.widget_lessonCount)}")
            view.setTextViewText(R.id.widget_fromTo, first.startDate.toString(context.getString(R.string.time_format)) +
                    "-" + last.endDate.toString(context.getString(R.string.time_format)))

            // find cuurent and next lesson
            var current: ScheduleFragment.Lesson? = null
            var next: ScheduleFragment.Lesson? = null
            for (lesson in day) {
                if (current == null && lesson.startDate.isBeforeNow && lesson.endDate.isAfterNow)
                    current = lesson

                if (next == null && lesson != current && lesson.startDate.isAfterNow)
                    next = lesson
            }

            // set current data
            if (current != null) {
                d("log", "current lesson: ${current.title}")
                view.setTextViewText(R.id.widget_currentLesson_title, current.title)
                view.setTextViewText(R.id.widget_currentLesson_room, current.room)
                view.setTextViewText(R.id.widget_currentLesson_time, current.endDate.toString(context.getString(R.string.time_format)))

                view.setViewVisibility(R.id.widget_currentLesson, View.VISIBLE)
            }

            // set next data
            if (next != null) {
                d("log", "next lesson: ${next.title}")
                view.setTextViewText(R.id.widget_nextLesson_title, next.title)
                view.setTextViewText(R.id.widget_nextLesson_room, next.room)
                view.setTextViewText(R.id.widget_nextLesson_time, next.startDate.toString(context.getString(R.string.time_format)))

                view.setViewVisibility(R.id.widget_nextLesson, View.VISIBLE)
                if (current != null) view.setViewVisibility(R.id.widget_lineBottom, View.VISIBLE)
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

