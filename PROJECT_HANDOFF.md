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

## Implemented in code (not device-tested)
- Android scaffold, Compose home/library UI, MediaStore audio scanning.
- Basic search across library/favorites.
- Favorite IDs persisted in SharedPreferences.
- User album persistence, two-column grid, create/rename/delete.
- Album detail: add/remove multiple real library tracks, select all/deselect all for visible filtered tracks, persist membership without touching source audio.
- Album gallery cover selection and display, with persisted read URI permission.
- Library multi-select, visible select-all/deselect-all, add selected songs to a destination album.
- Media3 ExoPlayer/Session dependencies, PlaybackService hosting ExoPlayer + MediaSession, foreground playback permissions and manifest service declaration.
- MainActivity now connects to PlaybackService through MediaController; track taps set a Media3 playlist and play; player state drives the basic now-playing controls; previous/next/stop/play/pause call the controller.

## Partial / not yet implemented
- Media3 controller integration compiles successfully in CI; no real-device testing.
- Playback controls are implemented in code, and the UI now re-checks the active Media3 media ID during its state poll so the highlighted track/index, play state, position, and duration recover when the controller state changes or reconnects. Queue behavior is still a simple view/editor over the full-library Media3 playlist; a dedicated custom queue builder remains.
- Background service notification/system controls need runtime validation on Poco M3.
- Playback queue currently mirrors the scanned library when a track is selected; advanced queue editing is not implemented.
- Supplied logo not yet in resources/launcher icon.
- Sorting, embedded/per-track artwork, Smart Mix, settings/language/RTL polish, equalizer, DJ, release signing and device tests remain.

## Exact next task
1. Verify CI for player-state synchronization commit `016bc5f09cc0c33b46bba837b161af0aee0f5ced`.
2. Improve queue behavior and handle the case where the current track disappears from the MediaStore library; later validate background playback and media notification on Poco M3.
3. Continue track artwork/sorting, Smart Mix, equalizer, DJ, settings/language/RTL, branding, and release readiness.

## Build and files
- Main branch: main.
- MainActivity: app/src/main/java/com/mplayer/app/MainActivity.kt.
- PlaybackService: app/src/main/java/com/mplayer/app/PlaybackService.kt.
- CI: .github/workflows/android.yml; debug artifact M-Player-debug.
- Latest playback controls commit: `1d040d99c2b194c468519853ab161f02b7b949da`.
- Latest Actions run at handoff update: queued; verify its final conclusion.

## Mandatory maintenance after each milestone
1. Update this file with current implementation state and exact next task.
2. Append a dated entry to DEVELOPMENT_LOG.md: what changed, files, commit SHA, CI status, limitations.
3. Separate implemented, partial, and not-started features.
4. Never call a feature complete merely because CI passes.

## Working rules
Use GitHub connector. Give direct links to changed files after commits. Do not ask for intermediate installs. Never claim Poco M3 testing unless the user tested it.
