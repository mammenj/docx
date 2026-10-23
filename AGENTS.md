# AGENTS.md

## Project

Android app for doctors managing outpatients. Register a patient (name, hospital UID, age, sex, phone), record visits with symptoms and vitals (BP, glucose, pulse, temperature, SpO2, weight), and review visit history.

- Package: `com.example.outpatient`
- Module: `:app` only
- UI: Kotlin + Jetpack Compose + Material 3, Navigation Compose
- Data: Room, local-only; no backend and no network calls

## Build environment

- Android SDK: `~/Android/Sdk`. `local.properties` already points there and must stay untracked.
- No `java` or `gradle` on `PATH`. Use Android Studio's JDK:
  `export JAVA_HOME=/usr/local/android-studio/jbr`
- Gradle wrapper 9.6.0, AGP 9.4.1. Always use `./gradlew`; do not install or rely on a system Gradle.
- compileSdk/targetSdk 37, minSdk 26. Only platform `android-37.0` and build-tools `36.0.0` are installed.
- AGP 9 uses **built-in Kotlin**. Do not add the `org.jetbrains.kotlin.android` plugin; it conflicts. Kotlin is configured through the Compose compiler plugin and KSP.
- KSP must be `>= 2.3.1` with AGP 9. Older versions fail with `KSP is not compatible with Android Gradle Plugin's built-in Kotlin`. Current: Kotlin 2.3.21, KSP 2.3.12.

## Commands

```bash
export JAVA_HOME=/usr/local/android-studio/jbr

./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests "*VitalsValidatorTest"
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # needs emulator/device
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Before finishing a change: run the focused test, then `:app:testDebugUnitTest` and `:app:lintDebug`. Instrumented tests live in `app/src/androidTest` and are the slow path; they need a running device/emulator.

## Layout

- `app/src/main/java/com/example/outpatient/data/local` — Room entities' DAOs, converters, database
- `app/src/main/java/com/example/outpatient/data/repository` — single repository used by all ViewModels
- `app/src/main/java/com/example/outpatient/domain` — pure validation/unit logic (test these on the JVM)
- `app/src/main/java/com/example/outpatient/ui` — Compose screens, ViewModels, navigation, theme
- `app/schemas/...` — generated Room schema JSON; commit changes and write migrations
- `app/src/test` — JVM tests; `app/src/androidTest` — Room/Compose device tests

## Data model rules

- **UID is the patient identity.** `patients.uid` has a unique index. Never deduplicate or look up patients by name or phone.
- **Separate patients from visits.** One patient has many visits; each visit owns its timestamp, symptoms, vitals, and notes. Do not overwrite patient data with the latest visit.
- **Vitals are typed fields, not strings.** BP is systolic/diastolic integers in mmHg; glucose is a value plus an explicit unit; also pulse, temperature, SpO2, and weight.
- **Never silently convert glucose units.** `mg/dL` and `mmol/L` are stored distinctly; conversion requires an explicit target unit.
- **Preserve clinician-entered text verbatim.** Do not normalize, autocorrect, or replace symptoms/notes with a diagnosis.
- **Timestamps:** persist UTC epoch millis, format in the device timezone only for display.
- **No hard deletes of clinical records.** Soft-delete patients and keep the audit trail.

Room: commit exported schema JSON, write a migration for every schema change, and never enable destructive migration in a release build.

## Privacy

Patient data is PHI. Never put real patient data in logs, crash reports, analytics, commit messages, screenshots, fixtures, or test names. Use clearly fake names and UIDs in tests. Do not add network, analytics, or crash-reporting dependencies that could transmit record contents.

## Testing

- Test validators and glucose conversion as pure JVM tests in `app/src/test`.
- Test DAOs against an in-memory Room database (unique UID constraint, soft delete, patient-scoped visits).
- Never use production or realistic patient records as fixtures.

## General rules

- Don't commit any changes without approval.
- Create a branch when there is a major or temporary change.
