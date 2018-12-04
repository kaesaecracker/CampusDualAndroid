package xyz.mattishub.campusDual

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.d
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        d("widget_provider", "onUpdate")

        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (appWidgetIds.isEmpty()) return

        for (appWidgetId in appWidgetIds) {
            d("widget_provider", "appWidgetId $appWidgetId")

            val intent = Intent(context, ScheduleWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

            val rv = RemoteViews(context.packageName, R.layout.widget)
            setHeaderData(context, rv)
            setOnClick(context, rv)
            rv.setRemoteAdapter(R.id.widget_lessonList, intent)

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }

    private fun setHeaderData(context: Context, widget: RemoteViews) {
        val gsonString = PreferenceManager.getDefaultSharedPreferences(context).getString(ScheduleSettingsKey, "")!!
        val schedule = stringToSchedule(gsonString)
        if (schedule == null) {
            d("widget_provider", "could not set header data: could not deserialize schedule")
            return
        }

        val day = schedule.getCurrentOrNextDay()
        if (day.size == 0) {
            d("widget_provider", "could not set header data: current day not found")
            return
        }

        widget.setTextViewText(R.id.widget_header_weekday, day[0].start.toString(context.getString(R.string.format_weekday)))
        widget.setTextViewText(R.id.widget_header_date, day[0].start.toString(context.getString(R.string.format_date)))
        widget.setTextViewText(R.id.widget_header_lessonCount, "${day.size} ${context.getString(R.string.widget_lessonCount)}")
        widget.setTextViewText(R.id.widget_header_fromTo, day[0].start.toString(context.getString(R.string.format_time)) +
                "-" + day[day.size -1].end.toString(context.getString(R.string.format_time)))
    }

    private fun setOnClick(context: Context, widget: RemoteViews) {
        val launchActivity = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0)
        widget.setOnClickPendingIntent(R.id.widget_header, pendingIntent)
    }
}

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        Log.d("widget_service", "onGetViewFactory")
        return WidgetRemoteViewsFactory(this.baseContext)
    }

    private class WidgetRemoteViewsFactory(val context: Context) : RemoteViewsService.RemoteViewsFactory {
        private var days: LessonList? = null
        private var nonPassedLessons: LessonList? = null

        override fun onDataSetChanged() {
            d("widget_factory", "onDataSetChanged")

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            val str = prefs.getString(ScheduleSettingsKey, "") ?: ""
            days = stringToSchedule(str)
        }

        override fun getViewAt(position: Int): RemoteViews? {
            d("widget_factory", "getViewAt($position)")

            if (position == AdapterView.INVALID_POSITION)
                return null // invalid pos
            if (nonPassedLessons == null || nonPassedLessons!!.size <= position)
                return null // no data or position out of bounds

            return createLessonView(nonPassedLessons!![position])
        }

        private fun createLessonView(lesson: Lesson): RemoteViews {
            d("widget_factory", "createLessonView(${lesson.title})")

            val view = RemoteViews(context.packageName, R.layout.widget_lesson)

            view.setTextViewText(R.id.widget_lesson_title, lesson.title)
            view.setTextViewText(R.id.widget_lesson_room, lesson.room)
            view.setTextViewText(R.id.widget_lesson_time, lesson.start.toString(context.getString(R.string.format_time)))

            return view
        }

        override fun getCount(): Int {
            nonPassedLessons = days?.getCurrentOrNextDay()?.whereEndNotInPast()
            onDataSetChanged()
            return nonPassedLessons?.size ?: 0
        }

        override fun getViewTypeCount(): Int = 1
        override fun hasStableIds(): Boolean = false
        override fun getLoadingView(): RemoteViews? = null
        override fun getItemId(position: Int): Long = nonPassedLessons?.get(position)?.startEpoch ?: 0L

        override fun onCreate() {
            d("widget_factory", "onCreate")
        }

        override fun onDestroy() {
            d("widget_factory", "onDestroy")
        }
    }
}

