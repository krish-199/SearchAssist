# F-Droid Build Configuration

This document contains information for F-Droid maintainers about building Search Assist.

## Build Requirements

- Gradle 8.x
- Android SDK with API level 36 (Android 15)
- Minimum SDK: API level 26 (Android 8.0)
- Kotlin 1.9+

## Build Instructions

The app uses a standard Gradle build process:

```bash
./gradlew assembleRelease
```

## Dependencies

All dependencies are managed through Gradle and are fetched from Maven Central and Google's Maven repository. No proprietary or non-free dependencies are used.

## Reproducible Builds

The app is configured for reproducible builds. To verify:
1. All dependencies use specific versions (no dynamic versions)
2. No timestamp or build-time variables are used
3. ProGuard configuration is deterministic

## Build Features

- ProGuard enabled for code optimization
- Resource shrinking enabled
- No native libraries
- No proprietary SDKs

## Notes for F-Droid

- The app requires accessibility service permission to function
- No analytics, tracking, or network requests
- Fully open source under MIT License
- No anti-features
