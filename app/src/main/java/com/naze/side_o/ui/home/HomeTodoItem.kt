package com.naze.side_o.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.draw.shadow
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.ui.theme.ActionComplete
import com.naze.side_o.ui.theme.ActionCompleteContent
import com.naze.side_o.ui.theme.ActionDelete
import com.naze.side_o.ui.theme.ActionDeleteContent
import com.naze.side_o.ui.theme.Primary
import com.naze.side_o.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeTodoItem(
    todo: TodoEntity,
    viewModel: HomeViewModel,
    allItems: List<TodoEntity>,
    swipeReversed: Boolean = false
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (swipeReversed) viewModel.setCompleted(todo.id, true)
                    else viewModel.markDeleted(todo.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {

                    if (swipeReversed) viewModel.markDeleted(todo.id)
                    else viewModel.setCompleted(todo.id, true)
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

    val cardShape = RoundedCornerShape(24.dp)
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
                        SwipeToDismissBoxValue.EndToStart ->
                            if (swipeReversed) ActionComplete else ActionDelete
                        SwipeToDismissBoxValue.StartToEnd ->
                            if (swipeReversed) ActionDelete else ActionComplete
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
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = ActionCompleteContent
                                    )
                                    Text(
                                        "Complete",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = ActionDeleteContent
                                    )
                                    Text(
                                        "Delete",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                if (swipeReversed) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = ActionDeleteContent
                                    )
                                    Text(
                                        "Delete",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionDeleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = ActionCompleteContent
                                    )
                                    Text(
                                        "Complete",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ActionCompleteContent,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
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
                .shadow(6.dp, cardShape, spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.08f))
                .combinedClickable(
                    onClick = {
                        editTitle = todo.title
                        showEditDialog = true
                    },
                    onLongClick = { showReorderDialog = true }
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.setImportant(todo.id, !todo.isImportant)
                        if (!todo.isImportant && index > 0) {
                            viewModel.reorder(allItems, index, 0)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (todo.isImportant) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (todo.isImportant) "핀 해제" else "핀",
                        tint = if (todo.isImportant) Primary else TextSecondary
                    )
                }
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
