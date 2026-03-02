# [008] 아카이브/삭제 영구 삭제 다이얼로그 및 스와이프 UI 개선

- **앱**: com.naze.do_swipe  
- **versionCode**: 4  
- **versionName**: 0.1.1  
- **대상 기능**: 아카이브 화면(완료한 일 / 삭제한 일), 스와이프 확인 다이얼로그, 공통 ConfirmDialog, 스와이프 배경 색상

---

## 1. 개요

아카이브·삭제 탭에서 다음을 적용했다.

- **워딩**: 스와이프 시 노출되는 "삭제" → **"영구 삭제"** 로 통일
- **확인 다이얼로그**: 삭제 방향으로 스와이프 시 즉시 삭제하지 않고, 카드가 원위치된 뒤 **ConfirmDialog**를 띄우고, 확인 시에만 영구 삭제 실행
- **공통 ConfirmDialog**: 설정의 데이터 초기화·아카이브 영구 삭제에서 공통 사용, **경고(삭제) 시 확인 버튼을 red 계열**로 표시
- **스와이프 배경 색상**: 파스텔 대신 **스와이프 전용 색**(SwipeActionComplete / SwipeActionDelete)으로 덜 비활성화된 느낌 적용  
  - 적용 위치: ArchiveItem, TrashItem, **HomeTodoItem**

---

## 2. 공통 ConfirmDialog 컴포넌트

### 2.1 파일 및 API

**파일**: `app/src/main/java/com/naze/do_swipe/ui/components/ConfirmDialog.kt`

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| title | String | 다이얼로그 제목 |
| message | String | 본문 메시지 |
| confirmText | String | 확인 버튼 텍스트 (예: "영구 삭제", "모두 삭제") |
| dismissText | String | 취소 버튼 텍스트 (예: "취소") |
| onConfirm | () -> Unit | 확인 시 콜백 |
| onDismiss | () -> Unit | 취소/바깥 터치 시 콜백 |
| isDestructive | Boolean | true 시 확인 버튼을 **red**(MaterialTheme.colorScheme.error)로 표시 |
| modifier | Modifier | 선택 |

### 2.2 스타일

- `AlertDialog` 기반
- `RoundedCornerShape(24.dp)`, `containerColor = MaterialTheme.colorScheme.surface`
- 제목: `headlineMedium`, 본문: `bodyMedium`
- **isDestructive == true**: 확인 버튼 `containerColor = error`, `contentColor = onError`

### 2.3 사용처

| 화면 | 용도 | isDestructive |
|------|------|----------------|
| SettingsScreen | 데이터 초기화 | true |
| ArchiveScreen | 항목 영구 삭제 확인 | true |

---

## 3. 스와이프 시 영구 삭제 확인 흐름

### 3.1 SwipeToDismissBox 확장

**파일**: `app/src/main/java/com/naze/do_swipe/ui/components/SwipeToDismissByPosition.kt`

추가 파라미터:

- `confirmBeforeDismissEndToStart: Boolean = false`
- `confirmBeforeDismissStartToEnd: Boolean = false`
- `onConfirmRequestedEndToStart: (() -> Unit)? = null`
- `onConfirmRequestedStartToEnd: (() -> Unit)? = null`

**동작**:

- 해당 방향에서 `confirmBeforeDismiss* == true` 이고 threshold를 넘겼을 때:
  - 카드를 밀어내는 애니메이션 **하지 않음**
  - offset을 **0으로 복귀**(animate back to 0)
  - `onDismiss*` 는 호출하지 않고, **onConfirmRequested*** 만 호출
- `false` 이면 기존처럼 애니메이션 후 `onDismiss*` 호출

### 3.2 ArchiveItem / TrashItem

- **삭제 방향**에만 `confirmBeforeDismiss* = true`, `onConfirmRequested* = { onRequestPermanentDelete() }` 전달
- **복원 방향**은 기존처럼 스와이프 확정 시 바로 `onRestore()` 호출
- 아이템 시그니처: `onRequestPermanentDelete: () -> Unit` (실제 삭제는 스크린에서 다이얼로그 확인 시 수행)

### 3.3 ArchiveScreen

