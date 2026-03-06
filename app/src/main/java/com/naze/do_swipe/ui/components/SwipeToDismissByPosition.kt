package com.naze.do_swipe.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SwipeDirection {
    StartToEnd,
    EndToStart
}

/**
 * 배경(background)과 콘텐츠(content)가 항상 동일한 크기를 가지는 스와이프 Box.
 *
 * - 내부 Box 하나가 콘텐츠 높이만큼만 차지하고, 배경은 그 내부 Box에 matchParentSize()로 동일 크기 보장
 * - 스와이프 확정은 위치(이동 거리)만으로 판단, velocity 미사용
 * - confirmBeforeDismiss* 가 true인 방향에서는 카드를 밀어내지 않고 offset을 0으로 복귀한 뒤 onConfirmRequested* 만 호출
 *
 * @param modifier 외부 레이아웃 Modifier
 * @param thresholdFraction 확정에 필요한 최소 이동 비율 (기본 0.5f = 50%)
 * @param confirmBeforeDismissEndToStart true면 왼쪽 스와이프 시 애니메이션 없이 리셋 후 onConfirmRequestedEndToStart만 호출
 * @param confirmBeforeDismissStartToEnd true면 오른쪽 스와이프 시 애니메이션 없이 리셋 후 onConfirmRequestedStartToEnd만 호출
 * @param onDismissStartToEnd 오른쪽 스와이프 확정 시 호출 (confirm 사용 시 미호출)
 * @param onDismissEndToStart 왼쪽 스와이프 확정 시 호출 (confirm 사용 시 미호출)
 * @param onConfirmRequestedStartToEnd confirmBeforeDismissStartToEnd일 때 오른쪽 스와이프 확정 시 호출
 * @param onConfirmRequestedEndToStart confirmBeforeDismissEndToStart일 때 왼쪽 스와이프 확정 시 호출
 * @param backgroundContent 배경 UI. direction = null이면 중립 상태
 * @param content 앞쪽 콘텐츠 (카드 등). swipeProgress(0f~1f), direction 전달. 이 크기가 전체 크기 기준
 */
@Composable
fun SwipeToDismissBox(
    modifier: Modifier = Modifier,
    clipToBounds: Boolean = true,
    thresholdFraction: Float = 0.5f,
    confirmBeforeDismissEndToStart: Boolean = false,
    confirmBeforeDismissStartToEnd: Boolean = false,
    onDismissStartToEnd: () -> Unit = {},
    onDismissEndToStart: () -> Unit = {},
    onConfirmRequestedEndToStart: (() -> Unit)? = null,
    onConfirmRequestedStartToEnd: (() -> Unit)? = null,
    backgroundContent: @Composable (direction: SwipeDirection?) -> Unit,
    content: @Composable (swipeProgress: Float, direction: SwipeDirection?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetPx = remember { mutableStateOf(0f) }

    // ✅ onSizeChanged: measure 단계 외부에서 안전하게 너비 취득
    //    pointerInput(Unit) 내부에서 state로 읽으므로 항상 최신값 참조
    var widthPx by remember { mutableStateOf(0f) }

    // ✅ derivedStateOf: offsetPx가 같은 방향으로 계속 변해도 direction이
    //    실제로 바뀔 때만 recomposition 발생
    val direction by remember {
        derivedStateOf {
            when {
                offsetPx.value > 0f -> SwipeDirection.StartToEnd
                offsetPx.value < 0f -> SwipeDirection.EndToStart
                else -> null
            }
        }
    }

    val swipeProgress by remember {
        derivedStateOf {
            val threshold = widthPx * thresholdFraction
            if (threshold <= 0f) 0f
            else when {
                offsetPx.value < 0f -> (-offsetPx.value / threshold).coerceIn(0f, 1f)
                offsetPx.value > 0f -> (offsetPx.value / threshold).coerceIn(0f, 1f)
                else -> 0f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(if (clipToBounds) Modifier.clipToBounds() else Modifier)
    ) {
        // 내부 Box: 높이는 콘텐츠만으로 결정 → 배경/콘텐츠가 이 크기에 맞춰 동일해짐
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> widthPx = size.width.toFloat() }
        ) {
            // 배경: 이 Box와 동일 크기
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                backgroundContent(direction)
            }

            // 콘텐츠: 이 Box의 크기 결정자 (fillMaxWidth + wrap content height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = offsetPx.value }
                    .pointerInput((thresholdFraction * 10).roundToInt()) {
                    // Int 키(1~9)로 변경 시 제스처 블록이 확실히 재시작되어 최신 임계점 즉시 반영
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, delta ->
                            offsetPx.value = (offsetPx.value + delta)
                                .coerceIn(-widthPx, widthPx)
                        },
                        onDragEnd = {
                            val current = offsetPx.value
                            val threshold = widthPx * thresholdFraction
                            scope.launch {
                                when {
                                    // ── 오른쪽으로 dismiss (또는 confirm 요청) ─────
                                    current >= threshold -> {
                                        if (confirmBeforeDismissStartToEnd) {
                                            animate(
                                                initialValue = current,
                                                targetValue = 0f,
                                                animationSpec = tween(200)
                                            ) { v, _ -> offsetPx.value = v }
                                            onConfirmRequestedStartToEnd?.invoke()
                                        } else {
                                            animate(
                                                initialValue = current,
                                                targetValue = widthPx,
                                                animationSpec = tween(300)
                                            ) { v, _ -> offsetPx.value = v }
                                            onDismissStartToEnd()
                                            offsetPx.value = 0f
                                        }
                                    }
                                    // ── 왼쪽으로 dismiss (또는 confirm 요청) ──────
                                    current <= -threshold -> {
                                        if (confirmBeforeDismissEndToStart) {
                                            animate(
                                                initialValue = current,
                                                targetValue = 0f,
                                                animationSpec = tween(200)
                                            ) { v, _ -> offsetPx.value = v }
                                            onConfirmRequestedEndToStart?.invoke()
                                        } else {
                                            animate(
                                                initialValue = current,
                                                targetValue = -widthPx,
                                                animationSpec = tween(300)
                                            ) { v, _ -> offsetPx.value = v }
                                            onDismissEndToStart()
                                            offsetPx.value = 0f
                                        }
                                    }
                                    // ── 미확정: 제자리로 복귀 ──────────────
                                    else -> {
                                        animate(
                                            initialValue = current,
                                            targetValue = 0f,
                                            animationSpec = tween(200)
                                        ) { v, _ -> offsetPx.value = v }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(
                                    initialValue = offsetPx.value,
                                    targetValue = 0f,
                                    animationSpec = tween(200)
                                ) { v, _ -> offsetPx.value = v }
                            }
                        }
                    )
                }
            ) {
                content(swipeProgress, direction)
            }
        }
    }
}
