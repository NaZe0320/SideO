# 홈 드래그 재정렬 & 고스트 아키텍처

- **앱**: com.naze.do_swipe  
- **versionCode**: 4  
- **versionName**: 0.1.1  
- **대상 기능**: 홈(Home) 화면의 **롱프레스 드래그 재정렬**에서 발생하던 제스처 유실·스크롤 충돌 문제를 해결하기 위해 적용한 **부모 제스처 + 고스트(Ghost) 오버레이 아키텍처**의 설계와 구현 정리 문서입니다.

---

## 1. 문제 상황 정리

- **문제 1: 리스트 최상단 아이템이 화면 상단에 고정되는 현상**
  - 최상단 `TodoItem`을 롱프레스해서 아래로 드래그하면, **리스트는 스크롤되는데 아이템 자체는 화면 최상단에 고정**되어 보이는 문제가 있었습니다.
  - 스크롤이 더 내려가도, 고정되어 있는 아이템과 실제 리스트 위치가 점점 어긋나 UX가 매우 어색해졌습니다.

- **문제 2: 리스트 영역(AppBar/입력란) 밖으로 나가면 제스처를 놓치는 현상**
  - 기존 구현에서는 각 `HomeTodoItem`이 카드 내부 `pointerInput + detectDragGesturesAfterLongPress`로 **자기 자신만** 드래그를 감지하고 있었습니다.
  - 손가락이 카드 바깥, 특히 **AppBar 영역이나 하단 입력란 영역으로 이동**하면 Compose가 “해당 컴포저블 영역을 벗어났다”고 판단하여 **드래그가 끊기고 아이템을 놓치는** 문제가 발생했습니다.

- **문제 3: 드래그 vs 스크롤 제스처 충돌**
  - 세로 드래그로 재정렬하려고 할 때, `LazyColumn`의 기본 스크롤 제스처와 경쟁하면서
    - 어떤 때는 **드래그보다 스크롤이 먼저 먹히고**,  
    - 어떤 때는 **스크롤이 멈추고 드래그만 인식되는**  
    불안정한 상태가 있었습니다.

- **문제 4: 고스트가 사라지거나, 아래쪽으로 튀어 보이는 현상 (고스트 가시성 이슈)**
  - 드래그 중 고스트(Ghost) 아이템이
    - 리스트/다른 UI 뒤에 가려져 “사라진 것처럼” 보이거나,
    - 생성 직후 **손가락보다 아래쪽에서 갑자기 나타났다 올라오거나**,  
    - 지속적으로 실제 손가락 위치보다 **조금 아래쪽에 따라오는** 문제가 있었습니다.
  - 이는 **레이어(zIndex/Popup)**와 **좌표계(root vs container)**를 혼용한 것이 주요 원인이었습니다.

---

## 2. 설계 원칙

1. **제스처 책임을 부모(Box)로 일원화**
   - 개별 `HomeTodoItem`이 아니라, **리스트 전체를 감싸는 상위 Box**가 드래그 제스처를 소유하도록 변경했습니다.
   - 상위 Box가 화면 전체를 덮도록 구성하면, 손가락이 AppBar나 하단 입력란까지 이동해도 **드래그 세션이 끊기지 않습니다.**

2. **드래그 중에는 기본 스크롤을 끄고, 코드로만 스크롤**
   - 드래그가 시작되면 `LazyColumn.userScrollEnabled = false`로 설정해 **사용자 스크롤 제스처를 잠시 비활성화**합니다.
   - 대신 포인터 위치가 리스트 상·하단 임계영역에 들어오면 `listState.scrollBy(...)`를 통해 **자동 스크롤만 허용**합니다.

3. **고스트(Ghost) 오버레이 + 원본 슬롯 비우기**
   - 리스트 본문에서는 **드래그 중인 원본 아이템을 투명(alpha 0) 또는 약간 디밍**하여 “자리가 비어 있는 것처럼” 보이게 합니다.
   - 실제로 손가락을 따라다니는 것은 **리스트 밖 최상단 레이어(Popup)에 렌더링되는 고스트 아이템**입니다.

4. **좌표계는 한 기준으로만 생각하기**
   - 포인터 좌표, 리스트 경계, 고스트 위치를 계산할 때 **같은 기준(루트 또는 컨테이너)**만 사용하도록 통일합니다.
   - 이 구현에서는 최종적으로 **루트 기준 Y(= 윈도우 기준) 하나**를 사용하고, Popup도 같은 기준으로 배치합니다.

---

## 3. 구현 구조 개요

- **제스처 소유자**
  - `HomeScreen`의 `Scaffold` content 내부 최상위 `Box`에 `pointerInput + detectDragGesturesAfterLongPress`를 붙였습니다.
  - 이 Box는 `innerPadding`까지 포함한 **화면 전체 영역**을 덮습니다.

