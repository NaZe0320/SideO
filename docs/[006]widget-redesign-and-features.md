# [006] 홈 위젯 리디자인 & 기능 개선

- **앱**: com.naze.do_swipe  
- **versionCode**: 3  
- **versionName**: 0.1  
- **대상 기능**: 홈 화면 TODO 위젯(2x2 / 3x2 / 4x2)

---

## 1. 개요

기존 홈 위젯은:

- 고정 5개 슬롯(`widget_todo_item_0` ~ `4`)에 텍스트만 뿌리는 단순 레이아웃
- 스크롤 불가, 헤더/더보기/`+` 버튼 없음

이라는 한계가 있었다.  

이번 변경의 목표는 다음과 같다.

- 제공된 디자인 시안처럼 **카드형 UI**와 **헤더 / 리스트 / 더보기 / `+` 버튼** 구조로 재구성
- 위젯에서 **할 일 완료 처리** 가능
- **스크롤로 최대 8개**까지 TODO 노출
- `Do! Swipe` 영역(헤더) 또는 **하단 더보기**를 누르면 앱의 홈 화면으로 이동
- `+` 버튼을 누르면 앱이 열리면서 **Home 입력란에 포커스 + 키보드 자동 표시**

---

## 2. 최종 UI 구조

### 2.1 레이아웃 개요

공통 레이아웃: `app/src/main/res/layout/widget_todo_list.xml`

- 루트: `FrameLayout` (배경 색상 + 패딩)
- 카드 컨테이너: `LinearLayout(@id/widget_card, background=@drawable/widget_card_background)`
  - **헤더 영역**: `LinearLayout(@id/widget_header_container)`
    - 앱 이름: `TextView(@id/widget_title)` → `"Do!Swipe"`
    - 선택적 뱃지: `TextView(@id/widget_header_badge)` → 현재는 `GONE` (향후 "4 tasks today" 등 표시 용)
    - `+` 버튼: `ImageButton(@id/widget_add_button)`
  - **리스트 영역**: `ListView(@id/widget_list)`
    - 위젯 아이템은 별도 레이아웃 `widget_todo_list_item.xml` 사용
  - **빈 상태 텍스트**: `TextView(@id/widget_empty_text)`  
    - TODO가 없을 때만 `VISIBLE`
  - **하단 더보기 영역**: `LinearLayout(@id/widget_more_container)`
    - 텍스트: `TextView(@id/widget_more_text)` → `"더보기"`

아이템 레이아웃: `app/src/main/res/layout/widget_todo_list_item.xml`

- 루트: `LinearLayout(@id/widget_item_root)`
- 체크 아이콘: `ImageView(@id/widget_item_check_icon)`
- 제목: `TextView(@id/widget_item_title)`
  - `TodoEntity.isImportant == true` 인 경우 `"⭐ "` prefix 붙여서 표시

카드 배경: `app/src/main/res/drawable/widget_card_background.xml`

- 흰색 배경 + 16dp 라운드 코너 + 내부 패딩

### 2.2 위젯 메타

모든 위젯은 동일한 레이아웃을 사용하고, 크기만 다르게 지정한다.

- `app/src/main/res/xml/app_widget_info_small.xml` → 2x2
- `app_widget_info.xml` → 기본 사이즈
- `app_widget_info_medium.xml` → 4x2

공통 속성:

- `android:initialLayout="@layout/widget_todo_list"`
- `android:resizeMode="horizontal|vertical"`
- `android:widgetCategory="home_screen"`

실제 렌더링은 안드로이드 런처가 위젯 크기에 따라 동일 레이아웃을 스케일링하는 방식이다.

---

## 3. 데이터 & 스크롤 구조

### 3.1 RemoteViewsService / RemoteViewsFactory 도입

파일:

- `app/src/main/java/com/naze/do_swipe/widget/TodoWidgetService.kt`

구성:

- `TodoWidgetService : RemoteViewsService`
  - `onGetViewFactory()`에서 `TodoWidgetFactory` 반환
