# CLAUDE.md — System Architecture & Engineering Guide

## Project Identity
- **Application Name**: Trip Expenditure
- **Application ID**: `com.aistudio.tripexpenditure.trpxvq`
- **Namespace**: `com.example`
- **Architecture**: Clean MVVM with 100% On-Device Room SQLite Persistence, Reactive Kotlin `StateFlow`, and Multi-Theme Jetpack Compose UI.

## Core Design Themes (Live-Switchable)
1. **Modern Horizon (`MODERN_HORIZON`)**: Deep Abyssal Navy (`#0B132B`), Slate Harbor (`#1C2541`), Warm Sunset Coral (`#FF6B4A`), Lagoon Cyan (`#00B4D8`).
2. **Minimalist Nordic (`MINIMALIST_NORDIC`)**: Warm Alabaster Paper (`#F6F5F0`), Pure White (`#FFFFFF`) with `1.dp` stone borders (`#D8D5CA`), Pine Needle Green (`#2D4A3E`), Terracotta Clay (`#C87D55`).
3. **High-Energy Pulse (`HIGH_ENERGY_PULSE`)**: Midnight Obsidian (`#090A10`), Dark Indigo (`#151826`), Electric Cyber Lime (`#CCFF00`), Neon Sunset Magenta (`#FF2A85`).

## Protected Files (DO NOT MODIFY WITHOUT EXPLICIT HUMAN APPROVAL)
- `debug.keystore` and `debug.keystore.base64`
- `signingConfigs` in `app/build.gradle.kts`
- `gradle/libs.versions.toml` core SDK/AGP version pins
