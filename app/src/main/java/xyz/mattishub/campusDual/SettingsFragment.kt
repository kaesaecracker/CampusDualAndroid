package xyz.mattishub.campusDual

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_settings.view.*
import me.kaesaecracker.campusDual.R

class SettingsFragment : Fragment() {
    companion object {
        const val setting_matric = "setting_matric"
        const val setting_hash = "setting_hash"
    }

    class LengthTextWatcher(private val inputLayout: TextInputLayout,
                            private val context: Context,
                            private val desiredLength: Int,
                            private val onDesiredLength: (text: String) -> Unit) : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            when {
                // empty
                s.isNullOrBlank() -> inputLayout.apply {
                    helperText = null
                    error = context.getString(R.string.settings_empty)
                }

                // too short
                s.length < desiredLength -> inputLayout.apply {
                    helperText = null
                    error = context.getString(R.string.settings_tooShort)
                }

                // correct
                s.length == desiredLength -> inputLayout.apply {
                    error = null
                    helperText = context.getString(R.string.settings_correct)
                    onDesiredLength(s.toString())
                }

                // too long
                s.length > desiredLength -> inputLayout.apply {
                    helperText = null
                    error = context.getString(R.string.settings_tooLong)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        fun saveToSettings(key: String): (String) -> Unit = {
            prefs.edit().putString(key, it).apply()
        }

        view.settings_matricNrEdit.apply {
            addTextChangedListener(
                    LengthTextWatcher(view.settings_matricNrLayout, context!!, 7, saveToSettings(setting_matric))
            )
            setText(prefs.getString(setting_matric, ""), TextView.BufferType.EDITABLE)
        }

        view.settings_hashEdit.apply {
            addTextChangedListener(
                    LengthTextWatcher(view.settings_hashLayout, context!!, 32, saveToSettings(setting_hash))
            )
            setText(prefs.getString(setting_hash, ""), TextView.BufferType.EDITABLE)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title = getString(R.string.title_activity_settings)
    }

    fun onUndoButtinClick(v: View) {

    }
}
