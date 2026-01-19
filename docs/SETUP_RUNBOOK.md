# Setup Runbook

## Environment requirements
- JDK 17 (source/target compatibility and Kotlin `jvmTarget` are set to 17); evidence: `app/build.gradle.kts` `compileOptions` and `kotlinOptions`.
- Android SDK with API level 35 for compile/target; evidence: `app/build.gradle.kts` `compileSdk = 35` and `targetSdk = 35`.
- Gradle 8.9 via wrapper; evidence: `gradle/wrapper/gradle-wrapper.properties` `distributionUrl`.

## Required environment variables
- `ANDROID_SDK_ROOT` or `ANDROID_HOME` pointing to the Android SDK; evidence: Android builds rely on SDK and a `local.properties` file exists at `local.properties`.
- `JAVA_HOME` pointing to a JDK 17 install; evidence: `app/build.gradle.kts` uses Java 17 compatibility.

## Clean machine setup steps
- Install JDK 17 and Android Studio, then install Android SDK Platform 35 and Build Tools.
- Create `local.properties` with `sdk.dir=...` (Android Studio can generate this on first sync); evidence: `local.properties` is present in repo root and used by Android Gradle tooling.
- Sync and build: `./gradlew :app:assembleDebug`.
- Run tests: `./gradlew :app:test` (unit) and `./gradlew :app:connectedAndroidTest` (device/emulator).
- Launch the app from Android Studio or with `./gradlew :app:installDebug` on a connected device.

## Common setup failures and fixes
- SDK location error (`SDK location not found`): fix `local.properties` or set `ANDROID_SDK_ROOT`; evidence: Android Gradle plugin uses SDK per `app/build.gradle.kts` Android block.
- Java version mismatch: ensure `JAVA_HOME` points to JDK 17 because `compileOptions` and `kotlinOptions` target 17.
- KSP/Room compile errors: run `./gradlew clean` and re-sync; evidence: `app/build.gradle.kts` applies `com.google.devtools.ksp` and Room compiler.
- Instrumented tests fail to run: ensure a device/emulator is attached for `connectedAndroidTest`; evidence: tests live in `app/src/androidTest/`.
