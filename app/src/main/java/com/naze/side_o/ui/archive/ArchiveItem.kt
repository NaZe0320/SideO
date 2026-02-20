package com.naze.side_o.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.ui.theme.ActionComplete
import com.naze.side_o.ui.theme.Primary
import com.naze.side_o.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ArchiveItem(
    todo: TodoEntity,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedAt = todo.completedAt ?: todo.createdAt
    val daysAgo = daysAgoFrom(completedAt)
    val completedAgoText = when {
        daysAgo == 0L -> "오늘 완료"
        daysAgo == 1L -> "1일 전 완료"
        else -> "${daysAgo}일 전 완료"
    }

    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggleSelection),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
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
