# [009] 알림 시간 설정 및 미완료 할일 Notification

- **앱**: com.naze.do_swipe  
- **versionCode**: 4  
- **versionName**: 0.1.1  
- **대상 기능**: 설정의 알림 ON/OFF, 알림 시간 설정, 매일 지정 시각에 미완료 할 일 개수·목록 Notification

---

## 1. 개요

설정 화면의 **알림** 스위치를 켜고 **알림 시간**을 지정하면, 매일 해당 시각에 완료되지 않은 할 일의 개수와 목록을 알려주는 **Notification**이 표시된다.

- **스케줄링**: WorkManager `PeriodicWorkRequest(24h)` + `setInitialDelay(다음 지정 시각까지)` 로 매일 한 번 실행
- **알림 표시**: Worker 실행 시 DB에서 미완료 할 일 조회 → Notification 채널 생성 후 제목(개수), 본문(목록 일부) 표시, 탭 시 MainActivity 실행
- **설정 연동**: 알림 ON 시 Work 예약, OFF 시 취소, 시간 변경 시 재예약
- **권한**: Android 13(API 33)+ 에서 알림 ON 시 `POST_NOTIFICATIONS` 런타임 권한 요청

---

## 2. 설정 저장 (SettingsRepository)

### 2.1 파일 및 추가 API

**파일**: `app/src/main/java/com/naze/do_swipe/data/preferences/SettingsRepository.kt`

| 항목 | 타입/값 | 설명 |
|------|---------|------|
| reminderHour | StateFlow\<Int\> | 알림 시 (0–23), 기본 9 |
| reminderMinute | StateFlow\<Int\> | 알림 분 (0–59), 기본 0 |
| setReminderTime(hour, minute) | Unit | 시·분 저장 (0–23, 0–59로 coerced) |
| isRemindersEnabled() | Boolean | 알림 켜짐 여부 (동기 조회, Worker용) |
| getReminderHour() / getReminderMinute() | Int | 저장된 시·분 (동기 조회, ViewModel/Worker용) |

**SharedPreferences 키**: `reminder_hour`, `reminder_minute`

---

## 3. 설정 UI (SettingsScreen)

### 3.1 알림 카드 구성

- **첫 번째 행 (스위치)**
  - 제목: "알림"
  - 부제목: 알림 ON → `매일 HH:MM에 미완료 할 일을 알려줘요` / OFF → `알림을 끄면 미완료 할 일 알림이 오지 않아요`
  - 스위치 ON 시: 권한 요청(API 33+) 후 `setRemindersEnabled(true)` 호출, OFF 시 `setRemindersEnabled(false)`

- **두 번째 행 (알림 시간)**
  - 제목: "알림 시간"
  - 부제목: 알림 ON → `매일 HH:MM에 알림` / OFF → `알림을 켜면 시간을 설정할 수 있어요`
  - 아이콘: 알림 ON 시 Primary, OFF 시 TextSecondary
  - 클릭: 알림이 켜져 있을 때만 **ReminderTimePickerDialog** 표시

### 3.2 ReminderTimePickerDialog

- **TimeInput** 사용 (시·분 텍스트 필드 + 증감 버튼, 시계 다이얼 대신 입력 방식)
- `rememberTimePickerState(initialHour, initialMinute, is24Hour = true)` 로 상태 관리
- 확인 시 `viewModel.setReminderTime(state.hour, state.minute)` 호출

### 3.3 권한

- `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` 으로 `POST_NOTIFICATIONS` 요청
- 허용 시에만 `setRemindersEnabled(true)` 호출; 거부 시 스위치는 OFF 유지

---

## 4. 스케줄링 (SettingsViewModel)

### 4.1 WorkManager 예약/취소

- **알림 ON** (`setRemindersEnabled(true)`): `scheduleReminderWork()` 호출
- **알림 OFF**: `WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)` (WORK_NAME = `"todo_reminder"`)
- **시간 변경** (`setReminderTime`): 저장 후 알림이 켜져 있으면 `scheduleReminderWork()` 재호출

### 4.2 scheduleReminderWork()

- `PeriodicWorkRequestBuilder<TodoReminderWorker>(24, TimeUnit.HOURS)`
  - `.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)` — `delayMs` = 다음 (hour, minute) 시각까지 남은 시간
- `WorkManager.getInstance(app).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)`

### 4.3 nextTriggerDelayMillis(hour, minute)

- `Calendar` 로 오늘 (hour, minute) 시각 계산
- 이미 지났으면 내일 같은 시각으로 설정 후, 현재 시각과의 차이(ms) 반환

---

## 5. TodoReminderWorker

**파일**: `app/src/main/java/com/naze/do_swipe/worker/TodoReminderWorker.kt`

