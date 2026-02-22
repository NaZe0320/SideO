package com.naze.side_o.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * 스와이프 방향. 배경 UI 표시용.
 */
enum class SwipeDirection {
    StartToEnd,
    EndToStart
}

/**
 * 수평 드래그만 감지하고, 손을 뗐을 때 **위치(이동 거리)**만으로 확정 여부를 판단하는 Modifier.
 * velocity는 사용하지 않음.
 *
 * @param offsetPx 현재 오프셋(px). 부모에서 Modifier.offset에 사용.
 * @param widthPx 콘텐츠 너비(px). threshold 계산용.
 * @param onDismissStartToEnd 오른쪽으로 threshold 이상 스와이프 시 호출.
 * @param onDismissEndToStart 왼쪽으로 threshold 이상 스와이프 시 호출.
 * @param thresholdFraction 확정에 필요한 최소 이동 비율. 기본 0.5f(50%).
 */
fun Modifier.swipeToDismissByPositionOnly(
    offsetPx: MutableState<Float>,
    widthPx: Float,
    onDismissStartToEnd: () -> Unit,
    onDismissEndToStart: () -> Unit,
    thresholdFraction: Float = 0.5f
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "swipeToDismissByPositionOnly"
        properties["thresholdFraction"] = thresholdFraction
    }
) {
    if (widthPx <= 0f) return@composed this
    val scope = rememberCoroutineScope()
    val threshold = widthPx * thresholdFraction
    this.pointerInput(widthPx, thresholdFraction) {
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, delta ->
                offsetPx.value = (offsetPx.value + delta).coerceIn(-widthPx, widthPx)
            },
            onDragEnd = {
                val current = offsetPx.value
                scope.launch {
                    when {
                        current > threshold -> {
                            animate(
                                initialValue = current,
                                targetValue = widthPx,
                                animationSpec = tween(300)
                            ) { value, _ -> offsetPx.value = value }
                            onDismissStartToEnd()
                        }
                        current < -threshold -> {
                            animate(
                                initialValue = current,
                                targetValue = -widthPx,
                                animationSpec = tween(300)
                            ) { value, _ -> offsetPx.value = value }
                            onDismissEndToStart()
                        }
                        else -> {
                            animate(
                                initialValue = current,
                                targetValue = 0f,
                                animationSpec = tween(200)
                            ) { value, _ -> offsetPx.value = value }
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
                    ) { value, _ -> offsetPx.value = value }
                }
            }
        )
    }
}

/**
 * 위치(이동 거리)만으로 스와이프 확정하는 Box. velocity 미사용.
 * 배경과 앞쪽 콘텐츠는 동일한 크기로 배치됨.
 *
 * @param modifier [Modifier].
 * @param thresholdFraction 확정 거리 비율. 기본 0.5f(50%). 호출부에서 변경 가능.
 * @param backgroundContent 방향에 따른 배경 (direction: null이면 중립).
 * @param onDismissStartToEnd 오른쪽 스와이프 확정 시.
 * @param onDismissEndToStart 왼쪽 스와이프 확정 시.
 * @param content 앞쪽 콘텐츠(카드 등).
 */
@Composable
fun SwipeToDismissByPositionBox(
    modifier: Modifier = Modifier,
    thresholdFraction: Float = 0.5f,
    backgroundContent: @Composable (direction: SwipeDirection?) -> Unit,
    onDismissStartToEnd: () -> Unit,
    onDismissEndToStart: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetPx = remember { mutableStateOf(0f) }
    var widthPx by remember { mutableStateOf(0f) }
    val direction = when {
        offsetPx.value > 0f -> SwipeDirection.StartToEnd
        offsetPx.value < 0f -> SwipeDirection.EndToStart
        else -> null
    }
    
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { constraints ->
        // 1) 콘텐츠(제스처·시각 이동 포함)를 부모와 동일 제약으로 한 번만 측정 → (w, h)
        val contentPlaceable = subcompose("content") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = offsetPx.value }
                    .swipeToDismissByPositionOnly(
                        offsetPx = offsetPx,
                        widthPx = widthPx,
                        onDismissStartToEnd = onDismissStartToEnd,
                        onDismissEndToStart = onDismissEndToStart,
                        thresholdFraction = thresholdFraction
                    )
            ) {
                content()
            }
        }.map { it.measure(constraints) }.firstOrNull()
            ?: return@SubcomposeLayout layout(0, 0) {}

        val w = contentPlaceable.width
        val h = contentPlaceable.height
        widthPx = w.toFloat()

        // 2) 배경을 콘텐츠와 동일한 고정 크기(w, h)로 측정
        val backgroundPlaceable = subcompose("background") {
            Box(modifier = Modifier.clipToBounds()) {
                backgroundContent(direction)
            }
        }.map { it.measure(Constraints.fixed(w, h)) }.firstOrNull()

        layout(w, h) {
            backgroundPlaceable?.place(0, 0)
            contentPlaceable.place(0, 0)
        }
    }
}
