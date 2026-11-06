---
name: build-and-verify
description: Use this skill to compile, assemble, and verify the SecureKeep project using the Gradle wrapper and Android Studio JBR.
---

# Build and Verify SecureKeep

This skill guides you through properly building and verifying the SecureKeep Android project.

## Environment Setup

The Gradle wrapper requires Java 17+. On Windows, point `JAVA_HOME` to Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Compilation Checks

### 1. Compile Kotlin Sources
Quickly check for syntax, type, and deprecation issues:
```powershell
.\gradlew compileDebugKotlin
```

### 2. Full Debug Assembly
Build the complete debug APK and verify all manifest merges, resource compilation, and dexing:
```powershell
.\gradlew assembleDebug
```

### 3. Run Unit Tests
Execute unit tests:
```powershell
.\gradlew test
```

## Verification Checklist

- [ ] `JAVA_HOME` points to valid JBR or JDK 17+.
- [ ] No compilation errors or unresolved references.
- [ ] No unstripped or missing libraries.
- [ ] Manifest merge succeeds with no attribute conflicts.