- **CoroutineWorker** 로 구현, `doWork()` 에서 `Dispatchers.IO` 로 실행
- `applicationContext as? TodoApplication` → `settingsRepository`, `repository` 사용
- `isRemindersEnabled() == false` 이면 알림 없이 `Result.success()` 반환
- `repository.getActiveTodosOnce()` 로 미완료 목록 조회 후 `NotificationHelper.showReminderNotification(context, count, titles)` 호출

---

## 6. NotificationHelper

**파일**: `app/src/main/java/com/naze/do_swipe/notification/NotificationHelper.kt`

### 6.1 채널

- **createReminderChannel(context)**: 채널 ID `todo_reminder`, 이름 "할 일 알림", IMPORTANCE_DEFAULT, 진동 사용

### 6.2 showReminderNotification(context, count, titles)

- **count == 0**: `showAllDoneNotification` — 제목 "모든 할 일을 마쳤어요", 본문 "오늘도 수고했어요!"
- **count >= 1**:
  - 제목: "할 일 1개가 남아 있어요" / "N개의 할 일이 남아 있어요"
  - 본문: titles 최대 5개까지 줄바꿈으로 표시, 초과 시 "외 N개" 추가, `BigTextStyle` 사용
- 클릭 시 `Intent` → MainActivity (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP), 고정 notification ID = 1

---

## 7. 데이터 계층 (미완료 일회성 조회)

### 7.1 TodoDao

- **getActiveTodosOnce()**: suspend, `getActiveTodos()` 와 동일 조건 (isCompleted=0, isDeleted=0, orderIndex·createdAt 정렬), `List<TodoEntity>` 반환

### 7.2 TodoRepository

- **getActiveTodosOnce()**: `dao.getActiveTodosOnce()` 위임

Worker에서 Flow 대신 한 번만 조회할 때 사용.

---

## 8. 의존성 및 권한

- **libs.versions.toml**: `workVersion = "2.9.0"`, `androidx-work-runtime-ktx`
- **app/build.gradle.kts**: `implementation(libs.androidx.work.runtime.ktx)`
- **AndroidManifest.xml**: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`

---

## 9. 데이터 흐름 요약

```
사용자: 알림 ON + 시간 09:00 설정
  → SettingsScreen: 권한 요청(API 33+), setRemindersEnabled(true) / setReminderTime(9, 0)
  → ViewModel: SettingsRepository 저장, scheduleReminderWork()
  → WorkManager: UniquePeriodicWork "todo_reminder" 등록 (initialDelay = 다음 09:00까지, 주기 24h)

매일 지정 시각에 Worker 실행
  → TodoReminderWorker.doWork(): isRemindersEnabled() 확인, getActiveTodosOnce() → NotificationHelper.showReminderNotification()
  → Notification 표시 (개수·목록), 탭 시 MainActivity
```

---

## 10. 수정/추가 파일 목록

| 구분 | 파일 | 내용 |
|------|------|------|
| 의존성 | `gradle/libs.versions.toml` | workVersion, androidx-work-runtime-ktx |
| 의존성 | `app/build.gradle.kts` | work-runtime-ktx |
| 권한 | `app/AndroidManifest.xml` | POST_NOTIFICATIONS |
| 설정 | `SettingsRepository.kt` | reminderHour/Minute, setReminderTime, isRemindersEnabled, getReminder* |
| 데이터 | `TodoDao.kt` | getActiveTodosOnce() |
| 데이터 | `TodoRepository.kt` | getActiveTodosOnce() |
| 신규 | `notification/NotificationHelper.kt` | 채널 생성, showReminderNotification, showAllDoneNotification |
| 신규 | `worker/TodoReminderWorker.kt` | doWork에서 미완료 조회 후 알림 표시 |
| 설정 UI | `SettingsViewModel.kt` | reminderHour/Minute, setReminderTime, scheduleReminderWork, nextTriggerDelayMillis |
| 설정 UI | `SettingsScreen.kt` | 알림 카드(스위치+알림 시간 행), ReminderTimePickerDialog(TimeInput), 권한 요청 |

---

## 11. 주의사항

- **미완료 0개**: 알림은 그대로 띄우며 "모든 할 일을 마쳤어요" 메시지 사용.
- **정확한 시각**: WorkManager는 배터리 최적화 등으로 수 분 단위 지연이 있을 수 있음. 정확한 시각이 필요하면 AlarmManager + setExactAndAllowWhileIdle 검토.
- **재부팅**: WorkManager가 주기 작업을 유지하므로 BOOT_COMPLETED 리시버는 별도로 두지 않음.
- **테스트**: 알림이 오는지 확인하려면 알림 시간을 현재 시각 1–2분 뒤로 설정하고, 앱을 백그라운드로 둔 뒤 대기. 기기 설정에서 앱 알림 허용 및 배터리 최적화 해제 여부 확인.
