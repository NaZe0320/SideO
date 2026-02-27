package com.naze.do_swipe.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.naze.do_swipe.R
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.data.local.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val MAX_ITEMS = 8

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return TodoWidgetFactory(applicationContext)
    }
}

class TodoWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodoEntity> = emptyList()

    override fun onCreate() {
        // 초기 데이터는 onDataSetChanged에서 로드
    }

    override fun onDataSetChanged() {
        val app = context.applicationContext as TodoApplication
        items = try {
            runBlocking {
                app.repository.getActiveTodos().first().take(MAX_ITEMS)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) {
            return null
        }

        val todo = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_todo_list_item)

        val displayTitle = if (todo.isImportant) "⭐ ${todo.title}" else todo.title
        views.setTextViewText(R.id.widget_item_title, displayTitle)

        val fillInIntent = Intent().apply {
            putExtra(TodoAppWidgetProvider.EXTRA_TODO_ID, todo.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

