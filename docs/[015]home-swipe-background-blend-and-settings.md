# [015] Home 스와이프 threshold 배경색 블렌딩 및 설정 온/오프

- **앱**: com.naze.do_swipe  
- **versionCode**: 5  
- **versionName**: 0.1.2  
- **관련 화면**: Home (`HomeScreen`, `HomeTodoItem`), 설정 (`SettingsScreen`), 아카이브/삭제 (`ArchiveItem`, `TrashItem`)  
- **핵심 변경**: 스와이프 진행률(threshold 대비)에 따라 앞쪽 카드 배경색을 삭제/완료 색으로 블렌딩하는 효과 추가, 설정에서 해당 효과 온/오프 가능

---

## 1. 요구사항 정리

- **스와이프 시 앞 카드 색상**
  - 삭제 방향으로 스와이프 시: 임계점(threshold)까지 진행률에 따라 앞 카드 배경이 `surface` → `SwipeActionDelete`로 블렌딩.
  - 완료 방향으로 스와이프 시: 동일하게 `surface` → `SwipeActionComplete`로 블렌딩.
  - 임계점에 도달하면 앞 카드 색이 뒤 배경(삭제/완료 색)과 동일한 수준이 됨.
- **설정**
  - 이 효과를 설정 화면에서 스위치로 켜고 끌 수 있음 (기본값: 켜짐).

---

## 2. 구현 방식

### 2.1 SwipeToDismissBox에 진행률(progress) 노출

**파일**: `app/src/main/java/com/naze/do_swipe/ui/components/SwipeToDismissByPosition.kt`

- **변경 전**: `content: @Composable () -> Unit` — 콘텐츠가 스와이프 진행 정보를 받지 않음.
- **변경 후**: `content: @Composable (swipeProgress: Float, direction: SwipeDirection?) -> Unit`
  - `swipeProgress`: 0f ~ 1f. threshold 구간 대비 이동 비율. threshold 도달 시 1f.
  - `direction`: 현재 스와이프 방향 (`StartToEnd` / `EndToStart` / `null`).
- **progress 계산**
  - `EndToStart`(왼쪽 스와이프): `(-offsetPx / (widthPx * thresholdFraction)).coerceIn(0f, 1f)`
  - `StartToEnd`(오른쪽 스와이프): `(offsetPx / (widthPx * thresholdFraction)).coerceIn(0f, 1f)`
  - `direction == null`: `progress = 0f`
- 내부에서 `content(swipeProgress, direction)` 형태로 호출.

### 2.2 HomeTodoItem에서 배경색 블렌딩 적용

**파일**: `app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt`

- **새 파라미터**: `swipeBackgroundBlendEnabled: Boolean = true`
- **Card 배경색**
  - `swipeBackgroundBlendEnabled == false`: `MaterialTheme.colorScheme.surface` 고정 (기존 동작).
  - `swipeBackgroundBlendEnabled == true`:
    - `direction`과 `swipeReversed`로 현재 스와이프 방향이 삭제인지 완료인지 판단.
    - `targetColor` = `SwipeActionDelete` 또는 `SwipeActionComplete`
    - `Color.lerp(surfaceColor, targetColor, swipeProgress)`를 Card `containerColor`로 사용.
- `cardContent`를 `(swipeProgress, direction)`을 인자로 받는 composable로 변경하고, `SwipeToDismissBox`의 content 람다 안에서 호출.
- `enableInteractions == false`(드래그 중 등)일 때는 `cardContent(0f, null)`로 호출해 항상 surface 색 유지.

### 2.3 설정 저장소·ViewModel·UI

**SettingsRepository** (`app/src/main/java/com/naze/do_swipe/data/preferences/SettingsRepository.kt`)

- `KEY_SWIPE_BACKGROUND_BLEND = "swipe_background_blend"` 추가.
- `_swipeBackgroundBlendEnabled: MutableStateFlow(Boolean)`, 기본값 `true`.
- `swipeBackgroundBlendEnabled: StateFlow<Boolean>`, `setSwipeBackgroundBlendEnabled(Boolean)` 추가. SharedPreferences 저장 후 StateFlow 갱신.

**SettingsViewModel** (`app/src/main/java/com/naze/do_swipe/ui/settings/SettingsViewModel.kt`)

- `swipeBackgroundBlendEnabled: StateFlow<Boolean>` (settings 값을 `stateIn`으로 노출).
- `setSwipeBackgroundBlendEnabled(enabled: Boolean)`에서 `settings.setSwipeBackgroundBlendEnabled(enabled)` 호출.

