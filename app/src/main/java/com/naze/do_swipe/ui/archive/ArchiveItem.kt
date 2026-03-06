package com.naze.do_swipe.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.naze.do_swipe.data.local.TodoEntity
import com.naze.do_swipe.ui.components.SwipeDirection
import com.naze.do_swipe.ui.components.SwipeToDismissBox
import com.naze.do_swipe.ui.theme.SwipeActionComplete
import com.naze.do_swipe.ui.theme.SwipeActionCompleteContent
import com.naze.do_swipe.ui.theme.SwipeActionDelete
import com.naze.do_swipe.ui.theme.SwipeActionDeleteContent
import com.naze.do_swipe.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveItem(
    todo: TodoEntity,
    onRestore: () -> Unit,
    onRequestPermanentDelete: () -> Unit,
    swipeReversed: Boolean,
    modifier: Modifier = Modifier
) {
    val completedAt = todo.completedAt ?: todo.createdAt
    val daysAgo = daysAgoFrom(completedAt)
    val completedAgoText = when {
        daysAgo == 0L -> "오늘 완료"
        daysAgo == 1L -> "1일 전 완료"
        else -> "${daysAgo}일 전 완료"
    }

    val cardShape = RoundedCornerShape(24.dp)
    SwipeToDismissBox(
        modifier = modifier.fillMaxWidth(),
        thresholdFraction = 0.5f,
        confirmBeforeDismissEndToStart = !swipeReversed,
        confirmBeforeDismissStartToEnd = swipeReversed,
        onDismissStartToEnd = {
            if (!swipeReversed) onRestore()
        },
        onDismissEndToStart = {
            if (swipeReversed) onRestore()
        },
        onConfirmRequestedEndToStart = if (!swipeReversed) ({ onRequestPermanentDelete() }) else null,
        onConfirmRequestedStartToEnd = if (swipeReversed) ({ onRequestPermanentDelete() }) else null,
        backgroundContent = { direction ->
            Card(
                modifier = Modifier.fillMaxSize(),
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
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = null,
                                        tint = SwipeActionCompleteContent
                                    )
                                    Text(
                                        "복원",
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
                                        "영구 삭제",
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
                                        "영구 삭제",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SwipeActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = null,
                                        tint = SwipeActionCompleteContent
                                    )
                                    Text(
                                        "복원",
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
    ) { _, _ ->
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = completedAgoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = null,
                    tint = SwipeActionComplete,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

internal fun daysAgoFrom(timestampMs: Long): Long {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    return TimeUnit.MILLISECONDS.toDays(diff)
}

internal fun formatShortDate(timestampMs: Long): String {
    val locale = Locale.getDefault()
    val pattern = if (locale.language == "ko") "yyyy/M/d" else "MMM d, yyyy"
    return SimpleDateFormat(pattern, locale).format(Date(timestampMs))
}
