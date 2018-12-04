package xyz.mattishub.campusDual.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log.d
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_schedule.*
import kotlinx.android.synthetic.main.fragment_schedule.view.*
import kotlinx.android.synthetic.main.view_lesson.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import xyz.mattishub.campusDual.*

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ScheduleAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mainActivity.supportActionBar?.show()
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        view.schedule_swipeToRefresh.setOnRefreshListener {
            view.schedule_swipeToRefresh.isRefreshing = true

            mainActivity.globalViewModel.downloadSchedule {
                GlobalScope.launch(Dispatchers.Main) {
                    view.schedule_swipeToRefresh.isRefreshing = false
                }
            }
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        d("schedule", "onActivityCreated")
        super.onActivityCreated(savedInstanceState)

        viewManager = LinearLayoutManager(context).apply {
            isSmoothScrollbarEnabled = false
            isItemPrefetchEnabled = true
        }

        viewAdapter = ScheduleAdapter(context!!)
        recyclerView = this.view!!.schedule_recyclerView.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        mainActivity.globalViewModel.getSchooldays().observe(this, Observer<LessonList> {
            if (it == null) {
                this@ScheduleFragment.showMessage(R.string.schedule_refreshFailed, Snackbar.LENGTH_INDEFINITE)
                return@Observer
            }

            viewAdapter.submitList(it._list)

            val lastRefreshMillis = mainActivity.globalViewModel.globalPrefs.getLong(LastRefreshSettingsKey, 0)
            val lastRefreshDate = DateTime(lastRefreshMillis, AppTimeZone).toString(getString(R.string.format_datetime))
            val successMsg = context!!.getString(R.string.schedule_elementsLoaded) + ": " +
                    it.size + "  ---  " + getString(R.string.schedule_lastRefresh) +
                    ": " + lastRefreshDate.toString()
            this@ScheduleFragment.showMessage(successMsg)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.schedule_menu, menu)
        if (menu == null) return

        tintMenuIcon(this.context!!, menu, R.id.action_schedule_to_settings, R.color.colorLightOnPrimary)
        tintMenuIcon(this.context!!, menu, R.id.action_issues, R.color.colorLightOnPrimary)
        tintMenuIcon(this.context!!, menu, R.id.action_playstore, R.color.colorLightOnPrimary)

        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.action_startFirstLaunch).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) return false

        when (item.itemId) {
            R.id.action_schedule_to_settings ->
                findNavController().navigate(ScheduleFragmentDirections.actionScheduleToSettings())
            R.id.action_startFirstLaunch ->
                findNavController().navigate(ScheduleFragmentDirections.actionScheduleToFirstLaunch())
            R.id.action_issues ->
                openChromeCustomTab(getString(R.string.issues_url), context!!)
            R.id.action_playstore ->
                openPlayStore()
            else -> return false
        }

        return true
    }


    private fun showMessage(stringId: Int, duration: Int = Snackbar.LENGTH_LONG) {
        d("mainActivity", "Show snackbar msg: [ID $stringId]")
        Snackbar.make(this.schedule_coordinator, stringId, duration)
                .show()
    }

    private fun showMessage(msg: String, duration: Int = Snackbar.LENGTH_LONG) {
        d("mainActivity", "Show snackbar msg: '$msg'")
        Snackbar.make(this.schedule_coordinator, msg, duration)
                .show()
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context!!.packageName}")))
        } catch (anfe: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context!!.packageName}")))
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
            holder.timeFromView.text = lesson.start.toString(context.resources.getString(R.string.format_time), null)
            holder.timeToView.text = lesson.end.toString(context.resources.getString(R.string.format_time), null)

            if (lesson.isFirstOfDay) {
                val dayHeaderWeekdayView = holder.dayHeader.findViewById<TextView>(R.id.dayheader_weekday)
                dayHeaderWeekdayView.text = lesson.start.toString(context.getString(R.string.format_weekday))

                val dayHeaderDateView = holder.dayHeader.findViewById<TextView>(R.id.dayheader_date)
                dayHeaderDateView.text = lesson.start.toString(context.getString(R.string.format_date))
                holder.dayHeader.visibility = View.VISIBLE
            } else {
                holder.dayHeader.visibility = View.GONE
            }
        }
    }
}
