# Smart Hotspot Manager — Improvement Plan v2

> Reviewed 2026-04-17. This document captures all known issues, API constraints,
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

| Feature                  | Free | Premium | Actual Status                                             |
|--------------------------|------|---------|-----------------------------------------------------------|
| One-tap hotspot toggle   | ✅   | ✅      | Works API <26 only; API 26+ opens system settings         |
| Quick Settings Tile      | ✅   | ✅      | Working (fixed PendingIntent for API 34+)                 |
| Session timer            | ✅   | ✅      | Working                                                   |
| Connected devices list   | ✅   | ✅      | **BROKEN on API 29+** — `/proc/net/arp` returns empty     |
| QR code sharing          | ✅   | ✅      | **Password is hardcoded** — QR won't actually connect     |
| Banner ads               | ✅   | ❌      | Working (test IDs)                                        |
| Interstitial ads         | ✅   | ❌      | Working (test IDs)                                        |
| Data limit monitor       | ❌   | ✅      | **Fake** — uses `elapsed * 0.05` time proxy               |
| Battery protector        | ❌   | ✅      | Working                                                   |
| Hotspot scheduler        | ❌   | ✅      | Working (but buried 3 taps deep, invisible to free users) |
| Hotspot config (SSID)    | ❌   | ✅      | **Only works API <26** — does nothing on modern phones    |
| AMOLED theme             | ❌   | ✅      | Working                                                   |
| Glass theme              | ❌   | ✅      | Built but **not exposed** in ThemePicker UI               |
| Home screen widget       | ✅   | ✅      | Working                                                   |
| Rating prompt            | ✅   | ✅      | Shows but positioned below fold on most screens           |
| `onRequestUpgrade`       | —    | —       | **No-op** — tapping does nothing                          |

---

## Critical Issues (Must Fix Before Publishing)

1. **`onRequestUpgrade` is a no-op** — the premium CTA does nothing
2. **Connected devices is broken on API 29+** — empty list on modern phones
3. **QR password is hardcoded** to `"Your_Password"`
4. **Data usage is fake** — grows linearly with time regardless of traffic
5. **Hotspot Config is sold as premium but fails silently on API 26+**
6. **Glass theme built but unreachable** — not in ThemePicker
7. **No limit adjustment UI** — sliders exist in ViewModel but not in UI

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

### 7.1.4 Fix QR password
- Use `HotspotManager.readPassword()` on API <28 where it works
- On API 28+: show a text input field in the QR sheet for user to type password
- Store last-used password in DataStore for convenience
- Files: `HomeScreen.kt`, `HomeViewModel.kt`, `PreferencesRepository.kt`

### 7.1.5 Fix or remove fake data usage
- Option A: Implement `NetworkStatsManager` on API 26+ for real usage data
  (requires `PACKAGE_USAGE_STATS` permission + user grant)
- Option B (simpler): Remove the data monitor entirely; replace with
  "Session duration" which is already accurate
- Option C: Keep as "Estimated usage" with clear disclaimer text
- Files: `HomeViewModel.kt`, `HomeScreen.kt`

### 7.1.6 Fix connected devices on modern Android
- `/proc/net/arp` is restricted starting API 29 (Android 10)
- Option A: Use `WifiManager.LocalOnlyHotspotCallback` (API 26+) or
  `TetheringManager.getConnectedDevices()` (API 30+, hidden API)
- Option B: Fall back to IP neighbor scanning via `ip neigh` command
- Option C: If hotspot is on but scan returns empty, show a helpful message:
  "Device detection is limited on Android 10+. Your hotspot is active and
  sharing your connection."
- Files: `DeviceScanner.kt`, `DevicesScreen.kt`

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

### 7.3.1 API-adaptive feature visibility (CRITICAL)

Add a `DeviceCapabilities` data class to `HotspotManager`:

```kotlin
data class DeviceCapabilities(
    val canToggleProgrammatically: Boolean,   // API < 26
    val canReadSsid: Boolean,                 // API < 28
    val canReadPassword: Boolean,             // API < 28
    val canEditConfig: Boolean,               // API < 26
    val canScanConnectedDevices: Boolean,     // API < 29 (reliable)
    val canUseTile: Boolean,                  // API >= 24
    val canUseNetworkStats: Boolean,          // API >= 26 + permission
    val needsNotificationPermission: Boolean, // API >= 33
    val needsExactAlarmPermission: Boolean,   // API >= 33
)
```

