package xyz.mattishub.campusDual

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log.d
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GlobalViewModel(val context: Context) : ViewModel() {
    val globalPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private lateinit var schooldays: MutableLiveData<LessonList>
    private val prefListener = PrefListener(ScheduleSettingsKey) {
        d("livedata", "prefListener fired")
        loadScheduleFromSettings()
    }

    init {
        globalPrefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    fun getSchooldays(): LiveData<LessonList> {
        if (!this::schooldays.isInitialized) {
            schooldays = MutableLiveData()
            loadScheduleFromSettings()
        }

        return schooldays
    }

    fun downloadSchedule(callback: (() -> Unit)) = GlobalScope.launch(Dispatchers.IO) {
        if (downloadAndSaveToSettings(context)) {
            loadScheduleFromSettings()
        }

        callback()
    }

    private fun loadScheduleFromSettings() = GlobalScope.launch(Dispatchers.IO) {
        val gsonString = globalPrefs.getString(ScheduleSettingsKey, "")!!
        val schedule = stringToSchedule(gsonString)
        schooldays.postValue(schedule)
    }

    class Factory(val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Context::class.java)
                    .newInstance(context)
        }
    }

    private class PrefListener(val onKey: String, val callback: (() -> Unit)) : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            d("livedata", "pref $key changed")
            if (key == onKey) callback()
        }
    }
}