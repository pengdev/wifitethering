# Smart Hotspot Manager — Improvement Plan v2

> Reviewed 2026-04-22. This document captures all known issues, API constraints,
> and the phased plan to ship a production-quality release.

---

## API-Level Capability Matrix

The app's `minSdk` is **21**, but hotspot APIs changed dramatically across
versions. The current code branches at API 26 (Oreo) for toggle/config, but the
UI does **not** adapt — features that silently fail are still shown.

| Capability               | API 21-25                    | API 26-27              | API 28-32            | API 33+ (Tiramisu)         | API 34+ (Upside Down Cake) |
|--------------------------|------------------------------|------------------------|----------------------|----------------------------|----------------------------|
| Programmatic toggle      | ✅ reflection                | ❌ blocked             | ❌                   | ❌                         | ❌                         |
| Read SSID                | ✅ `getWifiApConfiguration`  | ⚠️ deprecated, may work| ⚠️ unreliable        | ❌ removed                 | ❌                         |
| Read password            | ✅ `preSharedKey`            | ⚠️ deprecated          | ⚠️ unreliable        | ❌ removed                 | ❌                         |
| Set SSID/password        | ✅ `setWifiApConfiguration`  | ❌ blocked             | ❌                   | ❌                         | ❌                         |
| AP state detection       | ✅ `getWifiApState()`        | ⚠️ reflection          | ⚠️ reflection        | ⚠️ reflection              | ⚠️ reflection              |
| AP state callback        | ❌                           | ✅ `SoftApCallback`    | ✅                   | ✅                         | ✅                         |
| Connected devices (ARP)  | ✅ `/proc/net/arp`           | ✅                     | ⚠️ restricted        | ❌ empty on most devices   | ❌ confirmed broken (Pixel 6 Pro, Android 16) |
| Connected devices (native)| ❌                          | ❌                     | ❌                   | ✅ `TetheringManager`      | ✅ `TetheredClient` list   |
| Quick Settings Tile      | ❌                           | ✅ `TileService`       | ✅                   | ✅                         | ✅ (PendingIntent required) |
| Data usage stats         | ❌                           | ✅ `NetworkStatsManager`| ✅                  | ✅ (needs permission)      | ✅                         |
| Hotspot state callback   | ❌                           | ❌                     | ✅ `onStarted/Stopped`| ✅                        | ✅                         |
| Exact alarms (Scheduler) | ✅                           | ✅                     | ✅                   | ⚠️ `SCHEDULE_EXACT_ALARM`  | ⚠️ needs permission        |
| POST_NOTIFICATIONS       | Not required                 | Not required           | Not required         | ✅ runtime permission      | ✅                         |
| Dynamic colors (M3)      | ❌                           | ❌                     | ❌                   | ✅ `DynamicColors`         | ✅                         |
| Predictive back gesture  | ❌                           | ❌                     | ❌                   | ✅ `OnBackInvokedCallback` | ✅                         |

---

## Current Feature Status (Honest Assessment)

| Feature                  | Free | Premium | Actual Status                                                           |
|--------------------------|------|---------|-------------------------------------------------------------------------|
| One-tap hotspot toggle   | ✅   | ✅      | Works API <26 only; API 26+ opens system settings (UI reflects this)   |
| Quick Settings Tile      | ✅   | ✅      | Working (fixed PendingIntent for API 34+)                               |
| Session timer            | ✅   | ✅      | Working                                                                 |
| Connected devices list   | ✅   | ✅      | Hidden on API 29+ — feature removed from unsupported devices            |
| QR code sharing          | ✅   | ✅      | Works on API <28; hidden on API 28+ where credentials are unreadable    |
| Banner ads               | ✅   | ❌      | Working (test IDs)                                                      |
| Interstitial ads         | ✅   | ❌      | Working (test IDs)                                                      |
| Data limit monitor       | ❌   | ✅      | Working — real usage via `TrafficStats` delta (no permission needed)    |
| Battery protector        | ❌   | ✅      | Working — checks every 15 min via WorkManager                           |
| Hotspot scheduler        | ❌   | ✅      | Working; surfaced on home screen via Schedules chip                     |
| Hotspot config (SSID)    | ❌   | ✅      | Hidden on API 26+ — feature removed where it cannot function            |
| Material You colors      | ✅   | ✅      | Applied on API 31+ (dynamic color scheme from wallpaper)                |
| Home screen widget       | ✅   | ✅      | Working                                                                 |
| Rating prompt            | ✅   | ✅      | Triggers after 5th hotspot session as a dialog                          |
| Upgrade flow             | —    | —       | Working — UpgradeBottomSheet with billing integration                   |

