package com.naze.do_swipe.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import com.naze.do_swipe.ui.components.AppTopBarSub
import com.naze.do_swipe.ui.components.ConfirmDialog
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
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.ui.components.showUndoSnackbar
import com.naze.do_swipe.ui.theme.TextSecondary
import com.naze.do_swipe.ui.trash.TrashItem
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
    val swipeBackgroundBlendEnabled by app.settingsRepository.swipeBackgroundBlendEnabled.collectAsState(initial = false)
    val swipeThresholdFraction by app.settingsRepository.swipeThresholdFraction.collectAsState(initial = app.settingsRepository.getSwipeThresholdFraction())
    val sections by viewModel.sections.collectAsState()
    val deletedTodos by viewModel.deletedTodos.collectAsState()
    val pendingDeleteIds by viewModel.pendingDeleteIds.collectAsState()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (pendingDeleteId != null) {
        val idToDelete = pendingDeleteId!!
        ConfirmDialog(
            title = "영구 삭제",
            message = "이 항목을 영구 삭제할까요? 복구할 수 없습니다.",
            confirmText = "영구 삭제",
            dismissText = "취소",
            isDestructive = true,
            onConfirm = {
                pendingDeleteId = null
                viewModel.schedulePermanentDelete(idToDelete)
                scope.showUndoSnackbar(snackbarHostState, "삭제됨") {
                    viewModel.cancelPendingDelete(idToDelete)
                }
            },
            onDismiss = { pendingDeleteId = null }
        )
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
                            text = { Text("완료한 일") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("삭제한 일") }
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp)
        if (selectedTabIndex == 0) {
            val visibleSections = sections.map { s ->
                s to s.items.filter { it.id !in pendingDeleteIds }
            }.filter { (_, items) -> items.isNotEmpty() }
            if (visibleSections.isEmpty()) {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "완료된 할 일이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleSections.forEach { (section, filteredItems) ->
                        item(key = "header_${section.title}") {
                            Text(
                                text = section.title,
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
                                    scope.showUndoSnackbar(snackbarHostState, "복원됨") {
                                        viewModel.recomplete(todo.id)
                                    }
                                },
                                onRequestPermanentDelete = { pendingDeleteId = todo.id },
                                swipeReversed = swipeReversed,
                                swipeBackgroundBlendEnabled = swipeBackgroundBlendEnabled,
                                thresholdFraction = swipeThresholdFraction
                            )
                        }
                    }
                }
            }
        } else {
            val visibleDeleted = deletedTodos.filter { it.id !in pendingDeleteIds }
            if (visibleDeleted.isEmpty()) {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "삭제된 할 일이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "deleted_header") {
                        Text(
                            text = "최근 삭제",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(
                        items = visibleDeleted,
                        key = { it.id }
                    ) { todo ->
                        TrashItem(
                            todo = todo,
                            onRestore = {
                                viewModel.restore(todo.id)
                                scope.showUndoSnackbar(snackbarHostState, "복원됨") {
                                    viewModel.markDeletedAgain(todo.id)
                                }
                            },
                            onRequestPermanentDelete = { pendingDeleteId = todo.id },
                            swipeReversed = swipeReversed,
                            swipeBackgroundBlendEnabled = swipeBackgroundBlendEnabled,
                            thresholdFraction = swipeThresholdFraction
                        )
                    }
                }
            }
        }
    }
}
