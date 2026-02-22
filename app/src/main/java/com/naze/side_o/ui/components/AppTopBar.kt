package com.naze.side_o.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val AppTopBarPadding = 16.dp

/**
 * 공통 AppBar 홈형: 좌 아카이브 / 중앙 제목 / 우 설정.
 * 패딩·타이포·색상 통일.
 */
@Composable
fun AppTopBarHome(
    title: String,
    onArchiveClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTopBarPadding, vertical = AppTopBarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onArchiveClick) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = "아카이브",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 서브 화면용 AppBar: 뒤로가기 + 중앙 제목 + 오른쪽 48.dp 균형.
 * [bottomContent]가 있으면 그 아래에 추가 (예: TabRow).
 */
@Composable
fun AppTopBarSub(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTopBarPadding, vertical = AppTopBarPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(modifier = Modifier.size(48.dp)) {}
            }
            if (bottomContent != null) {
                bottomContent()
            }
        }
    }
}