---

## Critical Issues (Must Fix Before Publishing)

~~1. **`onRequestUpgrade` is a no-op**~~ — Fixed (Phase 7.2)
~~2. **Connected devices is broken on API 29+**~~ — Fixed: hidden on unsupported devices (Phase 7.5)
~~3. **QR password is hardcoded**~~ — Fixed: QR removed on API 28+ (Phase 7.5)
~~4. **Data usage is fake**~~ — Fixed: real `TrafficStats` delta (Phase 7.5)
~~5. **Hotspot Config fails silently on API 26+**~~ — Fixed: hidden on unsupported devices (Phase 7.5)
~~6. **Glass theme built but unreachable**~~ — Resolved: AMOLED/Glass themes dropped entirely (Phase 7.4)
~~7. **No limit adjustment UI**~~ — Fixed: sliders added to HomeScreen (Phase 7.1)

**No critical issues remain.**

---

## Phase 7.1 — Fix the Fundamentals

Priority: **MUST DO before release**

### 7.1.1 Wire up billing/upgrade flow
- Replace the no-op `onRequestUpgrade` lambda in `HomeRoute` with navigation
  to a proper upgrade bottom sheet or dialog showing benefits + price
- Even if billing isn't ready, show a "Coming soon" or "Contact developer" CTA
- Files: `HomeScreen.kt`, `AppNavHost.kt`

### 7.1.2 Add limit adjustment sliders
- Add `Slider` composables inside `ManagementCard` for adjusting MB and %
- Wire to existing `onUpdateDataLimit` / `onUpdateBatteryLimit` callbacks
- Files: `HomeScreen.kt`

### 7.1.3 Expose Glass theme in ThemePicker
- Add `GLASS` to the theme options list in `SettingsScreen.kt`
- Gate behind `isPremium` alongside AMOLED
- Files: `SettingsScreen.kt`

### ~~7.1.4~~ Fix QR password — DONE
- On API <28: QR uses `HotspotManager.readPassword()` and generates correct WiFi QR
- On API 28+: QR button hidden entirely — credentials are unreadable without @SystemApi access
- Fixed QR format: proper `T:nopass` when no password, special-character escaping
- Files: `HomeScreen.kt`, `QrGenerator.kt`

### ~~7.1.5~~ Fix or remove fake data usage — DONE
- Replaced time-proxy with `TrafficStats.getTotalRxBytes() + getTotalTxBytes()` delta
- Baseline captured at hotspot-on and persisted to DataStore (so background worker can use it too)
- No permission required; works on all API levels; handles `UNSUPPORTED` (-1) gracefully
- Files: `HomeViewModel.kt`, `PreferencesRepository.kt`, `HotspotMonitoringWorker.kt`

### ~~7.1.6~~ Fix connected devices on modern Android — DONE
- `TetheringManager` and `SoftApCallback` are both `@SystemApi` — unavailable to regular apps
- Chosen approach: hide the feature entirely on API 29+ where `/proc/net/arp` is restricted
- "Devices" chip on home screen gated on `canScanConnectedDevices` capability flag
- Files: `DeviceScanner.kt`, `HomeScreen.kt`

---

## Phase 7.2 — Premium Conversion Funnel

Priority: **Revenue multiplier — do before aesthetic polish**

### 7.2.1 "Peek" premium features
- Show Scheduler and Config as visible-but-locked items in Settings with a
  lock icon and "Upgrade to unlock" CTA
- Don't hide them — let free users *see* what they're missing
- Files: `SettingsScreen.kt`

### 7.2.2 Contextual upgrade prompts
- When user turns hotspot on for the 3rd+ time, show a soft bottom sheet:
  "Did you know? Premium lets you auto-stop when battery is low"
- Track hotspot-on count in DataStore
- Files: `HomeScreen.kt`, `HomeViewModel.kt`, `PreferencesRepository.kt`

### 7.2.3 Free trial of Battery Protector
- Let users try Battery Protector for 3 sessions, then lock it
- Track trial count in DataStore
- Files: `PreferencesRepository.kt`, `HomeViewModel.kt`

