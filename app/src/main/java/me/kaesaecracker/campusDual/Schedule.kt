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
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.async
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(context: Context, days: MutableList<Lesson>)
    : ArrayAdapter<Lesson>(context, 0, days) {

    private var mDays = days

    override fun getItem(position: Int): Lesson {
        return mDays[position]
    }

    fun epochToDateTimeString(epochSeconds: Int, formatString: String): String {
        val date = Date(epochSeconds * 1000L)
        val format = SimpleDateFormat(formatString, Locale("de", "de"))
        return format.format(date)
    }

    private val timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale("de"))
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd.MM", Locale("de"))
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val lesson = getItem(position)

        // initialize layout if needed
        val convertViewVar = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false)

        val previousIsDifferentDate = fun():Boolean {
            val previous = getItem(position -1)
            val prevDate = previous.getStartDate()
            val currDate = lesson.getStartDate()
            val prevCal = Calendar.getInstance()
            prevCal.time = prevDate
            val currCal = Calendar.getInstance()
            currCal.time = currDate
            return prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR)
        }

        val dayHeaderView = convertViewVar.findViewById<View>(R.id.lesson_dayheader)
        if (position == 0 || previousIsDifferentDate()) {
            val dayHeaderWeekdayView = dayHeaderView.findViewById<TextView>(R.id.dayheader_weekday)
            dayHeaderWeekdayView.text = SimpleDateFormat("EEEE", Locale("de"))
                    .format(lesson.getStartDate())

            val dayHeaderDateView = dayHeaderView.findViewById<TextView>(R.id.dayheader_date)
            dayHeaderDateView.text = dateFormat.format(lesson.getStartDate())
            dayHeaderView.visibility = View.VISIBLE
        } else {
            dayHeaderView.visibility = View.GONE
        }

        // time and date
        val timeView = convertViewVar.findViewById<TextView>(R.id.lesson_time)
        timeView.text = timeFormat.format(lesson.getStartDate()) +
                "-" + timeFormat.format(lesson.getEndDate())
        // room
        val roomView = convertViewVar.findViewById<TextView>(R.id.lesson_room)
        roomView.text = lesson.room

        // prof
        val profView = convertViewVar.findViewById<TextView>(R.id.lesson_prof)
        profView.text = lesson.instructor

        // title
        val titleView = convertViewVar.findViewById<TextView>(R.id.lesson_title)
        titleView.text = lesson.title


        // todo event handlers?

        return convertViewVar
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
                if (err != null) toast("Laden von CD fehlgeschlagen. Prüfe deine Login-Daten.")

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
    fun getStartDate(): Date {
        return Date(this.start.toLong() * 1000)
    }

    fun getEndDate(): Date {
        return Date(this.end.toLong() * 1000)
    }
}