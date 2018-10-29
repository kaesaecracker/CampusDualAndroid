package me.kaesaecracker.campusDual

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import androidx.annotation.ColorRes
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.snackbar.Snackbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log.i
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView

class MainActivity : AppCompatActivity() {
    
    private var viewModel: ScheduleViewModel? = null
    private var sharedPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get viewmodel + login data
        viewModel = ViewModelProviders.of(this).get(ScheduleViewModel::class.java)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val userId = sharedPref!!.getString("pref_userId", "")
        val password = sharedPref!!.getString("pref_password", "")

        // layout
        setContentView(R.layout.activity_main)
        val mainRootView = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.main_root)
        mainRootView.isRefreshing = true
        val scheduleAdapter = ScheduleAdapter(this, mutableListOf())
        val mainScheduleView = findViewById<ListView>(R.id.main_schedule)
        mainScheduleView.adapter = scheduleAdapter

        // actual schedule
        viewModel!!.getSchooldays(userId ?: "", password ?: "").observe(this, Observer { it ->
            scheduleAdapter.clear()
            if (it != null) scheduleAdapter.addAll(it)
            mainRootView.isRefreshing = false
        })

        // snackbar message
        viewModel!!.snackbarMessage.observe(this, Observer {
            i("log", "snackBarMessage received: $it")
            Snackbar.make(findViewById(R.id.main_root), it
                    ?: "Error showing message", Snackbar.LENGTH_INDEFINITE).show()
        })

        // refresh
        mainRootView.setOnRefreshListener {
            i("log", "swipe to refresh triggered")

            mainRootView.isRefreshing = true
            viewModel!!.refreshScheduleOnline()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_tollbar_menu, menu)

        tintMenuIcon(this@MainActivity, menu, R.id.action_refresh, android.R.color.white)
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

            R.id.action_refresh -> {
                viewModel!!.userId = sharedPref!!.getString("pref_userId", "")
                viewModel!!.password = sharedPref!!.getString("pref_password", "")

                val mainRootView = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.main_root)
                mainRootView.isRefreshing = true
                viewModel!!.refreshScheduleOnline()
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
