# HomeTodo 드래그 순서 변경 기능

할 일(Home) 화면에서 카드 순서를 **롱프레스 후 드래그**로 변경하는 기능의 설계와 구현을 정리한 문서입니다.

---

## 1. 개요

- **이전 방식**: 카드 롱프레스 시 "순서 변경" AlertDialog 표시 → "위로"/"아래로" 버튼으로 한 칸씩 이동.
- **현재 방식**: 드래그 핸들(≡ 아이콘) 롱프레스 후 수직 드래그로 원하는 위치까지 끌어다 놓기. Dialog 제거.

---

## 2. 목표 UX

| 단계 | 동작 |
|------|------|
| **롱프레스 시작** | 선택된 아이템이 살짝 커지고(Scale Up) 그림자가 짙어짐. 주변 아이템은 불투명도 감소(디밍)로 선택 아이템에 집중. |
| **드래그 중** | 아이템을 끌고 이동할 때, 다른 아이템들 사이로 들어가면 그 사이 공간이 부드럽게 벌어지는(Gap Expansion) 애니메이션. |
| **위치 고정** | 손을 떼면 해당 위치로 자석처럼 스냅되고, 커졌던 크기가 원래대로 복귀. |

---

## 3. 구조 요약

- **상태 소유**: 리스트 단위 상태(draggedIndex, dragOffsetY, dropTargetIndex 등)는 **HomeScreen**에서 관리.
- **제스처**: **드래그 핸들**(DragIndicator 아이콘) 영역에만 롱프레스 + 수직 드래그를 붙여, 카드 전체 스와이프(완료/삭제)와 충돌하지 않도록 함.
- **ViewModel**: 기존 `reorder(items, fromIndex, toIndex)` 그대로 사용. 손을 뗄 때 호출.

---

## 4. 수정된 파일

### 4.1 HomeScreen.kt

- **상태**
  - `draggedIndex: Int?` — 현재 드래그 중인 아이템 인덱스.
  - `dragOffsetY: Float` — 누적 수직 드래그 오프셋(px).
  - `itemHeightPx` — 한 아이템 높이 근사치(72.dp → px). 드롭 인덱스 계산용.
  - `dropTargetIndex` — 드래그 중 “놓을 위치” 인덱스. `(draggedIndex + (dragOffsetY / itemHeightPx).roundToInt()).coerceIn(0, lastIndex)` 로 계산.
- **리스트**
  - `items` → `itemsIndexed` 로 변경.
  - 각 아이템에 `index`, `isDragging`, `isDimmed`, `dragOffsetYPx`, `onDragStart` / `onDragMove` / `onDragEnd`, `topGapDp` 전달.
- **갭**
  - `dropTargetIndex == index` 인 아이템에만 `topGapDp = 32.dp`, 나머지는 `0.dp`.
- **onDragEnd**
  - 손을 뗀 시점의 `dragOffsetY`로 `toIndex` 계산 후 `viewModel.reorder(activeTodos, from, to)` 호출.
  - 이후 `draggedIndex`, `dragOffsetY` 초기화.

### 4.2 HomeTodoItem.kt

- **제거**
  - 순서 변경용 `showReorderDialog` 및 AlertDialog("순서 변경", 위로/아래로 버튼).
  - 카드 `combinedClickable`의 `onLongClick`.
- **추가 파라미터**
  - `index`, `isDragging`, `isDimmed`, `dragOffsetYPx`, `onDragStart`, `onDragMove`, `onDragEnd`, `topGapDp`.
- **드래그 핸들**
  - `Icons.Filled.DragIndicator` 에 `pointerInput` + `detectDragGesturesAfterLongPress` 적용. 롱프레스 후 `onDragStart`, 드래그 시 `onDragMove(delta.y)`, 손 뗄 때 `onDragEnd`.
- **시각**
  - `graphicsLayer`: `isDragging` 이면 scale 1.05f, `isDimmed` 이면 alpha 0.6f, **드래그 중일 때만** `translationY = dragOffsetYPx` 로 손가락을 따라 이동.
  - Card `elevation`: 드래그 중 8.dp, 평소 0.dp.
  - `animateDpAsState(topGapDp)`, `animateFloatAsState` 로 갭·스케일·알파 애니메이션.
- **z-order**
  - `Modifier.zIndex(if (isDragging) 1f else 0f)` 로 드래그 중인 카드가 다른 아이템 위에 그려지도록 함.
- **배경**
  - `backgroundContent`(스와이프 시 보이는 회색 배경)에 `Modifier.alpha(if (isDragging) 0f else 1f)` 적용. 드래그 시 원래 자리에 남던 회색이 보이지 않음.
- **SwipeToDismissBox**
  - `clipToBounds = !isDragging` 전달. 드래그 중에는 영역 밖으로 나가도 카드가 잘리지 않음.

### 4.3 SwipeToDismissByPosition.kt (SwipeToDismissBox)

- **파라미터 추가**
  - `clipToBounds: Boolean = true`
- **동작**
  - `clipToBounds == true` 일 때만 외부 Box에 `Modifier.clipToBounds()` 적용.
  - HomeTodoItem 에서는 드래그 시 `clipToBounds = false` 로 전달. Archive/Trash 등 다른 사용처는 기본값으로 기존처럼 클리핑 유지.

---

## 5. 적용했던 수정 사항 (이슈 대응)

1. **카드가 손가락을 따라오지 않음**  
   HomeScreen에서 `dragOffsetYPx` 를 드래그 중인 아이템에만 전달하고, HomeTodoItem의 Card에 `graphicsLayer { translationY = dragOffsetYPx }` 적용.

2. **순서가 실제로 변경되지 않음**  
   `onDragEnd` 에서 `dropTargetIndex` 를 composition 시점이 아닌 **콜백 실행 시점의** `dragOffsetY` 로 다시 계산해 `toIndex` 를 구한 뒤 `viewModel.reorder(activeTodos, from, to)` 호출.

3. **드래그한 카드가 다른 아이템 뒤로 가려짐**  
   드래그 중인 아이템에 `Modifier.zIndex(1f)` 적용해 맨 앞에 그리도록 함.

4. **드래그 시 원래 자리에 회색 배경이 남음**  
   `backgroundContent` 에 `alpha(if (isDragging) 0f else 1f)` 적용해 드래그 중에는 회색을 숨김.

5. **카드가 자기 영역을 벗어나면 잘려서 안 보임**  
   SwipeToDismissBox에 `clipToBounds` 파라미터 추가. HomeTodoItem에서 `clipToBounds = !isDragging` 으로 전달해 드래그 중에는 클리핑 비활성화.

---

## 6. 관련 경로

- `app/src/main/java/com/naze/side_o/ui/home/HomeScreen.kt`
- `app/src/main/java/com/naze/side_o/ui/home/HomeTodoItem.kt`
- `app/src/main/java/com/naze/side_o/ui/home/HomeViewModel.kt` — `reorder(items, fromIndex, toIndex)` 유지
- `app/src/main/java/com/naze/side_o/ui/components/SwipeToDismissByPosition.kt` — `SwipeToDismissBox`, `clipToBounds` 추가

---

*마지막 업데이트: 2025년 2월*
