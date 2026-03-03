# 홈 입력 바 Bottom Bar & 실행취소 스낵바 정책

- **앱**: com.naze.do_swipe  
- **versionCode**: 4  
- **versionName**: 0.1.1  
- **대상 기능**: 홈(Home) 화면의 **입력란(bottom bar) 배치**와 **실행취소 스낵바(Undo Snackbar)** 동작을 정리한 문서입니다.

---

## 1. 개요

- 홈 화면에서 새 할 일을 추가하는 입력란이 기존에는 **콘텐츠 Column의 마지막 뷰**로 배치되어 있었습니다.
  - 키보드·시스템 네비게이션 바와의 관계가 애매하고,
  - 스낵바와 겹쳐 보이는 문제 가능성이 있었습니다.
- 또한 완료/삭제/복원 등 여러 동작을 짧은 시간에 반복하면 **스낵바(토스트 역할)가 여러 개 중첩**되어,
  - 어떤 동작이 실행취소 대상인지 명확하지 않은 UX 문제가 있었습니다.

이를 해결하기 위해:

1. 홈 입력란을 `Scaffold`의 **`bottomBar` 슬롯**으로 이동하여 레이아웃 책임을 명확히 했습니다.
2. Home/Archive 화면에서 쓰는 **실행취소 스낵바를 공통 헬퍼 함수**로 추출하고,
3. **이전 스낵바를 먼저 닫고 새 스낵바를 띄우는 정책**으로 정리했습니다.

---

## 2. 홈 입력 바 Bottom Bar 설계

### 2.1 구조

- `HomeScreen` 의 `Scaffold` 구조:

```kotlin
Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = { AppTopBarHome(...) },
    bottomBar = {
        HomeInputBar(
            newTitle = newTitle,
            onTitleChange = { ... },
            onSubmit = { ... },
            focusRequester = focusRequester
        )
    }
) { innerPadding ->
    // 리스트/드래그/고스트 영역
}
```

- 입력 바는 `HomeScreen` 내부의 **프라이빗 컴포저블**로 분리:

```kotlin
@Composable
private fun HomeInputBar(
    newTitle: String,
    onTitleChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester
)
```

### 2.2 HomeInputBar 레이아웃

- `HomeInputBar` 내부:
  - `Surface` + `Row` 구조
  - 좌측 `OutlinedTextField`, 우측 `FilledIconButton(+)` 배치
  - 키보드/시스템 네비게이션 바를 고려한 인셋 처리:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
        .windowInsetsPadding(WindowInsets.navigationBars)
        .imePadding()
) { ... }
```

- `windowInsetsPadding(WindowInsets.navigationBars)`:
  - 하단 시스템 네비게이션 바 위로 입력 바가 올라와 있도록 보정.
- `imePadding()`:
  - 키보드가 올라올 때 입력 바가 **키보드 위로 자연스럽게 이동**하도록 처리.

### 2.3 스크롤/자동 스크롤과의 관계

- 리스트(`LazyColumn`)는 `Scaffold`의 `innerPadding` 안에서 `weight(1f)`로 채워지며,
  - `bottomBar` 높이를 포함한 패딩이 자동으로 적용되어,
  - **항상 입력 바 위까지만 리스트가 렌더링**됩니다.
- 기존의 “새 할 일 추가 시 마지막 아이템까지 스크롤” 기능은 그대로 유지됩니다.

---

## 3. 실행취소 스낵바 공통 헬퍼

### 3.1 헬퍼 위치와 시그니처

- 파일: `app/src/main/java/com/naze/do_swipe/ui/components/UndoSnackbar.kt`
- 공통 함수:

```kotlin
fun CoroutineScope.showUndoSnackbar(
    hostState: SnackbarHostState,
    message: String,
    onUndo: () -> Unit
)
```

- 사용처에서:
  - `remember { SnackbarHostState() }` 와 `rememberCoroutineScope()` 를 만든 후,
  - `scope.showUndoSnackbar(snackbarHostState, "메시지") { /* undo 로직 */ }` 형태로 호출합니다.

### 3.2 중첩 스낵바 방지 정책

- 핵심 정책: **새 스낵바를 보여주기 전에, 현재 떠 있는 스낵바를 먼저 닫는다.**

```kotlin
hostState.currentSnackbarData?.dismiss()

