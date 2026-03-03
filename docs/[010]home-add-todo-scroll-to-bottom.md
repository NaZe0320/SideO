# 할 일 추가 시 스크롤 최하단 이동

할 일(Home) 화면에서 **새 할 일을 추가했을 때** 리스트가 자동으로 최하단(새 아이템 위치)으로 스크롤되도록 하는 기능의 설계와 구현을 정리한 문서입니다.

---

## 1. 개요

- **목적**: 사용자가 하단 입력창에서 할 일을 입력하고 추가 버튼(또는 키보드 Done)을 누르면, 새로 추가된 아이템이 리스트 맨 아래에 나타나므로 **해당 위치로 스크롤**해 새 아이템이 보이도록 함.
- **동작**: `LazyListState`를 사용해 아이템 개수가 **증가했을 때만** 마지막 인덱스로 `animateScrollToItem` 호출. 완료/삭제로 개수가 줄어드는 경우에는 스크롤하지 않음.

---

## 2. 목표 UX

| 상황 | 동작 |
|------|------|
| **할 일 추가** | 리스트가 부드럽게 맨 아래로 스크롤되어 방금 추가한 아이템이 화면에 보임. |
| **할 일 완료/삭제** | 리스트 개수만 줄어들 뿐, 스크롤 위치는 유지. |
| **첫 진입·리스트 비어 있음** | 빈 상태 메시지 표시. 첫 아이템 추가 시 해당 아이템(인덱스 0)으로 스크롤. |

---

## 3. 구조 요약

- **데이터 순서**: `TodoRepository`에서 새 항목은 `getNextOrderIndex()`로 **맨 뒤**(최대 orderIndex + 1)에 추가되므로, UI 리스트에서도 새 아이템은 항상 **마지막 인덱스**에 위치함.
- **스크롤 트리거**: `activeTodos.size`가 **이전보다 커졌을 때만** 스크롤. 이전 개수는 `previousTodoCount`로 기억.
- **구현 위치**: `HomeScreen` 단일 파일 내 상태 + `LaunchedEffect`로 처리. ViewModel/Repository 변경 없음.

---

## 4. 수정된 파일

### 4.1 HomeScreen.kt

**추가 import**

- `androidx.compose.foundation.lazy.rememberLazyListState`

**추가 상태**

- `listState: LazyListState` — `rememberLazyListState()`로 생성. `LazyColumn`에 `state = listState`로 연결.
- `previousTodoCount: Int` — `remember { mutableStateOf(activeTodos.size) }`로 보관. 리스트 크기 변화 시 “이전 개수”와 비교해 **증가 시에만** 스크롤하기 위함.

**스크롤 로직 (LaunchedEffect)**

```kotlin
LaunchedEffect(activeTodos.size) {
    val current = activeTodos.size
    val prev = previousTodoCount
    if (current > prev && current > 0) {
        listState.animateScrollToItem(current - 1)
    }
    previousTodoCount = current
}
```

- `current > prev`: 아이템이 **추가**된 경우에만 스크롤 (완료/삭제 시에는 스크롤 안 함).
- `current > 0`: 빈 리스트가 아닐 때만 스크롤.
- `animateScrollToItem(current - 1)`: 마지막 아이템(새로 추가된 아이템) 인덱스로 애니메이션 스크롤.

**LazyColumn**

- 기존 `LazyColumn(modifier = ..., verticalArrangement = ..., content = { ... })`에 `state = listState` 인자 추가.

---

## 5. 참고 사항

- **displayItems vs activeTodos**: 드래그로 순서 변경 중일 때는 `displayItems`가 `visualItems`(임시 순서)를 쓰지만, **추가 직후**에는 `pendingItems == null`이므로 `displayItems`와 `activeTodos`의 길이·순서가 같고, 새 항목은 항상 `activeTodos.size - 1`에 있음. 따라서 `activeTodos.size - 1`로 스크롤하는 것이 올바름.
- **animateScrollToItem**: `LazyListState`의 메서드로, Jetpack Compose foundation의 `rememberLazyListState()` 사용 시 별도 extension import 없이 사용 가능.

---

## 6. 관련 경로

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt` — `listState`, `previousTodoCount`, `LaunchedEffect(activeTodos.size)`, `LazyColumn(state = listState)`

---

*마지막 업데이트: 2025년 3월*
