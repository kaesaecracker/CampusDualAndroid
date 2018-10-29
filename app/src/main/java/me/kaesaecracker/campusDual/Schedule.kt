package me.kaesaecracker.campusDual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import android.util.Log.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.async
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(context: Context, days: MutableList<Lesson>)
    : ArrayAdapter<Lesson>(context, 0, days) {

    private var mDays = days

    override fun getItem(position: Int): Lesson {
        return mDays[position]
    }

    private val locale = Locale.getDefault()
    private val timeFormat = SimpleDateFormat("HH:mm", locale)
    private val dateFormat = SimpleDateFormat("dd.MM", locale)
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
        timeView.text = timeFormat.format(lesson.startDate) +
                "-" + timeFormat.format(lesson.endDate)
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

class ScheduleViewModel : ViewModel() {
    var userId: String? = null
    var password: String? = null

    private var schooldays: MutableLiveData<List<Lesson>>? = null
    var snackbarMessage: MutableLiveData<String> = MutableLiveData()

    fun getSchooldays(userId: String, password: String): LiveData<List<Lesson>> {
        this.userId = userId
        this.password = password

        if (schooldays == null) {
            schooldays = MutableLiveData()

            refreshScheduleOnline(userId, password)
        }

        return schooldays!!
    }

    fun refreshScheduleOnline(userId: String? = this.userId, password: String? = this.password) {
        d("log", "refresh")
        this.userId = userId
        this.password = password

        async {
            val urlBase = "https://selfservice.campus-dual.de/room/json"

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
                d("log", "got response")

                val (schedule, err) = result
                if (err != null) {
                    toast("Laden von CD fehlgeschlagen. Prüfe deine Login-Daten.")
                    w("log", err)
                } else {
                    toast("${schedule?.size ?: "0"} Stunden geladen")
                }

                schooldays!!.value = schedule
            }
        }
    }

    private fun toast(s: String) {
        snackbarMessage.postValue(s)
        d("log", "toast: $s")
    }
}


class ScheduleDeserializer : ResponseDeserializable<List<Lesson>> {
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