- **state**: `pendingDeleteId: Long?` — 다이얼로그를 띄울 항목 ID
- `ArchiveItem` / `TrashItem` 의 `onRequestPermanentDelete` 에서 `pendingDeleteId = todo.id` 설정 → **ConfirmDialog** 표시
- **다이얼로그**
  - 제목: "영구 삭제"
  - 메시지: "이 항목을 영구 삭제할까요? 복구할 수 없습니다."
  - 확인: "영구 삭제", `isDestructive = true`
- **확인 시**: `viewModel.schedulePermanentDelete(id)`, Snackbar("삭제됨", 실행취소 시 `cancelPendingDelete`), `pendingDeleteId = null`
- **취소 시**: `pendingDeleteId = null` 만 수행

### 3.4 데이터 흐름 요약

```
사용자 스와이프(삭제 방향) → SwipeToDismissBox threshold 초과
  → confirmBeforeDismiss 이므로 offset 0으로 복귀, onConfirmRequested 호출
  → ArchiveScreen: pendingDeleteId = todo.id, ConfirmDialog 표시
  → 사용자 "영구 삭제" 확인 → schedulePermanentDelete(id), Snackbar, pendingDeleteId = null
```

---

## 4. 워딩 및 스와이프 배경 색상

### 4.1 워딩

- **ArchiveItem.kt**, **TrashItem.kt**: 스와이프 배경 텍스트 "삭제" → **"영구 삭제"** (두 방향 모두, `swipeReversed` 분기 유지)
- HomeTodoItem 은 홈에서의 "삭제"(휴지통 이동)이므로 기존 "삭제" 유지

### 4.2 스와이프 전용 색상 (Color.kt)

**파일**: `app/src/main/java/com/naze/do_swipe/ui/theme/Color.kt`

| 이름 | 용도 | 비고 |
|------|------|------|
| SwipeActionComplete | 완료/복원 배경 | 기존 ActionComplete 보다 선명한 민트 |
| SwipeActionCompleteContent | 완료/복원 아이콘·텍스트 | 흰색 |
| SwipeActionDelete | 삭제 배경 | 기존 ActionDelete 보다 선명한 red |
| SwipeActionDeleteContent | 삭제 아이콘·텍스트 | 흰색 |

기존 `ActionComplete` / `ActionDelete` 파스텔은 유지하고, 스와이프 배경에만 `SwipeAction*` 사용해 "덜 비활성화" 느낌을 줌.

### 4.3 적용 위치

| 파일 | 변경 내용 |
|------|-----------|
| ArchiveItem.kt | 배경/아이콘/텍스트 → SwipeAction* 사용, "영구 삭제" 워딩 |
| TrashItem.kt | 동일 |
| HomeTodoItem.kt | 배경/아이콘/텍스트 → SwipeAction* 사용 (워딩은 "완료"/"삭제" 유지) |

---

## 5. 수정/추가 파일 목록

| 구분 | 파일 | 내용 |
|------|------|------|
| 신규 | `ui/components/ConfirmDialog.kt` | 공통 AlertDialog, isDestructive 시 red 확인 버튼 |
| 수정 | `ui/components/SwipeToDismissByPosition.kt` | confirmBeforeDismiss*, onConfirmRequested* 추가 |
| 수정 | `ui/archive/ArchiveItem.kt` | "영구 삭제" 워딩, confirm + onRequestPermanentDelete, SwipeAction* 색상 |
| 수정 | `ui/trash/TrashItem.kt` | 동일 |
| 수정 | `ui/archive/ArchiveScreen.kt` | pendingDeleteId state, ConfirmDialog 표시, onRequestPermanentDelete 연동 |
| 수정 | `ui/settings/SettingsScreen.kt` | 데이터 초기화 다이얼로그를 ConfirmDialog로 교체, isDestructive = true |
| 수정 | `ui/theme/Color.kt` | SwipeActionComplete / SwipeActionDelete 계열 색상 추가 |
| 수정 | `ui/home/HomeTodoItem.kt` | 스와이프 배경/아이콘/텍스트에 SwipeAction* 색상 적용 |

---

## 6. 주의사항

- **HomeTodoItem**: 완료/삭제 스와이프는 **즉시 실행** 유지(확인 다이얼로그 없음). 스와이프 배경 색상만 SwipeAction* 로 통일.
- **다크 모드**: ConfirmDialog 확인 버튼 및 스와이프 색은 MaterialTheme / Color.kt 정의에 따라 자동 대응.
- **수정 다이얼로그(저장 버튼)**: 긍정 액션이므로 primary 유지. red는 경고/삭제용으로만 사용.
