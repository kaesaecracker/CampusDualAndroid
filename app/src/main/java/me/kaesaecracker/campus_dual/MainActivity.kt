package me.kaesaecracker.campus_dual

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import me.eugeniomarletti.extras.intent.IntentExtra
import me.eugeniomarletti.extras.intent.base.String
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

// TODO use ViewModel
// TODO maybe use Lifecycle
// TODO look up how to properly store passswords
// TODO pull to refresh
class MainActivity : AppCompatActivity(), AnkoLogger {
    //region helper objects and classes
    object IntentOptions {
        var Intent.loginUser by IntentExtra.String()
        var Intent.loginPassword by IntentExtra.String()
    }
    //endregion

    //region onSomething
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info("onCreate")

        // crashlytics
        Fabric.with(this, Crashlytics())

        // layout
        setContentView(R.layout.activity_main)

        // get login data
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val userId = sharedPref.getString("pref_userId", "")
        val password = sharedPref.getString("pref_password", "")

        // TODO livedata for schedule
        // TODO arrayadapter for lesson
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun onResume(){
        super.onResume()
        info("onResume")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    override fun onPause() {
        super.onPause()
        info("onPause")
    }
    //endregion

    //region Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        info("onCreateOptionsMenu")
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
        info("onOptionsItemSelected")
        when (item!!.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java).apply {}
                startActivity(intent)
                return true
            }

            R.id.action_refresh -> {
                TODO("implement refresh")
            }

            R.id.action_logout -> {
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                }

                startActivity(intent)
                finish()
                return true
            }
        }

        return false
    }

    private fun tintMenuIcon(context: Context, item: MenuItem, @ColorRes color: Int) {
        info("tintMenuIcon")

        val normalDrawable = item.icon
        val wrapDrawable = DrawableCompat.wrap(normalDrawable)
        DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))

        item.icon = wrapDrawable
    }
    //endregion

    fun observeSchoolday() {
        val schoolday = SchooldayLiveData(this)
        schoolday.observe(this, Observer {
            // TODO refresh ui
            info("new schoolday: $schoolday")
        })
    }
}
