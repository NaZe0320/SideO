package com.naze.side_o

import android.app.Application
import com.naze.side_o.data.local.TodoDatabase
import com.naze.side_o.data.repository.TodoRepository

class TodoApplication : Application() {

    val repository: TodoRepository by lazy {
        val dao = TodoDatabase.getInstance(this).todoDao()
        TodoRepository(dao)
    }
}