- `TodoWidgetFactory : RemoteViewsService.RemoteViewsFactory`
  - 내부 상태: `private var items: List<TodoEntity> = emptyList()`
  - `onDataSetChanged()`에서 `TodoApplication.repository.getActiveTodos()`를 통해 TODO 목록 로드
    - `MAX_ITEMS = 8` 상수 기반으로 `take(MAX_ITEMS)`만 유지
  - `getCount()` = `items.size`
  - `getViewAt(position)`:
    - 범위 검사(`position !in items.indices` → `null` 반환)
    - `RemoteViews(context.packageName, R.layout.widget_todo_list_item)` 생성
    - 제목 텍스트/중요도 반영
    - `fillInIntent`에 `TodoAppWidgetProvider.EXTRA_TODO_ID`를 넣어 클릭 시 TODO ID 전달

이 구조 덕분에:

- 위젯 리스트는 **스크롤 가능한 컬렉션(ListView)** 구조가 되고,
- 최대 8개까지만 항목이 채워지며,
- 더 많은 TODO는 앱 내 전체 목록에서 확인하도록 유도한다.

### 3.2 AppWidgetProvider에서의 연결

파일:

- `app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt`

핵심 메서드:

```kotlin:app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt
internal fun buildRemoteViews(
    context: Context,
    todos: List<TodoEntity>,
    appWidgetId: Int,
    layoutId: Int
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_todo_list)

    val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.widget_list, serviceIntent)
    views.setEmptyView(R.id.widget_list, R.id.widget_empty_text)

    // 완료 처리용 PendingIntent 템플릿
    val toggleIntent = Intent(context, TodoAppWidgetProvider::class.java).apply {
        action = ACTION_TOGGLE_TODO
    }
    val togglePendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        toggleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setPendingIntentTemplate(R.id.widget_list, togglePendingIntent)

    // 헤더 / 더보기 → 앱 홈 이동
    val mainIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val mainPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_header_container, mainPendingIntent)
    views.setOnClickPendingIntent(R.id.widget_more_container, mainPendingIntent)

    // + 버튼 → 앱 홈 + 입력창 포커스
    val addIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_ADD_FROM_WIDGET, true)
    }
    val addPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId + 1000,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

    return views
}
```

포인트:

- `setRemoteAdapter`로 위젯 리스트와 `TodoWidgetService`를 연결
- `setPendingIntentTemplate` + 아이템별 `setOnClickFillInIntent` 조합으로  
  **각 아이템 클릭 시 TODO ID를 브로드캐스트로 전달**
- 헤더/더보기/`+` 버튼 클릭 시 `MainActivity`로 진입하는 인텐트 구성

---

## 4. 위젯에서 할 일 완료 처리

### 4.1 동작 개요

1. 사용자가 위젯 리스트의 TODO 아이템을 탭
2. `TodoWidgetFactory.getViewAt()`에서 설정한 `fillInIntent`를 통해 TODO ID가 전달
3. `TodoAppWidgetProvider`가 `ACTION_TOGGLE_TODO` 브로드캐스트를 수신
4. Repository를 통해 해당 TODO를 완료 처리
5. `updateAllWidgets(context)` 호출로 모든 위젯 인스턴스가 갱신

관련 코드:

```kotlin:app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_TOGGLE_TODO) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId != -1L) {
            val application = context.applicationContext as TodoApplication
            application.applicationScope.launch {
                try {
                    application.repository.setCompleted(todoId, true)
                    updateAllWidgets(context)
                } catch (e: Exception) { }
            }
        }
    } else {
        super.onReceive(context, intent)
    }
}
```

---

## 5. 앱 이동 & `+` 버튼 동작

### 5.1 MainActivity 인텐트 플래그

위젯에서 앱으로 진입 시 다음 두 가지 케이스를 처리한다.

- **헤더 / 더보기 클릭**: 단순히 홈 화면으로 이동
- **`+` 버튼 클릭**: 홈 화면으로 이동하면서 **입력창 포커스 + 키보드 표시**

위젯 쪽 플래그:

- `TodoAppWidgetProvider.EXTRA_OPEN_ADD_FROM_WIDGET` (Boolean)
  - `+` 버튼 인텐트에만 `true`로 세팅

