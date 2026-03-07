# [018] Firebase Analytics 및 커스텀 이벤트 로깅

- **앱**: com.naze.do_swipe  
- **versionCode**: 7  
- **versionName**: 0.1.4  
- **대상 기능**: Firebase Analytics 연동, 8개 커스텀 이벤트 로깅 (first_open, task_created, task_swipe_completed, task_swipe_deleted, undo_clicked, reminder_enabled, notification_opened, widget_task_created)

---

## 1. 개요

Firebase Analytics SDK를 적용하고, 앱 내 주요 사용자 행동을 **커스텀 이벤트**로 로깅한다. 이벤트는 **Analytics 인터페이스**로 추상화되어 있어 테스트·비활성화 시 다른 구현체로 교체할 수 있다.

- **first_open**: Firebase 자동 수집(예약 이벤트). 별도 코드 없음.
- **task_created**: 할 일 추가 시 (HomeViewModel.addTodo 성공 후).
- **task_swipe_completed**: 홈에서 스와이프로 완료 처리 시.
- **task_swipe_deleted**: 홈에서 스와이프로 휴지통 이동 시.
- **undo_clicked**: 실행취소 스낵바에서 "실행취소" 버튼 클릭 시.
- **reminder_enabled**: 설정에서 알림(리마인더) ON 시.
- **notification_opened**: 알림을 탭해 앱이 열렸을 때.
- **widget_task_created**: 위젯의 "할 일 추가" 버튼으로 앱을 연 뒤, 사용자가 할 일을 저장했을 때.

---

## 2. Gradle 설정

### 2.1 버전 카탈로그 (libs.versions.toml)

| 항목 | 값 | 설명 |
|------|-----|------|
| firebaseBom | "33.7.0" | Firebase BOM 버전 |
| googleServices | "4.4.2" | Google Services 플러그인 버전 |
| firebase-bom | group/name/version.ref | platform 의존성용 |
| firebase-analytics-ktx | BOM 참조 | Analytics KTX |
| google-services | plugin id + version.ref | 앱 모듈에 적용 |

### 2.2 루트 build.gradle.kts

- `alias(libs.plugins.google.services) apply false` 추가

### 2.3 앱 build.gradle.kts

- **plugins**: `alias(libs.plugins.google.services)` 적용
- **dependencies**: `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.analytics.ktx)` 추가

### 2.4 google-services.json

- **release**: 패키지 `com.naze.do_swipe` (기존)
- **debug**: 패키지 `com.naze.do_swipe.debug` 클라이언트 추가 (디버그 빌드에서 processDebugGoogleServices 매칭용)

---

## 3. Analytics 추상화

### 3.1 인터페이스 및 이벤트 상수

**파일**: `app/src/main/java/com/naze/do_swipe/analytics/Analytics.kt`

| 항목 | 설명 |
|------|------|
| `Analytics` | `fun logEvent(name: String, params: Map<String, Any>? = null)` — 이벤트 로깅 인터페이스 |
| `AnalyticsEvents` | 이벤트 이름 상수 객체 (FIRST_OPEN, TASK_CREATED, TASK_SWIPE_COMPLETED, TASK_SWIPE_DELETED, UNDO_CLICKED, REMINDER_ENABLED, NOTIFICATION_OPENED, WIDGET_TASK_CREATED) |

### 3.2 Firebase 구현체

**파일**: `app/src/main/java/com/naze/do_swipe/analytics/FirebaseAnalyticsTracker.kt`

- `Analytics` 구현 클래스.
- 생성자에서 `FirebaseAnalytics.getInstance(context)` 보유.
- `logEvent(name, params)` → `params`를 `Bundle`로 변환 후 `firebaseAnalytics.logEvent(name, bundle)` 호출.
- `params == null`이면 `bundle`도 null로 전달.

### 3.3 Application 등록

**파일**: `app/src/main/java/com/naze/do_swipe/TodoApplication.kt`

