# [004] HomeTodo 롱프레스 드래그 & 스와이프 충돌 개선

- **앱**: com.naze.do_swipe  
- **versionCode**: 3  
- **versionName**: 0.1  
- **대상 화면**: HomeTodo 리스트 (할 일(Home) 화면)

---

## 1. 개요

Home 화면의 TODO 아이템은

- 좌/우 스와이프 → 완료/삭제 (`SwipeToDismissBox`)
- 위/아래 드래그 → 순서 변경(정렬)

을 동시에 지원한다.  
기존에는 **카드 내부 왼쪽의 드래그 핸들 아이콘에서 바로 드래그 제스처를 잡는 방식**이어서,

- 핸들 근처에서 **스와이프를 하려고 했는데 드래그가 먼저 잡히는** UX 문제가 있었다.

이 문서는 **드래그 시작 방식을 롱프레스 기반으로 바꾸고**,  
최종적으로 **“아이템 전체 롱프레스 → 드래그”** 모델로 정리한 과정을 기록한다.

---

## 2. 변경 전 문제

### 2.1 즉시 드래그 제스처의 문제

초기 구현에서는 `HomeTodoItem` 안의 **드래그 핸들 아이콘**에 직접 `pointerInput` + `detectDragGestures`를 붙였다.

- 사용자가 핸들 부근을 스와이프하려고 할 때,
  - `SwipeToDismissBox` 보다 **핸들의 드래그 제스처가 먼저 터치 이벤트를 소비**.
  - 결과적으로 **스와이프가 잘 안 먹히는 느낌**을 줌.

UX 관점에서:

- “왼쪽으로 스와이프해서 삭제/완료”라는 기본 제스처가,
- 단지 핸들 영역에 있다는 이유로 **예상과 다르게 동작**하는 문제가 있었다.

---

## 3. 단계적 개선 방향

### 3.1 롱프레스 기반 드래그로 변경

먼저, 드래그를 **바로 시작하지 않고 “길게 누른 후에만” 시작**하도록 변경했다.

- 제스처 API를 `detectDragGestures` → `detectDragGesturesAfterLongPress` 로 교체.
- 롱프레스 이전의 **짧은 스와이프 제스처는 상위 `SwipeToDismissBox`가 처리**하도록 유도.

이렇게 하면:

- “살짝 스와이프”는 **완료/삭제**로,
- “길게 누른 뒤 위/아래로 이동”은 **정렬 드래그**로,

더 명확하게 분리할 수 있다.

### 3.2 최종 결정: 아이템 전체 롱프레스 드래그

핸들에만 롱프레스를 붙이는 대신,  
최종적으로는 **카드 전체에 롱프레스 드래그를 붙이고, 핸들은 시각적 힌트만 담당**하도록 바꾸었다.

장점:

- 사용자는 “이 아이템을 옮기고 싶다” → **아이템 아무 곳이나 길게 누르고 드래그**라는,  
  더 자연스러운 멘탈 모델을 사용할 수 있다.
- 핸들은 **“이 아이템이 드래그 가능하다”는 affordance 역할**만 한다.
- 드래그 제스처가 아이템 전체에 균일하게 적용되므로, 터치 정밀도가 낮은 상황에서도 사용성이 좋다.

---

## 4. 최종 구현 내용

### 4.1 제스처 import

`HomeTodoItem.kt` 상단:

```kotlin
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
```

기존 `detectDragGestures` 대신 `detectDragGesturesAfterLongPress`를 사용한다.

### 4.2 Card 레벨 롱프레스 드래그

`HomeTodoItem` 내부의 카드에 **드래그 제스처를 부여**했다.

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            alpha = alpha
        )
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { currentOnDragStart() },
                onDrag = { _, delta -> currentOnDragMove(delta.y) },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() }
            )
        }
        .combinedClickable(
            onClick = {
                editTitle = todo.title
                showEditDialog = true
            }
        ),
    // ...
)
```

핵심:

- **롱프레스 인식 후**에만 `onDragStart`가 호출됨.
- 이후 위/아래 이동 시 `onDragMove(delta.y)` 로 리스트 재정렬 로직이 동작.
- `onDragEnd` / `onDragCancel`에서 드래그 상태를 정리.
- `combinedClickable`는 그대로 유지해 **탭 → 편집 다이얼로그** UX를 보존.

### 4.3 드래그 핸들 아이콘은 시각적 힌트만

이제 핸들 아이콘은 드래그 제스처를 직접 가지지 않는다.

```kotlin
Icon(
    imageVector = Icons.Filled.DragIndicator,
    contentDescription = "이동 가능",
    tint = TextSecondary,
    modifier = Modifier
        .padding(end = 16.dp)
)
```

- 사용자는 핸들을 보고 **“이 아이템은 이동 가능하다”**는 힌트를 얻는다.
- 실제 드래그는 **카드 어디서든 롱프레스 후 위/아래 이동**으로 시작된다.

---

## 5. UX 플로우 요약

1. **스와이프**
   - 카드 영역(핸들 포함)에서 **짧게 좌/우로 스와이프**:
     - `SwipeToDismissBox`가 제스처를 처리.
     - 방향 및 설정에 따라 완료/삭제 동작.
2. **드래그(정렬)**
   - 카드 아무 곳이나(핸들 포함) **길게 누른 상태에서 위/아래로 이동**:
     - `detectDragGesturesAfterLongPress`가 드래그를 시작.
     - 외부에서 주입된 `onDragStart` / `onDragMove` / `onDragEnd` 콜백을 통해  
       `HomeScreen` 쪽의 visualItems 재배치 로직이 동작.

결과적으로,

- **스와이프**와 **드래그**가 시간 축(롱프레스 vs 짧은 터치) 기준으로 분리되어
  - 스와이프 제스처가 “잡아먹히는” 문제를 크게 줄였다.

---

## 6. 테스트 시나리오

다음 케이스를 실제 기기/에뮬레이터에서 확인한다.

1. **카드 본문에서 좌/우 스와이프**
   - 완료/삭제 스와이프가 기존처럼 자연스럽게 동작하는지.
2. **드래그 핸들 근처에서 짧게 스와이프**
   - 드래그 대신 **스와이프 동작이 우선** 동작하는지.
3. **카드 아무 곳이나 길게 누른 뒤 위/아래로 이동**
   - 리스트 정렬 드래그가 자연스럽게 시작/종료되는지.
4. **빠르게 연속 동작**
   - 스와이프 후 곧바로 다른 아이템을 롱프레스 드래그해도 이상한 상태가 남지 않는지.

---

## 7. 관련 파일

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt`
- (드래그 정렬 로직) `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`  
  - visualItems 재배치 및 `onDragStart` / `onDragMove` / `onDragEnd` 처리 (참고: [002] 문서)

---

*마지막 업데이트: 2025년 2월*  
*관련 문서: [002] HomeTodo 드래그 — 근본 원인 수정 (visualItems 재배치)*,  
*           [003] Android 15 Edge-to-Edge & 더 넓은 화면 대응 정리*

