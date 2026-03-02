package com.naze.do_swipe.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodoReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? TodoApplication ?: return@withContext Result.failure()
        val settings = app.settingsRepository
        if (!settings.isRemindersEnabled()) {
            return@withContext Result.success()
        }
        val todos = app.repository.getActiveTodosOnce()
        val titles = todos.map { it.title }
        NotificationHelper.showReminderNotification(
            context = applicationContext,
            count = titles.size,
            titles = titles
        )
        Result.success()
    }
}
