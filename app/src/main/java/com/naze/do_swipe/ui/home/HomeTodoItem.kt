package com.naze.do_swipe.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.naze.do_swipe.data.local.TodoEntity
import com.naze.do_swipe.ui.components.SwipeDirection
import com.naze.do_swipe.ui.components.SwipeToDismissBox
import com.naze.do_swipe.ui.theme.SwipeActionComplete
import com.naze.do_swipe.ui.theme.SwipeActionCompleteContent
import com.naze.do_swipe.ui.theme.SwipeActionDelete
import com.naze.do_swipe.ui.theme.SwipeActionDeleteContent
import com.naze.do_swipe.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeTodoItem(
    modifier: Modifier = Modifier,
    todo: TodoEntity,
    index: Int,
    isDragging: Boolean,
    isDimmed: Boolean,
    onDragStart: () -> Unit,
    onDragMove: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    viewModel: HomeViewModel,
    swipeReversed: Boolean = false,
    onAfterComplete: ((Long) -> Unit)? = null,
    onAfterDelete: ((Long) -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember(todo.id) { mutableStateOf(todo.title) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "수정",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = {
                        if (it.length <= 60) {
                            editTitle = it
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(32.dp)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTodo(todo.copy(title = editTitle.trim()))
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    val cardShape = RoundedCornerShape(24.dp)
    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, animationSpec = tween(200))
    val alpha by animateFloatAsState(if (isDimmed) 0.6f else 1f, animationSpec = tween(150))
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    SwipeToDismissBox(
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .fillMaxWidth(),
        clipToBounds = !isDragging,
        thresholdFraction = 0.5f,
        backgroundContent = { direction ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isDragging) 0f else 1f),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (direction) {
                        SwipeDirection.EndToStart ->
                            if (swipeReversed) SwipeActionComplete else SwipeActionDelete
                        SwipeDirection.StartToEnd ->
                            if (swipeReversed) SwipeActionDelete else SwipeActionComplete
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (direction) {
                            SwipeDirection.EndToStart -> Arrangement.End
                            SwipeDirection.StartToEnd -> Arrangement.Start
                            null -> Arrangement.Center
                        }
                    ) {
                        when (direction) {
                            SwipeDirection.EndToStart -> {
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SwipeActionCompleteContent
                                    )
                                    Text(
                                        "완료",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SwipeActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = SwipeActionDeleteContent
                                    )
                                    Text(
                                        "삭제",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SwipeActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            SwipeDirection.StartToEnd -> {
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = SwipeActionDeleteContent
                                    )
                                    Text(
                                        "삭제",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SwipeActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SwipeActionCompleteContent
                                    )
                                    Text(
                                        "완료",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SwipeActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            null -> {}
                        }
                    }
                }
            }
        },
        onDismissStartToEnd = {
            if (swipeReversed) {
                viewModel.markDeleted(todo.id)
                onAfterDelete?.invoke(todo.id)
            } else {
                viewModel.setCompleted(todo.id, true)
                onAfterComplete?.invoke(todo.id)
            }
        },
        onDismissEndToStart = {
            if (swipeReversed) {
                viewModel.setCompleted(todo.id, true)
                onAfterComplete?.invoke(todo.id)
            } else {
                viewModel.markDeleted(todo.id)
                onAfterDelete?.invoke(todo.id)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { currentOnDragStart() },
                        onDrag = { _, delta -> currentOnDragMove(delta.y) },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() }
                    )
                }
                .combinedClickable(
                    onClick = {
                        editTitle = todo.title
                        showEditDialog = true
                    }
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = "이동 가능",
                    tint = TextSecondary,
                    modifier = Modifier
                        .padding(end = 16.dp)
                )
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
