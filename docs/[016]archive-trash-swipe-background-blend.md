# [016] 아카이브·삭제 탭 스와이프 배경색 블렌딩 적용

- **앱**: com.naze.do_swipe  
- **versionCode**: 5  
- **versionName**: 0.1.2  
- **관련 화면**: 아카이브 (`ArchiveScreen`, `ArchiveItem`, `TrashItem`)  
- **핵심 변경**: [015]에서 Home에 적용한 스와이프 threshold 배경색 블렌딩을 아카이브 화면의 **완료한 일**(ArchiveItem)·**삭제한 일**(TrashItem)에도 동일하게 적용. 설정에서 온/오프 가능.

---

## 1. 목표

[HomeTodoItem](app/src/main/java/com/naze/do_swipe/ui/home/HomeTodoItem.kt)에 이미 적용된 것과 동일하게, 아카이브 화면의 **ArchiveItem**(완료한 일)과 **TrashItem**(삭제한 일)에서도 스와이프 진행률(threshold 대비)에 따라 앞쪽 카드 배경색을 삭제/복원(완료) 색으로 블렌딩한다. 설정에서 "스와이프 시 배경색 블렌딩"이 꺼져 있으면 기존처럼 surface 고정 색을 유지한다.

---

## 2. 구현 방식

### 2.1 ArchiveItem 수정

**파일**: `app/src/main/java/com/naze/do_swipe/ui/archive/ArchiveItem.kt`

- **파라미터 추가**: `swipeBackgroundBlendEnabled: Boolean = true` (기본값 true로 기존 호출과 호환).
- **import 추가**: `androidx.compose.ui.graphics.lerp`
- **content 람다**: `{ _, _ ->` → `{ swipeProgress, direction ->` 로 변경해 `swipeProgress`, `direction` 사용.
- **앞쪽 Card의 containerColor**
  - `swipeBackgroundBlendEnabled == false` 또는 `direction == null` → `MaterialTheme.colorScheme.surface` (기존과 동일).
  - `swipeBackgroundBlendEnabled == true` 이고 `direction != null` →  
    `targetColor` = 배경과 동일한 규칙  
    (`EndToStart` → swipeReversed면 SwipeActionComplete, 아니면 SwipeActionDelete / `StartToEnd` → 그 반대),  
    `lerp(surfaceColor, targetColor, swipeProgress)` 를 Card `containerColor`로 사용.

### 2.2 TrashItem 수정

**파일**: `app/src/main/java/com/naze/do_swipe/ui/trash/TrashItem.kt`

- **파라미터 추가**: `swipeBackgroundBlendEnabled: Boolean = true`
- **import 추가**: `androidx.compose.ui.graphics.lerp`
- **content 람다**: `{ _, _ ->` → `{ swipeProgress, direction ->`
- **앞쪽 Card의 containerColor**: ArchiveItem과 동일한 규칙으로 `surfaceColor`, `targetColor`(direction + swipeReversed 기준), `lerp(surfaceColor, targetColor, swipeProgress)` 적용.

### 2.3 ArchiveScreen에서 설정 전달

**파일**: `app/src/main/java/com/naze/do_swipe/ui/archive/ArchiveScreen.kt`

- **설정 구독 추가**:  
  `val swipeBackgroundBlendEnabled by app.settingsRepository.swipeBackgroundBlendEnabled.collectAsState(initial = true)`  
  (기존 `swipeReversed` 구독 아래에 추가.)
- **ArchiveItem 호출부**: `swipeBackgroundBlendEnabled = swipeBackgroundBlendEnabled` 인자 추가.
- **TrashItem 호출부**: `swipeBackgroundBlendEnabled = swipeBackgroundBlendEnabled` 인자 추가.

설정 키·Repository·ViewModel·SettingsScreen은 [015]에서 이미 구현되어 있으므로 변경 없음.

---

## 3. 수정·연동 파일 목록

| 파일 | 변경 내용 |
|------|------------|
| `ui/archive/ArchiveItem.kt` | swipeBackgroundBlendEnabled 파라미터, lerp import, content(swipeProgress, direction), Card containerColor = lerp(surface, targetColor, progress) |
| `ui/trash/TrashItem.kt` | 동일 |
| `ui/archive/ArchiveScreen.kt` | swipeBackgroundBlendEnabled 구독, ArchiveItem/TrashItem에 인자 전달 |

---

## 4. 데이터 흐름 요약

- **설정**: [015]와 동일. SettingsRepository에 저장된 `swipeBackgroundBlendEnabled`를 ArchiveScreen에서 구독.
- **아카이브**: ArchiveScreen이 `app.settingsRepository.swipeBackgroundBlendEnabled.collectAsState(initial = true)` 로 구독 후, 완료한 일 탭의 ArchiveItem·삭제한 일 탭의 TrashItem에 각각 전달.
- **스와이프**: SwipeToDismissBox가 content(swipeProgress, direction)로 전달 → 두 아이템 모두 `lerp(surfaceColor, targetColor, swipeProgress)` 로 Card 배경색 계산.

---

## 5. 검증

- 컴파일: `:app:compileDebugKotlin` — **BUILD SUCCESSFUL**
- 동작: 아카이브 화면에서 완료한 일·삭제한 일 아이템을 스와이프하면 threshold까지 진행 시 앞 카드가 삭제/복원 색으로 블렌딩됨. 설정에서 스위치를 끄면 블렌딩 없이 surface 색 유지.

---

## 6. 기대 효과

- 홈과 동일한 블렌딩 UX가 아카이브·삭제 탭에도 적용되어 일관된 체감.
- 삭제 방향(영구 삭제)은 빨간 계열, 복원 방향은 초록 계열로 블렌딩되어 방향 인지가 쉬움.
- [015]에서 추가한 설정 하나로 홈·아카이브·삭제 탭 모두 동시에 온/오프 가능.

---

*마지막 업데이트: 2026-03-06*
