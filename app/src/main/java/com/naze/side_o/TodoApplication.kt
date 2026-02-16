package com.naze.side_o

import android.app.Application
import com.naze.side_o.data.local.TodoDatabase
import com.naze.side_o.data.preferences.SettingsRepository
import com.naze.side_o.data.repository.TodoRepository
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
