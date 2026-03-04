# 홈 드래그 드롭 낙관적 업데이트 & animateItem 적용

- **앱**: com.naze.do_swipe  
- **versionCode**: 5  
- **versionName**: 0.1.2  
- **대상 기능**: 홈(Home) 화면의 **롱프레스 드래그 재정렬** 후 발생하던 **Recomposition Flicker(튀는 현상)** 제거를 위한 **낙관적 업데이트** 적용과, 리스트 아이템 순서 변경 시 **animateItem** 애니메이션을 모든 아이템에 적용한 내용을 정리한 문서입니다.

---

## 1. 문제 상황 정리

- **증상**: 드래그를 끝내고 손을 떼는(onDragEnd) 순간, **자기 자리에 다시 두는 게 아닌 이상 무조건** 아이템이 원래 자리로 순간이동했다가, 곧이어 새 위치로 다시 이동하는 "튀는" 현상이 발생했습니다.
- **원인**:
  1. `onDragEnd`에서 `viewModel.reorder(...)`를 호출해 DB에 순서 변경을 요청한 뒤,
  2. `draggedItemId`, `dropTargetIndex` 등 드래그 상태를 **즉시 null**로 초기화합니다.
  3. 드래그 상태가 풀리면 UI는 **원본 리스트(activeTodos)**를 기준으로 다시 그리려 합니다.
  4. 그런데 ViewModel이 DB를 업데이트하고 새로운 순서의 `activeTodos`를 Flow로 내려보내기까지 **아주 짧은 지연**이 있습니다.
  5. 그 찰나 동안 **이전 순서의 activeTodos**가 그려지면서 아이템이 원래 자리로 "튀어 오른" 것처럼 보이고, DB 업데이트가 완료되면 새 순서로 다시 그려져 두 번 움직이는 것처럼 보였습니다.
- **원래 자리에 두면 안 튀는 이유**: 순서 변경이 없으므로 `reorder`가 호출되지 않고, `activeTodos`도 그대로라서 한 번만 그려지기 때문입니다.

---

## 2. 해결 방향: 낙관적 업데이트

- DB 업데이트가 완료되어 새 리스트가 내려올 때까지, UI가 **드롭한 위치의 리스트 상태**를 **임시로 기억**하도록 합니다.
- 드롭 직후에는 이 **임시 리스트**를 화면에 표시하고, `activeTodos`가 갱신되면 임시 리스트를 해제한 뒤 갱신된 데이터를 사용합니다.
- 이를 위해 `HomeScreen`에 **임시 상태 변수** `optimisticallyReorderedList`를 두고, 다음 네 가지를 수정했습니다.

---

## 3. 수정 사항 상세

### 3.1 임시 상태 변수 추가

- **위치**: `HomeScreen` 상단 변수 선언부, `autoScrollPerFramePx` 바로 아래.
- **추가 코드**:

```kotlin
// 드롭 후 DB 업데이트 딜레이 동안 튀는 현상을 막기 위한 임시 리스트
var optimisticallyReorderedList by remember { mutableStateOf<List<TodoEntity>?>(null) }
```

- 드롭 직후에만 값이 설정되고, `activeTodos`가 갱신되는 시점에 `null`로 해제됩니다.

### 3.2 displayItems 계산 로직 수정

- **의존성**: `remember(activeTodos, draggedFromIndex, dropTargetIndex)`에 `optimisticallyReorderedList`를 추가.
- **else 분기**: 드래그 중이 아닐 때, **임시 리스트가 있으면 임시 리스트를 먼저 표시**하고, 없으면 `activeTodos`를 표시하도록 변경.

```kotlin
val displayItems: List<TodoEntity> = remember(activeTodos, draggedFromIndex, dropTargetIndex, optimisticallyReorderedList) {
    val from = draggedFromIndex
    val to = dropTargetIndex
    if (from != null && to != null && from != to && activeTodos.isNotEmpty()) {
        activeTodos.toMutableList().apply { add(to, removeAt(from)) }
    } else {
        optimisticallyReorderedList ?: activeTodos
    }
}
```

- 드롭 직후 `draggedFromIndex`/`dropTargetIndex`가 null이 되더라도, `optimisticallyReorderedList`에 방금 놓은 순서가 들어 있으므로 화면이 "원래 순서"로 되돌아가지 않고 부드럽게 유지됩니다.

