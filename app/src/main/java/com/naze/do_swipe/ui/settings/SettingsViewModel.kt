package com.naze.do_swipe.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.data.preferences.SettingsRepository
import com.naze.do_swipe.data.preferences.ThemeMode
import com.naze.do_swipe.data.repository.TodoRepository
import com.naze.do_swipe.widget.TodoAppWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings: SettingsRepository =
        (application as TodoApplication).settingsRepository

    private val repository: TodoRepository =
        (application as TodoApplication).repository

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

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTodos()
            TodoAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}
