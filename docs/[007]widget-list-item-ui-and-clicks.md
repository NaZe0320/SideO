# [007] 위젯 리스트 아이템 UI 및 클릭 동작 정리

- **앱**: com.naze.do_swipe  
- **versionCode**: 4  
- **versionName**: 0.1.1
- **대상 기능**: 홈 위젯 리스트 아이템(체크박스 디자인, 터치 영역, 클릭 시 동작)

---

## 1. 개요

위젯 리스트에서 다음을 적용했다.

- **체크박스**: 커스텀 drawable 사용, 약간 둥근 사각형 형태로 통일
- **일반 아이템 터치**: 빈 공간/텍스트 터치는 무시, **체크박스만** 탭 시 할 일 완료 처리
- **더보기 아이템**: 탭 시 앱 홈 화면으로 이동
- **Android 12+**: 컬렉션 위젯에서 아이템별 데이터(todoId)가 정상 전달되도록 PendingIntent 플래그 수정

---

## 2. 체크박스 디자인

### 2.1 커스텀 drawable

**파일**: `app/src/main/res/drawable/widget_checkbox_off.xml`

- `shape="rectangle"`, `corners radius="4dp"` 로 약간 둥근 사각형
- `stroke` 1.5dp, 색상 `#9EA1B1`
- `solid` 투명

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <stroke android:width="1.5dp" android:color="#9EA1B1" />
    <solid android:color="@android:color/transparent" />
    <corners android:radius="4dp" />
</shape>
```

### 2.2 적용 위치

- **리스트 아이템**: `app/src/main/res/layout/widget_todo_list_item.xml`  
  - `ImageView(@id/widget_item_check_icon)` → `android:src="@drawable/widget_checkbox_off"`
- **소형/중형 위젯**: `widget_todo_small.xml`, `widget_todo_medium.xml`  
  - 각 체크 버튼 `android:src="@drawable/widget_checkbox_off"`

기존 `@android:drawable/checkbox_off_background` 대신 위 drawable을 사용해 위젯 전반에서 동일한 체크박스 모양을 유지한다.

---

## 3. 리스트 아이템 레이아웃 (widget_todo_list_item.xml)

### 3.1 구조 및 치수

- **루트**: `LinearLayout(@id/widget_item_root)`
  - `android:clickable="false"`, `android:focusable="false"`  
    → 리스트 아이템 전체에 대한 기본 탭 피드백/포커스 억제
- **체크박스**: `ImageView(@id/widget_item_check_icon)`
  - 크기 **16dp × 16dp** (기존 20dp에서 4dp 축소)
  - `android:src="@drawable/widget_checkbox_off"`
- **제목**: `TextView(@id/widget_item_title)`
  - 체크박스와 간격 **8dp** (`android:layout_marginStart="8dp"`)

### 3.2 요약

| 항목           | 값 |
|----------------|----|
| 체크박스 크기  | 16dp × 16dp |
| 체크박스–텍스트 간격 | 8dp |
| 루트 clickable/focusable | false |

---

## 4. 클릭 동작 설계

### 4.1 역할 구분

| 영역 | 뷰 ID | 동작 |
|------|--------|------|
| 일반 아이템 **루트**(빈 공간·텍스트) | `widget_item_root` | 아무 동작 없음 |
| 일반 아이템 **체크박스** | `widget_item_check_icon` | 해당 할 일 완료 처리 |
| **더보기** 아이템 | `widget_item_root` | 앱 홈 화면으로 이동 |

### 4.2 상수 (TodoAppWidgetProvider)

- `EXTRA_TODO_ID`: `"extra_todo_id"` — 브로드캐스트에 담는 할 일 ID
- `EXTRA_TODO_ID_IGNORE`: `-2L` — “무시” 의미, 앱 실행/완료 처리 모두 하지 않음
- 더보기 아이템은 `EXTRA_TODO_ID = -1L` 로 전달하며, Provider에서 “앱 열기”로 해석

---

## 5. TodoWidgetService — FillInIntent 설정

**파일**: `app/src/main/java/com/naze/do_swipe/widget/TodoWidgetService.kt`

### 5.1 더보기 아이템 (position == items.size)

- `R.layout.widget_todo_list_item_more` 사용
- `widget_item_root`에만 FillInIntent 설정
- `EXTRA_TODO_ID = -1L` → 앱 홈으로 이동

### 5.2 일반 리스트 아이템 (0 ≤ position < items.size)

- `R.layout.widget_todo_list_item` 사용
- **루트** `widget_item_root`:  
  `EXTRA_TODO_ID = EXTRA_TODO_ID_IGNORE` (-2L) → 아무 동작 없음
- **체크박스** `widget_item_check_icon`:  
  `EXTRA_TODO_ID = todo.id` → 해당 할 일 완료 처리

```kotlin
// 루트(빈 공간) 클릭 시 무시
val emptyIntent = Intent().apply {
    putExtra(TodoAppWidgetProvider.EXTRA_TODO_ID, TodoAppWidgetProvider.EXTRA_TODO_ID_IGNORE)
}
views.setOnClickFillInIntent(R.id.widget_item_root, emptyIntent)

