package com.naze.do_swipe.analytics

/**
 * Analytics 이벤트 로깅 인터페이스.
 * Firebase 구현체를 주입해 테스트/비활성화 대체 가능.
 */
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any>? = null)
}

object AnalyticsEvents {
    const val FIRST_OPEN = "first_open"
    const val TASK_CREATED = "task_created"
    const val TASK_SWIPE_COMPLETED = "task_swipe_completed"
    const val TASK_SWIPE_DELETED = "task_swipe_deleted"
    const val UNDO_CLICKED = "undo_clicked"
    const val REMINDER_ENABLED = "reminder_enabled"
    const val NOTIFICATION_OPENED = "notification_opened"
    const val WIDGET_TASK_CREATED = "widget_task_created"
}
