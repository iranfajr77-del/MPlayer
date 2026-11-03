# M Player — Living Project Handoff

Last updated: 2026-11-04
Repository: https://github.com/iranfajr77-del/MPlayer
Development history: [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md)

## Resume protocol
In every new chat, read this file and the latest entries in DEVELOPMENT_LOG.md from main before coding. Treat GitHub as source of truth. Inspect current code and latest Actions result. Continue from Exact next task; do not ask the user to repeat settled decisions.

## Product
- Offline Android music player for Xiaomi Poco M3; Kotlin + Jetpack Compose.
- App name M Player; application ID must remain com.mplayer.app.
- User is a beginner and does not write code; assistant handles implementation, user makes product decisions and performs final device testing.
- No intermediate APK installations; complete planned feature set before final installation.
- CI success means compile/build only, not real-device validation.

## Brand and UI
- Palette: black #090B0A, neon green #39FF14, orange #FF8A24.
- Use the exact user-supplied logo (headphones around M, green/orange glow, play symbol, equalizer) as launcher icon and in-app brand mark. Asset still needs adding to Android resources.
- Persian and English; Persian RTL, English LTR. Home: two-column album grid. Restore last-used page; first launch opens Albums.

## Required features
- Real MediaStore library; search, sorting, multi-select, select all, artwork/title/artist/duration.
- Albums: create, rename, delete, choose gallery cover, add/remove multiple local songs; persist all metadata and membership.
- Embedded track art by default; optional per-track gallery override without modifying audio.
- Playback: play/pause, previous/next, queue, repeat, shuffle, seek, background playback service and media notification.
- Persistent favorites; removing favorite never deletes source audio. DJ uses favorites only.
- Smart Mix: floating neon-green play control, random favorites without near repeats.
- Offline Professional DJ: approximate BPM/beat/mood analysis, two-player crossfade, 3/6/9/12 seconds (default 6), current/next, waveform/BPM/sync. Beat matching is approximate and needs device testing.
- Equalizer presets Normal, Pop, Rock, Jazz, Classical, Electronic, Hip-Hop, Bass Boost, Vocal; manual bands 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz; save/reset, bass boost, virtualizer. Device support varies.
- Settings: audio/equalizer, DJ, playback, sleep timer, library, notifications, privacy, about, language.
- Offline; no paid APIs or servers. Preserve data across upgrades; stable package ID and persistent release signing key needed.

## Current state — 2026-11-04
Implemented in code, not device-tested:
- Android scaffold, Compose home/library UI, MediaStore scan.
- Basic MediaPlayer playback and play/pause, stop, previous/next, auto-next.
- Search in library/favorites.
- Favorite IDs persisted in SharedPreferences.
- Basic album persistence, two-column grid, create/rename/delete.
- Album detail supports viewing assigned tracks, adding multiple real library tracks through checkbox selection, a Select all/Deselect all action for the currently visible (search-filtered) tracks, saving membership, and removing tracks from the album without deleting source audio.
- Album covers can be selected from the gallery while creating/editing an album; the URI and persistable read permission are retained, and cover images display in the two-column grid and album detail. Existing albums without covers remain compatible.

Incomplete/partial:
- Select-all in album assignment.
- Playback remains basic; no Media3 foreground playback service, queue, seek, repeat/shuffle, or media notification.
- Supplied logo not yet in resources/launcher icon.
- Sorting, multi-select library, artwork, Smart Mix, settings, RTL polish, equalizer, DJ, release signing and device tests remain.

## Exact next task
Implement library multi-select and select-all, with a clear selection mode and an action to add selected tracks to an album. Then prioritize the playback engine migration to Media3 background service, including queue/seek/repeat/shuffle and media notification.

## Build and files
- Main branch: main.
- MainActivity: app/src/main/java/com/mplayer/app/MainActivity.kt.
- CI: .github/workflows/android.yml; debug artifact M-Player-debug.
- Latest code commit: 20b7f33aed8dac09fa405bce5ee786f91654066b.
- Latest Actions run: 41729312397, queued when checked; verify its final conclusion before relying on the build.
- Scaffold: settings.gradle.kts, build.gradle.kts, gradle.properties, app/build.gradle.kts, app/src/main/AndroidManifest.xml.

## Mandatory maintenance after each milestone
1. Update this file with current implementation state and exact next task.
2. Append a dated entry to DEVELOPMENT_LOG.md: what changed, files, commit SHA, CI status, and limitations.
3. Separate implemented, partial, and not-started features.
4. Never call a feature complete merely because CI passes.

## Working rules
Use GitHub connector. Give direct links to changed files after commits. Do not ask for intermediate installs. Never claim Poco M3 testing unless the user tested it.
