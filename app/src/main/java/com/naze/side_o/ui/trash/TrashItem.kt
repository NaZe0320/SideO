package com.naze.side_o.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.ui.archive.daysAgoFrom
import com.naze.side_o.ui.components.SwipeDirection
import com.naze.side_o.ui.components.SwipeToDismissByPositionBox
import com.naze.side_o.ui.theme.ActionComplete
import com.naze.side_o.ui.theme.ActionCompleteContent
import com.naze.side_o.ui.theme.ActionDelete
import com.naze.side_o.ui.theme.ActionDeleteContent
import com.naze.side_o.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashItem(
    todo: TodoEntity,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit,
    swipeReversed: Boolean,
    modifier: Modifier = Modifier
) {
    val deletedAt = todo.deletedAt ?: todo.createdAt
    val daysAgo = daysAgoFrom(deletedAt)
    val deletedAgoText = when {
        daysAgo == 0L -> "오늘 삭제됨"
        daysAgo == 1L -> "1일 전 삭제"
        else -> "${daysAgo}일 전 삭제"
    }

    val cardShape = RoundedCornerShape(24.dp)
    SwipeToDismissByPositionBox(
        modifier = modifier.fillMaxWidth(),
        thresholdFraction = 0.5f,
        backgroundContent = { direction ->
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (direction) {
                        SwipeDirection.EndToStart ->
                            if (swipeReversed) ActionComplete else ActionDelete
                        SwipeDirection.StartToEnd ->
                            if (swipeReversed) ActionDelete else ActionComplete
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
                            SwipeDirection.StartToEnd -> {
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
                                        imageVector = Icons.Outlined.Restore,
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
                            null -> {}
                        }
                    }
                }
            }
        },
        onDismissStartToEnd = {
            if (swipeReversed) onDeletePermanent()
            else onRestore()
        },
        onDismissEndToStart = {
            if (swipeReversed) onRestore()
            else onDeletePermanent()
        }
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
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = deletedAgoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