**SettingsScreen** (`app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt`)

- "제스처 설정" 카드 내에 "스와이프 방향 반전" 아래 구분선과 함께 **스와이프 시 배경색 블렌딩** 스위치 추가.
  - 제목: "스와이프 시 배경색 블렌딩"
  - 부제: "임계점에 가까울수록 카드 색이 삭제/완료 색으로 변해요"
  - `viewModel.swipeBackgroundBlendEnabled.collectAsState()`, `viewModel.setSwipeBackgroundBlendEnabled(it)` 연결.

### 2.4 HomeScreen에서 설정 전달

**파일**: `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`

- `app.settingsRepository.swipeBackgroundBlendEnabled.collectAsState(initial = true)` 로 구독.
- 리스트의 `HomeTodoItem`과 드래그 중 Popup의 `HomeTodoItem` 모두에 `swipeBackgroundBlendEnabled = swipeBackgroundBlendFromPrefs` 전달.

### 2.5 ArchiveItem / TrashItem 호환

**파일**:  
- `app/src/main/java/com/naze/do_swipe/ui/archive/ArchiveItem.kt`  
- `app/src/main/java/com/naze/do_swipe/ui/trash/TrashItem.kt`

- `SwipeToDismissBox`의 `content` 시그니처가 `(swipeProgress, direction) -> Unit`으로 변경되었으므로, 두 파일 모두 content 람다를 `{ _, _ -> ... }` 형태로 수정.
- 두 화면에서는 progress 기반 배경색 변경을 사용하지 않으며, 기존처럼 고정 surface 색의 카드만 표시.

---

## 3. 수정·연동 파일 목록

| 파일 | 변경 내용 |
|------|------------|
| `ui/components/SwipeToDismissByPosition.kt` | content 시그니처 변경, swipeProgress 계산 및 전달 |
| `ui/home/HomeTodoItem.kt` | swipeBackgroundBlendEnabled 파라미터, cardContent(progress, direction), Color.lerp 배경색 |
| `ui/home/HomeScreen.kt` | swipeBackgroundBlendFromPrefs 구독 및 HomeTodoItem에 전달 |
| `data/preferences/SettingsRepository.kt` | KEY_SWIPE_BACKGROUND_BLEND, swipeBackgroundBlendEnabled StateFlow 및 setter |
| `ui/settings/SettingsViewModel.kt` | swipeBackgroundBlendEnabled StateFlow 및 setSwipeBackgroundBlendEnabled |
| `ui/settings/SettingsScreen.kt` | 제스처 설정 카드에 "스와이프 시 배경색 블렌딩" 스위치 추가 |
| `ui/archive/ArchiveItem.kt` | SwipeToDismissBox content 람다 시그니처 `{ _, _ -> ... }` 로 변경 |
| `ui/trash/TrashItem.kt` | SwipeToDismissBox content 람다 시그니처 `{ _, _ -> ... }` 로 변경 |

---

## 4. 데이터 흐름 요약

- **설정**: SettingsRepository ↔ SettingsViewModel ↔ SettingsScreen (스위치로 온/오프 저장).
- **홈**: HomeScreen이 SettingsRepository.swipeBackgroundBlendEnabled를 구독해 HomeTodoItem에 전달.
- **스와이프**: SwipeToDismissBox가 offset/width/threshold로 progress를 계산해 content(progress, direction)로 전달 → HomeTodoItem에서 progress와 direction으로 Card 배경색을 `lerp(surface, targetColor, progress)`로 계산.

---

## 5. 검증

- 컴파일: `:app:compileDebugKotlin` — **BUILD SUCCESSFUL**
- 동작: 스와이프 시 threshold까지 끌면 앞 카드가 삭제/완료 색으로 블렌딩됨. 설정에서 스위치를 끄면 블렌딩 없이 기존 surface 색 유지.

---

## 6. 기대 효과

- 스와이프 진행에 따라 앞 카드가 뒤 배경(삭제/완료) 색과 자연스럽게 블렌딩되어, threshold 도달 시 시각적으로 "같은 색"으로 맞춰짐.
- 삭제 방향은 빨간 계열, 완료 방향은 초록 계열로 각각 블렌딩되어 방향 인지가 쉬움.
- 설정에서 효과를 끌 수 있어 선호에 따라 선택 가능.

---

*마지막 업데이트: 2026-03-06*