- `val analytics: Analytics by lazy { FirebaseAnalyticsTracker(this) }` 추가.
- ViewModel·Composable에서 `(application as TodoApplication).analytics`로 접근.

---

## 4. 이벤트 로깅 위치

### 4.1 task_created

| 위치 | 파일 | 동작 |
|------|------|------|
| HomeViewModel.addTodo() | `ui/home/HomeViewModel.kt` | `repository.addTodo(...)` 성공 후 같은 launch 블록에서 `analytics.logEvent(AnalyticsEvents.TASK_CREATED)` 호출 |

### 4.2 task_swipe_completed / task_swipe_deleted

| 위치 | 파일 | 동작 |
|------|------|------|
| HomeTodoItem onAfterComplete | `ui/home/HomeScreen.kt` | 콜백 진입 시 `app.analytics.logEvent(AnalyticsEvents.TASK_SWIPE_COMPLETED)` 후 기존 스낵바·언두 흐름 |
| HomeTodoItem onAfterDelete | `ui/home/HomeScreen.kt` | 콜백 진입 시 `app.analytics.logEvent(AnalyticsEvents.TASK_SWIPE_DELETED)` 후 기존 스낵바·언두 흐름 |

- `app` = `LocalContext.current.applicationContext as TodoApplication`

### 4.3 undo_clicked

| 위치 | 파일 | 동작 |
|------|------|------|
| showUndoSnackbar | `ui/components/UndoSnackbar.kt` | 시그니처에 `onUndoFired: (() -> Unit)? = null` 추가. `SnackbarResult.ActionPerformed` 시 `onUndoFired?.invoke()` 후 `onUndo()` 호출 |
| HomeScreen | `ui/home/HomeScreen.kt` | 완료/삭제 스낵바 호출 시 `onUndoFired = { app.analytics.logEvent(AnalyticsEvents.UNDO_CLICKED) }` 전달 |
| ArchiveScreen | `ui/archive/ArchiveScreen.kt` | 삭제됨/복원됨 스낵바 3곳 모두 `onUndoFired = { app.analytics.logEvent(AnalyticsEvents.UNDO_CLICKED) }` 전달 |

### 4.4 reminder_enabled

| 위치 | 파일 | 동작 |
|------|------|------|
| setRemindersEnabled(true) | `ui/settings/SettingsViewModel.kt` | `enabled == true`일 때만 `(getApplication() as TodoApplication).analytics.logEvent(AnalyticsEvents.REMINDER_ENABLED)` 호출 후 기존 스케줄 로직 |

### 4.5 notification_opened

| 위치 | 파일 | 동작 |
|------|------|------|
| 알림 Intent | `notification/NotificationHelper.kt` | `showReminderNotification`·`showAllDoneNotification` 공통: MainActivity로 가는 Intent에 `putExtra("from_notification", true)` 추가 |
| MainActivity.onCreate | `MainActivity.kt` | `intent?.getBooleanExtra("from_notification", false) == true`이면 `app.analytics.logEvent(AnalyticsEvents.NOTIFICATION_OPENED)` 호출 후 `intent?.removeExtra("from_notification")` (재사용 시 중복 로깅 방지) |

### 4.6 widget_task_created

| 위치 | 파일 | 동작 |
|------|------|------|
| HomeViewModel.addTodo | `ui/home/HomeViewModel.kt` | 시그니처에 `fromWidget: Boolean = false` 추가. `fromWidget == true`일 때 `analytics.logEvent(AnalyticsEvents.WIDGET_TASK_CREATED)` 호출 (task_created와 별도로) |
| HomeScreen onSubmit | `ui/home/HomeScreen.kt` | `viewModel.addTodo(newTitle.trim(), newImportant, fromWidget = openAddOnStart)` — 위젯에서 "할 일 추가"로 연 경우 `openAddOnStart == true`이므로 해당 세션에서 추가하는 할 일마다 widget_task_created 로깅 |

- NavGraph에서 `openAddFromWidget`을 Home의 `openAddOnStart`로 전달하는 기존 구조 활용.

