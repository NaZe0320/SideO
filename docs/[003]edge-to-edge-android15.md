# [003] Android 15 Edge-to-Edge & 더 넓은 화면 대응 정리

- **앱**: com.naze.do_swipe  
- **versionCode**: 3  
- **versionName**: 0.1  
- **compileSdk / targetSdk**: 36

---

## 1. Play Console 경고 정리

### 1.1 지원 중단된 더 넓은 화면용 API/파라미터

- 콘솔 메시지:
  - “앱에서 더 넓은 화면용으로 지원 중단된 API 또는 파라미터를 사용합니다… Android 15에서 지원 중단되었습니다.”
- 실제로 지적된 항목:
  - `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`
  - `androidx.activity.EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode`
- 의미:
  - Android 15에서 **기존 cutout 및 edge‑to‑edge 처리 방식 일부가 deprecated** 되었음을 알리는 경고.
  - 해당 심볼들은 **AndroidX 내부 구현**에서 사용 중이며, 앱 레이어에서는 직접 호출하지 않음.

### 1.2 “일부 사용자에게는 더 넓은 화면이 표시되지 않을 수 있습니다”

- 콘솔 메시지 요약:
  - SDK 35(Android 15)를 타겟팅하는 앱은 **기본적으로 edge‑to‑edge**로 표시된다.
  - 앱이 **WindowInsets(상태바, 내비게이션바, 제스처 영역, 노치 등)** 를 제대로 처리해야  
    대형 화면 / 멀티 윈도우 / 폴더블 등에서 UI가 깨지지 않는다.
  - 대안으로 `enableEdgeToEdge()` (Kotlin) 또는 `EdgeToEdge.enable()` (Java)를 호출하라고 안내.

---

## 2. 코드 레벨 대응 현황

### 2.1 테마 레벨 edge‑to‑edge + status bar 아이콘 가독성

**파일**: `app/src/main/java/com/naze/do_swipe/ui/theme/Theme.kt`

#### 핵심 코드

```kotlin
@Composable
fun DoSwipeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController: WindowInsetsControllerCompat? =
            WindowCompat.getInsetsController(window, window.decorView)

        SideEffect {
            // 라이트 테마(밝은 배경)면 아이콘/텍스트를 어둡게, 다크 테마면 밝게
            windowInsetsController?.isAppearanceLightStatusBars = !resolvedDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### 효과

- **edge‑to‑edge 활성화**
  - `WindowCompat.setDecorFitsSystemWindows(window, false)` 사용.
  - Google 가이드에서 권장하는 modern 패턴이며, `enableEdgeToEdge()`가 내부적으로 하는 일과 동등.

- **status bar 가독성 확보**
  - `resolvedDarkTheme`에 따라 `isAppearanceLightStatusBars`를 토글:
    - 라이트 테마 → 밝은 배경 위에 **검은 아이콘/텍스트**
    - 다크 테마 → 어두운 배경 위에 **흰 아이콘/텍스트**
  - “status bar 배경이 하얀데 라이트 모드에서 글자가 안 보이는 문제” 해결.

- **deprecated API 회피**
  - `window.statusBarColor` / `setStatusBarColor()` / `setNavigationBarColor()` 등  
    Android 15에서 deprecated된 색상 변경 API를 **사용하지 않음**.

---

## 3. Composable 화면에서 WindowInsets 처리

Android 15의 “인셋 처리 필요” 요구사항에 대해, 주요 화면에서 이미 다음과 같이 대응 중.

### 3.1 상단 AppBar (status bar 인셋)

**파일**: `app/src/main/java/com/naze/do_swipe/ui/components/AppTopBar.kt`

- `WindowInsets.statusBars`를 사용해 **상단 AppBar를 status bar/노치 영역 아래로 내림**.

### 3.2 HomeScreen: navigation bar 인셋 + 전체 컨텐츠

**파일**: `app/src/main/java/com/naze/do_swipe/ui/home/HomeScreen.kt`

- `Scaffold`에서 `contentWindowInsets = WindowInsets(0, 0, 0, 0)`로 기본 인셋 비활성화.
- Snackbar 및 하단 입력 영역에 `WindowInsets.navigationBars`를 적용해  
  **navigation bar/제스처 영역과 겹치지 않도록** 처리.
- 상·하단 모두 인셋을 직접 다루는 구조로, edge‑to‑edge와 잘 맞는 패턴.

### 3.3 SettingsScreen / ArchiveScreen

**파일**:
- `app/src/main/java/com/naze/do_swipe/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/naze/do_swipe/ui/archive/ArchiveScreen.kt`

- 동일하게 `contentWindowInsets = WindowInsets(0, 0, 0, 0)` +  
  본문에 `windowInsetsPadding(WindowInsets.navigationBars)` 적용.
- 상단은 공통 `AppTopBarSub`를 통해 status bar 인셋 적용.

---

## 4. 남은 작업 / 운영 상 권장사항

### 4.1 필수 코드 변경 여부

- 현재 구조 상:
  - edge‑to‑edge 활성화
  - 상태바 아이콘 색 가독성
  - 상/하단 인셋 처리  
  가 이미 modern 패턴으로 구현되어 있음.
- 추가로 **필수적인 코드 변경은 없음**.

### 4.2 권장 테스트 시나리오

- **환경**
  - Android 15 에뮬레이터 또는 실기기
- **케이스**
  - 라이트/다크 모드
  - 세로/가로 회전
  - 분할 화면, 플로팅 창(멀티 윈도우)
  - 가능하면 태블릿/폴더블
- **체크 포인트**
  - 상단 AppBar가 status bar·노치에 가려지지 않는지
  - 하단 입력창/리스트/스낵바가 navigation bar/제스처 영역에 가려지지 않는지
  - 라이트 모드에서 status bar 글자/아이콘이 충분히 눈에 잘 띄는지

---

## 5. 요약

- Android 15에서 발생한
  - “지원 중단된 더 넓은 화면용 API/파라미터”
  - “일부 사용자에게는 더 넓은 화면이 표시되지 않을 수 있음”
  경고에 대해,
  - **테마 레벨 edge‑to‑edge + status bar 아이콘 색 토글**을 추가했고,
  - 각 화면에서 **WindowInsets를 이용해 status/nav bar 인셋을 명시적으로 처리**하였다.
- deprecated 상태바/내비게이션바 색상 API를 사용하지 않으면서,  
  Android 15 가이드에 부합하는 edge‑to‑edge 패턴을 따르고 있음.
- 남은 일은 다양한 화면/창 모드에서 **실기/에뮬레이터 테스트를 통해 UI 깨짐 여부를 검증**하는 것뿐이다.

