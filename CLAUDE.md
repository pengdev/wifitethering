# Smart Hotspot Manager (wifitethering)

## Package Name
com.geminiapps.wifitethering

## Build
- minSdk: 21
- targetSdk: 35
- compileSdk: 35
- versionCode: 2
- versionName: 2.0.0
- Java: 17

## Architecture
MVVM + Clean Architecture, single-module. Hilt DI, Jetpack Compose UI, Room, DataStore, WorkManager.

## Key Dependencies
- Jetpack Compose BOM
- Navigation Compose
- Hilt + KSP
- Material 3 + Material Icons Extended
- Lifecycle ViewModel Compose
- Splash Screen API
- ZXing Core (QR code generation)
- Play Billing (in-app purchases)
- Google AdMob (banner + interstitial)
- WorkManager

## Signing
Keystore config is in `.claude/settings.local.json` (gitignored).

## Play Store
- Package: com.geminiapps.wifitethering
- Default release track: internal

## API Notes
See `docs/IMPROVEMENT_PLAN.md` for the API capability matrix. Hotspot toggle
works only on API < 26; all UI is gated via `DeviceCapabilities`.
