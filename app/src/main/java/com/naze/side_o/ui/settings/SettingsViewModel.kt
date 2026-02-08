package com.naze.side_o.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naze.side_o.TodoApplication
import com.naze.side_o.data.preferences.SettingsRepository
import com.naze.side_o.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings: SettingsRepository =
        (application as TodoApplication).settingsRepository

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val swipeReversed: StateFlow<Boolean> = settings.swipeReversed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) {
        settings.setThemeMode(mode)
    }

    fun setSwipeReversed(reversed: Boolean) {
        settings.setSwipeReversed(reversed)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        settings.setRemindersEnabled(enabled)
    }
}
