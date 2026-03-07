package com.naze.do_swipe.ui.home

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.data.local.TodoEntity
import com.naze.do_swipe.ui.components.showUndoSnackbar
import com.naze.do_swipe.ui.theme.Primary
import com.naze.do_swipe.ui.theme.TextSecondary
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    val swipeBackgroundBlendFromPrefs by app.settingsRepository.swipeBackgroundBlendEnabled.collectAsState(initial = false)
    val swipeThresholdFromPrefs by app.settingsRepository.swipeThresholdFraction.collectAsState(initial = app.settingsRepository.getSwipeThresholdFraction())
    val activeTodos by viewModel.activeTodos.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newImportant by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    var previousTodoCount by remember { mutableStateOf(activeTodos.size) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val autoScrollEdgePx = with(density) { 96.dp.toPx() }
    val maxAutoScrollPerFramePx = with(density) { 22.dp.toPx() }

    var listBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var containerTopInRoot by remember { mutableStateOf(0f) }
    var dragStartContainerTopInRoot by remember { mutableStateOf(0f) }
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var draggedFromIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var fingerYInRoot by remember { mutableStateOf(0f) }
    var touchOffsetInItemPx by remember { mutableStateOf(0f) }
    var ghostTopInRoot by remember { mutableStateOf(0f) }
    var autoScrollPerFramePx by remember { mutableStateOf(0f) }
    // 드롭 후 DB 업데이트 딜레이 동안 튀는 현상을 막기 위한 임시 리스트
    var optimisticallyReorderedList by remember { mutableStateOf<List<TodoEntity>?>(null) }

    val draggedItem: TodoEntity? = remember(activeTodos, draggedItemId) {
        activeTodos.firstOrNull { it.id == draggedItemId }
    }

    val displayItems: List<TodoEntity> = remember(activeTodos, draggedFromIndex, dropTargetIndex, optimisticallyReorderedList) {
        val from = draggedFromIndex
        val to = dropTargetIndex
        if (from != null && to != null && from != to && activeTodos.isNotEmpty()) {
            activeTodos.toMutableList().apply { add(to, removeAt(from)) }
        } else {
            optimisticallyReorderedList ?: activeTodos
        }
    }

    LaunchedEffect(activeTodos) {
        optimisticallyReorderedList = null
        val current = activeTodos.size
        val prev = previousTodoCount
        if (current > prev && current > 0) {
            listState.animateScrollToItem(current - 1)
        }
        previousTodoCount = current
    }

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(draggedItemId, draggedItem) {
        if (draggedItemId != null && draggedItem == null) {
            draggedItemId = null
            draggedFromIndex = null
            dropTargetIndex = null
            autoScrollPerFramePx = 0f
        }
    }

    LaunchedEffect(draggedItemId, autoScrollPerFramePx) {
        if (draggedItemId == null || autoScrollPerFramePx == 0f) return@LaunchedEffect
        while (isActive && draggedItemId != null && autoScrollPerFramePx != 0f) {
            val consumed = listState.scrollBy(autoScrollPerFramePx)
            if (consumed == 0f) {
                autoScrollPerFramePx = 0f
                break
            }
            val localY = (fingerYInRoot - (listBoundsInRoot?.top ?: 0f)).toInt()
            val target = calculateTargetIndex(
                items = activeTodos,
                visible = listState.layoutInfo.visibleItemsInfo,
                localPointerY = localY
            )
            val from = draggedFromIndex
            dropTargetIndex = if (from != null && target != null && target != from) target else null
            delay(16)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            AppTopBarHome(
                title = "Do! Swipe",
                onArchiveClick = onNavigateToArchive,
                onSettingsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            HomeInputBar(
                newTitle = newTitle,
                onTitleChange = {
                    if (it.length <= 60) {
                        newTitle = it
                    }
                },
                onSubmit = {
                    if (newTitle.isNotBlank()) {
                        viewModel.addTodo(newTitle.trim(), newImportant)
                        newTitle = ""
                        newImportant = false
                    }
                },
                focusRequester = focusRequester
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onGloballyPositioned { coords ->
                    containerTopInRoot = coords.boundsInRoot().top
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val bounds = listBoundsInRoot ?: return@detectDragGesturesAfterLongPress
                            val pointerYInRoot = containerTopInRoot + offset.y
                            val localY = (pointerYInRoot - bounds.top).toInt()
                            val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                localY in info.offset until (info.offset + info.size)
                            } ?: return@detectDragGesturesAfterLongPress
                            val picked = activeTodos.getOrNull(hit.index) ?: return@detectDragGesturesAfterLongPress

                            dragStartContainerTopInRoot = containerTopInRoot
                            fingerYInRoot = pointerYInRoot
                            touchOffsetInItemPx = (localY - hit.offset).toFloat()
                            ghostTopInRoot = pointerYInRoot - touchOffsetInItemPx
                            draggedFromIndex = hit.index
                            dropTargetIndex = null
                            draggedItemId = picked.id
                        },
                        onDrag = { change, dragAmount ->
                            if (draggedItemId == null) return@detectDragGesturesAfterLongPress
                            change.consume()
                            fingerYInRoot += dragAmount.y
                            ghostTopInRoot = fingerYInRoot - touchOffsetInItemPx

                            val bounds = listBoundsInRoot
                            autoScrollPerFramePx = if (bounds != null) {
                                calculateAutoScrollPerFrame(
                                    pointerYInRoot = fingerYInRoot,
                                    listBoundsInRoot = bounds,
                                    edgePx = autoScrollEdgePx,
                                    maxPerFramePx = maxAutoScrollPerFramePx
                                )
                            } else {
                                0f
                            }

                            val localY = (fingerYInRoot - (bounds?.top ?: 0f)).toInt()
                            val target = calculateTargetIndex(
                                items = activeTodos,
                                visible = listState.layoutInfo.visibleItemsInfo,
                                localPointerY = localY
                            )
                            val from = draggedFromIndex
                            dropTargetIndex = if (from != null && target != null && target != from) target else null
                        },
                        onDragCancel = {
                            draggedItemId = null
                            draggedFromIndex = null
                            dropTargetIndex = null
                            autoScrollPerFramePx = 0f
                            dragStartContainerTopInRoot = containerTopInRoot
                        },
                        onDragEnd = {
                            val from = draggedFromIndex
                            val to = dropTargetIndex
                            if (from != null && to != null && from != to) {
                                optimisticallyReorderedList = activeTodos.toMutableList().apply { add(to, removeAt(from)) }
                                viewModel.reorder(activeTodos, from, to)
                            }
                            draggedItemId = null
                            draggedFromIndex = null
                            dropTargetIndex = null
                            autoScrollPerFramePx = 0f
                            dragStartContainerTopInRoot = containerTopInRoot
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                listBoundsInRoot = coords.boundsInRoot()
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            userScrollEnabled = draggedItemId == null,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            content = {
                                itemsIndexed(
                                    items = displayItems,
                                    key = { _, it -> it.id }
                                ) { _, todo ->
                                    val isSourceItem = todo.id == draggedItemId
                                    val sourceAlpha = when {
                                        !isSourceItem -> 1f
                                        draggedItem != null -> 0f
                                        else -> 0.35f
                                    }
                                    HomeTodoItem(
                                        modifier = Modifier
                                            .animateItem()
                                            .alpha(sourceAlpha),
                                        todo = todo,
                                        isDragging = false,
                                        isDimmed = draggedItemId != null && !isSourceItem,
                                        viewModel = viewModel,
                                        swipeReversed = swipeReversedFromPrefs,
                                        swipeBackgroundBlendEnabled = swipeBackgroundBlendFromPrefs,
                                        thresholdFraction = swipeThresholdFromPrefs,
                                        onAfterComplete = { id ->
                                            scope.showUndoSnackbar(snackbarHostState, "완료됨") {
                                                viewModel.setCompleted(id, false)
                                            }
                                        },
                                        onAfterDelete = { id ->
                                            scope.showUndoSnackbar(snackbarHostState, "휴지통으로 이동") {
                                                viewModel.restore(id)
                                            }
                                        },
                                        enableInteractions = draggedItemId == null
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (draggedItem != null) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(
                        x = 0,
                        y = (ghostTopInRoot - dragStartContainerTopInRoot).roundToInt()
                    ),
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                        clippingEnabled = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .shadow(6.dp, RoundedCornerShape(24.dp))
                            .alpha(0.96f)
                            .zIndex(1000f)
                    ) {
                        HomeTodoItem(
                            modifier = Modifier.fillMaxWidth(),
                            todo = draggedItem,
                            isDragging = true,
                            isDimmed = false,
                            viewModel = viewModel,
                            swipeReversed = swipeReversedFromPrefs,
                            swipeBackgroundBlendEnabled = swipeBackgroundBlendFromPrefs,
                            thresholdFraction = swipeThresholdFromPrefs,
                            enableInteractions = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeInputBar(
    newTitle: String,
    onTitleChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester
) {
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
                onValueChange = onTitleChange,
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
                    onDone = { onSubmit() }
                )
            )
            FilledIconButton(
                onClick = onSubmit,
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


private fun calculateTargetIndex(
    items: List<TodoEntity>,
    visible: List<LazyListItemInfo>,
    localPointerY: Int
): Int? {
    if (items.isEmpty() || visible.isEmpty()) return null
    val first = visible.first()
    val last = visible.last()
    return when {
        localPointerY <= first.offset -> 0
        localPointerY >= last.offset + last.size -> items.lastIndex
        else -> {
            visible.firstOrNull { info ->
                localPointerY in info.offset until (info.offset + info.size)
            }?.index ?: visible.minByOrNull { info ->
                abs(localPointerY - (info.offset + info.size / 2))
            }?.index
        }
    }?.coerceIn(0, items.lastIndex)
}

private fun calculateAutoScrollPerFrame(
    pointerYInRoot: Float,
    listBoundsInRoot: Rect,
    edgePx: Float,
    maxPerFramePx: Float
): Float {
    val topZone = listBoundsInRoot.top + edgePx
    val bottomZone = listBoundsInRoot.bottom - edgePx
    return when {
        pointerYInRoot < topZone -> {
            val ratio = ((topZone - pointerYInRoot) / edgePx).coerceIn(0f, 1f)
            -maxPerFramePx * ratio
        }
        pointerYInRoot > bottomZone -> {
            val ratio = ((pointerYInRoot - bottomZone) / edgePx).coerceIn(0f, 1f)
            maxPerFramePx * ratio
        }
        else -> 0f
    }
}
