package me.kaesaecracker.campusDual

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.annotation.ColorRes
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.snackbar.Snackbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import android.util.Log.i
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import me.eugeniomarletti.extras.intent.IntentExtra
import me.eugeniomarletti.extras.intent.base.String

// FIXME Crash if user and password are not set (empty server response)
// TODO maybe use Lifecycle
// TODO look up how to properly store passwords
// TODO pull to refresh
class MainActivity : AppCompatActivity() {
    //region variables
    private var viewModel: ScheduleViewModel? = null
    private var sharedPref: SharedPreferences? = null
    //endregion

    //region helper objects and classes
    object IntentOptions {
        var Intent.loginUser by IntentExtra.String()
        var Intent.loginPassword by IntentExtra.String()
    }
    //endregion

    //region onSomething
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
        var scheduleAdapter = ScheduleAdapter(this, mutableListOf())
        val mainScheduleView = findViewById<ListView>(R.id.main_schedule)
        mainScheduleView.adapter = scheduleAdapter

        // actual schedule
        viewModel!!.getSchooldays(userId, password).observe(this, Observer { it ->
            if (it != null) {
                // TODO just refresh the data
                scheduleAdapter = ScheduleAdapter(this, it as MutableList<Lesson>)
                mainScheduleView.adapter = scheduleAdapter
            }

            mainRootView.isRefreshing = false
        })

        // snackbar message
        viewModel!!.snackbarMessage.observe(this, Observer {
            i("log", "snackBarMessage received")
            Snackbar.make(findViewById(R.id.main_root), it ?: "Error showing message", Snackbar.LENGTH_LONG).show()
        })

        // refresh
        mainRootView.setOnRefreshListener {
            i("log", "swipe to refresh triggered")

            mainRootView.isRefreshing = true
            viewModel!!.refreshScheduleOnline()
        }


        // TODO livedata for schedule
        // TODO arrayadapter for lesson
    }
    //endregion

    //region Menu
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

        /****
        R.id.action_logout -> {
        val intent = Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        startActivity(intent)
        finish()
        return true
        }*/
        }

        return false
    }

    private fun tintMenuIcon(context: Context, menu: Menu?, id: Int, @ColorRes color: Int) {
        val item = menu!!.findItem(id)
        if (item != null) {
            val normalDrawable = item.icon
            val wrapDrawable = DrawableCompat.wrap(normalDrawable)
            DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))

            item.icon = wrapDrawable
        }
    }
    //endregion

    //region helper methods
    fun openChromeCustomTab(url: String) {
        val builder = CustomTabsIntent.Builder()
        // todo set toolbar color and/or setting custom actions before invoking build()

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }
    //endregion
}
