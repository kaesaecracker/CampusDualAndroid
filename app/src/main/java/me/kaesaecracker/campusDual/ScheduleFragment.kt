package me.kaesaecracker.campusDual

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.async
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {

    private var listView: ListView? = null
    private var refreshLayout: SwipeRefreshLayout? = null
    private var adapter: ScheduleAdapter? = null
    private var viewModel: ScheduleViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.schedule_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, ScheduleViewModelFactory(this.context!!)).get(ScheduleViewModel::class.java)
        listView = this.view!!.findViewById(R.id.schedule_listView)
        adapter = ScheduleAdapter(context!!)
        listView!!.adapter = adapter
        refreshLayout = this.view!!.findViewById(R.id.schedule_refreshLayout)

        viewModel!!.snackbarMessage.observe(this, Observer {
            Log.i("log", "snackBarMessage received: $it")
            Snackbar.make(
                    this.refreshLayout!!,
                    it ?: context!!.getString(R.string.error_showing_toast),
                    Snackbar.LENGTH_INDEFINITE
            ).show()
        })

        refreshLayout!!.setOnRefreshListener {
            Log.i("log", "swipe to refresh triggered")
            refreshOnline()
        }

        refreshOnline()
    }

    private fun refreshOnline() {
        refreshLayout!!.isRefreshing = true

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val userId = prefs.getString("pref_userId", "") ?: ""
        val password = prefs.getString("pref_password", "") ?: ""
        viewModel!!.refreshOnline(userId, password).observe(this, Observer { it ->
            adapter!!.clear()
            if (it != null) adapter!!.addAll(it)
            refreshLayout!!.isRefreshing = false
        })
    }

    private class ScheduleAdapter(context: Context, days: MutableList<Lesson> = mutableListOf())
        : ArrayAdapter<Lesson>(context, 0, days) {

        private var mDays = days

        override fun getItem(position: Int): Lesson {
            return mDays[position]
        }

        private val locale = Locale.getDefault()
        private val timeFormat = SimpleDateFormat(context.resources.getString(R.string.time_format), locale)
        private val dateFormat = SimpleDateFormat(context.resources.getString(R.string.date_format), locale)
        private val weekdayFormat = SimpleDateFormat("EEEE", locale)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val lesson = getItem(position)

            // initialize layout if needed
            val thisView = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false)

            val previousIsDifferentDate = fun(): Boolean {
                val previous = getItem(position - 1)
                val prevCal = Calendar.getInstance()
                prevCal.time = previous.startDate
                val currCal = Calendar.getInstance()
                currCal.time = lesson.startDate
                return prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR)
            }

            val dayHeaderView = thisView.findViewById<View>(R.id.lesson_dayheader)
            if (position == 0 || previousIsDifferentDate()) {
                val dayHeaderWeekdayView = dayHeaderView.findViewById<TextView>(R.id.dayheader_weekday)
                dayHeaderWeekdayView.text = weekdayFormat.format(lesson.startDate)

                val dayHeaderDateView = dayHeaderView.findViewById<TextView>(R.id.dayheader_date)
                dayHeaderDateView.text = dateFormat.format(lesson.startDate)
                dayHeaderView.visibility = View.VISIBLE
            } else {
                dayHeaderView.visibility = View.GONE
            }

            // time and date
            val timeView = thisView.findViewById<TextView>(R.id.lesson_time)
            timeView.text = timeFormat.format(lesson.startDate) + "-" + timeFormat.format(lesson.endDate)

            // room
            val roomView = thisView.findViewById<TextView>(R.id.lesson_room)
            roomView.text = lesson.room

            // prof
            val profView = thisView.findViewById<TextView>(R.id.lesson_prof)
            profView.text = lesson.instructor

            // title
            val titleView = thisView.findViewById<TextView>(R.id.lesson_title)
            titleView.text = lesson.title

            return thisView
        }
    }

    private class ScheduleViewModelFactory(private val context: Context) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(context) as T
        }
    }

    private class ScheduleViewModel(val context: Context) : ViewModel() {
        private var schooldays: MutableLiveData<List<Lesson>>? = null
        var snackbarMessage: MutableLiveData<String> = MutableLiveData()

        fun refreshOnline(userId: String, password: String):MutableLiveData<List<Lesson>> {
            Log.d("log", "refresh")
            if (schooldays == null) {
                schooldays = MutableLiveData()
            }

            async {
                val urlBase = context.resources.getString(R.string.backend_url)

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, +11)
                val startEpoch = calendar.timeInMillis / 1000
                calendar.add(Calendar.MONTH, 1)
                val endEpoch = calendar.timeInMillis / 1000

                val request = urlBase.httpGet(listOf(
                        "userid" to userId,
                        "hash" to password,
                        "start" to startEpoch,
                        "end" to endEpoch
                ))

                request.responseObject(ScheduleDeserializer()) { _, _, result ->
                    Log.d("log", "got response")

                    val (schedule, err) = result
                    if (err != null) {
                        toast(context.resources.getString(R.string.load_failed_check_login))
                        Log.w("log", err)
                    } else {
                        toast((schedule?.size?.toString() ?: "0") + " "
                                + context.resources.getString(R.string.lessons_loaded))
                    }

                    schooldays!!.value = schedule
                }
            }

            return schooldays!!
        }

        private fun toast(s: String) {
            snackbarMessage.postValue(s)
            Log.d("log", "toast: $s")
        }
    }


    private class ScheduleDeserializer : ResponseDeserializable<List<Lesson>> {
        override fun deserialize(content: String) = Gson().fromJson<List<Lesson>>(content, object : TypeToken<List<Lesson>>() {}.type)!!
    }

    data class Lesson(val title: String = "",
                      val start: String = "",
                      val end: String = "",
                      val allDay: Boolean = false,
                      val description: String = "",
                      val color: String = "",
                      val editable: Boolean = false,
                      val room: String = "",
                      val sroom: String = "",
                      val instructor: String = "",
                      val sinstructor: String = "",
                      val remarks: String = "") {
        private var _startDate: Date? = null
        private var _endDate: Date? = null

        val startDate: Date
            get() {
                if (_startDate == null) {
                    _startDate = Date(this.start.toLong() * 1000)
                }

                return _startDate!!
            }
        val endDate: Date
            get() {
                if (_endDate == null) {
                    _endDate = Date(this.end.toLong() * 1000)
                }

                return _endDate!!
            }
    }
}
