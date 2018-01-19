package me.kaesaecracker.campus_dual

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView
import me.eugeniomarletti.extras.intent.IntentExtra
import me.eugeniomarletti.extras.intent.base.String
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric



// TODO use ViewModel
// TODO maybe use Lifecycle
// TODO look up how to properly store passswords
// TODO pull to refresh
class MainActivity : AppCompatActivity() {
    private val binding: `null`.databinding.ActivityMainBinding? = null

    object IntentOptions {
        var Intent.loginUser by IntentExtra.String()
        var Intent.loginPassword by IntentExtra.String()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // crashlytics
        Fabric.with(this, Crashlytics())
        // layout
        setContentView(   R.layout.activity_main)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val userId = sharedPref.getString("pref_userId", "")
        val password = sharedPref.getString("pref_password", "")

        // TODO livedata for schedule
        // TODO arrayadapter for lesson
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

    fun tintMenuIcon(context: Context, item: MenuItem, @ColorRes color: Int) {
        val normalDrawable = item.icon
        val wrapDrawable = DrawableCompat.wrap(normalDrawable)
        DrawableCompat.setTint(wrapDrawable, context.resources.getColor(color))

        item.icon = wrapDrawable
    }
}
