package xyz.mattishub.campusDual

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import xyz.mattishub.campusDual.fragments.SettingsFragment


class MainActivity : AppCompatActivity() {

    private val navController by lazy { findNavController(R.id.main_navHost) }
    private val appBarConfiguration by lazy { AppBarConfiguration(navController.graph) }

    val globalViewModel: GlobalViewModel by lazy {
        ViewModelProviders
                .of(this, GlobalViewModel.Factory(baseContext))
                .get(GlobalViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set theme
        setTheme(R.style.AppTheme)

        // setup ui
        setContentView(R.layout.activity_main)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // open settings if matric or hash not set
        val matric = globalViewModel.globalPrefs.getString(SettingsFragment.setting_matric, "")
        val hash = globalViewModel.globalPrefs.getString(SettingsFragment.setting_hash, "")

        if (matric.isNullOrBlank() || hash.isNullOrBlank())
            navController.navigate(R.id.firstLaunchFragment)
    }

    override fun onStart() {
        super.onStart()
        setupBackgroundWorker()
        forceRefreshWidget(baseContext)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun setTheme(resid: Int) {
        super.setTheme(resid)

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // when (prefs.getString(SettingsFragment.setting_theme, SettingsFragment.setting_theme_default)) {

        when (globalViewModel.getTheme().value) {
            SettingsFragment.setting_theme_black ->
                theme.applyStyle(R.style.AppThemeOverlay_Black, true)
            SettingsFragment.setting_theme_dark ->
                theme.applyStyle(R.style.AppThemeOverlay_Dark, true)
            else ->
                theme.applyStyle(R.style.AppThemeOverlay_Light, true)
        }
    }
}



