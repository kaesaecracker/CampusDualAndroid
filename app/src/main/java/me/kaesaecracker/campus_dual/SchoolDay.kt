package me.kaesaecracker.campus_dual

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.beust.klaxon.Json

class SchoolDayAdapter(context: Context, days: ArrayList<SchoolDay>) : ArrayAdapter<SchoolDay>(context, 0, days) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertViewVar = convertView
        val day = getItem(position)

        // initialize layout if needed
        if (convertViewVar == null) {
            convertViewVar = LayoutInflater.from(context).inflate(R.layout.item_schoolday, parent, false)
        }

        // find views
        var dateView = convertViewVar!!.findViewById<TextView>(R.id.schoolday_date)
        var WeekDayView = convertViewVar!!.findViewById<TextView>(R.id.schoolday_day)

        // set data
        dateView.text = day.date
        WeekDayView.text = day.weekDay

        // add event handlers
        // todo event handlers?

        return convertViewVar!!
    }
}

data class SchoolDay(
        val date: String,
        val weekDay: String,
        val lessons: ArrayList<Lesson>
)
