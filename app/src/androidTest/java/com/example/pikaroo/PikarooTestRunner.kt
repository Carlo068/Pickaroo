package com.example.pikaroo

import android.app.Activity
import android.app.Application
import android.content.res.Resources
import android.os.Bundle
import android.os.PersistableBundle
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

/**
 * Runs the whole instrumented suite with the app in Spanish, whatever language the emulator uses.
 *
 * The tests look for Spanish text, so the language has to be fixed in one place. This runner sets
 * the default locale and pushes a Spanish configuration into the Resources of the Application, of
 * the target context and of every Activity right before its onCreate (the Resources are what
 * `stringResource` and `getString` read). It only touches the test process; the device language and
 * the installed app are left alone.
 *
 * Limits: Resources created later by the system for something that is not an Activity or the
 * Application (for example the foreground service's own context) still follow the device
 * language, and a configuration change that the system applies after the activity started can
 * undo the pin until the activity is created again.
 */
class PikarooTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        Locale.setDefault(TEST_LOCALE)
        pin(targetContext.resources)
        super.onCreate(arguments)
    }

    override fun callApplicationOnCreate(app: Application) {
        Locale.setDefault(TEST_LOCALE)
        pin(app.resources)
        super.callApplicationOnCreate(app)
    }

    override fun callActivityOnCreate(activity: Activity, icicle: Bundle?) {
        pinActivity(activity)
        super.callActivityOnCreate(activity, icicle)
    }

    override fun callActivityOnCreate(activity: Activity, icicle: Bundle?, persistentState: PersistableBundle?) {
        pinActivity(activity)
        super.callActivityOnCreate(activity, icicle, persistentState)
    }

    private fun pinActivity(activity: Activity) {
        Locale.setDefault(TEST_LOCALE)
        pin(activity.resources)
    }

    @Suppress("DEPRECATION")
    private fun pin(resources: Resources) {
        val configuration = android.content.res.Configuration(resources.configuration)
        configuration.setLocale(TEST_LOCALE)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    companion object {
        val TEST_LOCALE: Locale = Locale("es")
    }
}
