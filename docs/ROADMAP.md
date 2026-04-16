# WiFi Tethering App — Modern Rewrite & Feature Expansion

## Context

The existing app (`com.geminiapps.wifitethering`) is a published Play Store utility with good ratings. It is a legacy Eclipse ADT project (Java, min SDK 8, target SDK 18, single Activity) that provides WiFi hotspot shortcuts with AdMob banner ads. The codebase is too outdated to extend safely.

**Goal:** Full modern rewrite keeping the same package name (preserving ratings), adding hotspot management, connected devices tracking, scheduling reminders, and a freemium monetization model (ads for free users, one-time purchase to remove ads + unlock premium features).

**Approach:** Full Kotlin + Compose rewrite, same package name `com.geminiapps.wifitethering`, min SDK 21.

---

## Technical Constraints

- **Programmatic hotspot toggle:** Blocked by Android on API 26+ for Play Store apps. Strategy:
  - API 21–25: reflection-based `setWifiApEnabled()` (works, older devices)
  - API 26+: deep link to system tethering settings + notification-based scheduling reminders
- **Connected devices:** Scan `/proc/net/arp` — works on all supported API levels, no special permissions needed
- **Scheduling:** WorkManager schedules notifications with a deep-link action button into system tethering settings

---

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + StateFlow |
| DI | Hilt |
| Local storage | Room (scheduling, session data) + DataStore (preferences) |
| Background work | WorkManager |
| Ads | AdMob (banner + interstitial) |
| Billing | Google Play Billing Library v6 (one-time purchase) |
| Min SDK | 21 (Android 5.0, ~97% device coverage) |
| Target SDK | 35 |
| Build | Gradle with version catalogs (libs.versions.toml) |

---

## Monetization Model

**Free tier:**
- Full core functionality (hotspot toggle / settings shortcut)
- Connected devices list
- AdMob banner on main screen
- Interstitial ad on app open (max once per session)

**Premium (one-time in-app purchase — removes ads + unlocks features):**
- Remove all ads
- Hotspot scheduling (WorkManager-based reminder notifications)
- Custom hotspot name/password editor
- Battery-aware auto-remind (notify when hotspot is on and battery drops below threshold)
- Data session tracking (session duration, rough usage from NetworkStatsManager)
- AMOLED dark theme + additional themes

---

## Screens & Navigation

Single-activity, Compose NavHost:

```
Home
├── Connected Devices (bottom sheet or screen)
├── Scheduler (premium gate)
├── Hotspot Config (premium gate)
└── Settings
    ├── Upgrade to Premium
    └── About / Rate
```

### Home Screen
- Hotspot status card (on/off + current SSID)
- Primary action button: toggle (API < 26) or "Open Tethering Settings" (API 26+)
- Connected devices count chip → taps open Devices screen
- Session timer (how long hotspot has been on)
- AdMob banner at bottom (hidden for premium)

### Connected Devices Screen
- List of devices from ARP scan: hostname/IP, MAC address
- Refresh button + auto-refresh every 30s while hotspot is active
- Empty state when no devices connected

### Scheduler Screen (Premium)
- Add/edit schedule: day(s) of week, time, action (remind to enable / remind to disable)
- Toggle each schedule on/off
- WorkManager jobs per schedule; notification fires at time with deep-link to tethering settings

### Hotspot Config Screen (Premium)
- Read current SSID and password (via `WifiManager.getWifiApConfiguration()` — API 21+, deprecated API 30 but still readable)
- Edit SSID / password (works API 21–25 via reflection; API 26+ shows guide to edit in system settings)
- Select band: 2.4GHz / 5GHz (API 23+)

### Settings Screen
- Premium upgrade card (shows when not purchased)
- Theme picker (system / dark / amoled) — amoled is premium
- Rate the app
- About / version

---

## Architecture

