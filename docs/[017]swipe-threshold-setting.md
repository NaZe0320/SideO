# [017] 스와이프 확정 거리(임계점) 설정

- **앱**: com.naze.do_swipe
- **관련 화면**: 설정(SettingsScreen), 홈(HomeScreen), 아카이브(ArchiveScreen)
- **관련 컴포넌트**: HomeTodoItem, ArchiveItem, TrashItem, SwipeToDismissBox
- **핵심 변경**: 설정에서 스와이프 확정에 필요한 거리(임계점)를 0.1~0.9(10%~90%) 구간에서 10% 단위로 조절할 수 있도록 추가. 변경 시 즉시 반영되도록 pointerInput 키 및 초기값 보정 적용.

---

## 1. 목표

스와이프 시 "얼마나 밀어야 완료/삭제로 확정할지"를 사용자가 설정에서 조절할 수 있게 한다.

- **임계점**: `SwipeToDismissBox`의 `thresholdFraction`(기본 0.5 = 50%). 이 비율만큼 밀면 스와이프 확정.
- **범위**: 0.1 ~ 0.9 (10% ~ 90%), 10% 단위(0.1, 0.2, …, 0.9)로만 저장·표시.
- **적용 위치**: 홈(HomeTodoItem), 아카이브(ArchiveItem), 휴지통(TrashItem)의 스와이프 모두 동일 임계점 사용.

---

## 2. 구현 방식

### 2.1 설정 저장소

**파일**: `app/src/main/java/com/naze/do_swipe/data/preferences/SettingsRepository.kt`

- **키**: `KEY_SWIPE_THRESHOLD` (`"swipe_threshold"`)
- **상태**: `_swipeThresholdFraction: MutableStateFlow<Float>`, 기본값 `prefs.getFloat(KEY_SWIPE_THRESHOLD, 0.5f)`
- **저장**: `setSwipeThresholdFraction(value: Float)`  
  - `value.coerceIn(0.1f, 0.9f)` 후 `(value * 10).roundToInt() / 10f` 로 10% 단위로 반올림해 저장.
- **동기 읽기**: `getSwipeThresholdFraction(): Float` — `collectAsState(initial = ...)` 에서 플로우 첫 emit 전에도 저장된 값을 쓰기 위함.

### 2.2 ViewModel

**파일**: `app/src/main/java/com/naze/do_swipe/ui/settings/SettingsViewModel.kt`

- `swipeThresholdFraction: StateFlow<Float>` — `settings.swipeThresholdFraction.stateIn(..., 0.5f)`
- `setSwipeThresholdFraction(value: Float)` — `settings.setSwipeThresholdFraction(value)` 호출

### 2.3 설정 화면 UI

**파일**: `app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt`

- **위치**: 제스처 설정 카드 내부, "스와이프 시 배경색 블렌딩" 스위치 아래, `HorizontalDivider` 다음.
- **제목**: "스와이프 확정 거리"
- **부제**: "낮음: 살짝만 밀어도 완료/삭제 · 높음: 더 밀어야 확정 (현재 N%)"
- **Slider**
  - `valueRange = 0.1f..0.9f`, `steps = 7` (0.1 단위 9개 구간).
  - `onValueChange`: `(it * 10).roundToInt() / 10f` 로 10% 단위 스냅 후 ViewModel에 전달.
- **Thumb(움직이는 요소)**
  - 커스텀 thumb 사용: `thumb = { Box(Modifier.size(20.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }` — **primary 색 채운 원**.
  - `SliderDefaults.colors(thumbColor = Color.Transparent, ...)` 로 기본 thumb는 그리지 않아, 트랙/눈금이 가려지지 않도록 함.
  - **참고**: thumb 배경을 투명하게 해 트랙이 비치게 하려는 시도는 했으나, Material Slider 구조상 원하는 대로 동작하지 않아 **투명화는 미적용**. 현재는 채운 원만 표시.

### 2.4 아이템 컴포저블에 임계점 전달

| 파일 | 변경 |
|------|------|
| `ui/home/HomeTodoItem.kt` | 시그니처에 `thresholdFraction: Float = 0.5f` 추가. `SwipeToDismissBox(thresholdFraction = thresholdFraction, ...)` |
| `ui/archive/ArchiveItem.kt` | 동일 |
| `ui/trash/TrashItem.kt` | 동일 |

### 2.5 화면에서 설정 구독 후 전달

