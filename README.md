# SecureKeep 🛡️

**SecureKeep** is an advanced Android anti-theft and device security application designed to protect your smartphone against unauthorized access, theft, and tampering. Built with modern Android Jetpack components, Kotlin, and CameraX, SecureKeep monitors device sensors and triggers loud alarms, vibration, flashlight strobes, and automated email alerts.

---

## ✨ Features

### 📸 1. Intruder Alert & Selfie Capture
- **Failed Unlock Tracking**: Integrates with Android's Device Administration API to monitor unauthorized unlock attempts.
- **Headless CameraX Capture**: Secretly captures a front-facing photo of anyone attempting to break into your phone.
- **Automated Email Dispatch**: Sends an instant email notification with the intruder's photo and timestamp attached via background SMTP.
- **Secure Gallery**: In-app encrypted viewing gallery to review and manage captured intruder selfies.

### 📱 2. Don't Touch My Phone (Motion Detection)
- Utilizes the device's accelerometer to sense unexpected movement or relocation.
- Arm the alarm with a 10-second countdown; any movement triggers an instant siren.

### 👖 3. Anti-Pocket Detection
- Employs proximity and ambient light sensors to detect when the phone is slipped out of a pocket or bag in crowded places.

### 🔌 4. Charging Disconnect Detection
- Protects your phone when charging in public spaces (airports, cafes, offices).
- Sounds an alarm the moment the charging cable is unplugged.

### 📶 5. Wi-Fi Tamper Detection
- Triggers an alarm if someone attempts to enable or disable Wi-Fi without authorization while the device is locked.

### 🔋 6. Avoid Overcharging
- Alerts you when the battery reaches 100% full capacity, preventing battery degradation and overcharging.

### 🎧 7. Earphones Detection
- Monitors both 3.5mm wired headphones and Bluetooth connected earbuds.
- Sounds an alarm if earphones are unplugged or disconnected.

### 🔒 8. PIN Security & Customization
- **Master PIN Protection**: Alarms can only be deactivated by entering your secure PIN.
- **Customizable Alerts**: Toggle vibration, flashlight strobing, and select between internal siren tunes or system alarm sounds.

---

## 🛠️ Architecture & Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin (Java 17 compatibility) |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target / Compile SDK** | Android 15 / 16 (API 37) |
| **Build System** | Gradle 9.8.2 with Kotlin DSL (`build.gradle.kts`) & Version Catalog (`libs.versions.toml`) |
| **Camera Framework** | AndroidX CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle` 1.4.1) |
| **Service Layer** | AndroidX `LifecycleService` 2.8.7 with specialized foreground service types |
| **Permissions** | AndroidX Activity Result API (`ActivityResultLauncher`) |
| **Storage Architecture** | App-specific external storage (`getExternalFilesDir(Environment.DIRECTORY_PICTURES)`), requiring **zero** runtime storage permissions |
| **Email Dispatch** | JavaMail (`com.sun.mail:android-mail`) via Gmail SMTP in background coroutines |
| **UI Components** | ViewBinding, Material Components, ConstraintLayout, Glide |

---

## 📂 Project Structure

```
app/src/main/java/com/example/securekeep/
├── MainActivity.kt               # Main dashboard & module launcher
├── ApplicationClass.kt           # Application base class
├── AboutActivity.kt              # App info & Device Admin policies
├── SplashScreen.kt               # Splash screen
├── MyPreferences.kt              # Shared preference utilities
├── homeRCV/                      # Dashboard cards & RecyclerView adapter
├── alarmSetup/                   # Core alarm service, audio playback, PIN entry
│   ├── AlarmService.kt
│   └── EnterPinActivity.kt
├── intruderDetection/            # Intruder alert, CameraX selfie capture & email dispatch
│   ├── IntruderActivity.kt       # Sensitivity, threshold, and feature toggles
│   ├── IntruderSelfieActivity.kt # Intruder selfie gallery viewer
│   ├── PermissionActivity.kt     # Camera and overlay permission launcher
│   ├── intruderAdapter/          # Gallery adapter and model
│   └── intruderServices/
│       ├── IntruderTrackingService.kt # DeviceAdmin failed attempt receiver
│       ├── MagicServiceClass.kt       # CameraX headless photo capture service
│       └── MyDeviceAdminReceiver.kt   # Device Admin receiver
├── antiPocket/                   # Proximity and light sensor detection
├── chargingDetect/               # Power disconnect broadcast detection
├── earphoneDetection/            # Headphone & Bluetooth disconnect detection
├── overChargeDetection/          # Battery level broadcast detection
├── touchDetection/               # Accelerometer motion detection
├── wifiDetection/                # Wi-Fi network state detection
└── settings/                     # PIN management, email settings, and siren tones
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2+) or newer.
- **JDK**: Java 17+ (Android Studio's bundled JBR recommended).
- **Physical Device or Emulator**: Android 8.0 (API 26) or higher. *(Physical device recommended for camera and hardware sensor testing)*.

### Setup & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/SecureKeep.git
   cd SecureKeep
   ```

2. **Configure Java Environment**:
   Ensure `$env:JAVA_HOME` points to your JDK 17+ or Android Studio JBR:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   ```

3. **Compile and Assemble**:
   ```powershell
   # Compile Kotlin sources
   .\gradlew compileDebugKotlin

   # Build debug APK
   .\gradlew assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

4. **Run Unit Tests**:
   ```powershell
   .\gradlew test
   ```

---

## 🔐 Permissions & Privacy

SecureKeep adheres strictly to modern Android security and privacy guidelines:
- **No Scoped Storage Permissions**: The app does **not** request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `READ_MEDIA_IMAGES`. All photos are stored securely in app-isolated directories.
- **Camera Access**: Used exclusively to take a front-facing photo upon failed unlock attempts when Intruder Alert is armed.
- **Overlay & Device Admin**: Needed to display unlock PIN screens over the lock screen and detect consecutive failed device unlock attempts.
- **Modern Launchers**: All runtime permission requests utilize `registerForActivityResult()`.

---

## 📄 License

This project is licensed under the terms of the MIT License.

