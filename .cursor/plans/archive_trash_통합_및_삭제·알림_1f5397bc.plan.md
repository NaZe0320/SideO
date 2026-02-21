---
name: Archive Trash 통합 및 삭제·알림
overview: ArchiveScreen과 TrashScreen을 탭 하나로 통합하고, 완료에서 삭제는 영구 삭제로 변경하며, Archive/Trash 아이템을 HomeTodoItem과 동일한 SwipeToDismissBox로 통일. swipeReversed는 Screen에서 한 번만 구독해 매개변수로 전달하는 구조 유지. 완료/복원/삭제 시 Snackbar 알림과 3초 내 실행취소를 적용한다.
todos: []
isProject: false
---

# Archive·Trash 통합, 삭제 로직 개편, 알림·실행취소 (통합 계획)

---

## 1. swipeReversed를 Screen에서 구독해 매개변수로 넘기는 이유

**현재 구조**: [HomeScreen.kt](app/src/main/java/com/naze/side_o/ui/home/HomeScreen.kt)에서 `app.settingsRepository.swipeReversed.collectAsState(initial = false)`로 **한 번만** 구독하고, 각 `HomeTodoItem(..., swipeReversed = swipeReversedFromPrefs)`에 **매개변수**로 전달하고 있음.

이렇게 하는 이유:

- **구독 한 번**: 리스트에 아이템이 10개면, 값은 하나인데 10번 구독할 필요가 없음. Screen에서 한 번만 `collectAsState` 하면 되고, 아이템은 같은 boolean만 받음.
- **단방향 데이터 흐름**: 설정 값의 소유자는 Screen(또는 상위). 아이템은 “지금 스와이프 방향이 이거다”만 알면 되고, 설정 저장소/ViewModel을 알 필요 없음.
- **재사용·테스트**: `HomeTodoItem` / `ArchiveItem` / `TrashItem`을 Preview나 단위 테스트할 때 `swipeReversed = true/false`만 넘기면 됨. ViewModel이나 Application 의존성 없이 동작.
- **Compose 권장**: 상태는 상위에서 보관하고, 하위 Composable은 인자로 받는 “단일 진실 공급원” 패턴.

그래서 **ArchiveItem / TrashItem에서도** ViewModel state를 직접 접근하지 않고, **ArchiveTrashScreen에서** `settingsRepository.swipeReversed`를 한 번 구독한 뒤 `swipeReversed`를 **매개변수**로 각 아이템에 넘기는 방식을 유지하는 것이 맞음.

---

## 2. 통합 화면 + 삭제 로직 (기존 계획 본문)

- **라우트**: Archive 하나로 통합. 휴지통은 별도 라우트 제거, 탭으로만 진입.
- **데이터**: TodoDao에 `deletePermanently(id)`, TodoRepository에 `deletePermanently(id)` 추가.
- **ViewModel**: ArchiveViewModel + TrashViewModel 통합 → **ArchiveTrashViewModel**  
  - `completedTodos`, `deletedTodos` (StateFlow), `uncomplete(id)`, `restore(id)`, `deletePermanently(id)`.
- **삭제 규칙**  
  - 홈(active) 스와이프 삭제 → 휴지통(`markDeleted`).  
  - 아카이브/휴지통 스와이프 삭제 → **영구 삭제** (`deletePermanently`).  
  - 아카이브 스와이프 복원 → `setCompleted(id, false)`. 휴지통 스와이프 복원 → `restore(id)`.
- **UI**: 단일 **ArchiveTrashScreen** with TabRow("아카이브" | "휴지통").  
  - **ArchiveItem** / **TrashItem**을 HomeTodoItem처럼 **SwipeToDismissBox**로 구현.  
  - **swipeReversed**는 **Screen에서만** 구독하고, `swipeReversed`를 **매개변수**로 ArchiveItem/TrashItem에 전달해 방향 통일.
- 선택 모드(selectedIds) 및 FAB "영구 삭제" 제거. 모든 조작은 스와이프로만.

---

## 3. 알림 + 실행취소 (Snackbar, 3초)

- **방식**: Toast 대신 **Snackbar** 사용 (액션 버튼 지원). 메시지 + "실행취소" 버튼, **3초** 동안 표시.
- **적용 액션**  
  - 홈 완료 → "완료됨" / 실행취소 시 `setCompleted(id, false)`.  
  - 홈 휴지통 이동 → "휴지통으로 이동" / 실행취소 시 `restore(id)`.  
  - 아카이브 복원 → "복원됨" / 실행취소 시 `setCompleted(id, true)`.  
  - 아카이브 영구 삭제 → "삭제됨" / 실행취소 시 예약 삭제 취소(실제 삭제 안 함).  
  - 휴지통 복원 → "복원됨" / 실행취소 시 `markDeleted(id)`.  
  - 휴지통 영구 삭제 → "삭제됨" / 실행취소 시 예약 삭제 취소.
- **구현**  
  - **즉시 실행 후 취소**: 완료/휴지통 이동/복원은 그대로 실행 후 Snackbar 표시 → 실행취소 클릭 시 반대 연산 한 번 더.  
  - **영구 삭제**: 즉시 삭제하지 않고 ViewModel에서 **3초 지연 삭제** 예약(Job 또는 pending set). Snackbar 표시 → 실행취소 시 예약만 취소.
- **공통**: HomeScreen, ArchiveTrashScreen에 `SnackbarHostState` + `Scaffold(snackbarHost)`. 액션 시 `showSnackbar(..., actionLabel = "실행취소", duration = 3초)` 및 실행취소 콜백에서 ViewModel 메서드 호출.

---

## 4. 파일·구조 요약


| 구분        | 내용                                                                             |
| --------- | ------------------------------------------------------------------------------ |
| 데이터       | TodoDao/Repository `deletePermanently(id)` 추가                                  |
| 네비게이션     | Trash 라우트 제거, Archive = 탭(아카이브/휴지통)                                            |
| ViewModel | ArchiveTrashViewModel 통합, 영구삭제 예약(3초) 및 취소                                     |
| Screen    | ArchiveTrashScreen(탭 + 리스트), **swipeReversed** Screen에서 구독 후 아이템에 **매개변수**로 전달 |
| 아이템       | ArchiveItem/TrashItem → SwipeToDismissBox, 인자로 `swipeReversed` 수신              |
| 알림        | HomeScreen/ArchiveTrashScreen SnackbarHost, 3초 내 실행취소                          |


---

## 5. 계획 파일 정리

- 현재 **두 개**로 나뉜 계획(통합·삭제 로직 / 알림·실행취소)은 **위 내용 하나로 통합**하는 것이 맞음.
- 구현 시 참고할 문서는 **이 통합 계획 하나**만 두고, 기존에 나뉜 plan 문서는 이 통합본으로 대체하거나 삭제해도 됨.

