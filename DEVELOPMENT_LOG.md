# M Player — Development Log

This is an append-only milestone history. Add a new dated entry after each meaningful implementation or project decision. Never erase earlier milestones; correct mistakes with a later note.

## 2026-11-04 — Project baseline and handoff
- Confirmed product: offline Android music player for Poco M3; Kotlin + Jetpack Compose; package `com.mplayer.app`.
- Confirmed brand palette and user's supplied headphone/M logo as canonical identity.
- Confirmed no intermediate APK installation; only final planned installation, unless the user explicitly changes this.
- Added `PROJECT_HANDOFF.md` as the living project summary.

## 2026-11-04 — Initial app scaffold and first library screen
- Added Android project scaffold and GitHub Actions debug build workflow.
- Added basic Compose home/library screens and MediaStore audio scanning.
- CI completed successfully for earlier scaffold commits. This is compile/build evidence only, not device validation.

## 2026-11-04 — Basic playback controls
- Updated `MainActivity.kt` with basic MediaPlayer playback when a track is tapped, play/pause, stop, previous/next, and automatic next-track behavior.
- Limitations: playback is not yet a robust background service; no seek bar, queue management, repeat/shuffle, or media notification yet.

## 2026-11-04 — Search and favorites
- Added library/favorites search across title, artist, and album.
- Added favorite toggle and persistence using SharedPreferences.
- Favorites are IDs only; richer favorites screen and Smart Mix remain future work.

## 2026-11-04 — First album-management foundation
- Added persistent user album records in SharedPreferences.
- Added two-column album grid and create/rename/delete album actions.
- Important incomplete work: album detail song assignment/removal, gallery cover selection, album artwork display, and multi-select library workflow are not implemented yet.
- Latest code commit for this milestone: `e60a2b438b89c561ea99536068bf4c32b9eaa7f4`.
- Latest GitHub Actions run observed: `41728649635`; it was queued at the time checked. Recheck its final conclusion before relying on this build.

## Current next steps
1. Finish album detail page: select multiple songs from the real library, add/remove tracks, persist membership.
2. Add gallery album cover selection and display covers in the two-column grid.
3. Add supplied logo asset to in-app branding and launcher icon.
4. Replace basic playback with a reliable playback service and implement queue, seek, repeat/shuffle, notification/background playback.
5. Improve library: artwork, sorting, select all, track artwork override.
6. Implement equalizer with device/API capability checks.
7. Implement offline DJ mode with realistic limits and careful resource handling.
8. Complete settings, language/RTL, sleep timer, privacy/about, data migrations.
9. Configure stable release signing and final release build.
10. Final installation and real-device testing on Poco M3.

## Mandatory maintenance rule
After every meaningful implementation, decision, or CI result:
1. Update `PROJECT_HANDOFF.md` with the latest current state and next exact task.
2. Append a dated entry to this file describing what changed, which files changed, the commit SHA, CI result (queued/in progress/success/failure), and known limitations.
3. Keep completed, partially implemented, and not-yet-started features clearly separated.
4. Never describe a feature as finished merely because the project compiles.

## 2026-11-04 — Album track membership
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Album detail now has an Add songs mode showing tracks from the real MediaStore library, multi-select checkboxes, and a save action that persists selected track IDs into the chosen album.
- Album detail now shows a remove control for each included track; removing affects only the custom album membership, not the source audio file.
- Album counts derive from persisted membership.
- Code commit: `f42494256d8e629e9b800f944e9a60e49b0c318e`.
- GitHub Actions run `41729312397` was queued when checked; build result is not yet known.
- Not implemented in this milestone: gallery cover selection/display, select-all, artwork, album database migration. Device behavior has not been tested on Poco M3.


## 2026-11-04 — Album gallery covers
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added Android document picker for selecting an image from the device gallery when creating or editing an album.
- Took persistable read permission for the selected content URI and saved the URI alongside album metadata; old album records remain readable when no cover field exists.
- Album cover images now appear in the two-column album grid and album detail header. Clearing a cover only clears the app's reference; deleting an album does not delete the original gallery image.
- Code commit: `d4f4250129cd620e2d6b2fc56d58ca94192e07ce`.
- GitHub Actions run `41729754726` was queued when checked; final build result not yet known.
- Limitations: cover rendering and persisted URI access have not been tested on Poco M3; very large gallery images may require memory/performance tuning. Select-all and full library multi-select remain unimplemented.


## 2026-11-04 — Select all in album assignment
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added Select all/Deselect all for the currently visible tracks in an album's Add songs mode. The action respects the active search filter and preserves selections outside the visible filtered subset when toggling the visible set.
- Added a selected-track count beside the action; the existing Add selected button saves membership.
- Code commit: `20b7f33aed8dac09fa405bce5ee786f91654066b`.
- CI status: pending at the time of this log update; recheck the latest Actions run.
- Limitations: not device-tested; full library multi-select is still pending.


## 2026-11-04 — Library multi-select and add to album
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added a dedicated Select/Done mode to the full library, per-track checkboxes, selected count, and Select all/Deselect all for currently visible (search-filtered) songs.
- Added Add to album action with a destination-album chooser; selected track IDs are merged into the chosen album and persisted. Existing audio files are not copied, moved, or deleted.
- Code commit: `f694b46178a9555f0ed879039ff7c310b4434b4d`.
- CI: workflow run not yet visible in the latest Actions listing at the time of this log update; check its result before relying on the build.
- Limitations: not tested on Poco M3; library selection behavior and persistence need device validation. Next: migrate playback to Media3 background service with queue, seek, repeat/shuffle and media notification.


## 2026-11-04 — Media3 background playback foundation
- Added Media3 ExoPlayer and Session dependencies at stable version 1.11.1 in `app/build.gradle.kts`.
- Added `PlaybackService.kt`, a `MediaSessionService` owning ExoPlayer and a MediaSession so audio can outlive the Activity and Media3 can provide system media controls/notification.
- Declared foreground media playback permissions and the service in `AndroidManifest.xml`.
- Commits: service `f295a77c5df81957054c66933a906cd6006b793b`; dependencies `047afa348c55b52e0968ac010d0a7f79fe2d38f0`; manifest `8c6f1c92d007b01cb5cce39cb8da7d3a348a4160`.
- CI: build runs triggered; check latest Actions before treating this foundation as compile-verified.
- Limitation: MainActivity still uses legacy MediaPlayer and is not yet connected to PlaybackService. This is only the service foundation, not completed background playback. Next: connect via MediaController, move queue and controls to ExoPlayer, then add seek/repeat/shuffle.
