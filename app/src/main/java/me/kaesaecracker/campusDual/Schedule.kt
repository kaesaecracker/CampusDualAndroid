package me.kaesaecracker.campusDual

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import com.google.firebase.crash.FirebaseCrash
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ScheduleAdapter(context: Context, days: Array<Day>)
    : ArrayAdapter<Day>(context, 0, days) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        FirebaseCrash.log("getView")

        val day = getItem(position)

        FirebaseCrash.log("lessons this day: " + day.lessons.size)

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
    fun getSchooldays(userId: String, password: String): LiveData<List<Day>> {
        this.userId = userId
        this.password = password

        if (schooldays == null) {
            schooldays = MutableLiveData()

            if (userId != "" && password != "") {
                loadSchooldays(userId, password)
            }
        }

        return schooldays!!
    }

    val apiBaseUrl = "http://li1810-192.members.linode.com/cd_api/"
    fun loadSchooldays(userId: String? = this.userId, password: String? = this.password) {
        FirebaseCrash.log("loadSchooldays; userId='$userId'; password='$password'")

        // TODO response header handling
        Fuel.Companion.post(apiBaseUrl + "GetScheduleJsonWithAuth.php", listOf(
                Pair("userId", this.userId),
                Pair("password", this.password)
        )).responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    // TODO do something useful
                    FirebaseCrash.log("fuel response failure")
                    FirebaseCrash.log("request: $request")
                    FirebaseCrash.log("response: $response")
                    FirebaseCrash.log("result: $result")
                }

                is Result.Success -> {
                    FirebaseCrash.log("fuel response success")

                    val days = parseSchedule(result.value.array())
                    schooldays!!.value = days.toList()
                }
            }
        }
    }

    private fun parseSchedule(jsonSchedule: JSONArray): List<Day> {
        val schedule = ArrayList<Day>()
        for (dayIndex in 0 until jsonSchedule.length()) {
            val jsonDay = jsonSchedule.getJSONObject(dayIndex)

            //jsonDay.keys().forEach { info { it + "="+jsonDay.get(it)::class.simpleName } }

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