Then use it to drive the UI:

| Current Behavior                                       | Fixed Behavior                                                                     |
|--------------------------------------------------------|------------------------------------------------------------------------------------|
| "Hotspot Configuration" shown as premium on all devices | Only show on API <26. On API 26+ show "Open system hotspot settings" instead      |
| QR sheet always shows password field                   | On API 28+ show text field for user to enter password manually                     |
| Toggle button says "Turn On Hotspot" on all devices    | On API 26+ say "Open Hotspot Settings" to set correct user expectations            |
| Quick Settings Tile tip never shown                    | On API 24+ show one-time "Add our Quick Settings Tile for faster access" prompt    |
| Data Limit shows fake usage on all devices             | On API 26+ use `NetworkStatsManager` for real data; on API <26 show "Session time" |
| Connected Devices always shown                         | On API 29+ show disclaimer or hide the feature                                     |
| Scheduler uses exact alarms without checking           | On API 33+ check `canScheduleExactAlarms()` and guide user to grant permission     |

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

### 7.5.3 Real data usage via `NetworkStatsManager` (API 26+)

Replace the `elapsed * 0.05` time proxy with actual bytes from
`NetworkStatsManager.querySummaryForDevice()` scoped to the hotspot network
interface. This gives real uploaded/downloaded bytes for the session.

- On API 26+: query `NetworkStatsManager` using `ConnectivityManager` to get
  the active tethering network interface
- Requires: `READ_NETWORK_USAGE_HISTORY` (system) OR guide the user to grant
  `PACKAGE_USAGE_STATS` via Settings deeplink (`ACTION_USAGE_ACCESS_SETTINGS`)
- If permission not granted: show current "Estimated (time-based)" label with
  a tappable "Enable real usage stats" link that opens the system settings page
- On API <26: keep the time-based estimate with disclaimer
- Files: `HomeViewModel.kt`, `HotspotManager.kt`, `HomeScreen.kt`,
  `PreferencesRepository.kt`

### 7.5.4 Material You dynamic colors (API 31+)

One-line change in `MainActivity.onCreate()`:

```kotlin
DynamicColors.applyToActivityIfAvailable(this)
```

Makes the app adopt the user's wallpaper-derived color palette on Android 12+.
No additional theme work needed — Material3 handles token mapping automatically.
Add `com.google.android.material:material` dependency if not already present
(likely already transitive via Compose Material3).

- Gate with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` check inside the
  call or let `applyToActivityIfAvailable` handle it (it no-ops on older devices)
- Files: `MainActivity.kt`, `build.gradle.kts` (ensure material dep)

### 7.5.5 Exact alarm permission guidance for Scheduler (API 33+)

The scheduler uses `AlarmManager.setExact()` which requires
`SCHEDULE_EXACT_ALARM` permission on API 33+. Without it, alarms are
inexact and may fire up to 15 minutes late — silently.

- Before scheduling, check `AlarmManager.canScheduleExactAlarms()`
- If false on API 33+: show an inline warning in `SchedulerScreen` with a
  button that deep-links to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`
- Do not block the user from creating schedules — just warn them that timing
  may be approximate until permission is granted
- Files: `SchedulerScreen.kt`, `SchedulerViewModel.kt`

### 7.5.6 Predictive back gesture (API 33+)

Replace any `onBackPressed()` usage with `OnBackInvokedCallback` registered
via `OnBackInvokedDispatcher`. For Compose navigation, ensure
`NavHost` handles back correctly with `predictiveBackProgress` if using
`androidx.navigation:navigation-compose` 2.7+.

- Audit `MainActivity` and any `BackHandler` composables for deprecated patterns
- Add `android:enableOnBackInvokedCallback="true"` to `<application>` in manifest
- Files: `AndroidManifest.xml`, `MainActivity.kt`, any screen with `BackHandler`

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
