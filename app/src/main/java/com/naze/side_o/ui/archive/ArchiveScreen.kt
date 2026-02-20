package com.naze.side_o.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.side_o.ui.theme.TextSecondary

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections by viewModel.sections.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    Scaffold(
        modifier = modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (selectedIds.isNotEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.deleteSelected() },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null
                            )
                        },
                        text = { Text("영구 삭제") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
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
                            text = "Archive",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToTrash) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "휴지통",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.title}") {
                        Text(
                            text = section.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(
                        items = section.items,
                        key = { it.id }
                    ) { todo ->
                        ArchiveItem(
                            todo = todo,
                            selected = todo.id in selectedIds,
                            onToggleSelection = { viewModel.toggleSelection(todo.id) }
                        )
                    }
                }
            }
            Text(
                text = "선택한 항목을 휴지통으로 이동할 수 있습니다.",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
    }
}

// @Composable
// private fun HistoryLimitCard(
//     onLearnMoreClick: () -> Unit,
//     modifier: Modifier = Modifier
// ) {
//     Card(
//         modifier = modifier.fillMaxWidth(),
//         shape = RoundedCornerShape(12.dp),
//         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
//     ) {
//         Column(
//             modifier = Modifier.padding(16.dp),
//             verticalArrangement = Arrangement.spacedBy(8.dp)
//         ) {
//             Text(
//                 text = "History Limit",
//                 style = MaterialTheme.typography.titleMedium,
//                 color = MaterialTheme.colorScheme.onSurface
//             )
//             Text(
//                 text = "Showing last 30 items. Upgrade to Premium for full history.",
//                 style = MaterialTheme.typography.bodyMedium,
//                 color = MaterialTheme.colorScheme.onSurfaceVariant
//             )
//             Button(
//                 onClick = onLearnMoreClick,
//                 modifier = Modifier.align(Alignment.CenterHorizontally)
//             ) {
//                 Text("Learn More")
//             }
//         }
//     }
// }