`MainActivity`:

```kotlin:app/src/main/java/com/naze/do_swipe/MainActivity.kt
val openAddFromWidget = intent?.getBooleanExtra(
    TodoAppWidgetProvider.EXTRA_OPEN_ADD_FROM_WIDGET,
    false
) ?: false

setContent {
    // ...
    DoSwipeTheme(themeMode = themeMode) {
        NavGraph(openAddFromWidget = openAddFromWidget)
    }
}
```

`NavGraph`:

```kotlin:app/src/main/java/com/naze/do_swipe/navigation/NavGraph.kt
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    openAddFromWidget: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            // ...
            HomeScreen(
                viewModel = viewModel,
                onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                openAddOnStart = openAddFromWidget
            )
        }
        // ...
    }
}
```

### 5.2 HomeScreen에서 입력 포커스 & 키보드 표시

파일:

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`

핵심 부분:

```kotlin:app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt
val focusRequester = remember { FocusRequester() }
val keyboardController = LocalSoftwareKeyboardController.current

LaunchedEffect(openAddOnStart) {
    if (openAddOnStart) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
```

입력 필드:

```kotlin:app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt
OutlinedTextField(
    value = newTitle,
    onValueChange = { /* ... */ },
    modifier = Modifier
        .weight(1f)
        .focusRequester(focusRequester),
    // ...
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
```

결과적으로:

- 위젯의 `+` 버튼을 누르면,
  - `MainActivity` → `NavGraph` → `HomeScreen(openAddOnStart = true)` 경로로 진입
  - 진입 직후 입력 필드에 포커스 & 키보드 자동 표시
- 위젯 헤더/더보기로 진입한 경우에는 `openAddOnStart = false` 이므로 기존과 동일하게 동작

---

## 6. 제약 사항 및 고려 사항

- 안드로이드 홈 위젯 특성상 **위젯 내부에서 직접 키보드를 띄워 텍스트를 입력**하는 것은 불가능하다.
  - 따라서 `+` 버튼은 **앱을 열고, 앱 내 입력 UX로 자연스럽게 이어지도록** 설계했다.
- 리스트는 최대 8개까지만 표시하며, 더 많은 TODO는 앱 내 전체 리스트에서 확인하도록 유도한다.
- 위젯에서 완료 처리만 지원하고, 편집/삭제/정렬 등 고급 동작은 앱 내 Home 화면에서 수행한다.

---

## 7. 테스트 시나리오

1. **위젯 최초 추가**
   - 2x2 / 3x2 / 4x2 크기로 각각 위젯을 추가했을 때 레이아웃이 깨지지 않는지.
2. **TODO 개수별 UI**
   - 0개: `widget_empty_text`가 보이고 리스트/더보기 영역이 자연스러운지.
   - 1~8개: 스크롤 유무와 관계없이 항목이 모두 표현되는지.
   - 9개 이상: 8개까지만 보이고 나머지는 앱 내에서 확인 가능한지.
3. **완료 처리**
   - 각 리스트 아이템을 탭했을 때 완료 처리되고, 모든 위젯 인스턴스가 자동 갱신되는지.
4. **앱 이동**
   - 헤더(`Do!Swipe`) 또는 하단 `더보기`를 탭했을 때 앱 홈 화면으로 이동하는지.
5. **`+` 버튼 동작**
   - `+` 버튼 탭 시 앱이 열리면서 Home 화면 하단 입력란에 포커스가 가고, 키보드가 바로 뜨는지.
   - 앱이 이미 실행 중인 상태에서도 동일하게 동작하는지.

---

## 8. 관련 파일

- `app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt`
- `app/src/main/java/com/naze/do_swipe/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo_list.xml`
- `app/src/main/res/layout/widget_todo_list_item.xml`
- `app/src/main/java/com/naze/do_swipe/MainActivity.kt`
- `app/src/main/java/com/naze/do_swipe/navigation/NavGraph.kt`
- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`

---

*마지막 업데이트: 2026년 2월*  

