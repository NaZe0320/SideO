package com.naze.side_o.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.naze.side_o.data.local.TodoEntity
import com.naze.side_o.ui.theme.StarOutlineGray
import com.naze.side_o.ui.theme.StarYellow
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
