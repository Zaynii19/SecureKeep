---
name: add-security-feature
description: Use this skill when adding or refactoring an anti-theft or sensor detection module in SecureKeep.
---

# Adding a Security Feature to SecureKeep

This runbook details how to implement a new detection module following SecureKeep's architecture patterns.

## Architecture Pattern

Every detection feature in SecureKeep typically consists of:
1. **Activity**: A settings/toggle screen (`<Feature>Activity.kt`) with an activation countdown, power toggle button, and feature options (vibrate, flash, sound).
2. **Foreground Service**: Background monitor (`<Feature>DetectionService.kt`) listening to system events or sensors.
3. **Preferences**: Persistence via `SharedPreferences` (tracking alert state, vibration, flash, sensitivity).
4. **Dashboard Tile**: A card in `MainActivity.kt` grid via `homeRCV/RCVModel`.

## Step-by-Step Implementation

### Step 1: Create the Detection Service
- Inherit `android.app.Service` (or `androidx.lifecycle.LifecycleService` if using camera).
- Implement `startForegroundService()` with notification channel.
- Register system listeners (`BroadcastReceiver`, `SensorEventListener`, etc.).
- When an intrusion event triggers:
  - Check if alarm is already active.
  - Launch `EnterPinActivity` or start `AlarmService`.
- In `onDestroy()`, cleanly unregister all receivers and sensor listeners.

### Step 2: Register in `AndroidManifest.xml`
- Declare the `<service>` with appropriate `android:foregroundServiceType`:
  - `specialUse`, `camera`, `connectedDevice`, or `mediaPlayback`.
- Add necessary system `<uses-permission>` tags if required.

### Step 3: Implement Feature Activity
- Use ViewBinding: `val binding by lazy { Activity<Feature>Binding.inflate(layoutInflater) }`.
- Manage permissions using `registerForActivityResult(ActivityResultContracts.RequestPermission())`.
- Check if service is already running via `MainActivity.isServiceRunning(...)`.
- Include 10-second activation countdown dialog before starting the service.

### Step 4: Register in `MainActivity.kt`
- Add an entry in `initializeCategoryList()`:
  ```kotlin
  categoryList.add(RCVModel(R.drawable.icon_name, "Title", "Description"))
  ```
- Wire navigation in `navBar.setNavigationItemSelectedListener` and RecyclerView click listener.

### Step 5: Verification
- Verify with `./gradlew compileDebugKotlin` and `./gradlew assembleDebug`.

