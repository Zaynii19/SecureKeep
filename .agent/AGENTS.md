# SecureKeep - Agent Guidelines

SecureKeep is an Android anti-theft and device security application built with Kotlin, Android Jetpack, and CameraX.

---

## Architecture & Technology Stack

- **Target / Compile SDK**: Android 37 (`minSdk = 26`)
- **Language**: Kotlin with Java 17 compatibility
- **Build System**: Gradle 9.8.2 with Kotlin DSL (`build.gradle.kts`) and Version Catalog (`gradle/libs.versions.toml`)
- **Camera & Selfie**: Jetpack CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle` 1.4.1) running headlessly in `LifecycleService` (`androidx.lifecycle:lifecycle-service` 2.8.7)
- **Permissions**: AndroidX Activity Result API (`ActivityResultContracts.RequestPermission()`, `ActivityResultContracts.StartActivityForResult()`)
- **Storage**: App-specific external storage (`getExternalFilesDir(Environment.DIRECTORY_PICTURES)`), requiring no runtime storage permissions
- **Email Dispatch**: JavaMail via Gmail SMTP in background coroutines

---

## Build & Verification Commands

Always configure `$env:JAVA_HOME` pointing to Android Studio's JBR or a valid JDK 17+ environment:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Compile Kotlin classes
.\gradlew compileDebugKotlin

# Assemble debug APK
.\gradlew assembleDebug

# Run unit tests
.\gradlew test

# Clean project
.\gradlew clean
```

---

## Core Development Guidelines

1. **Permissions Architecture**:
   - Always use `registerForActivityResult(ActivityResultContracts.RequestPermission())` or `registerForActivityResult(ActivityResultContracts.StartActivityForResult())`.
   - Never use deprecated `ActivityCompat.requestPermissions()`, `onRequestPermissionsResult()`, or `startActivityForResult()`.
   - Do NOT declare or request scoped storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`). All captured assets must be stored in app-specific storage (`getExternalFilesDir(...)` or `cacheDir`).

2. **Camera & Foreground Services**:
   - Headless photo capture must run in a service extending `androidx.lifecycle.LifecycleService`.
   - Manifest entry must declare `android:foregroundServiceType="camera"`.
   - Use `ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()` for responsive intruder selfie capture.
   - Always ensure `stopSelf()` is called when operations finish (or in a coroutine `finally` block when sending emails).

3. **Git Hygiene**:
   - Do not stage or commit files without explicit user instructions.