### 7.2.4 Premium badge in status card
- Show a small "PRO" chip/badge when user is premium
- Reinforces the purchase and creates envy when seen in screenshots
- Files: `HomeScreen.kt`

---

## Phase 7.3 — UX Flow & API-Adaptive UI

Priority: **Polish — adds professional quality**

### ~~7.3.1~~ API-adaptive feature visibility — DONE

`DeviceCapabilities` data class in `HotspotManager` drives all UI gating:

```kotlin
data class DeviceCapabilities(
    val canToggleProgrammatically: Boolean,   // API < 26
    val canReadSsidAndPassword: Boolean,       // API < 28
    val canEditConfig: Boolean,               // API < 26
    val canScanConnectedDevices: Boolean,     // API < 29 (reliable ARP)
    val canUseTile: Boolean,                  // API >= 24
    val needsNotificationPermission: Boolean, // API >= 33
)
```

| Feature                  | Guarded by               | Behavior on unsupported device              |
|--------------------------|--------------------------|---------------------------------------------|
| Programmatic toggle      | `canToggleProgrammatically` | Button opens system settings instead      |
| QR code share button     | `canReadSsidAndPassword` | Hidden                                      |
| Connected Devices chip   | `canScanConnectedDevices`| Hidden                                      |
| Hotspot Configuration    | `canEditConfig`          | Hidden from Settings (both free and premium)|
| Exact alarm warning      | `canScheduleExactAlarms` | Warning banner shown in SchedulerScreen     |
| Notification permission  | `needsNotificationPermission` | Requested at runtime on API 33+        |

### 7.3.2 Conditional Smart Management
- Only show the management section when hotspot is ON or limits are actively enabled
- Files: `HomeScreen.kt`

### 7.3.3 Scrollable LazyColumn
- Convert home screen from `Column` to `LazyColumn` for better scroll/performance
- Files: `HomeScreen.kt`

### 7.3.4 Surface Scheduler on home screen
- Add a "Schedules" chip next to "View Connected Devices" — reduces 3-tap depth to 1
- Files: `HomeScreen.kt`, `AppNavHost.kt`

### 7.3.5 First-launch welcome sheet
- On first app open, show bottom sheet explaining 3 key features
- Track `hasSeenOnboarding` in DataStore
- Files: `HomeScreen.kt`, `PreferencesRepository.kt`

### 7.3.6 Better rating prompt timing
- Move rating to trigger after 5th successful hotspot session
- Show as a dialog instead of inline card (more visible, less cluttered)
- Files: `HomeScreen.kt`, `HomeViewModel.kt`

### 7.3.7 Active monitoring indicator
- When Battery/Data protection is running, show animated shield icon in TopAppBar
- Files: `HomeScreen.kt`

---

## Phase 7.4 — Theme

~~Priority: Delight — wow factor for screenshots and reviews~~

**Decision (2026-04-17): Removed AMOLED and Glass themes entirely.** The added
complexity (animated gradient, frosted card borders, CompositionLocal plumbing,
premium gating) was not worth the maintenance cost for a utility app. Theme
system is now System / Dark / Light only.

- `AppTheme` enum: `SYSTEM`, `DARK`, `LIGHT`
- `ThemePicker`: 3 plain FilterChip options, no premium gating
- `UpgradeBottomSheet`: removed "Exclusive Themes" feature row
- Removed: `MeshGradientBackground`, `LocalIsGlassTheme`, `glassBorder()`, AMOLED
  color overrides, all animation imports

~~7.4.1~~ ~~7.4.2~~ ~~7.4.3~~ — dropped

---

## Phase 7.5 — Modern Android API Upgrades

Priority: **High value — fixes real breakage and enables native-quality features on API 27-34+**

### ~~7.5.1~~ Replace reflection AP state with `SoftApCallback` — BLOCKED: @SystemApi

The current `getWifiApState()` via reflection is fragile and breaks silently on
some OEM builds. `WifiManager.registerSoftApCallback()` is the official API and
provides real-time state + connected client count without reflection.

- Register callback in `HotspotManager` on API 27+; fall back to reflection on older devices
- `SoftApCallback.onStateChanged()` replaces the polling loop for AP on/off state
- `SoftApCallback.onNumClientsChanged()` gives a live client count even when full
  device enumeration is unavailable (useful on API 29+ where ARP is restricted)
