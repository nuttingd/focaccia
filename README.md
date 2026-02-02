<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="Focaccia icon" />
</p>

# Focaccia

An NFC-powered app blocker for Android. Select which apps to block, then scan a physical NFC tag to temporarily unlock them for 30 minutes.

## How it works

An accessibility service listens for app launches. When a blocked app's window appears, Focaccia redirects to a blocking screen. Scanning a registered NFC tag starts a 30-minute unlock countdown (shown as a foreground notification) during which all apps are accessible. The launcher, dialer, system settings, and Focaccia itself are never blocked.

## Building

Requires JDK 21+ and Android SDK 36. Min SDK is 28 (Android 9).

```sh
./gradlew assembleDebug
```

For a signed release build, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details, then:

```sh
./gradlew assembleRelease
```

## Testing

```sh
./gradlew test
```

## License

[MIT](LICENSE)
