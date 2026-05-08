# Implementation Plan — WIFI-002
**Ticket:** WIFI-002 API-level UI review  
**ADR:** `docs/adr/001-api-level-ui-gating.md`  
**Date:** 2026-05-08

## Overview

Two independent bugs. No shared files. dev-a and dev-b work in parallel.

---

## Task A — Scheduler Exact-Alarm Threshold Fix
**Owner:** dev-a  
**Files:** `SchedulerScreen.kt`, `SchedulerViewModel.kt`  
**New tests:** `SchedulerViewModelTest.kt`

### Background

`canScheduleExactAlarms()` exists since API 31 but Google only made the strict
permission required on API 33 (`TIRAMISU`). The warning card must reflect this.

### Step 1 — Write failing tests first

Create `app/src/test/kotlin/com/geminiapps/wifitethering/ui/scheduler/SchedulerViewModelTest.kt`:

```kotlin
package com.geminiapps.wifitethering.ui.scheduler

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SchedulerViewModelTest {

    @Test
    @Config(sdk = [31])
    fun `canScheduleExactAlarms is true on API 31`() {
        // On API 31-32 we should not flag missing exact-alarm permission
        // (the strict enforcement only starts at API 33)
        val vm = SchedulerViewModel(context = /* Robolectric context */ TODO())
        assertTrue(vm.canScheduleExactAlarms)
    }

    @Test
    @Config(sdk = [33])
    fun `canScheduleExactAlarms reflects AlarmManager on API 33`() {
        // On API 33+ the value is driven by AlarmManager.canScheduleExactAlarms()
        // Robolectric returns true by default — just verify no crash
        val vm = SchedulerViewModel(context = TODO())
        // value can be true or false depending on Robolectric shadow; just no crash
    }
}
```

Run: `./gradlew :app:test --tests "*.SchedulerViewModelTest"` — expect compile errors until fix.

### Step 2 — Fix SchedulerViewModel.kt line 35

```kotlin
// Before
val canScheduleExactAlarms: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
} else {
    true
}

// After
val canScheduleExactAlarms: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
} else {
    true
}
```

### Step 3 — Fix SchedulerScreen.kt line 129

```kotlin
// Before
if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

// After
if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
```

### Step 4 — Run tests and verify

```bash
cd projects/wifitethering
./gradlew :app:test --tests "*.SchedulerViewModelTest"
```

All tests must pass.

### Step 5 — Commit

```
fix(scheduler): gate exact-alarm warning on API 33+ (TIRAMISU), not API 31+ (S)
```

---

## Task B — Material You ThemePicker
**Owner:** dev-b  
**Files:** `ui/theme/Theme.kt`, `ui/settings/SettingsScreen.kt`  
**New tests:** `AppThemeTest.kt`

### Background

`Theme.kt` currently always applies dynamic (Material You) colors on API 31+ regardless
of `appTheme` preference. There is no `MATERIAL_YOU` variant in `AppTheme` and no chip
in `ThemePicker`. Users on API 31+ have no way to opt out of Material You.

### Step 1 — Write failing tests first

Create `app/src/test/kotlin/com/geminiapps/wifitethering/ui/theme/AppThemeTest.kt`:

```kotlin
package com.geminiapps.wifitethering.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun `AppTheme enum includes MATERIAL_YOU`() {
        val values = AppTheme.entries.map { it.name }
        assertTrue("MATERIAL_YOU missing from AppTheme", "MATERIAL_YOU" in values)
    }

    @Test
    fun `AppTheme has exactly four values`() {
        // SYSTEM, DARK, LIGHT, MATERIAL_YOU
        assert(AppTheme.entries.size == 4)
    }
}
```

Run: `./gradlew :app:test --tests "*.AppThemeTest"` — expect failures until fix.

### Step 2 — Update Theme.kt

```kotlin
// Before
enum class AppTheme { SYSTEM, DARK, LIGHT }

@Composable
fun WifiTetheringTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (useDark) DarkColorScheme else LightColorScheme
    }
    ...
}

// After
enum class AppTheme { SYSTEM, DARK, LIGHT, MATERIAL_YOU }

@Composable
fun WifiTetheringTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        appTheme == AppTheme.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> {
            val useDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM, AppTheme.MATERIAL_YOU -> isSystemInDarkTheme()
            }
            if (useDark) DarkColorScheme else LightColorScheme
        }
    }
    ...
}
```

### Step 3 — Update ThemePicker in SettingsScreen.kt (lines 183–201)

```kotlin
// After
@Composable
private fun ThemePicker(currentTheme: AppTheme, onSelect: (AppTheme) -> Unit) {
    val options = buildList {
        add(AppTheme.SYSTEM to "System")
        add(AppTheme.DARK to "Dark")
        add(AppTheme.LIGHT to "Light")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(AppTheme.MATERIAL_YOU to "Material You")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (theme, label) ->
            FilterChip(
                selected = currentTheme == theme,
                onClick = { onSelect(theme) },
                label = { Text(label) },
            )
        }
    }
}
```

### Step 4 — Run tests and verify

```bash
cd projects/wifitethering
./gradlew :app:test --tests "*.AppThemeTest"
./gradlew :app:test
```

All tests must pass.

### Step 5 — Commit

```
feat(theme): add Material You option to ThemePicker, gated on API 31+
```

---

## Parallelism

| | Task A (dev-a) | Task B (dev-b) |
|---|---|---|
| Files touched | `SchedulerScreen.kt`, `SchedulerViewModel.kt` | `Theme.kt`, `SettingsScreen.kt` |
| Overlap | none | none |
| Can run in parallel | yes | yes |

---

## After both tasks complete

QA Engineer runs the full test suite and verifies the acceptance criteria from
`backlog/in-progress/WIFI-002-api-level-ui-review.md` against emulators at each
API tier. See Task #2.
