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
import xyz.mattishub.campusDual.fragments.SettingsFragment

class GlobalViewModel(val context: Context) : ViewModel() {
    val globalPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private lateinit var schooldays: MutableLiveData<LessonList>
    private val schedulePrefListener = PrefListener(ScheduleSettingsKey) { _, _ ->
        loadScheduleFromSettings()
    }

    private lateinit var theme: MutableLiveData<String>
    private val themePrefListener = PrefListener(SettingsFragment.setting_theme) { _, _ ->
        getTheme() // ensure initialized
        theme.postValue(globalPrefs.getString(SettingsFragment.setting_theme, SettingsFragment.setting_theme_default))
    }

    init {
        globalPrefs.registerOnSharedPreferenceChangeListener(schedulePrefListener)
        globalPrefs.registerOnSharedPreferenceChangeListener(themePrefListener)
    }

    fun getTheme(): LiveData<String> {
        if (!this::theme.isInitialized) {
            theme = MutableLiveData()
            theme.value = globalPrefs.getString(SettingsFragment.setting_theme, SettingsFragment.setting_theme_default)
        }

        return theme
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

        getSchooldays() // ensure initialized
        schooldays.postValue(schedule)
    }

    class Factory(val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Context::class.java)
                    .newInstance(context)
        }
    }

    private class PrefListener(val onKey: String, val callback: ((SharedPreferences?, String?) -> Unit)) : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            d("livedata", "pref $key changed")
            if (key == onKey) callback(sharedPreferences, key)
        }
    }
}