# Background & Detection Services Rules

These rules govern the implementation of background alert and sensor detection services in SecureKeep.

## 1. Foreground Service Compliance
- Any persistent monitoring service (motion, pocket, charger, wifi, earphone, battery) must run as a foreground service with an active notification.
- Appropriate foreground service types must be declared in `AndroidManifest.xml` and supplied to `startForeground()`:
  - `specialUse` for anti-theft trigger services
  - `connectedDevice` for earphone/Bluetooth detection
  - `mediaPlayback` for alarm audio services

## 2. Notification Channels
- Always ensure notification channels are created prior to calling `startForeground()`.
- Channel importance should match service visibility (`IMPORTANCE_LOW` for passive monitoring, `IMPORTANCE_HIGH` for alert triggers).

## 3. Resource Cleanup
- Sensor listeners (`SensorEventListener`), broadcast receivers (`BroadcastReceiver`), and audio players (`MediaPlayer`) must be unregistered and released in `onDestroy()`.
- Wake locks (`PowerManager.WakeLock`) must be acquired with timeouts and released safely.