- **상태 관리 (`HomeScreen`)**
  - `draggedItemId: Long?` — 현재 드래그 중인 아이템 id
  - `draggedFromIndex: Int?` — 드래그 시작 시 리스트 인덱스
  - `dropTargetIndex: Int?` — 현재 손가락 위치 기준 재배치될 타겟 인덱스
  - `fingerYInRoot: Float` — 손가락의 루트 기준 Y 좌표
  - `touchOffsetInItemPx: Float` — 아이템 안에서 손가락이 위치한 상대 Y 오프셋
  - `ghostTopInRoot: Float` — 고스트 상단의 루트 기준 Y 좌표
  - `autoScrollPerFramePx: Float` — 자동 스크롤 속도(프레임당 px)
  - `dragStartContainerTopInRoot: Float` — 드래그 시작 시 컨테이너 top (Popup offset 기준 보정용)

- **리스트 재배치 뷰 상태**
  - `displayItems`는 `activeTodos`와 `draggedFromIndex/dropTargetIndex`를 기반으로 **임시 순서를 반영한 리스트**입니다.
  - UI에서는 항상 `displayItems`를 그려서, **드래그 중에도 재정렬 결과가 실시간으로 보이게** 합니다.

- **고스트 렌더링**
  - 리스트 내부: 원본 아이템은 `todo.id == draggedItemId`일 때 alpha를 0(`draggedItem != null`인 경우)으로 설정해 **빈 슬롯처럼** 보이게 합니다.
  - 리스트 외부 최상단: `Popup(alignment = Alignment.TopStart, offset = IntOffset(0, ...))` 안에서 `HomeTodoItem`을 다시 렌더링하여 고스트를 표시합니다.

---

## 4. 주요 구현 흐름

### 4.1 드래그 시작 (`onDragStart`)

1. **루트 기준 포인터 Y 계산**
   - `pointerYInRoot = containerTopInRoot + offset.y`
2. **리스트 내에서 어떤 아이템을 잡았는지 탐색**
   - `List<LazyListItemInfo>` (`listState.layoutInfo.visibleItemsInfo`) 중  
     `localY`가 `info.offset ..< info.offset + info.size` 안에 들어가는 아이템을 찾습니다.
3. **상태 초기화**
   - `dragStartContainerTopInRoot = containerTopInRoot`
   - `fingerYInRoot = pointerYInRoot`
   - `touchOffsetInItemPx = (localY - hit.offset)`  
   - `ghostTopInRoot = pointerYInRoot - touchOffsetInItemPx`
   - `draggedFromIndex = hit.index`, `dropTargetIndex = null`, `draggedItemId = picked.id`

### 4.2 드래그 중 (`onDrag`)

1. **포인터 이동량 반영**
   - `fingerYInRoot += dragAmount.y`
   - `ghostTopInRoot = fingerYInRoot - touchOffsetInItemPx`
2. **자동 스크롤 속도 계산**
   - 포인터가 리스트 상·하단에서 `edgePx`(예: 96dp) 안쪽으로 들어오면  
     거리 비율에 따라 `autoScrollPerFramePx`를 `-max..+max` 사이로 설정합니다.
3. **목표 인덱스 계산**
   - `localPointerY = fingerYInRoot - listBoundsInRoot.top`
   - `visibleItemsInfo`를 순회해 어느 아이템 영역에 있는지 찾은 후, 그 인덱스를 `dropTargetIndex`로 설정합니다.
   - `draggedFromIndex`와 같으면 `null`로 두어 “변화 없음”으로 취급합니다.

### 4.3 자동 스크롤 루프 (`LaunchedEffect(draggedItemId, autoScrollPerFramePx)`)

- 드래그 중(`draggedItemId != null`)이면서 `autoScrollPerFramePx != 0f`일 때,  
  `while` 루프로 `listState.scrollBy(autoScrollPerFramePx)`를 호출합니다.
- 스크롤 결과가 0이면(리스트 끝) 자동 스크롤을 멈춥니다.
- 각 프레임(약 16ms)마다 다시 `calculateTargetIndex(...)`를 호출해 타겟 인덱스를 업데이트합니다.

### 4.4 드래그 종료/취소 (`onDragEnd` / `onDragCancel`)

- `onDragEnd`:
  - `from = draggedFromIndex`, `to = dropTargetIndex`가 모두 유효하고 서로 다를 때만  
    `viewModel.reorder(activeTodos, from, to)`를 호출해 실제 순서를 반영합니다.
  - 이후 모든 드래그 관련 상태(id, index, autoScroll)를 초기화합니다.
- `onDragCancel`:
  - 뷰모델 호출 없이 상태만 초기화합니다.

---

## 5. 고스트 레이어 & 좌표계 이슈와 해결 과정

### 5.1 리스트 뒤로 가려져 보이던 문제

