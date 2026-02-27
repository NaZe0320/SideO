package com.naze.do_swipe.ui.home

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.naze.do_swipe.ui.components.AppTopBarHome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.data.local.TodoEntity
import com.naze.do_swipe.ui.theme.Primary
import com.naze.do_swipe.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToArchive: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    openAddOnStart: Boolean = false
) {
    val app = LocalContext.current.applicationContext as TodoApplication
    val swipeReversedFromPrefs by app.settingsRepository.swipeReversed.collectAsState(initial = false)
    val activeTodos by viewModel.activeTodos.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newImportant by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var pendingItems by remember { mutableStateOf<List<TodoEntity>?>(null) }
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val itemHeightPx = with(density) { (72.dp + 8.dp).toPx() }
    val dropTargetIndex: Int? = draggedIndex?.let { d ->
        val raw = (d + (dragOffsetY / itemHeightPx).roundToInt())
            .coerceIn(0, activeTodos.lastIndex.coerceAtLeast(0))
        if (raw == d) null else raw
    }
    LaunchedEffect(activeTodos) {
        pendingItems = null
    }
    val visualItems: List<TodoEntity> = remember(activeTodos, draggedIndex, dropTargetIndex) {
        val d = draggedIndex
        val t = dropTargetIndex
        if (d != null && t != null) {
            activeTodos.toMutableList().apply { add(t, removeAt(d)) }
        } else {
            activeTodos
        }
    }
    val displayItems: List<TodoEntity> = pendingItems ?: visualItems

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun showSnackbarWithUndo(message: String, onUndo: () -> Unit) {
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
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        },
        topBar = {
            AppTopBarHome(
                title = "Do! Swipe",
                onArchiveClick = onNavigateToArchive,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTodos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "할 일이 없습니다",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        itemsIndexed(
                            items = displayItems,
                            key = { _, it -> it.id }
                        ) { index, todo ->
                            val isDragging = todo.id == activeTodos.getOrNull(draggedIndex ?: -1)?.id
                            HomeTodoItem(
                                modifier = if (!isDragging) Modifier.animateItem() else Modifier,
                                todo = todo,
                                index = index,
                                isDragging = isDragging,
                                isDimmed = draggedIndex != null && !isDragging,
                                onDragStart = {
                                    draggedIndex = activeTodos.indexOfFirst { it.id == todo.id }
                                    dragOffsetY = 0f
                                },
                                onDragMove = { deltaY -> dragOffsetY += deltaY },
                                onDragEnd = {
                                    val from = draggedIndex
                                    val to = dropTargetIndex
                                    if (from != null && to != null && from != to) {
                                        pendingItems = activeTodos.toMutableList()
                                            .apply { add(to, removeAt(from)) }
                                        viewModel.reorder(activeTodos, from, to)
                                    }
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                },
                                viewModel = viewModel,
                                swipeReversed = swipeReversedFromPrefs,
                                onAfterComplete = { id ->
                                    showSnackbarWithUndo("완료됨") { viewModel.setCompleted(id, false) }
                                },
                                onAfterDelete = { id ->
                                    showSnackbarWithUndo("휴지통으로 이동") { viewModel.restore(id) }
                                }
                            )
                        }
                    }
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            if (it.length <= 60) {
                                newTitle = it
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                "다음 할 일은?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(32.dp),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newTitle.isNotBlank()) {
                                    viewModel.addTodo(newTitle.trim(), newImportant)
                                    newTitle = ""
                                    newImportant = false
                                }
                            }
                        )
                    )
                    FilledIconButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addTodo(newTitle.trim(), newImportant)
                                newTitle = ""
                                newImportant = false
                            }
                        },
                        modifier = Modifier.padding(4.dp),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = Primary,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "추가",
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
