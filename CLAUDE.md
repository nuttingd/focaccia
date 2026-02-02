# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Focaccia is an Android app blocker that uses an accessibility service to intercept app launches. Users select which apps to block, then scan a physical NFC tag to temporarily unlock them for 30 minutes. Built with Kotlin, Jetpack Compose, and Material3.

## Build & Test Commands

```sh
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # signed release (needs keystore.properties)
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
./gradlew lintDebug              # lint checks
bash scripts/deploy.sh           # build, install, enable accessibility service, launch on device
```

Requires JDK 21 and Android SDK 36. Min SDK is 28.

## Architecture

Single-module Android app (`app/`) using MVVM with Jetpack Compose.

**Core components:**

- **AppBlockerAccessibilityService** — listens for `TYPE_WINDOW_STATE_CHANGED` events and redirects blocked apps to `BlockedActivity`. Throttles blocking to 1 event/second. Certain apps are always exempt: launcher, dialer, SystemUI, Settings, and Focaccia itself.
- **BlockedActivity** — full-screen overlay shown when a blocked app is intercepted. Supports NFC scanning to unlock.
- **UnlockCountdownService** — foreground service that manages the 30-minute unlock window with a persistent notification (updates every 15s, includes "Reblock" action).
- **AppListViewModel** — single `UiState` data class exposed as `StateFlow`; drives all UI state.
- **BlockedAppsRepository** — SharedPreferences-backed persistence (keys: `blocked_apps`, `blocking_enabled`, `nfc_tag_id`, `blocking_disabled_until`).
- **MainActivity** — Compose UI for app selection, NFC tag registration, and blocking toggle.

**Package:** `dev.nutting.focaccia`

## Key Patterns

- State flows through a single `UiState` data class in the ViewModel
- NFC uses foreground dispatch (registered in onResume, unregistered in onPause); tag IDs stored as hex strings
- Debug-only features (NFC registration button, simulate-tap buttons) are guarded with `BuildConfig.DEBUG`
- No DI framework; repository is a singleton accessed via `Application` context

## Release Pipeline

Uses semantic-release on the `main` branch with Conventional Commits. CI (GitHub Actions) builds a signed APK and creates a GitHub release. Version codes follow `MAJOR*10000 + MINOR*100 + PATCH` (managed by `scripts/set-version.sh`).