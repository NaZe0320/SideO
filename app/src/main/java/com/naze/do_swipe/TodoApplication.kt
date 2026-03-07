package com.naze.do_swipe

import android.app.Application
import com.naze.do_swipe.data.local.TodoDatabase
import com.naze.do_swipe.data.preferences.SettingsRepository
import com.naze.do_swipe.data.repository.TodoRepository
import com.naze.do_swipe.widget.TodoAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: TodoRepository by lazy {
        val dao = TodoDatabase.getInstance(this).todoDao()
        TodoRepository(dao)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            if (!settingsRepository.hasSeededTutorialTodos()) {
                repository.seedTutorialTodos()
                settingsRepository.setTutorialSeeded()
                TodoAppWidgetProvider.updateAllWidgets(applicationContext)
            }
        }
    }
}

