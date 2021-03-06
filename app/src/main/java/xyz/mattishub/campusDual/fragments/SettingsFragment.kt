package xyz.mattishub.campusDual.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_settings.view.*
import xyz.mattishub.campusDual.R
import xyz.mattishub.campusDual.mainActivity


class SettingsFragment : Fragment() {
    companion object {
        const val setting_matric = "setting_matric"
        const val setting_hash = "setting_hash"
        const val setting_backend = "setting_backend"
        const val setting_theme = "setting_theme"
        const val setting_theme_default = "default"
        const val setting_theme_light = "light"
        const val setting_theme_dark = "dark"
        const val setting_theme_black = "black"
        const val setting_force_secure = "setting_force_secure"
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
        val prefs = mainActivity.globalViewModel.globalPrefs

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
            setText(prefs.getString(setting_backend, getString(R.string.url_default_backend)))
            addTextChangedListener(
                    SaveSettingTextWatcher(setting_backend, prefs)
            )
        }

        view.settings_resetBackendBtn.setOnClickListener {
            val defBackend = getString(R.string.url_default_backend)
            prefs.edit()
                    .putString(setting_backend, defBackend)
                    .apply()
            view.settings_backendEdit.text = Editable.Factory.getInstance().newEditable(defBackend)
        }

        view.settings_themeCard_radioGroup.apply {
            check(when (prefs.getString(setting_theme, setting_theme_default)) {
                setting_theme_light -> R.id.settings_themeCard_radioGroup_light
                setting_theme_dark -> R.id.settings_themeCard_radioGroup_dark
                setting_theme_black -> R.id.settings_themeCard_radioGroup_black
                else -> R.id.settings_themeCard_radioGroup_light
            })

            setOnCheckedChangeListener { _, checkedId ->
                prefs.edit()
                        .putString(setting_theme, when (checkedId) {
                            R.id.settings_themeCard_radioGroup_light -> setting_theme_light
                            R.id.settings_themeCard_radioGroup_dark -> setting_theme_dark
                            R.id.settings_themeCard_radioGroup_black -> setting_theme_black
                            else -> setting_theme_default
                        })
                        .apply()

                showMessageWithAction(R.string.settings_theme_restartMessage, R.string.settings_theme_restartButton, Snackbar.LENGTH_INDEFINITE) {
                    mainActivity.finish()
                    startActivity(mainActivity
                            .baseContext
                            .packageManager!!
                            .getLaunchIntentForPackage(context!!.packageName)!!
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }
            }
        }

        fun refreshForceSecureConnectionExplanation(isEnabled: Boolean) {
            view.settings_forceSecureConnectionText.text = if (isEnabled)
                getString(R.string.settings_forceSecureConnectionEnabledExplanation)
            else
                getString(R.string.settings_forceSecureConnectionDisabledExplanation)
        }

        val settingForceSecure = prefs.getBoolean(setting_force_secure, false)
        refreshForceSecureConnectionExplanation(settingForceSecure)
        view.settings_forceSecureConnectionSwitch.apply {
            isChecked = settingForceSecure
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit()
                        .putBoolean(setting_force_secure, isChecked)
                        .apply()
                refreshForceSecureConnectionExplanation(isChecked)
            }
        }

        mainActivity.supportActionBar?.show()
        return view
    }

    private fun showMessageWithAction(@StringRes message: Int, @StringRes actionCaption: Int, length: Int, callback: () -> Unit) {
        Snackbar.make(this.view!!, message, length)
                .setAction(actionCaption) {
                    callback()
                }
                .show()
    }
}
