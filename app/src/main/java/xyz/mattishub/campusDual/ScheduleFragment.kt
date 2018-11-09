package xyz.mattishub.campusDual

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.kaesaecracker.campusDual.R

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ScheduleAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val preferenceListener = { _: SharedPreferences?, key: String? ->
        d("schedule", "pref change: $key")
        if (key == ScheduleSettingsKey)
            loadFromSettings()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        d("schedule", "onCreateView")
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        d("schedule", "onActivityCreated")
        super.onActivityCreated(savedInstanceState)

        viewManager = LinearLayoutManager(context).apply {
            isSmoothScrollbarEnabled = false
            isItemPrefetchEnabled = true
        }

        viewAdapter = ScheduleAdapter(context!!)

        recyclerView = this.view!!.findViewById<RecyclerView>(R.id.schedule_listView).apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        recyclerView.adapter = viewAdapter

        PreferenceManager.getDefaultSharedPreferences(context!!)
                .registerOnSharedPreferenceChangeListener(preferenceListener)

        loadFromSettings()
        GlobalScope.launch { downloadAndSaveToSettings(context!!) }
    }

    override fun onResume() {
        super.onResume()

        (activity as AppCompatActivity).title = getString(R.string.app_name)
    }

    fun loadFromSettings() {
        d("schedule", "loading from settings")

        val gsonString = PreferenceManager.getDefaultSharedPreferences(context!!).getString(ScheduleSettingsKey, "")!!
        val schedule = stringToSchedule(gsonString)
        if (schedule == null) {
            d("schedule", "could not deserialize schedule")
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            d("schedule", "submitting new data to viewAdapter")
            viewAdapter.submitList(schedule.toLessonList())
        }
    }

    private class ScheduleAdapter(val context: Context) :
            ListAdapter<Lesson, ScheduleAdapter.ScheduleViewHolder>(LessonDiffCallback()) {

        class ScheduleViewHolder(view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
            val titleView: TextView by lazy { view.findViewById<TextView>(R.id.lesson_title) }
            val timeView: TextView by lazy { view.findViewById<TextView>(R.id.lesson_time) }
            val roomView: TextView by lazy { view.findViewById<TextView>(R.id.lesson_room) }
            val profView: TextView by lazy { view.findViewById<TextView>(R.id.lesson_prof) }
            val dayHeader: ConstraintLayout by lazy { view.findViewById<ConstraintLayout>(R.id.lesson_dayheader) }
        }

        class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
            override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson) = oldItem == newItem
            override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson) = oldItem.startEpoch == newItem.startEpoch
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_lesson, parent, false) as ConstraintLayout
            return ScheduleViewHolder(view)
        }

        override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
            val lesson = getItem(position)
            holder.titleView.text = lesson.title
            holder.profView.text = lesson.instructor
            holder.roomView.text = lesson.room
            holder.timeView.text = lesson.start.toString(context.resources.getString(R.string.time_format), null) +
                    "-" + lesson.end.toString(context.resources.getString(R.string.time_format), null)

            val previousIsDifferentDate = fun(): Boolean {
                val previous = getItem(position - 1).start
                val current = lesson.start
                return previous.dayOfYear != current.dayOfYear
            }

            if (position == 0 || previousIsDifferentDate()) {
                val dayHeaderWeekdayView = holder.dayHeader.findViewById<TextView>(R.id.dayheader_weekday)
                dayHeaderWeekdayView.text = lesson.start.toString(context.getString(R.string.weekday_format))

                val dayHeaderDateView = holder.dayHeader.findViewById<TextView>(R.id.dayheader_date)
                dayHeaderDateView.text = lesson.start.toString(context.getString(R.string.date_format))
                holder.dayHeader.visibility = View.VISIBLE
            } else {
                holder.dayHeader.visibility = View.GONE
            }
        }
    }
}
