# CameraX & Headless Camera Services

These rules govern camera capture implementation in background/foreground services.

## 1. Lifecycle Management
- **LifecycleService Base**: Any Service using Jetpack CameraX must extend `androidx.lifecycle.LifecycleService` to supply a valid `LifecycleOwner` for use-case binding.
- **Manifest Type**: The service entry in `AndroidManifest.xml` must declare `android:foregroundServiceType="camera"`.
- **Target SDK 34+ Type Passing**: When starting foreground on API 34+, specify the camera foreground service type:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
  } else {
      startForeground(NOTIFICATION_ID, notification)
  }
  ```

## 2. Capture Configuration
- Use `CameraSelector.DEFAULT_FRONT_CAMERA` for intruder selfie capture.
- Configure `ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()` to ensure minimal shutter latency.
- Save directly to target file using `ImageCapture.OutputFileOptions.Builder(file).build()`.
- Unbind previous use-cases before binding: `cameraProvider.unbindAll()`.

## 3. Teardown & Lifecycle
- Every code branch (success, failure, exception) must trigger `stopSelf()`.
- If an asynchronous operation (e.g. email sending via coroutines) follows image capture, invoke `stopSelf()` within a `finally` block to avoid premature service termination.