---

## 5. 데이터 흐름 요약

```
[할 일 추가]
  사용자 입력 후 제출 → HomeViewModel.addTodo(..., fromWidget)
    → repository.addTodo() → analytics.logEvent(TASK_CREATED)
    → fromWidget == true 이면 analytics.logEvent(WIDGET_TASK_CREATED)

[스와이프 완료/삭제]
  HomeTodoItem 스와이프 완료/삭제 → onAfterComplete / onAfterDelete
    → analytics.logEvent(TASK_SWIPE_COMPLETED | TASK_SWIPE_DELETED)
    → showUndoSnackbar(..., onUndoFired = { analytics.logEvent(UNDO_CLICKED) })

[설정 알림 ON]
  setRemindersEnabled(true) → analytics.logEvent(REMINDER_ENABLED) → scheduleReminderWork()

[알림 탭]
  NotificationHelper Intent putExtra("from_notification", true)
    → MainActivity.onCreate()에서 감지 → analytics.logEvent(NOTIFICATION_OPENED) → removeExtra

[first_open]
  Firebase SDK 자동 수집 (별도 코드 없음)
```

---

## 6. 수정/추가 파일 목록

| 구분 | 파일 | 내용 |
|------|------|------|
| 의존성 | `gradle/libs.versions.toml` | firebaseBom, googleServices, firebase-bom, firebase-analytics-ktx, google-services 플러그인 |
| 의존성 | `build.gradle.kts` | google-services 플러그인 apply false |
| 의존성 | `app/build.gradle.kts` | google-services 플러그인, firebase-bom, firebase-analytics-ktx |
| 설정 | `app/google-services.json` | debug 패키지 `com.naze.do_swipe.debug` 클라이언트 추가 |
| 신규 | `analytics/Analytics.kt` | Analytics 인터페이스, AnalyticsEvents 상수 |
| 신규 | `analytics/FirebaseAnalyticsTracker.kt` | Firebase Analytics 구현체 |
| 앱 | `TodoApplication.kt` | analytics 프로퍼티 (FirebaseAnalyticsTracker) |
| UI/로직 | `ui/home/HomeViewModel.kt` | addTodo(..., fromWidget), TASK_CREATED·WIDGET_TASK_CREATED 로깅 |
| UI/로직 | `ui/home/HomeScreen.kt` | TASK_SWIPE_* 로깅, onUndoFired 전달, addTodo(..., fromWidget = openAddOnStart) |
| UI/로직 | `ui/components/UndoSnackbar.kt` | onUndoFired 파라미터 추가, ActionPerformed 시 호출 |
| UI/로직 | `ui/archive/ArchiveScreen.kt` | showUndoSnackbar 호출 3곳에 onUndoFired 전달 |
| UI/로직 | `ui/settings/SettingsViewModel.kt` | setRemindersEnabled(true) 시 REMINDER_ENABLED 로깅 |
| 알림 | `notification/NotificationHelper.kt` | 알림 Intent에 from_notification extra 추가 |
| 진입점 | `MainActivity.kt` | from_notification 감지 시 NOTIFICATION_OPENED 로깅 후 extra 제거 |

---

## 7. 주의사항

- **first_open**: Firebase 예약 이벤트로, 앱 최초 설치·실행 시 SDK가 자동 수집. 커스텀 로직 없음.
- **디버그 빌드**: `google-services.json`에 `com.naze.do_swipe.debug` 클라이언트가 없으면 processDebugGoogleServices 실패. Firebase Console에서 디버그 패키지용 앱을 추가한 뒤 새 JSON으로 교체해도 됨.
- **이벤트 확인**: Firebase Console → Analytics → 이벤트에서 수집 여부 확인. 디버그 빌드는 DebugView에서 실시간 확인 가능.
- **테스트/비활성화**: `Analytics` 인터페이스를 no-op 구현체로 교체하면 모든 로깅을 끌 수 있음.
