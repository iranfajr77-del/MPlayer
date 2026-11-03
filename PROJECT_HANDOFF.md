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
- Embedded track art by default; per-track gallery override implemented and CI-verified, without modifying audio.
- Playback: play/pause, previous/next, queue, repeat, shuffle, seek, background playback service and media notification.
- Persistent favorites; removing favorite never deletes source audio. DJ uses favorites only.
- Smart Mix: floating neon-green play control, random favorites without near repeats.
- Offline Professional DJ: approximate BPM/beat/mood analysis, two-player crossfade, 3/6/9/12 seconds (default 6), current/next, waveform/BPM/sync. Beat matching is approximate and needs device testing.
- Equalizer presets Normal, Pop, Rock, Jazz, Classical, Electronic, Hip-Hop, Bass Boost, Vocal; manual bands 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz; save/reset, bass boost, virtualizer. Device support varies.
- Settings: bilingual settings screen with language selection, About/version, basic music-permission/privacy explanation, and persistent 15/30/45/60-minute sleep timer. Audio/equalizer, DJ, playback, library and notification controls remain to be built.
- Offline; no paid APIs or servers. Preserve data across upgrades; stable package ID and persistent release signing key needed.

## Implemented in code (not device-tested)
- Android scaffold, Compose home/library UI, MediaStore audio scanning.
- Basic search across library/favorites.
- Favorite IDs persisted in SharedPreferences.
- User album persistence, two-column grid, create/rename/delete.
- Album detail: add/remove multiple real library tracks, select all/deselect all for visible filtered tracks, persist membership without touching source audio.
- Album gallery cover selection and display, with persisted read URI permission.
- Per-track gallery cover override and reset; override URIs are persisted by track ID, custom artwork is preferred in track rows and now-playing display, and source audio is untouched (Build 43 CI success).
- Library multi-select, visible select-all/deselect-all, add selected songs to a destination album.
- Media3 ExoPlayer/Session dependencies, PlaybackService hosting ExoPlayer + MediaSession, foreground playback permissions and manifest service declaration.
- Library sorting modes (title A–Z/Z–A, artist, album, duration ascending/descending), persisted in SharedPreferences and applied after search/favorite/album filters.
- Smart Mix home quick-access tile: builds a randomized playlist from favorites only, with a persisted previous-first-track guard to avoid starting two consecutive mixes with the same song (commit `b0822ef62d40cd14bd9a34da76ce01176ea40466`; CI succeeded in run `41736421878`).
- MainActivity now connects to PlaybackService through MediaController; track taps set a Media3 playlist and play; player state drives the basic now-playing controls; previous/next/stop/play/pause call the controller.

## Partial / not yet implemented
- Per-track custom artwork is implemented and CI-verified, but gallery permission persistence, display after restart, reset, and now-playing refresh have not been tested on Poco M3.
- Media3 controller integration compiles successfully in CI; no real-device testing.
- Playback controls are implemented in code, and the UI now re-checks the active Media3 media ID during its state poll so the highlighted track/index, play state, position, and duration recover when the controller state changes or reconnects. Queue behavior is still a simple view/editor over the full-library Media3 playlist; a dedicated custom queue builder remains.
- Background service notification/system controls need runtime validation on Poco M3.
- Playback queue currently mirrors the scanned library when a track is selected; advanced queue editing is not implemented.
- Supplied logo not yet in resources/launcher icon.
- Sorting and Smart Mix are implemented in code; Build 47 and Smart Mix CI run `41736421878` succeeded.
- Embedded/per-track artwork, album workflows, and playback controls are implemented in code but need final device validation.
- Supplied logo asset is still not in Android resources/launcher icon; only a temporary text M is shown in the app header.
- Language choice is persisted, last main page is restored, and a bilingual Settings screen is implemented with About/version, permission/privacy explanation, and sleep timer choices. Settings code commit `3c848ea13a1daf63915702f19f01ac4afc10e349` passed CI run `41737345325`. Sleep timer code commit `c0d88f1b8f5f7c93b2bd70967ccd7f33acebb8b6`; follow-up fix `16aed5f0c8a86a2c135cbece0c117be7792d81d5` makes the timer effect observe MediaController reconnection. Build 55 passed CI (run `41737599825`); follow-up build is pending. Full RTL/LTR review and device validation remain.
- Equalizer, DJ mode, detailed privacy/about policy, release signing and device tests remain. Sleep timer is implemented in code; its effect now restarts against the latest MediaController if the player connects/reconnects before the deadline. Timing and pause behavior are not device-tested.

## Exact next task
1. Add the supplied headphone/M logo to Android resources and use it for the launcher icon and in-app brand mark.
2. Implement and validate device-supported equalizer controls with safe fallbacks.
3. Implement offline DJ mode only if two-player mixing can be integrated safely without destabilizing current playback.
4. Finish settings, full RTL/LTR review, custom queue, detailed privacy/about, and playback lifecycle checks.
5. Prepare stable release signing and a final APK only after the feature set is implemented; then perform full Poco M3 validation.

## Build and files
- Main branch: main.
- MainActivity: app/src/main/java/com/mplayer/app/MainActivity.kt.
- PlaybackService: app/src/main/java/com/mplayer/app/PlaybackService.kt.
- CI: .github/workflows/android.yml; debug artifact M-Player-debug.
- Latest feature code commit: `16aed5f0c8a86a2c135cbece0c117be7792d81d5` (sleep timer reconnect fix; CI pending). Build 55 (`41737599825`) succeeded.
- Smart Mix documentation commit: `28b8bb7a67fb706f9336ae7c2a67152bda3c0f20`.
- Latest Actions result must be checked for the Smart Mix code commit; do not treat it as verified until CI succeeds.

## Mandatory maintenance after each milestone
1. Update this file with current implementation state and exact next task.
2. Append a dated entry to DEVELOPMENT_LOG.md: what changed, files, commit SHA, CI status, limitations.
3. Separate implemented, partial, and not-started features.
4. Never call a feature complete merely because CI passes.

## Working rules
Use GitHub connector. Give direct links to changed files after commits. Do not ask for intermediate installs. Never claim Poco M3 testing unless the user tested it.
