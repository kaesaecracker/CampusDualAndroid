package xyz.mattishub.campusDual

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import xyz.mattishub.campusDual.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private val navController by lazy { findNavController(R.id.main_navHost) }
    private val appBarConfiguration by lazy { AppBarConfiguration(navController.graph) }
    private var currentTheme = ""

    val globalViewModel: GlobalViewModel by lazy {
        ViewModelProviders
                .of(this, GlobalViewModel.Factory(baseContext))
                .get(GlobalViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set theme according to setting
        currentTheme = globalViewModel.getTheme().value ?: SettingsFragment.setting_theme_default
        setTheme(when (currentTheme) {
            SettingsFragment.setting_theme_dark -> R.style.AppTheme_Dark
            SettingsFragment.setting_theme_black -> R.style.AppTheme_Black
            SettingsFragment.setting_theme_light -> R.style.AppTheme_Light
            else -> R.style.AppTheme
        })

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

}
