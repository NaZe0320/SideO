# HomeTodo 드래그 — 근본 원인 수정 (visualItems 재배치)

할 일(Home) 화면 드래그 앤 드롭에서 발견된 **근본 원인**을 정리하고, **visualItems 재배치 방식**과 **rememberUpdatedState** 적용으로 수정한 내용을 기록한 문서입니다.

---

## 1. 개요

- **이전 방식**: `translationY`로 드래그 중 시각만 이동 + `topGapDp`로 드롭 위치에 32dp 갭 추가.  
  → 드롭 위치 아래 모든 아이템이 연쇄로 밀리는 문제 발생.
- **현재 방식**: 드래그 중 **리스트를 실시간 재정렬(visualItems)** 해서 LazyColumn에 전달.  
  → 갭/translationY 없이 위치만 바뀌어 연쇄 밀림 제거.  
  추가로 **rememberUpdatedState**로 pointerInput 콜백의 stale closure 방지.

---

## 2. 근본 원인 분석

### 2.1 값 누적 / Stale closure

- `pointerInput(Unit)` 안의 콜백이 **초기 컴포지션 시점의 람다를 고정 캡처**함.
- `rememberUpdatedState` 없이 사용하면 **stale closure**로 인해 `onDragEnd` 등이 이전 상태로 실행될 수 있음.

### 2.2 연쇄 밀림 (설계 문제)

- **기존**: A(0)는 `translationY`로만 시각 이동(레이아웃 공간은 그대로), B(1)에 `topGapDp = 32.dp` 추가.
- **결과**: B뿐 아니라 C, D까지 모두 32dp 아래로 밀림.
- **원인**: topGapDp + translationY 방식 자체가 “한 칸에만 갭”이 아니라 “그 아래 전부 밀기”가 됨.

---

## 3. 해결책: visualItems 재배치

드래그 중에 **아이템 리스트를 실시간으로 재정렬**해 LazyColumn에 넘기는 방식.

- 예: A(0), B(1), C(2)에서 A를 index=1로 드래그 시  
  **visualItems** → `[B, A(드래그중), C]`  
  → B가 0번, A가 1번 위치에 자연스럽게 배치, C는 그대로 → **연쇄 밀림 없음**.

---

## 4. 수정된 파일

### 4.1 HomeScreen.kt

- **dropTargetIndex**
  - `draggedIndex?.let { d -> ... }` 형태로 정리.
  - `raw == d`이면 `null` (자기 자신은 드롭 타깃 아님).
- **visualItems**
  - `remember(activeTodos, draggedIndex, dropTargetIndex)`로 계산.
  - `draggedIndex`, `dropTargetIndex`가 모두 non-null일 때만  
    `activeTodos.toMutableList().apply { add(t, removeAt(d)) }` 로 재배치.
  - 그 외에는 `activeTodos` 그대로.
- **LazyColumn items**
  - `items` → **visualItems** 사용.
  - **isDragging**: `activeTodos` 기준 id 비교  
    `todo.id == activeTodos.getOrNull(draggedIndex ?: -1)?.id`.
  - **onDragStart**: `draggedIndex = activeTodos.indexOfFirst { it.id == todo.id }`, `dragOffsetY = 0f`.
  - **onDragEnd**: `from = draggedIndex`, `to = dropTargetIndex`로 null 체크 후  
    `from != to`일 때만 `viewModel.reorder(activeTodos, from, to)` 호출.  
    항상 `draggedIndex = null`, `dragOffsetY = 0f`로 리셋 (return@ 제거).
- **제거**
  - `topGapDp`, `dragOffsetYPx` 인자 및 관련 로직.
- **animateItem**
  - 비드래그 아이템에만 `Modifier.animateItem()` 적용 후  
    `HomeTodoItem(modifier = if (!isDragging) Modifier.animateItem() else Modifier, ...)` 로 전달.

### 4.2 HomeTodoItem.kt

- **시그니처**
  - **추가**: `modifier: Modifier = Modifier`.
  - **제거**: `dragOffsetYPx`, `topGapDp`.
- **SwipeToDismissBox**
  - `modifier`를 인자로 받아 사용. `padding(top = animatedGapDp)` 제거.
- **rememberUpdatedState**
  - `currentOnDragStart`, `currentOnDragMove`, `currentOnDragEnd`로  
    `onDragStart` / `onDragMove` / `onDragEnd`를 항상 최신으로 참조.
- **pointerInput**
  - `detectDragGestures`에서 위 세 콜백 사용 (stale closure 방지).
- **Card graphicsLayer**
  - **translationY** 제거. 위치는 visualItems 재배치로만 결정.
- **제거**
  - `animatedGapDp`(animateDpAsState(topGapDp)), 관련 import(Dp, animateDpAsState).

---

## 5. 변경 요약표

| 변경 항목 | 이전 | 이후 | 효과 |
|-----------|------|------|------|
| LazyColumn 데이터 | activeTodos | visualItems | 연쇄 밀림 제거 |
| 드래그 시각 | translationY | 재배치 위치에 렌더 | 자연스러운 UX |
| topGapDp | 32dp 갭 추가 | 제거 | 연쇄 밀림 제거 |
| pointerInput 콜백 | 고정 캡처 | rememberUpdatedState | 값 누적/stale 방지 |
| onDragEnd | return@ 포함 | null 체크 후 리셋 | 리셋 보장 |
| 비드래그 아이템 | — | Modifier.animateItem() | 위치 이동 애니메이션 |

---

## 6. 관련 경로

- `app/src/main/java/com/naze/side_o/ui/home/HomeScreen.kt`
- `app/src/main/java/com/naze/side_o/ui/home/HomeTodoItem.kt`

---

*마지막 업데이트: 2025년 2월*