- **문제**: 고스트를 `Box(zIndex=...)`로만 렌더링했을 때, 실제 디바이스에서 리스트나 다른 레이아웃에 가려져 사라져 보이는 케이스가 있었습니다.
- **해결**:
  - 고스트를 `Popup`으로 렌더링해 **윈도우 최상단 레이어**에 그리도록 변경했습니다.
  - `Popup(alignment = Alignment.TopStart, ...)`와 함께 `enableInteractions = false`로 설정해 오직 시각적인 고스트 역할만 수행하게 했습니다.

### 5.2 생성 시 아래쪽으로 튀었다가 올라오는 점프

- **문제**:
  - 고스트 생성 직후 첫 프레임에서, 손가락보다 아래쪽에서 갑자기 나타났다 제 위치로 올라오는 **강한 점프** 현상이 있었습니다.
  - 원인: `draggedItemId`를 먼저 세팅하고, `ghostTopInRoot`/`containerTopInRoot`가 아직 최신값이 아닌 시점에서 Popup이 그려졌기 때문입니다.
- **해결**:
  - 드래그 시작 시 **좌표 관련 상태(fingerYInRoot, touchOffsetInItemPx, ghostTopInRoot)**를 먼저 계산하고,  
    마지막에 `draggedItemId`를 세팅하도록 **순서를 변경**했습니다.

### 5.3 고스트가 전체적으로 아래로 치우쳐 있는 문제

- **문제**:
  - 점프는 사라졌지만, 드래그 내내 고스트가 실제 손가락 위치보다 **눈에 띄게 아래쪽**에 따라다니는 현상이 있었습니다.
  - 원인: `Popup` offset 계산에서 `containerTopInRoot`를 잘못 보정하거나, 컨테이너 기준과 루트 기준을 섞어 사용한 것.
- **해결**:
  - 드래그 시작 시점의 `containerTopInRoot`를 `dragStartContainerTopInRoot`에 고정 저장.
  - Popup offset을  
    `y = (ghostTopInRoot - dragStartContainerTopInRoot).roundToInt()`  
    로 계산해, **드래그 세션 내내 같은 기준축**(시작 시점 컨테이너 top 기준)을 사용하도록 고정했습니다.

---

## 6. 수정된 파일 요약

### 6.1 `HomeScreen.kt`

- **추가/변경된 주요 상태**
  - `draggedItemId`, `draggedFromIndex`, `dropTargetIndex`
  - `fingerYInRoot`, `touchOffsetInItemPx`, `ghostTopInRoot`
  - `autoScrollPerFramePx`, `dragStartContainerTopInRoot`
- **부모 제스처**
  - `Box(...).pointerInput(Unit) { detectDragGesturesAfterLongPress(...) }`
- **스크롤 잠금**
  - `LazyColumn(state = listState, userScrollEnabled = draggedItemId == null, ...)`
- **자동 스크롤**
  - `LaunchedEffect(draggedItemId, autoScrollPerFramePx) { while(...) listState.scrollBy(...) }`
- **고스트 Popup**
  - `Popup(alignment = Alignment.TopStart, offset = IntOffset(0, (ghostTopInRoot - dragStartContainerTopInRoot).roundToInt())) { HomeTodoItem(..., isDragging = true, enableInteractions = false) }`

### 6.2 `HomeTodoItem.kt`

- **역할 단순화**
  - 세로 드래그 관련 `pointerInput` 및 콜백(`onDragStart/onDragMove/onDragEnd`) 제거.
  - 아이템은 **표시/탭 편집/좌우 스와이프**만 담당.
- **고스트/원본 공통 UI**
  - `enableInteractions` 플래그를 도입하여
    - 리스트 내 원본: `enableInteractions = draggedItemId == null` (드래그 중에는 편집/스와이프 비활성)
    - 고스트: `enableInteractions = false` (순수 시각용)

---

## 7. 기대 UX 정리

| 상황 | 기대 동작 |
|------|-----------|
| 최상단 아이템 롱프레스 후 아래로 드래그 | 아이템이 손가락과 함께 자연스럽게 이동하고, 리스트가 하단으로 자동 스크롤됨. 더 이상 화면 최상단에 고정되지 않음. |
| 드래그 도중 손가락이 AppBar/입력란까지 이동 | 드래그 세션이 유지되고, 고스트가 그대로 손가락을 따라옴. 아이템을 놓치지 않음. |
| 리스트 중간에서 롱프레스 후 위/아래로 크게 드래그 | 위/아래 엣지에 닿으면 자동 스크롤이 작동하고, 드랍 위치에 맞게 순서가 재배치됨. |
| 드래그 중 시각 피드백 | 리스트에서는 원본 슬롯이 비어 보이고(또는 살짝 디밍), 화면 상단 레이어에서 고스트 카드가 손가락 근처에 떠 있음. |

---

## 8. 관련 경로

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`
- `app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt`

---

*마지막 업데이트: 2026년 3월*
