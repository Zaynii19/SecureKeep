# Android Permissions & Storage Rules

These rules govern all permission requests and file storage patterns within SecureKeep.

## 1. Permission Requests
- **Modern API Mandatory**: All runtime permission requests and activity results must use the AndroidX Activity Result API:
  - Single permission: `registerForActivityResult(ActivityResultContracts.RequestPermission())`
  - Multiple permissions: `registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`
  - Activity for result (e.g. system settings / overlay / device admin): `registerForActivityResult(ActivityResultContracts.StartActivityForResult())`
- **Deprecated APIs Forbidden**: Never use `ActivityCompat.requestPermissions()`, `onRequestPermissionsResult()`, or `startActivityForResult()`.

## 2. Storage Guidelines
- **No Scoped Storage Permissions**: Never declare or request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, or `READ_MEDIA_VISUAL_USER_SELECTED`.
- **App-Specific Storage**: All photos, logs, and temporary files must be saved in app-specific storage:
  - Images: `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`
  - Cache/temp files: `context.cacheDir`
- These directories require zero runtime storage permissions on Android 4.4+ (API 19+) through Android 15+ (API 35+).

