package xyz.mattishub.campusDual

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
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
        const val setting_backend = "setting_backend"
    }

    open class DefaultTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    class LengthTextWatcher(private val inputLayout: TextInputLayout,
                            private val desiredLength: Int,
                            private val onDesiredLength: (text: String) -> Unit) : DefaultTextWatcher() {
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

    class SaveSettingTextWatcher(private val settingsKey: String,
                                 private val prefs: SharedPreferences) : DefaultTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            prefs.edit()
                    .putString(settingsKey, s.toString())
                    .apply()

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
            setText(prefs.getString(setting_matric, ""), TextView.BufferType.EDITABLE)
            addTextChangedListener(
                    LengthTextWatcher(view.settings_matricNrLayout, 7, saveToSettings(setting_matric))
            )
        }

        view.settings_hashEdit.apply {
            setText(prefs.getString(setting_hash, ""), TextView.BufferType.EDITABLE)
            addTextChangedListener(
                    LengthTextWatcher(view.settings_hashLayout, 32, saveToSettings(setting_hash))
            )
        }

        view.settings_backendEdit.apply {
            setText(prefs.getString(setting_backend, getString(R.string.default_backend_url)))
            addTextChangedListener(
                    SaveSettingTextWatcher(setting_backend, prefs)
            )
        }

        view.settings_resetBackendBtn.setOnClickListener {
            val defBackend = getString(R.string.default_backend_url)
            prefs.edit()
                    .putString(setting_backend, defBackend)
                    .apply()
            view.settings_backendEdit.text = Editable.Factory.getInstance().newEditable(defBackend)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title = getString(R.string.title_activity_settings)
    }
}
