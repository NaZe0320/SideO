package com.naze.side_o.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.naze.side_o.R
import com.naze.side_o.TodoApplication
import com.naze.side_o.data.local.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TodoAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TODO = "com.naze.side_o.ACTION_TOGGLE_TODO"
        const val EXTRA_TODO_ID = "extra_todo_id"
        private const val MAX_ITEMS = 5

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val idsDefault = appWidgetManager.getAppWidgetIds(ComponentName(context, TodoAppWidgetProvider::class.java))
            val idsSmall = appWidgetManager.getAppWidgetIds(ComponentName(context, TodoAppWidgetProviderSmall::class.java))
            val idsMedium = appWidgetManager.getAppWidgetIds(ComponentName(context, TodoAppWidgetProviderMedium::class.java))
            if (idsDefault.isNotEmpty()) {
                context.sendBroadcast(Intent(context, TodoAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsDefault)
                })
            }
            if (idsSmall.isNotEmpty()) {
                context.sendBroadcast(Intent(context, TodoAppWidgetProviderSmall::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsSmall)
                })
            }
            if (idsMedium.isNotEmpty()) {
                context.sendBroadcast(Intent(context, TodoAppWidgetProviderMedium::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsMedium)
                })
            }
        }

        internal fun buildRemoteViews(
            context: Context,
            todos: List<TodoEntity>,
            appWidgetId: Int,
            layoutId: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)
            val itemContainerIds = listOf(
                R.id.widget_todo_item_0,
                R.id.widget_todo_item_1,
                R.id.widget_todo_item_2,
                R.id.widget_todo_item_3,
                R.id.widget_todo_item_4
            )
            val titleViewIds = listOf(
                R.id.widget_todo_title_0,
                R.id.widget_todo_title_1,
                R.id.widget_todo_title_2,
                R.id.widget_todo_title_3,
                R.id.widget_todo_title_4
            )
            val checkButtonIds = listOf(
                R.id.widget_todo_check_button_0,
                R.id.widget_todo_check_button_1,
                R.id.widget_todo_check_button_2,
                R.id.widget_todo_check_button_3,
                R.id.widget_todo_check_button_4
            )
            val displayTodos = todos.take(MAX_ITEMS)
            if (displayTodos.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                itemContainerIds.forEach { itemId -> views.setViewVisibility(itemId, View.GONE) }
            } else {
                views.setViewVisibility(R.id.widget_empty_text, View.GONE)
                displayTodos.forEachIndexed { index, todo ->
                    views.setViewVisibility(itemContainerIds[index], View.VISIBLE)
                    val displayTitle = if (todo.isImportant) "⭐ ${todo.title}" else todo.title
                    views.setTextViewText(titleViewIds[index], displayTitle)
                    val checkButtonIntent = Intent(context, TodoAppWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_TODO
                        putExtra(EXTRA_TODO_ID, todo.id)
                    }
                    views.setOnClickPendingIntent(
                        checkButtonIds[index],
                        PendingIntent.getBroadcast(context, (appWidgetId * 1000 + index), checkButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                }
                for (i in displayTodos.size until MAX_ITEMS) views.setViewVisibility(itemContainerIds[i], View.GONE)
            }
            return views
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val application = context.applicationContext as TodoApplication
        application.applicationScope.launch {
            val todos = try {
                application.repository.getActiveTodos().first()
            } catch (e: Exception) {
                emptyList()
            }

            appWidgetIds.forEach { appWidgetId ->
                val views = buildRemoteViews(context, todos, appWidgetId, R.layout.widget_todo_list)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_TODO) {
            val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
            if (todoId != -1L) {
                val application = context.applicationContext as TodoApplication
                application.applicationScope.launch {
                    try {
                        application.repository.setCompleted(todoId, true)
                        updateAllWidgets(context)
                    } catch (e: Exception) { }
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }
}

/** 2x2 소형 위젯 */
class TodoAppWidgetProviderSmall : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val application = context.applicationContext as TodoApplication
        application.applicationScope.launch {
            val todos = try { application.repository.getActiveTodos().first() } catch (e: Exception) { emptyList() }
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, TodoAppWidgetProvider.buildRemoteViews(context, todos, appWidgetId, R.layout.widget_todo_small))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TodoAppWidgetProvider.ACTION_TOGGLE_TODO) {
            val todoId = intent.getLongExtra(TodoAppWidgetProvider.EXTRA_TODO_ID, -1L)
            if (todoId != -1L) {
                val application = context.applicationContext as TodoApplication
                application.applicationScope.launch {
                    try {
                        application.repository.setCompleted(todoId, true)
                        TodoAppWidgetProvider.updateAllWidgets(context)
                    } catch (e: Exception) { }
                }
            }
        } else super.onReceive(context, intent)
    }
}

/** 4x2 중형 위젯 */
class TodoAppWidgetProviderMedium : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val application = context.applicationContext as TodoApplication
        application.applicationScope.launch {
            val todos = try { application.repository.getActiveTodos().first() } catch (e: Exception) { emptyList() }
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, TodoAppWidgetProvider.buildRemoteViews(context, todos, appWidgetId, R.layout.widget_todo_medium))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TodoAppWidgetProvider.ACTION_TOGGLE_TODO) {
            val todoId = intent.getLongExtra(TodoAppWidgetProvider.EXTRA_TODO_ID, -1L)
            if (todoId != -1L) {
                val application = context.applicationContext as TodoApplication
                application.applicationScope.launch {
                    try {
                        application.repository.setCompleted(todoId, true)
                        TodoAppWidgetProvider.updateAllWidgets(context)
                    } catch (e: Exception) { }
                }
            }
        } else super.onReceive(context, intent)
    }
}