// 체크박스 클릭 시 완료 처리
val checkIntent = Intent().apply {
    putExtra(TodoAppWidgetProvider.EXTRA_TODO_ID, todo.id)
}
views.setOnClickFillInIntent(R.id.widget_item_check_icon, checkIntent)
```

---

## 6. TodoAppWidgetProvider — PendingIntent 및 onReceive

### 6.1 리스트 템플릿 PendingIntent (FLAG_MUTABLE)

**파일**: `app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt`

컬렉션 뷰(ListView)에서 **아이템별 FillInIntent 데이터가 템플릿에 합쳐지려면** Android 12 이상에서 **FLAG_MUTABLE** 이 필요하다.  
FLAG_IMMUTABLE이면 extras가 전달되지 않아 `EXTRA_TODO_ID`가 항상 기본값(-1L)으로 인식되고, 의도치 않게 앱만 열리는 현상이 발생할 수 있다.

```kotlin
val togglePendingIntent = PendingIntent.getBroadcast(
    context,
    appWidgetId,
    toggleIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
)
views.setPendingIntentTemplate(R.id.widget_list, togglePendingIntent)
```

### 6.2 onReceive — EXTRA_TODO_ID 분기

- `EXTRA_TODO_ID == EXTRA_TODO_ID_IGNORE` (-2L)  
  → **즉시 return** (아무 동작 없음)
- `EXTRA_TODO_ID == -1L`  
  → **MainActivity 실행** (더보기 클릭)
- 그 외 (유효한 todo.id)  
  → **해당 할 일 완료** 후 `updateAllWidgets(context)` 호출

동일한 분기 로직을 **TodoAppWidgetProvider**, **TodoAppWidgetProviderSmall**, **TodoAppWidgetProviderMedium** 의 `onReceive`에 모두 적용했다.

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_TOGGLE_TODO) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId == EXTRA_TODO_ID_IGNORE) return
        if (todoId == -1L) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            return
        }
        // 완료 처리 및 위젯 갱신
        // ...
    } else {
        super.onReceive(context, intent)
    }
}
```

---

## 7. 관련 파일

| 구분 | 경로 |
|------|------|
| Drawable | `app/src/main/res/drawable/widget_checkbox_off.xml` |
| 리스트 아이템 레이아웃 | `app/src/main/res/layout/widget_todo_list_item.xml` |
| 더보기 아이템 레이아웃 | `app/src/main/res/layout/widget_todo_list_item_more.xml` |
| 소형/중형 위젯 레이아웃 | `widget_todo_small.xml`, `widget_todo_medium.xml` |
| Provider | `app/src/main/java/com/naze/do_swipe/widget/TodoAppWidgetProvider.kt` |
| RemoteViewsFactory | `app/src/main/java/com/naze/do_swipe/widget/TodoWidgetService.kt` |

---

## 8. 참고 — [006]과의 관계

- [006] 위젯 리디자인에서 리스트 구조, RemoteViewsService, setPendingIntentTemplate/setOnClickFillInIntent 기본 구조를 정리했다.
- 본 문서([007])는 그 위에 **체크박스 UI 통일**, **일반 아이템은 체크박스만 완료·나머지 터치 무시**, **FLAG_MUTABLE로 Android 12+ 클릭 데이터 전달**을 반영한 변경을 문서화한 것이다.

---

*마지막 업데이트: 2026년 3월*
