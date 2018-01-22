package me.kaesaecracker.campusDual

import android.arch.lifecycle.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ListView
import me.eugeniomarletti.extras.intent.IntentExtra
import me.eugeniomarletti.extras.intent.base.String
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crash.FirebaseCrash

// FIXME Crash if user and password are not set (empty server response)
// TODO maybe use Lifecycle
// TODO look up how to properly store passwords
// TODO pull to refresh
class MainActivity : AppCompatActivity() {
    //region variables
    private var viewModel: ScheduleViewModel? = null
    private var sharedPref: SharedPreferences? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
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
        FirebaseCrash.log("onCreate")

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // layout
        setContentView(R.layout.activity_main)

        // get login data
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val userId = sharedPref!!.getString("pref_userId", "")
        val password = sharedPref!!.getString("pref_password", "")

        if (userId == "" || password == ""){

        }

        // set viewproviders
        viewModel = ViewModelProviders.of(this).get(ScheduleViewModel::class.java)
        viewModel!!.getSchooldays(userId, password).observe(this, Observer { it ->
            FirebaseCrash.log("schedule: $it")

            if (it != null){
                FirebaseCrash.log("it != null")

                val adapter = ScheduleAdapter(this, it.toTypedArray())
                val listView = findViewById<ListView>(R.id.main_schedule)
                listView.adapter = adapter
            }
        })

        // TODO livedata for schedule
        // TODO arrayadapter for lesson
    }
    //endregion

    //region Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        FirebaseCrash.log("onCreateOptionsMenu")
        menuInflater.inflate(R.menu.main_tollbar_menu, menu)

        var menuItem = menu!!.findItem(R.id.action_refresh)
        if (menuItem != null) {
            tintMenuIcon(this@MainActivity, menuItem, android.R.color.white)
        }

        menuItem = menu.findItem(R.id.action_settings)
        if (menuItem != null) {
            tintMenuIcon(this@MainActivity, menuItem, android.R.color.white)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        FirebaseCrash.log("onOptionsItemSelected")
        when (item!!.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java).apply {}
                startActivity(intent)
                return true
            }

            R.id.action_refresh -> {
                viewModel!!.userId = sharedPref!!.getString("pref_userId", "")
                viewModel!!.password = sharedPref!!.getString("pref_password", "")

                viewModel!!.loadSchooldays()

                //TODO("implement refresh")
            }

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

    private fun tintMenuIcon(context: Context, item: MenuItem, @ColorRes color: Int) {
        FirebaseCrash.log("tintMenuIcon")

        val normalDrawable = item.icon
        val wrapDrawable = DrawableCompat.wrap(normalDrawable)
        DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))

        item.icon = wrapDrawable
    }
    //endregion
}