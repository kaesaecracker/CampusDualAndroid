package xyz.mattishub.campusDual

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.kaesaecracker.campusDual.R

class MainActivity : AppCompatActivity() {

    private val navController by lazy { findNavController(R.id.main_navHost) }
    private val appBarConfiguration by lazy { AppBarConfiguration(navController.graph) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // open settings if matric or hash not set
        val pref = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val matric = pref.getString(SettingsFragment.setting_matric, "")
        val hash = pref.getString(SettingsFragment.setting_hash, "")

        if (matric.isNullOrBlank() || hash.isNullOrBlank())
            navController.navigate(R.id.action_schedule_to_settings)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) GlobalScope.launch {
            setupBackgroundWorker()
            forceRefreshWidget()
        }
    }

    fun forceRefreshWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(baseContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(baseContext, ScheduleWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_lessonList)
    }
}