val result = hostState.showSnackbar(
    message = message,
    actionLabel = "실행취소",
    duration = SnackbarDuration.Short
)
if (result == SnackbarResult.ActionPerformed) {
    onUndo()
}
```

- 결과적으로:
  - 여러 개의 완료/삭제/복원 동작이 짧은 시간에 연속 실행되더라도,
  - **항상 “가장 최근 동작”에 대한 스낵바와 실행취소만 남게** 됩니다.
  - 사용자 입장에서는 “마지막 행동만 취소 가능”이라는 직관적인 UX를 제공.

### 3.3 Home / Archive 에서의 사용

- **HomeScreen**
  - 완료 스와이프:

    ```kotlin
    scope.showUndoSnackbar(snackbarHostState, "완료됨") {
        viewModel.setCompleted(id, false)
    }
    ```

  - 휴지통으로 이동:

    ```kotlin
    scope.showUndoSnackbar(snackbarHostState, "휴지통으로 이동") {
        viewModel.restore(id)
    }
    ```

- **ArchiveScreen**
  - 영구 삭제 예약 후:

    ```kotlin
    scope.showUndoSnackbar(snackbarHostState, "삭제됨") {
        viewModel.cancelPendingDelete(idToDelete)
    }
    ```

  - 완료 → 미완료 복원 / 삭제 → 다시 삭제 등도 동일 패턴으로 사용.

---

## 4. 스낵바 위치와 하단 패딩 정책

### 4.1 홈 화면 스낵바 하단 패딩 제거

- `HomeScreen` 의 `Scaffold` 에서:

```kotlin
snackbarHost = {
    SnackbarHost(snackbarHostState)
}
```

- 기존에는:

```kotlin
SnackbarHost(
    snackbarHostState,
    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
)
```

처럼 네비게이션 바 인셋을 한 번 더 적용하고 있었음.

- 이제:
  - `bottomBar` 자체가 `navigationBars` 인셋을 처리하므로,
  - 스낵바에는 별도의 하단 인셋을 주지 않아도 **bottomBar 바로 위**에 자연스럽게 뜹니다.
  - 결과적으로 **스낵바–입력 바–네비게이션 바 사이의 불필요한 여백**이 사라집니다.

### 4.2 UX 기대 동작

| 상황 | 기대 동작 |
|------|-----------|
| 할 일을 스와이프로 빠르게 여러 개 완료/삭제 | 화면 하단에 항상 하나의 스낵바만 보이고, 내용은 마지막 동작 기준으로 갱신됨 |
| 스낵바의 실행취소 클릭 | 마지막 동작만 취소되고, 그 이전 스낵바들은 이미 dismiss 처리되어 중첩되지 않음 |
| 키보드가 올라온 상태에서 입력 | 입력란은 키보드 위로 이동(`imePadding`), 스낵바는 그 위의 안전한 위치에 표시 |
| 시스템 네비게이션 바가 있는 기기 | 입력란은 네비게이션 바 위에, 스낵바는 입력란 바로 위에 표시되어 하단에 과한 여백 없음 |

---

## 5. 관련 파일

- `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`
  - `Scaffold` 의 `bottomBar` 및 `HomeInputBar` 정의
  - 홈 화면에서 실행취소 스낵바 호출 위치
- `app/src/main/java/com/naze/do_swipe/ui/archive/ArchiveScreen.kt`
  - 아카이브/휴지통 화면에서 실행취소 스낵바 호출 위치
- `app/src/main/java/com/naze/do_swipe/ui/components/UndoSnackbar.kt`
  - `showUndoSnackbar` 공통 헬퍼 정의

---

*마지막 업데이트: 2026년 3월*

