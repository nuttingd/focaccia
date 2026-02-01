<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="Focaccia icon" />
</p>

# Focaccia

Android app blocker that uses an accessibility service to intercept app launches.
Users select which apps to block, then scan a physical NFC tag to temporarily unlock them for 30 minutes.

## How it works

The accessibility service listens for `TYPE_WINDOW_STATE_CHANGED` events. When a blocked app's window appears, Focaccia immediately redirects to a blocking screen. Scanning a registered NFC tag starts a 30-minute countdown (shown as a foreground notification) during which all apps are accessible.

Certain apps are never blocked: the launcher, dialer, system settings, and Focaccia itself.

## Requirements

- JDK 17
- Android SDK 35

Min SDK is 28 (Android 9).

## Build

```sh
./gradlew assembleDebug
```

For a signed release build, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details, then:

```sh
./gradlew assembleRelease
```

## Test

```sh
./gradlew test                        # unit tests
./gradlew connectedAndroidTest        # instrumented tests (needs a device/emulator)
```

The E2E test requires the accessibility service to be enabled and blocking toggled on.

## Permissions

- `QUERY_ALL_PACKAGES` — enumerate installed apps
- `NFC` — read NFC tags
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — unlock countdown notification
- `POST_NOTIFICATIONS` — show the countdown
- `BIND_ACCESSIBILITY_SERVICE` — the core blocking mechanism
