package org.alignertracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.alignertracker.app.ui.AlignerTheme
import org.alignertracker.app.ui.TrackerApp
import org.alignertracker.app.ui.TrackerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TrackerApplication).container
        val model =
            ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        TrackerViewModel(
                            container.repository,
                            container.reminderSettings,
                            container.reminderScheduler,
                            container.photoStore,
                            container.clockGuard,
                            org.alignertracker.app.ui.ResourceUserErrorText(applicationContext),
                        )
                            as T
                },
            )[TrackerViewModel::class.java]
        setContent { AlignerTheme { TrackerApp(model) } }
    }
}
