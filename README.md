# Trip Expenditure

Trip Expenditure is an Android travel and expense companion built for organizing a trip, recording deposits by traveler, splitting shared expenses, tracking itinerary stops and travel documents, and exporting detailed reports.

The app uses a Clean MVVM structure, Jetpack Compose, and a fully on-device Room/SQLite database. It requires no backend service or cloud account for its core workflow.

**Package / application ID:** `com.tripexpenditure.trpxvq`  
**Namespace:** `com.example`  
**Minimum Android:** API 24 (Android 7.0)  
**Compile / target SDK:** API 36  
**Current version:** 1.1 (`versionCode` 2)

## Features

### Trip planning
- Create and delete trips
- Store destination, date range, base currency, and traveler names
- Select an active trip and persist it across app launches
- Track itinerary stops by day, with GPS coordinates, visited status, and traveler votes
- Save offline travel documents such as booking references, vouchers, and rail passes
- Add custom map waypoints

### Deposits and travelers
- Add travelers with name, phone, email, and role or note
- Record deposits by traveler name
- View each traveler’s deposited amount and remaining balance
- Delete individual traveler records or remove traveler references by name

### Expense splitting
- Record expenses with payer, amount, currency, category, date, and participants
- Support equal, exact, and percentage split modes
- Normalize expenses to the app-wide base currency
- Calculate balances and suggested settlements
- Mark settlements and delete expenses

### Reporting and backup
- Generate detailed PDF reports
- Generate UTF-8 CSV reports
- Save or share exported reports through Android system apps
- Export and import a compressed whole-app backup archive

### Settings and themes
- Choose an application currency: INR, USD, EUR, GBP, JPY, AUD, CAD, CHF, AED, or SGD
- Persist the active currency, theme, and active trip in Room
- Switch live between three themes:
  - **Modern Horizon** — abyssal navy, slate harbor, coral, and lagoon cyan
  - **Minimalist Nordic** — alabaster, white, pine green, and terracotta
  - **High-Energy Pulse** — obsidian, indigo, cyber lime, and neon magenta

## Architecture

The app follows a clean MVVM-style dependency flow:

```text
UI (Compose screens and components)
  -> TripExpenditureViewModel (StateFlow UI state and actions)
  -> TripRepository (domain-facing Room operations)
  -> Room DAO / SQLite entities

Domain helpers
  -> ExpenseCalculator
  -> TripReportExporter
  -> TripBackupManager
  -> TripDateUtils
```

- `MainActivity.kt` owns the Compose entry point, bottom navigation, dialogs, document pickers, PDF/CSV sharing, and backup import/export launchers.
- `TripExpenditureViewModel` exposes one reactive UI state through `StateFlow` and coordinates repository operations.
- `TripRepository` adapts Room data for the UI and domain layers.
- Room entities represent trips, itinerary stops, travelers, deposits, expenses, documents, map pins, and application settings.
- `ExpenseCalculator` handles split calculations and settlement recommendations.
- `TripReportExporter` produces PDF and CSV reports and shares cached export files.
- `TripBackupManager` exports and imports compressed backup archives.

## Project structure

```text
app/
  src/
    main/
      java/com/example/
        MainActivity.kt                 Compose entry point and app navigation
        data/
          local/                        Room entities, DAO, and database
          repository/                   TripRepository
        domain/                         Calculation, date, export, and backup helpers
        ui/
          components/                   Compose modals and reusable UI components
          screens/                      Dashboard, trip overview, and settings
          theme/                        Theme palettes and design tokens
          viewmodel/                    StateFlow MVVM state and actions
      res/                              Android resources, launcher icons, fonts, XML
    test/                               JVM and Robolectric tests
    androidTest/                        Android instrumentation tests
  build.gradle.kts                      App build, signing, dependencies, APK sync

docs/
  00_Baseline_State.md                  Architecture and database baseline
  changes/delta_spec.md                 v2 feature and behavior specification

gradle/
  libs.versions.toml                    Dependency and SDK version catalog

CLAUDE.md                               Engineering architecture and protected-file notes
metadata.json                           App description and capability metadata
```

## Technology stack

- Kotlin 2.2.10
- Android Gradle Plugin 9.1.1
- Jetpack Compose with Material 3
- Compose BOM 2024.09.00
- AndroidX Lifecycle / ViewModel Compose
- Room 2.7.0 and KSP 2.3.5
- Kotlin Coroutines 1.10.2
- Java 11 source and target compatibility
- JUnit, AndroidX Test, Espresso, and Robolectric
- Android `FileProvider`, Storage Access Framework, and system document pickers

The project does not currently use navigation-compose, Retrofit, Firebase, or external network APIs in the active app dependency set. Firebase and other optional dependencies remain commented in the Gradle configuration for future use.

## Database

All application data is stored locally using Room SQLite.

| Entity | Purpose |
|---|---|
| `TripEntity` | Trip title, destination, dates, currency, and traveler list |
| `ItineraryStopEntity` | Daily itinerary stops, coordinates, vote list, and visited state |
| `TravelerEntity` | Traveler name and contact/role details |
| `DepositEntity` | Deposits associated with traveler names |
| `ExpenseEntity` | Currency, payer, participants, split mode, and normalized amount |
| `TravelDocumentEntity` | Booking references, vouchers, and travel documents |
| `CustomMapPinEntity` | Offline custom map waypoints |
| `AppSettingsEntity` | Application currency, active theme, and active trip |

Room indexes child records by `tripId` so trip data can be queried efficiently and removed together when a trip is deleted.

## Build and run

### Requirements

- Android Studio with a compatible Android SDK
- JDK 11 or newer
- Android device or emulator running API 24 or later

Open the repository as a Gradle project, allow dependency synchronization, and run the `app` configuration.

### Command line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests
./gradlew connectedDebugAndroidTest
```

On successful debug/release builds, the Gradle configuration syncs a single versioned APK into:

```text
apk/TripExpenditure-v1.1.apk
```

The APK directory is not present until a build creates it.

## Signing configuration

The project has protected signing configuration in `app/build.gradle.kts`.

- Debug builds use `debug.keystore`.
- Release builds use `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables when available; they fall back to `my-upload-key.jks`.
- Do not modify signing configurations or replace `debug.keystore` files without explicit approval.

Core SDK, Kotlin, AGP, KSP, and version-pinned dependencies are defined in `gradle/libs.versions.toml` and should be changed only intentionally.

## Testing

The test sets include:

- JVM unit tests for domain and UI behavior
- Robolectric tests for Android-aware behavior
- Compose UI test dependencies
- Android instrumentation tests

Run the JVM suite first during development:

```bash
./gradlew testDebugUnitTest
```

## Engineering notes

- Keep app data and UI state local unless a feature is deliberately expanded to use a remote service.
- Preserve the package namespace and application ID when making package or build changes.
- Follow `CLAUDE.md` and `docs/00_Baseline_State.md` before architectural edits.
- Protected signing and version configuration must not be changed casually.
- A settings change or theme switch should remain persisted in Room rather than being held only in transient UI state.
