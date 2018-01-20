package me.kaesaecracker.campus_dual

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SchooldayAdapter(context: Context, days: Array<Schoolday>)
    : ArrayAdapter<Schoolday>(context, 0, days), AnkoLogger {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        info("getView")

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

class SchooldayLiveData(context: Context):LiveData<Array<Schoolday>>(), AnkoLogger {
    override fun onActive() {
        super.onActive()
        info("onActive")
        // TODO listen to data chanes (=refresh each hour?)
    }

    override fun onInactive() {
        super.onInactive()
        info("onInactive")
        // TODO pause autorefresh
    }
}

class SchooldayViewModel :ViewModel(){
    private var schooldays: MutableLiveData<Array<Schoolday>>? = null;
    fun getSchooldays():LiveData<Array<Schoolday>>{
        if (schooldays == null){
            schooldays = MutableLiveData()
            loadSchooldays()
        }

        return schooldays!!
    }

    val apiBaseUrl = "http://li1810-192.members.linode.com/cd_api/"
    private fun loadSchooldays() {
        // TODO response header handling
        Fuel.Companion.post(apiBaseUrl+"", listOf(
                Pair("userId", userId),
                Pair("password", password)
        )).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    // TODO do something useful
                }

                is Result.Success -> {
                    data = Klaxon().parse<Array<Schoolday>>(result.value)
                }
            }
        }
    }

}


data class Schoolday(
        val date: String,
        val weekDay: String,
        val lessons: ArrayList<Lesson>
)