- Files: `HotspotManager.kt`, `HomeViewModel.kt`

### ~~7.5.2~~ Real connected devices via `TetheringManager` — BLOCKED: @SystemApi

`TetheringManager.registerTetheringEventCallback()` with
`TetheringEventCallback.onClientsChanged()` returns a `Collection<TetheredClient>`
containing each client's MAC address, hostname, and address type. This is the
only reliable, non-reflection path on Android 10+.

- On API 30+: use `TetheringManager` as primary scan method
- On API 29: try `ip neigh` command (existing fallback)
- On API <29: use `/proc/net/arp` (existing)
- Remove the `ScanUnavailableState` fallback; it should only show if API <30
  fallbacks genuinely fail, not as default behavior on modern devices
- Requires: `ACCESS_NETWORK_STATE` (already held); no new dangerous permissions
- Files: `DeviceScanner.kt`, `DevicesViewModel.kt`, `DevicesScreen.kt`

### ~~7.5.3~~ Real data usage — DONE

`NetworkStatsManager` requires `PACKAGE_USAGE_STATS` which users must grant
manually — impractical. Implemented `TrafficStats` delta instead:

- `TrafficStats.getTotalRxBytes() + getTotalTxBytes()` — public API, no permission
- Baseline captured at hotspot-on, persisted to DataStore (`traffic_session_baseline`)
- `HotspotMonitoringWorker` reads the same baseline to check data limit independently
- Handles `UNSUPPORTED` (-1) and device-reboot edge cases
- Files: `HomeViewModel.kt`, `PreferencesRepository.kt`, `HotspotMonitoringWorker.kt`

### ~~7.5.4~~ Material You dynamic colors (API 31+) — DONE

- `dynamicDarkColorScheme()`/`dynamicLightColorScheme()` applied in `Theme.kt` on API 31+
- Falls back to static `DarkColorScheme`/`LightColorScheme` on older devices
- Files: `Theme.kt`

### ~~7.5.5~~ Exact alarm permission guidance for Scheduler (API 33+) — DONE

- `SchedulerViewModel.canScheduleExactAlarms` checks `AlarmManager.canScheduleExactAlarms()` on API 31+
- `ExactAlarmWarning` card shown in `SchedulerScreen` when permission missing
- "Fix" button deep-links to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`
- Files: `SchedulerScreen.kt`, `SchedulerViewModel.kt`

### ~~7.5.6~~ Predictive back gesture (API 33+) — DONE

- `android:enableOnBackInvokedCallback="true"` added to `<application>` in manifest
- Compose `NavHost` handles back natively; no deprecated `onBackPressed()` usage
- Files: `AndroidManifest.xml`

---

## Execution Order

```
Phase 7.1 (Fundamentals)  ──►  Phase 7.2 (Revenue)  ──►  Phase 7.3 (UX/API)  ──►  Phase 7.4 (Polish)  ──►  Phase 7.5 (Modern APIs)
     │                              │                           │                                                │
     ├─ 7.1.1 Billing flow          ├─ 7.2.1 Peek features     ├─ 7.3.1 API-adaptive UI                        ├─ 7.5.1 SoftApCallback
     ├─ 7.1.2 Limit sliders         ├─ 7.2.2 Contextual upsell ├─ 7.3.2 Conditional mgmt                       ├─ 7.5.2 TetheringManager devices
     ├─ 7.1.3 ~~Glass theme~~       ├─ 7.2.3 Free trial        ├─ 7.3.3 LazyColumn                             ├─ 7.5.3 Real data usage
     ├─ 7.1.4 QR password           └─ 7.2.4 PRO badge         ├─ 7.3.4 Surface scheduler                      ├─ 7.5.4 Material You colors
     ├─ 7.1.5 Data usage                                        ├─ 7.3.5 Welcome sheet                          ├─ 7.5.5 Exact alarm permission
     └─ 7.1.6 Connected devices                                 ├─ 7.3.6 Rating prompt                          └─ 7.5.6 Predictive back gesture
                                                                 └─ 7.3.7 Monitor indicator
```

> **Quick wins first.** Phase 7.1 tasks are mostly low effort but fix real
> broken functionality. Completing them prevents 1-star reviews. Phase 7.2 is
> the revenue multiplier — do it before any aesthetic polish.
