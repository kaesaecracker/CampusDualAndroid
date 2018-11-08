package me.kaesaecracker.campusDual

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.experimental.async

class ScheduleFragment : Fragment() {

    private var listView: ListView? = null
    private var adapter: ScheduleAdapter? = null
    private val preferenceListener = { _: SharedPreferences?, key: String? ->
        d("schedule", "pref change: $key")
        if (key == ScheduleSettingsKey)
            loadFromSettings()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.schedule_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        listView = this.view!!.findViewById(R.id.schedule_listView)
        adapter = ScheduleAdapter(context!!)
        listView!!.adapter = adapter

        PreferenceManager.getDefaultSharedPreferences(context!!)
                .registerOnSharedPreferenceChangeListener(preferenceListener)

        loadFromSettings()
        async { downloadAndSaveToSettings(context!!) }
    }

    fun loadFromSettings() {
        d("schedule", "loading from settings")

        adapter!!.clear()

        val gsonString = PreferenceManager.getDefaultSharedPreferences(context!!).getString(ScheduleSettingsKey, "")!!
        val schedule = stringToSchedule(gsonString)
        if (schedule == null) {
            d("schedule", "could not deserialize schedule")
            return
        }

        adapter!!.addAll(schedule.toLessonList())
    }

    private class ScheduleAdapter(context: Context, days: MutableList<Lesson> = mutableListOf())
        : ArrayAdapter<Lesson>(context, 0, days) {

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val lesson = getItem(position)!!

            // initialize layout if needed
            val thisView = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false)

            val previousIsDifferentDate = fun(): Boolean {
                val previous = getItem(position - 1)!!.start
                val current = lesson.start
                return previous.dayOfYear != current.dayOfYear
            }

            val dayHeaderView = thisView.findViewById<View>(R.id.lesson_dayheader)
            if (position == 0 || previousIsDifferentDate()) {
                val dayHeaderWeekdayView = dayHeaderView.findViewById<TextView>(R.id.dayheader_weekday)
                dayHeaderWeekdayView.text = lesson.start.toString(context.getString(R.string.weekday_format))

                val dayHeaderDateView = dayHeaderView.findViewById<TextView>(R.id.dayheader_date)
                dayHeaderDateView.text = lesson.start.toString(context.getString(R.string.date_format))
                dayHeaderView.visibility = View.VISIBLE
            } else {
                dayHeaderView.visibility = View.GONE
            }

            // time and date
            thisView.findViewById<TextView>(R.id.lesson_time).text =
                    lesson.start.toString(context.resources.getString(R.string.time_format), null) +
                    "-" + lesson.end.toString(context.resources.getString(R.string.time_format), null)
            // room
            thisView.findViewById<TextView>(R.id.lesson_room).text = lesson.room
            // prof
            thisView.findViewById<TextView>(R.id.lesson_prof).text = lesson.instructor
            // title
            thisView.findViewById<TextView>(R.id.lesson_title).text = lesson.title

            return thisView
        }
    }
}
