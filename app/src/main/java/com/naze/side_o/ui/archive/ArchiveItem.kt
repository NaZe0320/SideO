package com.naze.side_o.ui.archive

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
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
    val shortDate = formatShortDate(completedAt)

    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onToggleSelection),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = completedAgoText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = shortDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
