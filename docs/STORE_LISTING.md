# Play Store Listing — Smart Hotspot Manager v2.0.0

## App Name
Tethering Shortcut

## Short Description
The simplest one-tap WiFi Hotspot toggle. Now with smart scheduling and usage monitoring.

## Full Description
**Take control of your mobile hotspot with Tethering Shortcut.**

Tired of digging through layers of settings just to share your internet? Tethering Shortcut
provides a beautiful, modern interface to manage your WiFi hotspot with a single tap.

Whether you're sharing data with your tablet, laptop, or a friend, our app makes it
effortless, efficient, and smart.

### Key Features
- **One-Tap Toggle:** Launch your hotspot instantly from a single button (API 21-25) or
  jump directly to system settings with one tap (API 26+, as required by Android).
- **Smart Scheduler:** Plan your hotspot usage. Set reminders to enable or disable your
  hotspot at specific times — perfect for commuters and work-from-home users.
- **Connected Devices List:** See which devices are sharing your data, with IP and MAC
  address details (available on Android 9 and below).
- **Session Tracking:** Monitor how long your hotspot has been active to manage battery
  and data usage.
- **Home Screen Widget & Quick-Settings Tile:** Toggle your hotspot without even opening
  the app.
- **Modern Design:** A clean Jetpack Compose Material 3 interface with multiple themes
  including Dark Mode, Glass, and dynamic Material You colors (Android 12+).

### Premium Features (One-Time Purchase)
- **Zero Ads:** Remove all banners and interruptions.
- **Data Cap Monitor:** Automatically stop the hotspot when a data limit is reached.
- **Battery Protector:** Stop the hotspot automatically when battery drops below a
  threshold.
- **Advanced Scheduler:** Unlimited scheduling reminders.
- **Glass Theme:** Animated frosted-glass premium look.
- **Custom Config:** Quick access to edit your Hotspot SSID and password (Android 7
  and below only, where the API permits this).

### Why Tethering Shortcut?
Most phones bury tethering three or four menus deep. This app was built to solve that
one problem reliably. It's lightweight, respects your privacy, and requests only the
permissions it actually uses.

*Note: On Android 8.0 (Oreo) and above, direct programmatic hotspot control is blocked
by the OS. The one-tap button opens the system Wi-Fi hotspot settings page directly,
making it still the fastest path to your hotspot controls.*

---

## Content Rating
Everyone

## Category
Tools

## Package Name
com.geminiapps.wifitethering

## Version
2.0.0 (versionCode 2)

---

## Screenshots Required (MANUAL ACTION)

Minimum 2 phone screenshots required for submission. Capture using
`android screen capture --annotate` from an emulator or device running the
**release build** (not debug — ad banners are suppressed in debug).

| # | Screen | Notes |
|---|--------|-------|
| 1 | Home screen — hotspot ON state | Shows status card, PRO badge if premium, banner ad (free) |
| 2 | Premium upgrade sheet | UpgradeBottomSheet with feature list and purchase button |
| 3 (optional) | Scheduler screen | Add schedule dialog open |
| 4 (optional) | Settings / Theme picker | Glass theme selected |

Feature graphic: 1024x500 px — not yet generated.

---

## Reviewed
- [x] Description accurate against feature status in `docs/IMPROVEMENT_PLAN.md`
- [x] API limitations disclosed in copy (Oreo note, connected devices caveat)
- [x] Premium features list matches `UpgradeBottomSheet` feature list
- [ ] Screenshots captured (requires device/emulator — manual)
- [ ] Feature graphic generated (1024x500 — manual)
