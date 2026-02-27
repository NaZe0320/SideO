# [005] 설정 화면 데이터 초기화 & TODO 제목 길이 제한

- **앱**: com.naze.do_swipe  
- **versionCode**: 3  
- **versionName**: 0.1  
- **대상 화면**: 설정(Settings), 홈(Home) TODO 리스트

---

## 1. 개요

이번 변경에서는 다음 세 가지를 추가/변경했다.

- **설정 화면에서 모든 TODO 데이터를 초기화하는 기능**
- **TODO 제목 길이 60자 제한 (입력/수정/DB 레벨 전체)**
- **설정 화면 하단의 개인정보처리방침/이용약관 링크 + 실제 버전 표기 & “Made by 무제상자” 문구**

---

## 2. 설정 화면 데이터 초기화

### 2.1 UI 구성 (`SettingsScreen.kt`)

- **데이터 섹션 추가**
  - 기존 섹션(제스처 설정, 알림, 디자인) 아래에 `데이터` 섹션을 추가.
  - 카드 안에 `데이터 초기화` 항목을 배치.

```103:233:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt
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
```

- **아이콘/색상**
  - 아이콘: `Icons.Outlined.DeleteForever` 사용 → “영구 삭제” 의미를 명확히.
  - 색상: `iconTint = ActionDelete` 로 다른 파괴적 액션들과 톤을 맞춤.

### 2.2 데이터 초기화 다이얼로그

- `showDataClearDialog` 상태를 두고, true 일 때 `AlertDialog` 표시.
- 내용:
  - 제목: `데이터 초기화`
  - 본문: `모든 할 일 데이터를 삭제할까요? 삭제 후에는 복구할 수 없습니다.`  
    (줄바꿈으로 경고 문구를 강조)

- 버튼:
  - **모두 삭제**: `Button`(FilledButton) 으로 강조.
  - **취소**: `TextButton`.
  - 버튼 텍스트 컬러를 명시해 흐릿하게 보이지 않도록 함.

```86:121:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt
if (showDataClearDialog) {
    AlertDialog(
        onDismissRequest = { showDataClearDialog = false },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "데이터 초기화",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "모든 할 일 데이터를 삭제할까요?\n삭제 후에는 복구할 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.clearAllData()
                    showDataClearDialog = false
                }
            ) {
                Text(
                    text = "모두 삭제",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { showDataClearDialog = false }) {
                Text(
                    text = "취소",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}
```

### 2.3 ViewModel & Repository & Dao 연동

#### SettingsViewModel (`SettingsViewModel.kt`)

- `TodoApplication` 에서 `TodoRepository` 를 주입받아 데이터 초기화 호출을 담당.

```13:35:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsViewModel.kt
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings: SettingsRepository =
        (application as TodoApplication).settingsRepository

    private val repository: TodoRepository =
        (application as TodoApplication).repository

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val swipeReversed: StateFlow<Boolean> = settings.swipeReversed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) { /* ... */ }
    fun setSwipeReversed(reversed: Boolean) { /* ... */ }
    fun setRemindersEnabled(enabled: Boolean) { /* ... */ }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTodos()
        }
    }
}
```

#### TodoDao (`TodoDao.kt`)

- Room DAO 에 전체 삭제 쿼리 추가.

```10:18:app/src/main/java/com/naze/do_swipe/data/local/TodoDao.kt
@Dao
interface TodoDao {
    // ...

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}
```

#### TodoRepository (`TodoRepository.kt`)

- DAO 의 `deleteAll()` 을 감싸는 `clearAllTodos()` 추가.

```1:18:app/src/main/java/com/naze/do_swipe/data/repository/TodoRepository.kt
private const val MAX_TITLE_LENGTH = 60

class TodoRepository(
    private val dao: TodoDao
) {
    // ...

    suspend fun clearAllTodos() {
        dao.deleteAll()
    }
}
```

---

## 3. TODO 제목 60자 길이 제한

### 3.1 UI 입력 제한 – 새 TODO (`HomeScreen.kt`)

- 홈 화면 하단의 새 TODO 입력 필드에서 `onValueChange` 시 길이를 60자로 제한.

```193:233:app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt
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
        OutlinedTextField(
            value = newTitle,
            onValueChange = {
                if (it.length <= 60) {
                    newTitle = it
                }
            },
            // ...
        )
        // + 추가 버튼
    }
}
```

- 사용자는 UI 상에서 60자 이상 입력할 수 없고, 기존의 추가 버튼/Enter 동작은 동일하게 유지된다.

### 3.2 UI 입력 제한 – 수정 다이얼로그 (`HomeTodoItem.kt`)

- TODO 수정 다이얼로그의 `OutlinedTextField` 에도 동일한 60자 제한 적용.

```73:106:app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt
if (showEditDialog) {
    AlertDialog(
        // ...
        text = {
            OutlinedTextField(
                value = editTitle,
                onValueChange = {
                    if (it.length <= 60) {
                        editTitle = it
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(32.dp)
                    ),
                // ...
            )
        },
        // ...
    )
}
```

