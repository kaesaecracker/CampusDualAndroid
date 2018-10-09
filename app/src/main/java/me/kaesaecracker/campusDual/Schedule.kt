package me.kaesaecracker.campusDual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.Log.d
import android.util.Log.i
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ScheduleAdapter(context: Context, days: MutableList<Day>)
    : ArrayAdapter<Day>(context, 0, days) {

    private var mDays = days

    override fun getItem(position: Int): Day {
        return mDays[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val day = getItem(position)

        // initialize layout if needed
        var convertViewVar = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_schoolday, parent, false)

        // find views
        val dateView = convertViewVar.findViewById<TextView>(R.id.schedule_date)
        val weekDayView = convertViewVar.findViewById<TextView>(R.id.schedule_day)
        val lessonsView = convertViewVar.findViewById<LinearLayout>(R.id.schedule_lessons)

        // remove old lessons
        lessonsView.removeAllViews()

        // set data
        dateView.text = day.date
        weekDayView.text = day.weekDay
        for (lesson in day.lessons) {
            val container = LayoutInflater.from(context).inflate(R.layout.item_lesson, null) as ConstraintLayout
            lessonsView.addView(container)

            // time
            val timeView = container.findViewById<TextView>(R.id.lesson_time)
            timeView.text = lesson.time

            // room
            val roomView = container.findViewById<TextView>(R.id.lesson_room)
            roomView.text = lesson.room

            // prof
            val profView = container.findViewById<TextView>(R.id.lesson_prof)
            profView.text = lesson.instructor

            // title
            val titleView = container.findViewById<TextView>(R.id.lesson_title)
            titleView.text = lesson.title
        }

        // todo event handlers?

        return convertViewVar
    }
}

class ScheduleViewModel : ViewModel() {
    var userId: String? = null
    var password: String? = null

    private var schooldays: MutableLiveData<List<Day>>? = null
    var snackbarMessage: MutableLiveData<String> = MutableLiveData()

    fun getSchooldays(userId: String, password: String): LiveData<List<Day>> {
        this.userId = userId
        this.password = password

        if (schooldays == null) {
            schooldays = MutableLiveData()

            refreshScheduleOnline(userId, password)
        }

        return schooldays!!
    }

    fun refreshScheduleOnline(userId: String? = this.userId, password: String? = this.password) {
        this.userId = userId
        this.password = password

        FuelManager.instance.basePath = "https://cdapi.mattishub.xyz/"
        "/GetScheduleJsonWithAuth.php".httpGet(
                listOf("userId" to (userId ?: ""),
                        "password" to (password ?: "")))
                .header("Content-Type" to "application/json")
                .allowRedirects(true)
                .responseJson { _, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            toast("Login fehlgeschlagen - " + result.error.exception.localizedMessage)
                            d("log", "Failure: " + result.error.localizedMessage)
                        }

                        is Result.Success -> {
                            if (!response.headers.containsKey("API_STATUS")
                                    || !response.headers.get("API_STATUS")!!.contains("200")) {
                                i("log", "Bad API status " + response.headers.toString())
                                toast("Login fehlgeschlagen - hast du deine Daten in den Einstellungen eingetragen?")
                                return@responseJson
                            }

                            val days = parseSchedule(result.get().array())
                            schooldays!!.value = days.toList()
                        }
                    }

                }
    }

    private fun toast(s: String) {
        snackbarMessage.postValue(s)
    }

    private fun parseSchedule(jsonSchedule: JSONArray): List<Day> {
        val schedule = ArrayList<Day>()
        for (dayIndex in 0 until jsonSchedule.length()) {
            val jsonDay = jsonSchedule.getJSONObject(dayIndex)

            var date = ""
            var weekDay = ""
            var lessons: JSONArray? = null
            for (key in jsonDay.keys()) {
                if (key.trim() == "date") date = jsonDay.getString(key)
                if (key.trim() == "weekDay") weekDay = jsonDay.getString(key)
                if (key.trim() == "lessons") lessons = jsonDay.getJSONArray(key)
            }

            schedule.add(Day(date, weekDay, parseLessons(lessons)))
        }

        return schedule
    }

    private fun parseLessons(jsonLessons: JSONArray?): List<Lesson> {
        val lessons = ArrayList<Lesson>()
        if (jsonLessons == null) return lessons

        //info { "Lesson count in json: " + jsonLessons.length() }

        for (lessonIndex in 0 until jsonLessons.length()) {
            val jsonLesson = jsonLessons[lessonIndex] as JSONObject

            // get lesson properties
            val name = jsonLesson["name"] as String
            val prof = jsonLesson["prof"] as String
            val room = jsonLesson["room"] as String
            val time = jsonLesson["time"] as String

            lessons.add(Lesson(name, time, room, prof))
        }

        return lessons.toList()
    }

}

data class Day(
        val date: String,
        val weekDay: String,
        val lessons: List<Lesson>
)

data class Lesson(
        val title: String,
        val time: String,
        val room: String,
        val instructor: String
)
