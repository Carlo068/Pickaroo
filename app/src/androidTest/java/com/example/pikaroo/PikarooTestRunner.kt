package com.example.pikaroo

import android.app.Activity
import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
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
 * The pin applies only while no app language has been chosen (the per-app locale list is empty).
 * Once a test picks a language in the app, the activity is created with that language and the
 * runner leaves it alone, so the in-app choice is what the test sees. A test that changes the
 * language must put it back to `AppLanguage.SYSTEM` in its `@After`: on Android 13+ the system
 * keeps the per-app locale, even across reinstalls. As a safety net, [ClearAppLocale] clears any
 * app language before every test, so a leak from one test cannot turn the others English.
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
        val withListener = Bundle(arguments ?: Bundle.EMPTY)
        val listeners = listOfNotNull(withListener.getString("listener"), ClearAppLocale::class.java.name)
        withListener.putString("listener", listeners.joinToString(","))
        super.onCreate(withListener)
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
        if (appLanguageChosen(activity)) return
        Locale.setDefault(TEST_LOCALE)
        pin(activity.resources)
    }

    /** True when the app has its own language (anything but "System default"). Works on every API level. */
    private fun appLanguageChosen(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            !context.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty
        } else {
            !AppCompatDelegate.getApplicationLocales().isEmpty
        }

    @Suppress("DEPRECATION")
    private fun pin(resources: Resources) {
        val configuration = android.content.res.Configuration(resources.configuration)
        configuration.setLocale(TEST_LOCALE)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    /** Runs before every test (and its @Before): puts the app language back to "System default". */
    class ClearAppLocale : RunListener() {
        override fun testStarted(description: Description) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync {
                if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
        }
    }

    companion object {
        val TEST_LOCALE: Locale = Locale("es")
    }
}
