# Delta Specification — Trip Expenditure (v2 User Controls & Settings Update)

## 1. ADDED / ENHANCED CAPABILITIES
- **Deposit by Traveler Name (`DepositEntity`)**: Dedicated Room table and UI allowing users to record, view, and delete deposits by traveler name, with live per-traveler deposit totals and remaining trip pool calculations.
- **Travelers Details Add & Delete (`TravelerEntity`)**: Full traveler management (Name, Phone/Email, Note/Role) with individual Add and Delete actions per traveler.
- **Zero Pre-Filled Data**: Removed all initial seed trips/stops/expenses and cleared pre-populated text field values so all forms and lists start 100% clean.
- **Reboot-Persistent Settings (`AppSettingsEntity` + Synchronous Commit)**: Dedicated Settings screen and Room table storing Global Application Currency, Active Theme, and Active Trip ID so settings never reset on reboot.
- **Trip Delete Option**: Delete button with confirmation dialog on both the Dashboard trip cards and Trip Overview screen, removing the trip and all associated child records.
- **Global Application Currency Setting**: Application-wide currency selector in Settings (`INR`, `USD`, `EUR`, `GBP`, `JPY`, `AUD`, `CAD`, `CHF`, `AED`, `SGD`) that applies across the entire app.

## 2. MODIFIED
- `app/src/main/java/com/example/data/local/Entities.kt`
- `app/src/main/java/com/example/data/local/TripExpenditureDao.kt`
- `app/src/main/java/com/example/data/local/TripExpenditureDatabase.kt`
- `app/src/main/java/com/example/data/local/SeedData.kt`
- `app/src/main/java/com/example/data/repository/TripRepository.kt`
- `app/src/main/java/com/example/domain/ExpenseCalculator.kt`
- `app/src/main/java/com/example/ui/viewmodel/TripExpenditureViewModel.kt`
- `app/src/main/java/com/example/ui/components/ActionModals.kt`
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/ui/screens/TripOverviewScreen.kt`
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` (New)
- `app/src/main/java/com/example/MainActivity.kt`

## 3. PROTECTED (DO NOT TOUCH)
- `debug.keystore` and `signingConfigs` in `app/build.gradle.kts`.
