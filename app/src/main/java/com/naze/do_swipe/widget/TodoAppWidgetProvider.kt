package com.naze.do_swipe.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.naze.do_swipe.MainActivity
import com.naze.do_swipe.R
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.data.local.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TodoAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TODO = "com.naze.do_swipe.ACTION_TOGGLE_TODO"
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_OPEN_ADD_FROM_WIDGET = "extra_open_add_from_widget"
        private const val MAX_ITEMS = 8

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
            val views = RemoteViews(context.packageName, R.layout.widget_todo_list)

            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty_text)

            val toggleIntent = Intent(context, TodoAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TODO
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, togglePendingIntent)

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_container, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_more_container, mainPendingIntent)

            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ADD_FROM_WIDGET, true)
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 1000,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

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
