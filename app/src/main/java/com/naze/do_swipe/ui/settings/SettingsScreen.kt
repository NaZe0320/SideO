package com.naze.do_swipe.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Tune
import com.naze.do_swipe.ui.components.ConfirmDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.naze.do_swipe.data.preferences.ThemeMode
import com.naze.do_swipe.ui.theme.ActionComplete
import com.naze.do_swipe.ui.theme.ActionDelete
import com.naze.do_swipe.ui.theme.Primary
import com.naze.do_swipe.ui.components.AppTopBarSub
import com.naze.do_swipe.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val swipeReversed by viewModel.swipeReversed.collectAsState()
    val swipeBackgroundBlendEnabled by viewModel.swipeBackgroundBlendEnabled.collectAsState()
    val swipeThresholdFraction by viewModel.swipeThresholdFraction.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDataClearDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setRemindersEnabled(true)
    }

    fun requestReminderEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> viewModel.setRemindersEnabled(true)
                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setRemindersEnabled(true)
        }
    }

    if (showThemeDialog) {
        ThemeSelectDialog(
            currentMode = themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDataClearDialog) {
        ConfirmDialog(
            title = "데이터 초기화",
            message = "모든 할 일 데이터를 삭제할까요?\n삭제 후에는 복구할 수 없습니다.",
            confirmText = "모두 삭제",
            dismissText = "취소",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllData()
                showDataClearDialog = false
            },
            onDismiss = { showDataClearDialog = false }
        )
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m ->
                viewModel.setReminderTime(h, m)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBarSub(
                title = "설정",
                onBackClick = onNavigateBack
            )
        }
    ) { contentPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            SectionLabel(text = "제스처 설정")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.SwapHoriz,
                    iconTint = Primary,
                    title = "스와이프 방향 반전",
                    subtitle = if (swipeReversed) "왼쪽: 삭제 / 오른쪽: 완료" else "왼쪽: 완료 / 오른쪽: 삭제",
                    checked = swipeReversed,
                    onCheckedChange = { viewModel.setSwipeReversed(it) }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    icon = Icons.Outlined.SwapHoriz,
                    iconTint = Primary,
                    title = "스와이프 시 배경색 블렌딩",
                    subtitle = "임계점에 가까울수록 카드 색이 삭제/완료 색으로 변해요",
                    checked = swipeBackgroundBlendEnabled,
                    onCheckedChange = { viewModel.setSwipeBackgroundBlendEnabled(it) }
                )
                HorizontalDivider()
                SettingsSliderItem(
                    icon = Icons.Outlined.Tune,
                    iconTint = Primary,
                    title = "스와이프 확정 거리",
                    subtitle = "낮음: 살짝만 밀어도 완료/삭제 · 높음: 더 밀어야 확정 (현재 ${(swipeThresholdFraction * 100).toInt()}%)",
                    value = swipeThresholdFraction,
                    valueRange = 0.1f..0.9f,
                    steps = 7,
                    onValueChange = { viewModel.setSwipeThresholdFraction((it * 10).roundToInt() / 10f) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(text = "알림")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Notifications,
                        iconTint = Primary,
                        title = "알림",
                        subtitle = if (remindersEnabled) {
                            String.format("매일 %02d:%02d에 미완료 할 일을 알려줘요", reminderHour, reminderMinute)
                        } else {
                            "알림을 끄면 미완료 할 일 알림이 오지 않아요"
                        },
                        checked = remindersEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) requestReminderEnable()
                            else viewModel.setRemindersEnabled(false)
                        }
                    )
                    HorizontalDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.Schedule,
                        iconTint = if (remindersEnabled) Primary else TextSecondary,
                        title = "알림 시간",
                        subtitle = if (remindersEnabled) {
                            String.format("매일 %02d:%02d에 알림", reminderHour, reminderMinute)
                        } else {
                            "알림을 켜면 시간을 설정할 수 있어요"
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        onClick = {
                            if (remindersEnabled) {
                                showTimePickerDialog = true
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(text = "디자인")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsRowItem(
                    icon = Icons.Outlined.DarkMode,
                    iconTint = Primary,
                    title = "테마",
                    subtitle = when (themeMode) {
                        ThemeMode.SYSTEM -> "시스템 설정 따름"
                        ThemeMode.LIGHT -> "라이트"
                        ThemeMode.DARK -> "다크"
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(text = "데이터")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsRowItem(
                    icon = Icons.Outlined.DeleteForever,
                    iconTint = ActionDelete,
                    title = "데이터 초기화",
                    subtitle = "모든 할 일 데이터를 영구 삭제",
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    onClick = { showDataClearDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(PRIVACY_POLICY_URL)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = "개인정보처리방침",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(TERMS_OF_SERVICE_URL)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = "이용약관",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    null
                } ?: ""
                Text(
                    text = "Version $versionName • Made by 무제상자",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (iconTint == MaterialTheme.colorScheme.primary || iconTint == Primary || iconTint == ActionDelete || iconTint == ActionComplete)
                        iconTint.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (iconTint == Primary || iconTint == ActionComplete || iconTint == ActionDelete)
                        iconTint.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (iconTint == Primary || iconTint == ActionComplete || iconTint == ActionDelete)
                        iconTint.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.padding(top = 8.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        thumbTrackGapSize = 0.dp,
                        drawStopIndicator = null
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private const val PRIVACY_POLICY_URL = "https://do-swipe.web.app/privacy.html"
private const val TERMS_OF_SERVICE_URL = "https://do-swipe.web.app/terms.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("알림 시간", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            TimeInput(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ThemeSelectDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "테마 선택",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "시스템 설정 따름"
                                ThemeMode.LIGHT -> "라이트"
                                ThemeMode.DARK -> "다크"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mode == currentMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
