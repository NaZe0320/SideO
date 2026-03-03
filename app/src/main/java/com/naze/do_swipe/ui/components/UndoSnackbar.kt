package com.naze.do_swipe.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 실행취소 액션이 있는 스낵바를 공통으로 보여주는 헬퍼.
 * 새 스낵바를 보여주기 전에 기존 스낵바를 먼저 닫아서 중첩을 방지한다.
 */
fun CoroutineScope.showUndoSnackbar(
    hostState: SnackbarHostState,
    message: String,
    onUndo: () -> Unit
) {
    launch {
        // 이미 표시 중인 스낵바가 있으면 닫아서 큐에 쌓이지 않도록 한다.
        hostState.currentSnackbarData?.dismiss()

        val result = hostState.showSnackbar(
            message = message,
            actionLabel = "실행취소",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo()
        }
    }
}

