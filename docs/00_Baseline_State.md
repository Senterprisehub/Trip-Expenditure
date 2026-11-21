# 00 Baseline State & Architecture Map

## 1. Tech Stack & Locked Versions
- **Android Gradle Plugin (AGP)**: `9.1.1`
- **Kotlin**: `2.2.10`
- **KSP**: `2.3.5`
- **Compile / Target SDK**: `36` (`minSdk = 24`)
- **UI Framework**: Jetpack Compose (`compose-bom = 2024.09.00`) + Material 3
- **Local Persistence**: AndroidX Room (`2.7.0`) with KSP compiler & Kotlin Coroutines (`1.10.2`)

## 2. Database Schema & Table Relationships (On-Device Room SQLite)
- `trips` (`TripEntity`) — 1:N with `itinerary_stops`, `expenses`, `travel_documents`, and `custom_map_pins`.
- `itinerary_stops` (`ItineraryStopEntity`) — Indexed by `tripId` and `dayNumber`, storing GPS coordinates (`latitude`, `longitude`) and group member votes (`votersCsv`).
- `expenses` (`ExpenseEntity`) — Indexed by `tripId`, storing multi-currency amounts, base-currency normalized amounts, payer, participants, split mode (`EQUAL`, `EXACT`, `PERCENTAGE`), and settlement flags.
- `travel_documents` (`TravelDocumentEntity`) — Indexed by `tripId`, storing offline booking references, vouchers, and rail passes.
- `custom_map_pins` (`CustomMapPinEntity`) — Indexed by `tripId`, storing user-dropped offline map waypoints.

## 3. Protected Core Files
- `debug.keystore` / `debug.keystore.base64`
- `app/build.gradle.kts` signing configurations
- `settings.gradle.kts` repository resolution blocks
