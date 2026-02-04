package com.naze.side_o.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.naze.side_o.ui.theme.CompleteGreen
import com.naze.side_o.ui.theme.CompleteGreenContent
import com.naze.side_o.ui.theme.DeleteRed
import com.naze.side_o.ui.theme.DeleteRedContent
import com.naze.side_o.ui.theme.StarOutlineGray
import com.naze.side_o.ui.theme.StarYellow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToArchive: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTodos by viewModel.activeTodos.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newImportant by remember { mutableStateOf(false) }

    val dateStr = remember {
        val locale = Locale.getDefault()
        val pattern = if (locale.language == "ko") "yyyy년 M월 d일 EEEE" else "MMMM d, yyyy, EEEE"
        SimpleDateFormat(pattern, locale).format(Date())
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = onNavigateToArchive) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = "아카이브"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "설정"
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter a task...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        trailingIcon = {
                            IconButton(onClick = { newImportant = !newImportant }) {
                                Icon(
                                    imageVector = if (newImportant) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (newImportant) "중요" else "일반",
                                    tint = if (newImportant) StarYellow else StarOutlineGray
                                )
                            }
                        },
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
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addTodo(newTitle.trim(), newImportant)
                                newTitle = ""
                                newImportant = false
                            }
                        }
                    ) {
                        Text("추가")
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTodos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "할 일이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = {
                        items(
                            items = activeTodos,
                            key = { it.id }
                        ) { todo ->
                            HomeTodoItem(
                                todo = todo,
                                viewModel = viewModel,
                                allItems = activeTodos
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeTodoItem(
    todo: TodoEntity,
    viewModel: HomeViewModel,
    allItems: List<TodoEntity>
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    viewModel.markDeleted(todo.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    viewModel.setCompleted(todo.id, true)
                    true
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember(todo.id) { mutableStateOf(todo.title) }
    var showReorderDialog by remember { mutableStateOf(false) }
    val index = allItems.indexOfFirst { it.id == todo.id }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("수정") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTodo(todo.copy(title = editTitle.trim()))
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showReorderDialog) {
        AlertDialog(
            onDismissRequest = { showReorderDialog = false },
            title = { Text("순서 변경") },
            text = { Text("위로 또는 아래로 이동") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            if (index > 0) {
                                viewModel.reorder(allItems, index, index - 1)
                                showReorderDialog = false
                            }
                        }
                    ) {
                        Text("위로")
                    }
                    TextButton(
                        onClick = {
                            if (index in 0 until allItems.lastIndex) {
                                viewModel.reorder(allItems, index, index + 1)
                                showReorderDialog = false
                            }
                        }
                    ) {
                        Text("아래로")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReorderDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }

    val cardShape = RoundedCornerShape(12.dp)
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> DeleteRed
                        SwipeToDismissBoxValue.StartToEnd -> CompleteGreen
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                            SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                            else -> Arrangement.Center
                        }
                    ) {
                        when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = DeleteRedContent
                                )
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeleteRedContent,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = CompleteGreenContent
                                )
                                Text(
                                    "Complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CompleteGreenContent,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = {
                        editTitle = todo.title
                        showEditDialog = true
                    },
                    onLongClick = { showReorderDialog = true }
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.setImportant(todo.id, !todo.isImportant) }
                ) {
                    Icon(
                        imageVector = if (todo.isImportant) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (todo.isImportant) "중요 해제" else "중요",
                        tint = if (todo.isImportant) StarYellow else StarOutlineGray
                    )
                }
            }
        }
    }
}
