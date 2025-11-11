# Words! - Kotlin Multiplatform Port

This is a Kotlin Multiplatform port of the Words! game, targeting Android, iOS, and Desktop platforms.

## Building

```bash
# Build shared module
./gradlew :shared:build

# Run tests
./gradlew :shared:test

# Build Android app
./gradlew :androidApp:assembleDebug

# Run Desktop app
./gradlew :desktopApp:run

# Package Desktop app
./gradlew :desktopApp:packageDistributionForCurrentOS
```

## Dependencies

- Kotlin 2.0.21
- Kotlinx Coroutines 1.7.3
- Kotlinx Serialization 1.6.2
- Kotlinx DateTime 0.5.0
- Compose Multiplatform 1.6.0
- Android Gradle Plugin 8.2.0
- Material3 for Compose
- JDK 17 or higher
- Android SDK (for Android builds)
- Xcode (for iOS builds)
- Gradle 8.2+
