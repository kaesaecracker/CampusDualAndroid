package xyz.mattishub.campusDual

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.mattishub.campusDual.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private val navController by lazy { findNavController(R.id.main_navHost) }
    private val appBarConfiguration by lazy { AppBarConfiguration(navController.graph) }

    val globalViewModel: GlobalViewModel by lazy {
        ViewModelProviders
                .of(this, GlobalViewModelFactory(baseContext))
                .get(GlobalViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // open settings if matric or hash not set
        val matric = globalViewModel.globalPrefs.getString(SettingsFragment.setting_matric, "")
        val hash = globalViewModel.globalPrefs.getString(SettingsFragment.setting_hash, "")

        if (matric.isNullOrBlank() || hash.isNullOrBlank())
            navController.navigate(R.id.firstLaunchFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) GlobalScope.launch {
            setupBackgroundWorker()
            forceRefreshWidget(baseContext)
        }
    }

}
