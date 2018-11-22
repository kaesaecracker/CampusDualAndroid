package xyz.mattishub.campusDual

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.view_lesson.view.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.schedule_menu, menu)
        if (menu == null) return

        tintMenuIcon(this.context!!, menu, R.id.action_schedule_to_settings, android.R.color.white)
        tintMenuIcon(this.context!!, menu, R.id.action_issues, android.R.color.white)
        tintMenuIcon(this.context!!, menu, R.id.action_releases, android.R.color.white)
        tintMenuIcon(this.context!!, menu, R.id.action_forceRefresh, android.R.color.white)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) return false

        when (item.itemId) {
            R.id.action_schedule_to_settings ->
                findNavController().navigate(ScheduleFragmentDirections.actionScheduleToSettings())
            R.id.action_startFirstLaunch ->
                findNavController().navigate(ScheduleFragmentDirections.actionScheduleToFirstLaunch())
            R.id.action_forceRefresh ->
                forceRefresh()
            R.id.action_releases ->
                openChromeCustomTab(getString(R.string.releases_url), context!!)
            R.id.action_issues ->
                openChromeCustomTab(getString(R.string.issues_url), context!!)
            R.id.action_playstore ->
                openPlayStore()
            else -> return false
        }

        return true
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context!!.packageName}")))
        } catch (anfe: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context!!.packageName}")))
        }
    }

    private fun forceRefresh() {
        GlobalScope.launch {
            val success = downloadAndSaveToSettings(context!!)
            if (success)
                this@ScheduleFragment.loadFromSettings()
            else
                this@ScheduleFragment.mainActivity.showMessage(R.string.schedule_refreshFailed)
        }
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
            val titleView: TextView by lazy { view.lesson_title }
            val timeFromView: TextView by lazy { view.lesson_timeFrom }
            val timeToView: TextView by lazy { view.lesson_timeTo }
            val roomView: TextView by lazy { view.lesson_room }
            val profView: TextView by lazy { view.lesson_prof }
            val dayHeader: ConstraintLayout by lazy { view.lesson_dayheader as ConstraintLayout }
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
            holder.timeFromView.text = lesson.start.toString(context.resources.getString(R.string.time_format), null)
            holder.timeToView.text = lesson.end.toString(context.resources.getString(R.string.time_format), null)

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
