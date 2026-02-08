package com.naze.side_o.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun from(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(ThemeMode.from(prefs.getInt(KEY_THEME, 0)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _swipeReversed = MutableStateFlow(prefs.getBoolean(KEY_SWIPE_REVERSED, false))
    val swipeReversed: StateFlow<Boolean> = _swipeReversed.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS, false))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME, mode.value).apply()
        _themeMode.value = mode
    }

    fun setSwipeReversed(reversed: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_REVERSED, reversed).apply()
        _swipeReversed.value = reversed
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply()
        _remindersEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "side_o_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SWIPE_REVERSED = "swipe_reversed"
        private const val KEY_REMINDERS = "reminders_enabled"
    }
}
