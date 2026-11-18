# پاسور

Android native project (Kotlin + Jetpack Compose) for a Persian Hokm card game.

## Current MVP
- Offline single-player against three bots
- Four seats, two opposing teams
- Standard 52-card deck
- Five cards are dealt first; the hakem chooses trump, then the remaining eight cards are dealt
- Follow-suit rule and automatic trick winner
- First team to seven tricks wins a hand; match score tracked to seven
- Hakem rotates between seats
- Editable player name and randomized Iranian bot names
- Black/gold interface, green table, and cream/gold patterned card backs
- Simple vector launcher icon
- Versioned Bluetooth message protocol with private per-seat card views (protocol layer only; not yet wired into live gameplay)

## Not yet implemented
- Bluetooth lobby UI, runtime permission request, host socket, and connection to already-paired devices (basic transport only; automatic discovery/pairing, live seat assignment, authoritative game synchronization, and multi-phone testing remain)
- Finished Persian carpet artwork
- Advanced strategic bot behavior, special scoring variants, and exhaustive rules tests

## Build
Open this folder as an Android Studio project, or use the repository Actions workflow. Minimum Android 6.0 (API 23).
