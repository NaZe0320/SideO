package com.naze.do_swipe.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.analytics.AnalyticsEvents
import com.naze.do_swipe.data.preferences.SettingsRepository
import com.naze.do_swipe.data.preferences.ThemeMode
import com.naze.do_swipe.data.repository.TodoRepository
import com.naze.do_swipe.worker.TodoReminderWorker
import com.naze.do_swipe.widget.TodoAppWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings: SettingsRepository =
        (application as TodoApplication).settingsRepository

    private val repository: TodoRepository =
        (application as TodoApplication).repository

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val swipeReversed: StateFlow<Boolean> = settings.swipeReversed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val swipeBackgroundBlendEnabled: StateFlow<Boolean> = settings.swipeBackgroundBlendEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val swipeThresholdFraction: StateFlow<Float> = settings.swipeThresholdFraction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)
    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val reminderHour: StateFlow<Int> = settings.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)
    val reminderMinute: StateFlow<Int> = settings.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setThemeMode(mode: ThemeMode) {
        settings.setThemeMode(mode)
    }

    fun setSwipeReversed(reversed: Boolean) {
        settings.setSwipeReversed(reversed)
    }

    fun setSwipeBackgroundBlendEnabled(enabled: Boolean) {
        settings.setSwipeBackgroundBlendEnabled(enabled)
    }

    fun setSwipeThresholdFraction(value: Float) {
        settings.setSwipeThresholdFraction(value)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        settings.setRemindersEnabled(enabled)
        if (enabled) {
            (getApplication() as TodoApplication).analytics.logEvent(AnalyticsEvents.REMINDER_ENABLED)
            scheduleReminderWork()
        } else {
            WorkManager.getInstance(getApplication()).cancelUniqueWork(WORK_NAME)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        settings.setReminderTime(hour, minute)
        if (settings.isRemindersEnabled()) {
            scheduleReminderWork()
        }
    }

    private fun scheduleReminderWork() {
        val app = getApplication<TodoApplication>()
        val hour = settings.getReminderHour()
        val minute = settings.getReminderMinute()
        val delayMs = nextTriggerDelayMillis(hour, minute)
        val request = PeriodicWorkRequestBuilder<TodoReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun nextTriggerDelayMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var trigger = cal.timeInMillis
        if (trigger <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            trigger = cal.timeInMillis
        }
        return trigger - now
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTodos()
            TodoAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}

private const val WORK_NAME = "todo_reminder"
