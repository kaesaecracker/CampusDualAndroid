package me.kaesaecracker.campusDual

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
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // layout
        setContentView(R.layout.activity_main)
        if (findViewById<FrameLayout>(R.id.main_container) != null) {
            if (savedInstanceState != null) return
            supportFragmentManager.beginTransaction()
                    .add(R.id.main_container, ScheduleFragment())
                    .commit()
        }

        // open settings if matric or hash not set
        val pref = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val matric = pref.getString(SettingsFragment.setting_matric, "")
        val hash = pref.getString(SettingsFragment.setting_hash, "")

        if (matric.isNullOrBlank() || hash.isNullOrBlank()) {
            // load from old settings keys if necessary
            val oldUserId = pref.getString("pref_userId", "")
            if (!oldUserId.isNullOrBlank()) {
                pref.edit()
                        .putString(SettingsFragment.setting_hash, oldUserId)
                        .putString(SettingsFragment.setting_matric, pref.getString("pref_password", ""))
                        .apply()
            }

            openSettings()
        }
    }

    private fun openSettings() {
        if (supportFragmentManager.backStackEntryCount == 0)
            supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.main_container, SettingsFragment())
                    .addToBackStack("settings")
                    .commit()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_tollbar_menu, menu)
        if (menu == null) return true

        tintMenuIcon(this@MainActivity, menu, R.id.action_settings, android.R.color.white)
        tintMenuIcon(this@MainActivity, menu, R.id.action_issues, android.R.color.white)
        tintMenuIcon(this@MainActivity, menu, R.id.action_releases, android.R.color.white)
        tintMenuIcon(this@MainActivity, menu, R.id.action_forceRefresh, android.R.color.white)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_settings -> openSettings()

            R.id.action_forceRefresh -> GlobalScope.launch {
                val success = downloadAndSaveToSettings(this@MainActivity.baseContext)
                d("main", "Force sync: $success")

                while (this@MainActivity.supportFragmentManager.backStackEntryCount > 0) {
                    this@MainActivity.supportFragmentManager.popBackStack()
                }

                this@MainActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_container, ScheduleFragment())
                        .commit()
            }


            R.id.action_releases -> openChromeCustomTab(getString(R.string.releases_url))
            R.id.action_issues -> openChromeCustomTab(getString(R.string.issues_url))
            R.id.action_playstore ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (anfe: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
        }

        return false
    }

    private fun tintMenuIcon(context: Context, menu: Menu, id: Int, @ColorRes color: Int) {
        val item = menu.findItem(id)
        if (item != null) {
            val normalDrawable = item.icon
            val wrapDrawable = DrawableCompat.wrap(normalDrawable)
            if (Build.VERSION.SDK_INT >= 23) {
                DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color, context.theme))
            } else {
                @Suppress("DEPRECATION")
                DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))
            }

            item.icon = wrapDrawable
        }
    }

    private fun openChromeCustomTab(url: String) {
        d("log", "chrome custom tab $url")
        val builder = CustomTabsIntent.Builder()

        builder.setToolbarColor(resources.getColor(R.color.colorPrimary))
        builder.setSecondaryToolbarColor(resources.getColor(R.color.colorAccent))
        builder.setShowTitle(true)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }
}
