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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.naze.side_o.TodoApplication
import com.naze.side_o.ui.theme.TextSecondary
import com.naze.side_o.ui.trash.TrashItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as TodoApplication
    val swipeReversed by app.settingsRepository.swipeReversed.collectAsState(initial = false)
    val sections by viewModel.sections.collectAsState()
    val deletedTodos by viewModel.deletedTodos.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showSnackbarWithUndo(
        message: String,
        onUndo: () -> Unit
    ) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "실행취소",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                                text = "아카이브",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(modifier = Modifier.padding(8.dp)) {}
                    }
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("완료") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("삭제됨") }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        if (selectedTabIndex == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
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
                            onRestore = {
                                viewModel.uncomplete(todo.id)
                                showSnackbarWithUndo("복원됨") { viewModel.recomplete(todo.id) }
                            },
                            onDeletePermanent = {
                                viewModel.schedulePermanentDelete(todo.id)
                                showSnackbarWithUndo("삭제됨") { viewModel.cancelPendingDelete(todo.id) }
                            },
                            swipeReversed = swipeReversed
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "deleted_header") {
                    Text(
                        text = "RECENTLY DELETED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(
                    items = deletedTodos,
                    key = { it.id }
                ) { todo ->
                    TrashItem(
                        todo = todo,
                        onRestore = {
                            viewModel.restore(todo.id)
                            showSnackbarWithUndo("복원됨") { viewModel.markDeletedAgain(todo.id) }
                        },
                        onDeletePermanent = {
                            viewModel.schedulePermanentDelete(todo.id)
                            showSnackbarWithUndo("삭제됨") { viewModel.cancelPendingDelete(todo.id) }
                        },
                        swipeReversed = swipeReversed
                    )
                }
            }
        }
    }
}