### Package structure
```
com.geminiapps.wifitethering
├── ui/
│   ├── home/          HomeScreen, HomeViewModel
│   ├── devices/       DevicesScreen, DevicesViewModel
│   ├── scheduler/     SchedulerScreen, SchedulerViewModel
│   ├── config/        HotspotConfigScreen, HotspotConfigViewModel
│   ├── settings/      SettingsScreen, SettingsViewModel
│   └── theme/         Theme, Colors, Typography
├── domain/
│   ├── HotspotManager.kt       (toggle, status, SSID/password read/write)
│   ├── DeviceScanner.kt        (ARP table parser)
│   ├── BillingManager.kt       (Play Billing wrapper)
│   └── SessionTracker.kt       (session start/stop, duration)
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── ScheduleDao.kt
│   │   └── SessionDao.kt
│   ├── model/
│   │   ├── Schedule.kt
│   │   └── ConnectedDevice.kt
│   └── PreferencesRepository.kt  (DataStore)
├── worker/
│   └── ScheduleNotificationWorker.kt
└── di/
    └── AppModule.kt
```

### Data flow
- ViewModels collect StateFlow from domain classes
- HotspotManager reads `WifiManager` + `/proc/net/arp`; emits status as Flow
- BillingManager exposes `isPremium: StateFlow<Boolean>` consumed by all ViewModels to gate premium UI
- Room stores Schedule entities; WorkManager reads them on device boot + after edits

---

## Assets to Reuse from Current Project

- App icons: `res/drawable-hdpi/ic_launcher.png`, `-mdpi`, `-xhdpi`, `-xxhdpi`
- Package name: `com.geminiapps.wifitethering`
- Existing AdMob App ID and Ad Unit IDs (update in new project)

---

## Required Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />        <!-- API 33+ -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />   <!-- reschedule WorkManager on reboot -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"
    android:maxSdkVersion="25" />   <!-- reflection toggle on older APIs only -->
```

---

## Verification Checklist

- [x] `./gradlew assembleDebug` compiles clean
- [ ] API 21 emulator: hotspot toggle via reflection works
- [ ] API 30+ device: "Open Tethering Settings" deep link works
- [ ] Physical device: connect second device, verify it appears in Connected Devices list
- [ ] Scheduler: set schedule 1 min from now, verify notification fires with settings shortcut
- [ ] Billing: use Play Billing test account to purchase premium; ads disappear, premium screens unlock
- [ ] AdMob: banner visible for free user, absent for premium user

---

## Before Publishing

- [ ] Replace test AdMob IDs in `res/values/strings.xml` with real IDs from AdMob console
- [ ] Replace `admob_app_id` with real App ID
- [ ] Set up `remove_ads_premium` in-app product in Play Console
- [ ] Test billing with Play Billing test accounts
- [ ] Request notification permission at runtime (Android 13+)

---

## Implementation Phases

### Phase 1 — Foundation ✓
- [x] Create new Gradle project (Kotlin DSL, version catalog, same package name)
- [x] Set up Compose, Hilt, Room, DataStore
- [x] Implement HotspotManager (status reading + API-level-aware toggle/deep-link)
- [x] Home screen: status card + action button + basic UI
- [x] Migrate app icons from old project

### Phase 2 — Connected Devices ✓
- [x] DeviceScanner: parse `/proc/net/arp`
- [x] Devices screen with auto-refresh every 30s

### Phase 3 — Monetization ✓
- [x] AdMob integration (banner on Home + interstitial on app open, once per session)
- [x] Play Billing Library v7 integration (one-time purchase product)
- [x] BillingManager + `isPremium` StateFlow
- [x] Premium gate UI on premium screens

### Phase 4 — Premium Features ✓
- [x] Hotspot Config screen (SSID/password read + conditional edit by API level)
- [x] Scheduler screen + ScheduleNotificationWorker (WorkManager)
- [x] Boot receiver to reschedule WorkManager jobs after reboot
- [x] Session tracking (session timer in HomeScreen)
- [x] AMOLED + extra themes (system/dark/light/amoled in theme picker)

### Phase 5 — Polish ✓
- [x] Settings screen (rate app, theme picker, premium upgrade card)
- [x] Upgrade / paywall card with feature list
- [ ] Update Play Store listing description to reflect new features
- [ ] Test on multiple API levels (21, 26, 30, 33+)
- [x] Runtime notification permission request (Android 13+)
