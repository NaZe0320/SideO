package com.naze.do_swipe

import android.app.Application
import com.naze.do_swipe.data.local.TodoDatabase
import com.naze.do_swipe.data.preferences.SettingsRepository
import com.naze.do_swipe.data.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TodoApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: TodoRepository by lazy {
        val dao = TodoDatabase.getInstance(this).todoDao()
        TodoRepository(dao)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }
}
