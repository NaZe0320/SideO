package com.naze.side_o.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            Surface(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로"
                            )
                        }
                        Text(
                            text = "아카이브",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToTrash) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "휴지통"
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sections.forEach { section ->
                item(key = "header_${section.title}") {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
