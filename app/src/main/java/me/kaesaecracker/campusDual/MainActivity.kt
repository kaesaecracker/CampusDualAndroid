package me.kaesaecracker.campusDual

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log.d
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.DrawableCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // layout
        setContentView(R.layout.activity_main)
        if (findViewById<FrameLayout>(R.id.main_container) != null) {
            if (savedInstanceState != null) return

            val scheduleFragment = ScheduleFragment()
            scheduleFragment.arguments = intent.extras

            supportFragmentManager.beginTransaction()
                    .add(R.id.main_container, scheduleFragment)
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_tollbar_menu, menu)
 nm
        tintMenuIcon(this@MainActivity, menu, R.id.action_settings, android.R.color.white)
        tintMenuIcon(this@MainActivity, menu, R.id.action_issues, android.R.color.white)
        tintMenuIcon(this@MainActivity, menu, R.id.action_releases, android.R.color.white)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java).apply {}
                startActivity(intent)
                return true
            }

            R.id.action_releases -> openChromeCustomTab(getString(R.string.releases_url))
            R.id.action_issues -> openChromeCustomTab(getString(R.string.issues_url))
        }

        return false
    }

    private fun tintMenuIcon(context: Context, menu: Menu?, id: Int, @ColorRes color: Int) {
        val item = menu!!.findItem(id)
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
