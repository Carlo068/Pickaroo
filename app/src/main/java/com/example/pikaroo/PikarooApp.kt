package com.example.pikaroo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pikaroo.common.di.AppContainer
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.preferences.PreferencesStringStore
import com.example.pikaroo.ui.driver.location.LocationSourceFactory
import org.osmdroid.config.Configuration
import java.io.File

class PikarooApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // osmdroid exige un user agent propio y, desde Android 10, carpetas dentro de la app.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
    val locationSourceFactory: LocationSourceFactory by lazy {
        LocationSourceFactory(this, container.driverLocationPreferences, container.driverRepository)
    }

    val container: AppContainer by lazy {
        AppContainer(
            sessionStore = PreferencesStringStore(this),
            mockApiFactory = { MockPickarooApi.create(this) }
        )
    }
}

fun Context.appContainer(): AppContainer = (applicationContext as PikarooApp).container

fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PikarooApp).container