- 사용자는 다이얼로그에서도 60자 이상 입력할 수 없고, 저장 로직은 기존과 동일하다.

### 3.3 데이터 레이어 제한 – Repository (`TodoRepository.kt`)

#### 공통 상수

- 파일 상단에 제목 최대 길이 상수 정의.

```1:8:app/src/main/java/com/naze/do_swipe/data/repository/TodoRepository.kt
private const val MAX_TITLE_LENGTH = 60

class TodoRepository(
    private val dao: TodoDao
) { /* ... */ }
```

#### addTodo – 새 TODO 저장 시

- 저장하기 전에 공통 정규화 로직을 통해 트림 + 60자 컷.

```17:25:app/src/main/java/com/naze/do_swipe/data/repository/TodoRepository.kt
suspend fun addTodo(title: String, isImportant: Boolean = false) {
    val normalized = title.trim().take(MAX_TITLE_LENGTH)
    if (normalized.isBlank()) return
    val orderIndex = dao.getNextOrderIndex()
    val entity = TodoEntity(
        title = normalized,
        isImportant = isImportant,
        createdAt = System.currentTimeMillis(),
        orderIndex = orderIndex
    )
    dao.insert(entity)
}
```

- 결과:
  - 공백만 입력하거나, 60자 초과로 잘렸더니 비어버린 경우 저장을 막는다.

#### updateTodo – 기존 TODO 수정 시

- 전달된 `TodoEntity` 의 `title` 도 동일하게 정규화 후 저장.

```28:31:app/src/main/java/com/naze/do_swipe/data/repository/TodoRepository.kt
suspend fun updateTodo(entity: TodoEntity) {
    val normalized = entity.title.trim().take(MAX_TITLE_LENGTH)
    if (normalized.isBlank()) return
    dao.update(entity.copy(title = normalized))
}
```

- UI 외 다른 경로(향후 기능 추가 시 등)로 길이가 긴 문자열이 들어와도,
  - DB 에는 항상 60자까지만 저장되도록 보장한다.

---

## 4. 설정 화면 하단 – 약관/개인정보 & 버전/제작자

### 4.1 개인정보처리방침 / 이용약관 텍스트 버튼

- 메인 카드들 아래, 하단 버전 텍스트 위쪽에 **가볍게 노출되는 텍스트 버튼 두 개**를 배치.
  - `개인정보처리방침`
  - `이용약관`
  - 가운데 구분 점(`•`) 표시.

- 클릭 시 외부 브라우저로 지정된 URL 을 연다.

```235:276:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt
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
    // 아래 버전 텍스트
}
```

- URL 상수 (추후 Remote Config 로 교체 가능):

```428:433:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt
private const val PRIVACY_POLICY_URL = "https://example.com/privacy"
private const val TERMS_OF_SERVICE_URL = "https://example.com/terms"
```

### 4.2 실제 버전 + “Made by 무제상자”

- 기존 텍스트:
  - `Version 1.0 • Made with Love`

- 변경 후:
  - 런타임에 `PackageManager` 를 사용해 앱의 실제 `versionName` 을 읽고,
  - `"Version $versionName • Made by 무제상자"` 형태로 표시.

```279:287:app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt
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
```

---

## 5. 테스트 체크리스트

1. **데이터 초기화**
   - 설정 → `데이터` 섹션 → `데이터 초기화` 를 탭.
   - 다이얼로그에서
     - `취소` → 데이터가 그대로 유지되는지.
     - `모두 삭제` → 모든 TODO 가 삭제되고, 홈 화면/위젯에 더 이상 표시되지 않는지.
2. **제목 60자 제한**
   - 새 TODO 입력란에 60자 초과 텍스트를 붙여넣기/입력 시,
     - 60자까지만 들어가는지.
   - TODO 수정 다이얼로그에서도 동일하게 60자까지만 편집 가능한지.
3. **DB 레벨 확인(선택)**
   - 매우 긴 문자열을 강제로 저장 시도했을 때,
     - Room DB 에 실제로 60자까지만 저장되는지.
4. **설정 화면 하단 UI**
   - `개인정보처리방침`, `이용약관` 클릭 시 외부 브라우저가 열린 뒤, 올바른 URL 로 이동하는지.
   - 버전 텍스트에 실제 앱 버전이 표시되고, `"Made by 무제상자"` 문구가 들어가는지.

---

## 6. 관련 파일

- `app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/naze/do_swipe/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/naze/do_swipe/data/local/TodoDao.kt`
- `app/src/main/java/com/naze/do_swipe/data/repository/TodoRepository.kt`
- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`
- `app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt`

---

*마지막 업데이트: 2025년 2월*  
*관련 문서: [002] HomeTodo 드래그 — 근본 원인 수정 (visualItems 재배치),  
           [004] HomeTodo 롱프레스 드래그 & 스와이프 충돌 개선*

