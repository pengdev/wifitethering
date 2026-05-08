# QA Report — WIFI-002
**Date:** 2026-05-08  **Result:** PASS

## Test Results
- Unit tests (debug): 41 passed, 0 failed, 3 skipped
- Unit tests (release): 41 passed, 0 failed, 3 skipped
- Instrumentation tests: not run — no emulator/device available
- Skipped tests: 3 `@Ignore`-annotated AdMob credential checks (intentional; require real AdMob IDs before release)

## Coverage
JaCoCo is not configured in the project Gradle build. The `jacocoTestReport` task does not exist; no XML/HTML coverage report was generated. Coverage was assessed qualitatively per-criterion (see below). Configuring JaCoCo is logged as a follow-up item — not a blocker for this ticket.

## Issues Found / Tests Added

**Test infrastructure fix:**
`ProductionCredentialsTest` was failing because `src/test/resources/values/strings.xml` was missing from the test classpath. The test uses `getResourceAsStream("values/strings.xml")`, which requires the file to be present as a test resource. The file was created at:

`app/src/test/resources/values/strings.xml`

This mirrors the content of `app/src/main/res/values/strings.xml` and is a test artifact, not a production code change. After this fix all 41 tests pass.

## Acceptance Criteria

| Criterion | PASS/FAIL | Notes |
|-----------|-----------|-------|
| Toggle button label is "Open Settings to Enable" on API 26+ when hotspot is off | PASS | `HomeScreen.kt:368` — `else` branch (canToggleProgrammatically=false) returns "Open Settings to Enable". `MainActionButtonLabelTest` (4 tests) covers all label combinations. |
| POST_NOTIFICATIONS requested at first hotspot-on on API 33+ | PASS | `HomeScreen.kt:80-82` gates the permission launcher on `Build.VERSION.SDK_INT >= TIRAMISU`. `HomeViewModelNotificationTest` (2 tests) + `DeviceCapabilitiesTest.needsNotificationPermission` (3 tests) cover the flag boundary. |
| Exact-alarm warning gated on TIRAMISU (API 33), not S (API 31) | PASS | `SchedulerScreen.kt:129` and `SchedulerViewModel.kt:35` both use `TIRAMISU`. `ExactAlarmThresholdTest` (4 tests) explicitly verifies absent on API 31 and 32, present on API 33. |
| Material You ThemePicker chip visible on API 31+ only | PASS | `SettingsScreen.kt:189` — chip added to options list only when `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (API 31). `AppThemeTest` (2 tests) confirms MATERIAL_YOU enum variant exists. |

## Visual Verification
No emulator or device was available. Instrumentation tests and screenshot capture were skipped. This is noted — the implementation was verified via code review and unit test coverage only.

## Follow-up Items (non-blocking)
1. Configure JaCoCo in `app/build.gradle.kts` to enable line coverage reporting in CI.
2. Remove the `// canScheduleExactAlarms tests will be added after dev-b completes B2` comment in `DeviceCapabilitiesTest.kt:119` — the functionality lives in `SchedulerViewModel` and is tested by `ExactAlarmThresholdTest`.
3. Replace AdMob test IDs and remove `@Ignore` from `ProductionCredentialsTest` before Play Store release (per inline instructions in that file).
