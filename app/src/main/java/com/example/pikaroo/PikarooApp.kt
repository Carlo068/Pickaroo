package com.example.pikaroo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pikaroo.common.di.AppContainer
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.preferences.PreferencesStringStore

class PikarooApp : Application() {
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
