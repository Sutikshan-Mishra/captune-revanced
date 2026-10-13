package dev.hd450btnc.companion

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hd450btnc.companion.settings.ThemeMode
import dev.hd450btnc.companion.ui.AppRoot
import dev.hd450btnc.companion.ui.theme.StudioTheme

class MainActivity : ComponentActivity() {
    private val controller: AppController
        get() = (application as CompanionApp).controller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by controller.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            // Keep status/navigation bar icon contrast in sync with the in-app theme choice.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            StudioTheme(dark = dark) {
                AppRoot(controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.headset.start()
    }

    override fun onStop() {
        controller.headset.stop()
        controller.flush()
        super.onStop()
    }
}
