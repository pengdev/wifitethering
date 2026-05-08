---
id: WIFI-001
type: qa-report
date: 2026-05-08
result: PASS (automatable criteria) — MANUAL STEPS REQUIRED before release
---

# QA Report — WIFI-001

**Date:** 2026-05-08
**Result:** CONDITIONAL PASS — all automatable criteria pass; two criteria require human action before release

## Test Results

### Unit tests (`./gradlew test`)

Build: SUCCESSFUL

| Suite | Tests | Passed | Skipped | Failed |
|---|---|---|---|---|
| PurchaseHandlerTest | 7 | 7 | 0 | 0 |
| ProductionCredentialsTest | 4 | 1 | 3 | 0 |
| **Total** | **11** | **8** | **3** | **0** |

The 3 skipped tests in `ProductionCredentialsTest` are intentionally `@Ignore`-annotated AdMob credential checks. They are part of the manual release gate (see Acceptance Criteria below).

**Note:** MockK logs several Byte Buddy warnings about Java 24 not being fully supported. These are non-fatal — all 7 `PurchaseHandlerTest` cases pass despite the warnings. This is a known MockK/Byte Buddy version lag; no tests were skipped or failed because of it.

### Instrumentation tests (`./gradlew connectedAndroidTest`)

Not run — no device or emulator available. Noted in report.

## Coverage

JaCoCo task (`jacocoTestReport`) is not registered in this project's Gradle configuration. Coverage cannot be generated via `./gradlew jacocoTestReport`.

Coverage assessment based on test file audit:

| Module | Testable pure logic | Tests present | Assessment |
|---|---|---|---|
| `domain/billing/PurchaseHelpers.kt` | `determinePremiumStatus`, `filterUnacknowledged` | PurchaseHandlerTest (7 tests) | Full branch coverage of both functions |
| `config/ProductionCredentialsTest` (strings.xml) | `premium_product_id` resource | 1 test passing; 3 @Ignored pending AdMob | Billing ID covered; AdMob IDs blocked on manual step |
| All other modules (BillingManager, ViewModels, UI, Workers, DAO) | Android-context-dependent | None | Not coverable by unit tests without device; instrumentation tests needed |

The domain billing helpers — the only pure JVM-testable production code changed in this ticket — have complete test coverage. The remaining codebase requires instrumentation tests on a device.

## Acceptance Criteria

| # | Criterion | Result | Notes |
|---|---|---|---|
| 1 | App builds with `./gradlew :app:bundleRelease` without warnings about test credentials | PASS | Build SUCCESSFUL; `app-release.aab` present at `app/build/outputs/bundle/release/app-release.aab`. One pre-existing icon deprecation warning (unrelated to credentials). |
| 2 | Banner and interstitial ads load on a physical device or release-signed emulator | MANUAL REQUIRED | AdMob IDs still set to Google test publisher (`ca-app-pub-3940256099942544`). Three `@Ignore`d tests in `ProductionCredentialsTest` guard this gate. Must obtain real AdMob IDs, update `app/src/main/res/values/strings.xml`, remove `@Ignore`, and verify on a release build. |
| 3 | Purchasing the premium product succeeds for a license tester and unlocks premium features | MANUAL REQUIRED | `premium_product_id` is correctly set to `premium_monthly` (unit test passes). End-to-end purchase flow requires a Play Console license tester account — cannot be automated. |
| 4 | Purchasing is acknowledged (no refund loop); `isPremium` persists across restarts | PASS (code review) | `BillingManager.handlePurchases` calls `filterUnacknowledged` then `acknowledgePurchase` for each unacknowledged item (`BillingManager.kt:131-138`). `isPremium` is written to DataStore via `PreferencesRepository.setPremium` (`PreferencesRepository.kt:131-133`) backed by `booleanPreferencesKey("is_premium")`, which persists across restarts. Verified by code review; end-to-end verification requires a device (see criterion 3). |
| 5 | All existing unit tests pass: `./gradlew test` | PASS | 8 tests pass, 3 intentionally skipped. 0 failures. |
| 6 | Store listing copy reviewed and screenshots captured | PARTIAL | `docs/STORE_LISTING.md` is complete and accurate (API caveats noted, screenshot checklist present). Screenshots not yet captured — requires a release build on a device/emulator per instructions in STORE_LISTING.md. Feature graphic (1024x500) not confirmed. |
| 7 | QA report filed at `projects/wifitethering/docs/qa-WIFI-001.md` | PASS | This document. |

## Issues Found / Tests Added

No new tests added. No production bugs found.

**Manual actions blocking release (not QA failures — known pre-conditions):**

1. **AdMob IDs** — `app/src/main/res/values/strings.xml` lines 5–8: replace the three `ca-app-pub-3940256099942544` test IDs with real IDs from the AdMob console. Then remove `@Ignore` from the three AdMob tests in `ProductionCredentialsTest.kt` and re-run `./gradlew test` to confirm they pass.

2. **Screenshots and feature graphic** — Capture home screen (hotspot ON) and UpgradeBottomSheet on a release build. Generate a 1024x500 feature graphic. See `docs/STORE_LISTING.md` for the checklist.

3. **End-to-end billing** — Verify full upgrade flow with a Play Console license tester account before uploading to the internal track.

## Summary

All automatable criteria pass. The implementation is correct: `premium_product_id` is `premium_monthly`, purchases are acknowledged before `isPremium` is persisted, and the release bundle builds clean. Human review is needed to complete the AdMob ID wiring, screenshots, and license-tester billing verification before the release manager uploads to the Play Store internal track.
