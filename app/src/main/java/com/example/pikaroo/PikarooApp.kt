package com.example.pikaroo

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pikaroo.common.di.AppContainer
import com.example.pikaroo.common.mock.MockPickarooApi

class PikarooApp : Application() {
    val container = AppContainer(mockApiFactory = { MockPickarooApi.create(this) })
}

fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PikarooApp).container
