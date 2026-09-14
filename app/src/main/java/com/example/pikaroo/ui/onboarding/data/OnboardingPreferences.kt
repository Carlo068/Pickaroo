package com.example.pikaroo.ui.onboarding.data

import android.content.Context
import com.example.pikaroo.common.preferences.AppPreferences

class OnboardingPreferences(context: Context) {
    private val preferences = AppPreferences(context)

    fun hasCompletedOnboarding(): Boolean =
        preferences.getBoolean(KEY_COMPLETED_ONBOARDING)

    fun setOnboardingCompleted() {
        preferences.putBoolean(KEY_COMPLETED_ONBOARDING, true)
    }

    companion object {
        private const val KEY_COMPLETED_ONBOARDING =
            "completed_onboarding"
    }
}