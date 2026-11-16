package com.example.pikaroo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.pikaroo.ui.navigation.AppNavigation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app draws behind the system bars on every Android version (Android 15+ forces it
        // anyway). Its screens are light, so the bar icons are dark; screens with an orange header
        // switch the status bar icons to light with LightStatusBarIcons().
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, DARK_SCRIM),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, DARK_SCRIM)
        )
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }

    private companion object {
        /** Behind the bars only where the system cannot draw dark icons (navigation bar below Android 8). */
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
