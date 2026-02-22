package com.naze.side_o.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import com.naze.side_o.ui.components.AppTopBarSub
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    val pendingDeleteIds by viewModel.pendingDeleteIds.collectAsState()
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
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        },
        topBar = {
            AppTopBarSub(
                title = "아카이브",
                onBackClick = onNavigateBack,
                bottomContent = {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Archive") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Trash") }
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        if (selectedTabIndex == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { section ->
                    val filteredItems = section.items.filter { it.id !in pendingDeleteIds }
                    if (filteredItems.isEmpty()) return@forEach
                    item(key = "header_${section.title}") {
                        Text(
                            text = section.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(
                        items = filteredItems,
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
                    .windowInsetsPadding(WindowInsets.navigationBars)
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
                    items = deletedTodos.filter { it.id !in pendingDeleteIds },
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
