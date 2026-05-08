# 001. API-Level UI Gating Fixes
**Status:** Accepted  **Date:** 2026-05-08

## Context

WIFI-002 audit revealed two API-level gating bugs:

1. The exact-alarm warning card in `SchedulerScreen` fires on API 31+ (`VERSION_CODES.S`)
   but the ticket and capability matrix specify API 33+ (`TIRAMISU`) — the level where
   `POST_NOTIFICATIONS` and stricter alarm permission enforcement were introduced.

2. `Theme.kt` unconditionally applies Material You (dynamic) colors on API 31+ regardless
   of user preference. `SettingsScreen` has no user-visible swatch for Material You, so
   there is no way to opt in or out of it. The test matrix requires a dedicated chip
   visible only on API 31+.

## Options Considered

### Option A: Fix threshold constants only (minimal)
Change `S` → `TIRAMISU` in SchedulerScreen + SchedulerViewModel. Leave Material You
always-on for API 31+ (no user toggle).

- Pro: Tiny diff, zero risk to Material You behavior.
- Con: Leaves the missing Material You chip as an open gap in the test matrix.

### Option B: Fix thresholds + add Material You as explicit user preference (chosen)
Fix the alarm thresholds AND add `MATERIAL_YOU` to `AppTheme` with a gated FilterChip
in ThemePicker. Dynamic colors are applied only when `appTheme == MATERIAL_YOU`.

- Pro: Closes both bugs; gives users explicit control; test matrix fully satisfied.
- Con: Existing API 31+ users who relied on implicit Material You will default to the
  fixed dark/light scheme until they select the new chip — a one-time UX change.

### Option C: Keep Material You always-on, add chip as visual indicator only
Show the chip as always-selected on API 31+ with no real toggle.

- Con: Misleading UI; does not satisfy the acceptance criterion "no silent no-op".

## Decision

Option B. Both bugs are fixed in one pass with no shared files between the two changes,
enabling parallel implementation by two developer agents.

## Consequences

- **Positive:** Exact-alarm warning no longer shows on API 31–32. Material You is
  now an explicit user choice. Test matrix fully covered.
- **Negative:** API 31+ users upgrading will see their theme revert to System/Dark/Light
  (whichever was stored). Existing DataStore value `"SYSTEM"/"DARK"/"LIGHT"` remains
  valid; only new `"MATERIAL_YOU"` is added — no migration needed.
