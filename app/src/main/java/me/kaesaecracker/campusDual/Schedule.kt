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

    fun epochToDateTimeString(epochSeconds: Int, formatString: String): String {
        val date = Date(epochSeconds * 1000L)
        val format = SimpleDateFormat(formatString, Locale("de", "de"))
        return format.format(date)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val lesson = getItem(position)

        // initialize layout if needed
        val convertViewVar = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false)

        // time and date
        val timeView = convertViewVar.findViewById<TextView>(R.id.lesson_time)
        timeView.text = epochToDateTimeString(lesson.start.toInt(), "HH:mm") +
                "-" + epochToDateTimeString(lesson.end.toInt(), "HH:mm")
        val dateView = convertViewVar.findViewById<TextView>(R.id.lesson_date)
        dateView.text = epochToDateTimeString(lesson.start.toInt(), "EE dd.MM")

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

            val startEpoch = System.currentTimeMillis() / 1000
            val endEpoch = Math.round(startEpoch + 1.21e+6)

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
                  val remarks: String = "")