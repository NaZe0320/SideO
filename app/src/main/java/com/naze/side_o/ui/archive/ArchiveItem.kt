package com.naze.side_o.ui.archive

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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.ui.theme.ActionComplete
import com.naze.side_o.ui.theme.ActionCompleteContent
import com.naze.side_o.ui.theme.ActionDelete
import com.naze.side_o.ui.theme.ActionDeleteContent
import com.naze.side_o.ui.theme.Primary
import com.naze.side_o.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveItem(
    todo: TodoEntity,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit,
    swipeReversed: Boolean,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (swipeReversed) onRestore()
                    else onDeletePermanent()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (swipeReversed) onDeletePermanent()
                    else onRestore()
                    true
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )

    val completedAt = todo.completedAt ?: todo.createdAt
    val daysAgo = daysAgoFrom(completedAt)
    val completedAgoText = when {
        daysAgo == 0L -> "오늘 완료"
        daysAgo == 1L -> "1일 전 완료"
        else -> "${daysAgo}일 전 완료"
    }

    val cardShape = RoundedCornerShape(24.dp)
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart ->
                            if (swipeReversed) ActionComplete else ActionDelete
                        SwipeToDismissBoxValue.StartToEnd ->
                            if (swipeReversed) ActionDelete else ActionComplete
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
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
                        horizontalArrangement = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                            SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                            else -> Arrangement.Center
                        }
                    ) {
                        when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.DoneAll,
                                        contentDescription = null,
                                        tint = ActionCompleteContent
                                    )
                                    Text(
                                        "복원",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = ActionDeleteContent
                                    )
                                    Text(
                                        "삭제",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = ActionDeleteContent
                                    )
                                    Text(
                                        "삭제",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.DoneAll,
                                        contentDescription = null,
                                        tint = ActionCompleteContent
                                    )
                                    Text(
                                        "복원",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                Icon(
                    imageVector = if (todo.isImportant) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = if (todo.isImportant) Primary else TextSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
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
                    tint = ActionComplete,
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
