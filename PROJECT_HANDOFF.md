# M Player — Project Handoff

Last updated: 2026-11-04

## Product
- Android offline music player for Xiaomi Poco M3.
- App name: M Player. Package/application ID must remain `com.mplayer.app`.
- Kotlin + Jetpack Compose. User is a beginner and does not write code; assistant owns implementation, user approves product decisions and installs/tests only at final milestone.
- Do not ask user to install intermediate builds. Build/check in GitHub Actions during development; final install only after feature set is implemented.
- Be honest: CI success is compile/build evidence, not proof of device behavior.

## Brand and UI
- Primary palette: black #090B0A, neon green #39FF14, orange #FF8A24.
- Use the user's supplied M Player logo (headphones around M, green/orange glow, play symbol and equalizer) as the canonical app icon and in-app brand mark. The actual logo asset needs to be added to the Android resources; do not redraw it as a letter M.
- Persian and English UI; Persian should be RTL and English LTR.
- Home page album grid: two columns.
- Restore last-used page; default to Albums on first launch.

## Product requirements
- Scan real local audio via MediaStore; no fake demo tracks.
- Albums: create, rename, delete, choose a cover image from gallery, add/remove multiple local songs; album metadata and membership persist across app restarts and app updates.
- Library: vertical list, embedded artwork where available, title/artist/duration, search, sorting, multi-select, select all, add to album.
- Track artwork: use embedded artwork by default; allow user-selected per-track artwork override without modifying source audio.
- Playback: actual audio playback, play/pause, previous/next, queue, repeat, shuffle, seek/progress, background playback and media notification.
- Favorites: persistent; removing a favorite never deletes the source audio. DJ mode uses Favorites only.
- Smart Mix: floating neon-green play control starts random favorite playback with no near repeats.
- Professional DJ mode, fully offline: approximate BPM/beat/mood analysis where feasible, crossfade with two players, configurable 3/6/9/12 sec (default 6), current/next tracks, waveform/BPM/sync status. Be honest that beat matching is approximate and device testing is required.
- Equalizer: presets Normal, Pop, Rock, Jazz, Classical, Electronic, Hip-Hop, Bass Boost, Vocal; manual 10 bands (31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz); save custom settings, reset, bass boost, virtualizer. Device support varies.
- Settings: audio/equalizer, DJ, playback, sleep timer, library, notifications, privacy, about, language.
- All core functions offline; no paid APIs or servers.
- Preserve user data across upgrades. Stable package ID and a persistent release signing key are required before publishing upgradeable releases.

## Current repository
- https://github.com/iranfajr77-del/MPlayer
- Main branch: main
- MainActivity currently contains basic Compose UI, MediaStore scan, basic MediaPlayer playback controls, search and persistent favorite IDs in SharedPreferences.
- Workflow: .github/workflows/android.yml builds debug APK and uploads artifact M-Player-debug.
- Build has succeeded for commit 5c31faacc8fc44ff8d56088424de48f53b0bb8f2 (2026-11-04). CI success does not mean tested on Poco M3.
- Earlier scaffold files include app/build.gradle.kts, AndroidManifest.xml, settings.gradle.kts, root build.gradle.kts, gradle.properties.
- Current app is incomplete: album management, real logo integration, polished UI, robust playback/background service, queue, artwork, equalizer, DJ engine, settings, release signing, and device tests remain.

## Next implementation sequence
1. Album model + persistent storage + create/rename/delete + two-column album grid.
2. Album details + multi-select song assignment/removal + gallery album covers.
3. Add supplied logo asset and proper launcher icon.
4. Playback engine/service, queue, seek, repeat/shuffle, notification/background playback.
5. Track art, sorting, library polish, favourites UX and Smart Mix.
6. Equalizer and DJ implementation with realistic limitations.
7. Settings, localization/RTL, persistence/migrations, privacy/about.
8. Release signing strategy, CI release build, final install and real-device test.

## Working rules
- Use GitHub connector for repository changes.
- After every committed file, give direct GitHub file link.
- No APK installation until final planned milestone.
- Never claim a feature is complete unless code actually implements it; never claim device testing without user testing.