### 3.3 LaunchedEffect 수정 — activeTodos 갱신 시 임시 리스트 해제

- **키 변경**: `LaunchedEffect(activeTodos.size)` → `LaunchedEffect(activeTodos)`  
  - `.size`만 보면 개수만 바뀔 때만 실행되므로, **순서만 바뀐 경우**를 놓칩니다. `activeTodos` 전체를 관찰해 DB에서 새 리스트가 올 때마다 실행되도록 합니다.
- **블록 맨 앞 추가**: `optimisticallyReorderedList = null`  
  - DB 업데이트가 완료되어 새 데이터가 오면 임시 리스트를 해제하고, 이후에는 `displayItems`가 `activeTodos`만 사용합니다.

```kotlin
LaunchedEffect(activeTodos) {
    optimisticallyReorderedList = null
    val current = activeTodos.size
    val prev = previousTodoCount
    if (current > prev && current > 0) {
        listState.animateScrollToItem(current - 1)
    }
    previousTodoCount = current
}
```

### 3.4 onDragEnd 로직 수정 — 드롭 순간 임시 리스트 저장

- **순서**: `viewModel.reorder(...)` 호출 **전에**, 방금 놓은 순서를 `optimisticallyReorderedList`에 저장합니다.
- **저장 내용**: `activeTodos.toMutableList().apply { add(to, removeAt(from)) }` — 드롭된 결과와 동일한 리스트입니다.

```kotlin
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
```

- 이렇게 하면 드래그 상태를 null로 초기화한 직후에도, `displayItems`는 `optimisticallyReorderedList`를 사용해 "드롭한 위치"를 그대로 유지하고, DB에서 새 `activeTodos`가 올 때까지 튀지 않습니다.

---

## 4. animateItem() 적용 변경

- **위치**: `LazyColumn` 내부 `itemsIndexed` 블록의 `HomeTodoItem`에 전달하는 `modifier`.
- **기존**: 드래그 소스 아이템(`isSourceItem`)이 아닐 때만 `Modifier.animateItem()`을 적용하고, 소스 아이템에는 적용하지 않았습니다.
- **변경**: 조건문을 제거하고 **모든 아이템**에 `Modifier.animateItem()`을 적용합니다.

```kotlin
HomeTodoItem(
    modifier = Modifier
        .animateItem()
        .alpha(sourceAlpha),
    todo = todo,
    // ...
)
```

- 순서가 바뀔 때 드래그한 아이템을 포함한 전체 리스트가 `animateItem()`으로 부드럽게 재배치되도록 합니다.

---

## 5. 동작 흐름 요약

1. 사용자가 드롭(onDragEnd) → `optimisticallyReorderedList`에 재정렬된 리스트 저장 → `viewModel.reorder(...)` 호출 → 드래그 상태 null로 초기화.
2. Recomposition 시 `displayItems`는 `optimisticallyReorderedList`를 사용 → **드롭한 위치 그대로 표시**(튀지 않음).
3. ViewModel이 DB를 업데이트하고 Flow로 새 `activeTodos`를 내려줌.
4. `LaunchedEffect(activeTodos)` 실행 → `optimisticallyReorderedList = null` → `displayItems`가 `activeTodos`만 사용 → 화면이 DB와 일치한 상태로 유지.

---

## 6. 참고 사항

- **calculateTargetIndex**의 가장자리 처리(`localPointerY <= first.offset` → 0, `localPointerY >= last.offset + last.size` → `items.lastIndex`)는 화면 양끝으로 밀어낼 때의 튀는 현상을 잡는 데 필요하므로, 이번 수정과 별개로 **그대로 유지**했습니다.

---

## 7. 수정된 파일 요약

- **app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt**
  - `optimisticallyReorderedList` 상태 추가.
  - `displayItems`의 `remember` 의존성 및 else 분기 수정.
  - `LaunchedEffect(activeTodos.size)` → `LaunchedEffect(activeTodos)` 및 `optimisticallyReorderedList = null` 추가.
  - `onDragEnd`에서 `optimisticallyReorderedList` 저장 후 `viewModel.reorder` 호출.
  - `HomeTodoItem`의 `modifier`를 `Modifier.animateItem().alpha(sourceAlpha)`로 통일.

---

## 8. 관련 경로

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`

---

*마지막 업데이트: 2026년 3월*
