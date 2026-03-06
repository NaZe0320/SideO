# Home 스와이프 시 리스트 패딩 경계 잘림 개선

- **앱**: com.naze.do_swipe  
- **versionCode**: 5  
- **versionName**: 0.1.2  
- **관련 화면**: Home (`HomeScreen`, `HomeTodoItem`)  
- **핵심 변경**: Todo 아이템을 좌우 스와이프할 때 리스트 좌우 여백(24dp) 경계에서 먼저 잘려 보이던 현상을 제거하고, 패딩 영역까지 자연스럽게 이동하도록 수정

---

## 1. 문제 요약

- 기존 Home 리스트는 좌우 24dp 여백을 사용하고 있었고, 아이템 스와이프 중 카드가 부모 경계에서 클리핑되어 보였습니다.
- 특히 좌측 스와이프 시, 카드가 화면 밖으로 부드럽게 빠져나가기 전에 `24dp` 안쪽 지점부터 잘리는 느낌이 발생했습니다.

---

## 2. 원인

원인은 두 가지가 겹쳐 있었습니다.

1. 리스트 여백이 컨테이너(`Box`)의 `Modifier.padding(horizontal = 24.dp)`로 적용되어, 실제 스와이프 가능한 시각 영역이 줄어드는 구조였습니다.
2. `HomeTodoItem` 내부 `SwipeToDismissBox`가 기본적으로 경계 클리핑(`clipToBounds`)을 사용해, 스와이프 중 카드가 자신의 레이아웃 박스를 넘어갈 때 잘려 보였습니다.

---

## 3. 해결 방식

### 3.1 HomeScreen: 패딩 적용 위치 변경

- `LazyColumn` 바깥 `Box`의 `.padding(horizontal = 24.dp)` 제거
- 같은 여백을 `LazyColumn`의 `contentPadding = PaddingValues(horizontal = 24.dp)`로 이동

이렇게 하면 리스트는 전체 폭을 유지하면서, 각 아이템 콘텐츠 여백만 24dp로 유지됩니다.

### 3.2 HomeTodoItem: 스와이프 클리핑 해제

- `SwipeToDismissBox`의 `clipToBounds`를 `false`로 변경

```kotlin
SwipeToDismissBox(
    modifier = modifier
        .zIndex(if (isDragging) 1f else 0f)
        .fillMaxWidth(),
    clipToBounds = false,
    thresholdFraction = 0.5f,
    ...
)
```

이 변경으로 스와이프 도중 카드가 부모 경계를 넘어도 잘리지 않고 자연스럽게 이동합니다.

---

## 4. 수정 파일

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`
  - `PaddingValues` import 추가
  - 리스트 컨테이너의 수평 패딩 제거
  - `LazyColumn`에 `contentPadding = PaddingValues(horizontal = 24.dp)` 추가
- `app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt`
  - `SwipeToDismissBox(clipToBounds = false)`로 변경

---

## 5. 검증

- 컴파일 검증: `:app:compileDebugKotlin`
- 결과: **BUILD SUCCESSFUL**

---

## 6. 기대 효과

- 좌우 스와이프 시 카드가 `24dp` 여백 경계에서 먼저 잘려 보이는 부자연스러운 현상 제거
- 기존 레이아웃 여백(24dp)과 아이템 간격은 유지
- 스와이프 애니메이션 체감 품질 개선

---

*마지막 업데이트: 2026-03-06*