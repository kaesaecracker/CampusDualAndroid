package me.kaesaecracker.campus_dual

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import me.eugeniomarletti.extras.intent.IntentExtra
import me.eugeniomarletti.extras.intent.base.String
import android.R.attr.name
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_lesson.*


class MainActivity : AppCompatActivity() {
    object IntentOptions {
        var Intent.loginUser by IntentExtra.String()
        var Intent.loginPassword by IntentExtra.String()
    }

    var lessonList: Array<Lesson>? = arrayOf()
    inner class LessonArrayAdapter(context: Context) : ArrayAdapter<Lesson>(context, R.layout.item_lesson) {
        private val mInflator: LayoutInflater = LayoutInflater.from(context)

        override fun getItemId(index: Int) = index.toLong()
        override fun getCount():Int = lessonList?.size ?: 0
        override fun getItem(index: Int) = lessonList!![index]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView

            // Get the data item for this position
            val lesson = getItem(position) as Lesson

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.item_lesson, parent, false)
            }

            // Lookup view for data population
            val title = convertView!!.findViewById<TextView>(R.id.lesson_title)
            val room = convertView.findViewById<TextView>(R.id.lesson_room)

            // Populate the data into the template view using the data object
            title.text = lesson.title
            room.text = lesson.room

            // Return the completed view to render on screen
            return convertView
        }
    }

    private var viewModel: MyViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MyViewModel::class.java)
        viewModel!!.getLessons().observe(this, Observer { lessons -> lessonList = lessons?.toTypedArray() })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_tollbar_menu, menu)

        var menuItem = menu!!.findItem(R.id.action_refresh)
        if (menuItem != null) {
            tintMenuIcon(this@MainActivity, menuItem, android.R.color.white)
        }

        menuItem = menu!!.findItem(R.id.action_settings)
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
                viewModel!!.refreshLessons()
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