| 파일 | 변경 |
|------|------|
| `ui/home/HomeScreen.kt` | `swipeThresholdFromPrefs by app.settingsRepository.swipeThresholdFraction.collectAsState(initial = app.settingsRepository.getSwipeThresholdFraction())`. 리스트 아이템·드래그 팝업 두 곳의 `HomeTodoItem`에 `thresholdFraction = swipeThresholdFromPrefs` 전달. |
| `ui/archive/ArchiveScreen.kt` | `swipeThresholdFraction by ... collectAsState(initial = app.settingsRepository.getSwipeThresholdFraction())`. `ArchiveItem`·`TrashItem` 호출부에 `thresholdFraction = swipeThresholdFraction` 전달. |

### 2.6 임계점 즉시 반영 버그 수정

**파일**: `app/src/main/java/com/naze/do_swipe/ui/components/SwipeToDismissByPosition.kt`

- **원인**: `pointerInput(Unit)` 사용 시 제스처 블록이 한 번만 시작되고, 설정에서 임계점을 바꿔도 블록이 재시작하지 않아 예전 값(예: 0.5f)이 클로저에 남음. 그래서 "첫 스와이프만 0.5", "새로 만든 아이템도 0.5"처럼 보이는 현상 발생.
- **수정**: `pointerInput(Unit)` → `pointerInput((thresholdFraction * 10).roundToInt())` 로 변경. Int 키(1~9)로 두어 임계점 변경 시 제스처 블록이 확실히 재시작되도록 함.
- **import**: `kotlin.math.roundToInt` 추가.

---

## 3. 수정·연동 파일 목록

| 파일 | 변경 내용 |
|------|------------|
| `data/preferences/SettingsRepository.kt` | KEY_SWIPE_THRESHOLD, swipeThresholdFraction StateFlow, setSwipeThresholdFraction(10% 단위 반올림), getSwipeThresholdFraction() |
| `ui/settings/SettingsViewModel.kt` | swipeThresholdFraction StateFlow, setSwipeThresholdFraction() |
| `ui/settings/SettingsScreen.kt` | 제스처 설정 카드에 스와이프 확정 거리 항목(SettingsSliderItem), 10% 스냅, 커스텀 thumb(채운 원), 설명 문구 |
| `ui/components/SwipeToDismissByPosition.kt` | pointerInput 키를 (thresholdFraction*10).roundToInt() 로 변경 |
| `ui/home/HomeTodoItem.kt` | thresholdFraction 파라미터, SwipeToDismissBox에 전달 |
| `ui/archive/ArchiveItem.kt` | 동일 |
| `ui/trash/TrashItem.kt` | 동일 |
| `ui/home/HomeScreen.kt` | getSwipeThresholdFraction() 초기값으로 구독, HomeTodoItem 두 곳에 thresholdFraction 전달 |
| `ui/archive/ArchiveScreen.kt` | getSwipeThresholdFraction() 초기값으로 구독, ArchiveItem/TrashItem에 thresholdFraction 전달 |

---

## 4. 데이터 흐름 요약

```
SettingsRepository (SharedPreferences, getSwipeThresholdFraction)
    ↓
SettingsViewModel (StateFlow) ←→ SettingsScreen (Slider, 10% 스냅)
    ↓
HomeScreen / ArchiveScreen (collectAsState(initial = getSwipeThresholdFraction()))
    ↓
HomeTodoItem / ArchiveItem / TrashItem (thresholdFraction 인자)
    ↓
SwipeToDismissBox(thresholdFraction) — pointerInput((thresholdFraction*10).roundToInt())
```

---

## 5. Thumb(슬라이더 움직이는 요소) 관련

- **현재**: primary 색으로 채운 원(20.dp). `SliderDefaults.colors(thumbColor = Color.Transparent)` 로 기본 thumb 비 drawing → 트랙이 덜 가려짐.
- **투명화 시도**: thumb 배경만 투명하게 해 트랙이 비치게 하려 했으나, Material3 Slider 구조상 원하는 대로 적용되지 않아 **미적용** 상태로 둠.

---

## 6. 검증 포인트

- 설정에서 10%~90% 구간을 10% 단위로 변경 시 저장·표시 일치.
- 설정 변경 후 홈/아카이브로 돌아오면 **첫 스와이프부터** 새 임계점 적용.
- 새로 추가한 할 일 아이템도 **저장된 임계점**이 즉시 적용됨.

---

*마지막 업데이트: 2026-03-06*
