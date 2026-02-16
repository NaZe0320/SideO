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
        private const val ACTION_TOGGLE_TODO = "com.naze.side_o.ACTION_TOGGLE_TODO"
        private const val EXTRA_TODO_ID = "extra_todo_id"
        private const val MAX_ITEMS = 5

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TodoAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, TodoAppWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
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
                val views = buildRemoteViews(context, todos, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_TODO) {
            val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
            if (todoId != -1L) {
                val application = context.applicationContext as TodoApplication
                application.applicationScope.launch {
                    try {
                        application.repository.setCompleted(todoId, true)
                        // 위젯 갱신
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        // 에러 처리 (로그 등)
                    }
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        todos: List<TodoEntity>,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_todo_list)

        // 항목 컨테이너 ID 리스트
        val itemContainerIds = listOf(
            R.id.widget_todo_item_0,
            R.id.widget_todo_item_1,
            R.id.widget_todo_item_2,
            R.id.widget_todo_item_3,
            R.id.widget_todo_item_4
        )

        // 항목 제목 TextView ID 리스트
        val titleViewIds = listOf(
            R.id.widget_todo_title_0,
            R.id.widget_todo_title_1,
            R.id.widget_todo_title_2,
            R.id.widget_todo_title_3,
            R.id.widget_todo_title_4
        )

        // 항목 체크 버튼 ID 리스트
        val checkButtonIds = listOf(
            R.id.widget_todo_check_button_0,
            R.id.widget_todo_check_button_1,
            R.id.widget_todo_check_button_2,
            R.id.widget_todo_check_button_3,
            R.id.widget_todo_check_button_4
        )

        val displayTodos = todos.take(MAX_ITEMS)

        if (displayTodos.isEmpty()) {
            // 할 일이 없을 때
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            itemContainerIds.forEach { itemId ->
                views.setViewVisibility(itemId, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)

            displayTodos.forEachIndexed { index, todo ->
                val containerId = itemContainerIds[index]
                val titleId = titleViewIds[index]
                val checkButtonId = checkButtonIds[index]
                
                views.setViewVisibility(containerId, View.VISIBLE)

                // 중요 항목은 별표 추가
                val displayTitle = if (todo.isImportant) {
                    "⭐ ${todo.title}"
                } else {
                    todo.title
                }
                views.setTextViewText(titleId, displayTitle)

                // 체크 버튼 클릭 이벤트 설정
                val checkButtonIntent = Intent(context, TodoAppWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_TODO
                    putExtra(EXTRA_TODO_ID, todo.id)
                }
                val checkPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (appWidgetId * 1000 + index),
                    checkButtonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                views.setOnClickPendingIntent(checkButtonId, checkPendingIntent)
            }

            // 나머지 빈 슬롯 숨기기
            for (i in displayTodos.size until MAX_ITEMS) {
                views.setViewVisibility(itemContainerIds[i], View.GONE)
            }
        }

        return views
    }
}
