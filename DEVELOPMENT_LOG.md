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


## 2026-11-04 — Playback controller connection
- Updated MainActivity.kt in commit 5fc5c04f4a851f4cb784c7bde25a0d73bb5bd219.
- Replaced Activity-owned MediaPlayer playback with MediaController connected to PlaybackService. Track selection sets a Media3 playlist with metadata; basic play, pause, stop, previous and next use the controller.
- CI run 41730953346 was in progress when checked. Device behavior is not tested.
- Seek/progress, repeat/shuffle, queue editing and background notification validation remain pending.


## 2026-11-04 — Playback seek, repeat, shuffle and queue controls
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added a live playback progress slider with elapsed/total time and seek-on-release, using MediaController position/duration.
- Added shuffle toggle and a three-state repeat cycle (off → all → one → off).
- Added a queue dialog showing the current Media3 playlist; tapping a track jumps to it, and non-current entries can be removed from the queue.
- Code commit: `1d040d99c2b194c468519853ab161f02b7b949da`.
- CI: pending; verify the Actions run for this commit before relying on the build.
- Limitations: no device testing yet; queue currently starts as the scanned library playlist, not a custom queue builder. Playback notification/background behavior still needs Poco M3 validation.


## 2026-11-04 — Stable seek-slider dragging
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added an `isSeeking` guard so the periodic playback-position poll does not pull the slider away from the user's finger while dragging; seek is applied when the drag finishes.
- Code commit: `b41c4647592be985e09dee9ddd4d2a68c55c2974`.
- GitHub Actions run `41733082738` was queued at the time of this log update; verify its final result.
- Limitations: this is a UI-state correction only; playback and seek behavior still require real-device testing on Poco M3. Queue still mirrors the library playlist rather than a fully custom queue builder.


## 2026-11-04 — Media3 player-state synchronization
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- The periodic controller sync now checks the active Media3 media ID and reconciles the highlighted library index. It also clears stale playback position/duration and playing state when the controller has no current item, while preserving the seek-drag guard.
- Code commit: `016bc5f09cc0c33b46bba837b161af0aee0f5ced`.
- CI: triggered by the code commit; verify the run before relying on the build.
- Limitations: not tested on Poco M3; if the playing track is removed from MediaStore it still cannot be represented in the library-based player bar. Queue remains a full-library playlist rather than a custom queue builder.


## 2026-11-04 — Per-track custom gallery artwork (Build 43)
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added a per-track gallery image picker using Android's document picker. The selected URI is persisted in app preferences by MediaStore track ID, with persistable read permission.
- Track rows and the now-playing artwork prefer the custom image, falling back to embedded album artwork when no override exists or the custom image cannot be decoded.
- Added a per-track reset action to remove the override and restore the default embedded artwork. The source audio file and its metadata are not modified.
- Code commit: `f6e9a0ca9ab7ab707b54ece8eec960338aad1479`.
- GitHub Actions run `41734647794` (Build 43): SUCCESS. Kotlin compilation, debug APK build, and artifact upload all completed successfully. Artifact: `M-Player-debug` (~20.1 MiB).
- Build 42 failed because its earlier intermediate commit updated the call sites before updating the `SongRow` signature and picker state; Build 43 includes the completed matching implementation and passed.
- Limitations: no installation or runtime test on Poco M3; image permission persistence, display after app restart, reset behavior, and now-playing refresh still need final device validation. CI success is not device validation.


## 2026-11-04 — Smart Mix from favorites
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Added a Smart Mix quick-access tile on the home screen when the user has at least one favorite.
- Smart Mix builds a randomized Media3 playlist using only favorited local tracks. It avoids starting with the same first track as the previous Smart Mix session when more than one favorite exists; each track appears at most once in that generated playlist.
- Favorite records and source audio files are not modified.
- Code commit: `b0822ef62d40cd14bd9a34da76ce01176ea40466`.
- CI: workflow is expected from the push; status must be checked before treating this milestone as build-verified.
- Limitations: no Poco M3 runtime testing. A dedicated persistent DJ history/advanced no-repeat algorithm is not included in this basic Smart Mix milestone.


## 2026-11-04 — Persist language and restore last main page
- Updated `app/src/main/java/com/mplayer/app/MainActivity.kt`.
- Persian/English selection is now loaded from and saved to SharedPreferences, so the selected language survives app restarts.
- The last main destination is restored on launch. Transient album-detail navigation is normalized to the home screen because an album detail depends on transient navigation state.
- Code commit: `574a32d43ebd72a5dc51e4ee0fb596b883f2bcfb`.
- GitHub Actions run `41736919539` was queued when checked; build result is pending.
- Limitations: complete RTL/LTR layout review, dedicated settings UI, sleep timer, equalizer, DJ mode, logo/launcher icon, and Poco M3 runtime validation remain.


## 2026-11-04 — Settings screen and sleep timer
- Added a bilingual Settings entry point and screen in `MainActivity.kt`.
- Settings include persistent Persian/English selection, About/version information, and a plain explanation of the music-library permission and where to manage it in Android.
- Settings code commit: `3c848ea13a1daf63915702f19f01ac4afc10e349`; GitHub Actions run `41737345325` (Build 54) succeeded.
- Added a persistent sleep timer with 15, 30, 45 and 60 minute choices and cancellation. Its end timestamp is stored in SharedPreferences; when the deadline is reached the MediaController is asked to pause.
- Sleep timer code commit: `c0d88f1b8f5f7c93b2bd70967ccd7f33acebb8b6`; GitHub Actions run `41737599825` (Build 55) was in progress at the time of this log update.
- Limitations: timer behavior, process-restart behavior, and actual playback pause are not yet tested on Poco M3. Equalizer, DJ mode, launcher/in-app supplied logo, full RTL/LTR review, release signing and final device validation remain.


## 2026-11-04 — Sleep timer reconnect reliability
- Updated `MainActivity.kt` so the sleep-timer coroutine also observes `mediaController`; if the playback controller connects or reconnects while a timer is active, the deadline is recalculated and the latest controller receives the pause command.
- Feature commit: `16aed5f0c8a86a2c135cbece0c117be7792d81d5`.
- Build 55 for the original persistent sleep-timer implementation completed successfully (run `41737599825`). The reconnect-fix commit triggers a new CI run; pending at log update.
- Limitation: timer behavior still requires runtime testing on the Poco M3; CI does not validate real audio playback or background lifecycle.


## 2026-11-04 — Custom playback queue builder
- Updated `MainActivity.kt` with a queue-builder dialog opened from the playback queue. Users can select multiple tracks from the device library and append them to the current Media3 queue; selecting an item in the queue starts it, and non-current items can be removed.
- Commit: `90a45725fe944039b546fffc54d5e37abda6f358`.
- CI status: build triggered by the commit; awaiting result.
- Limitations: queue reordering/drag-and-drop and named saved queues are not implemented. No Poco M3 runtime validation yet.


## 2026-11-04 — Device equalizer integration
- Added Android platform Equalizer controls and device preset selection in the app.
- PlaybackService publishes the active audio session ID to shared preferences for the effect.
- Requires validation on Poco M3; equalizer availability and preset list depend on device audio effects.


## 2026-11-04 — Rebuild request (Build 74)
- Triggered a fresh Android CI build without changing application source code, to reproduce the Build 73 application from the same `main` source state and regenerate its debug APK artifact.
- This is a rebuild only; it does not add or modify app features.
- Build result and artifact availability must be verified from the new GitHub Actions run.


## 2026-11-12 — AI Animation Studio prototype
- Added `AI-Animation-Studio/README.md`, `requirements.txt`, and `generate.py`.
- Added `.github/workflows/ai-animation.yml` with manual dispatch and an MP4/SRT/story artifact.
- Pipeline: LLM7 anonymous chat completion attempt (sample-story fallback), Persian Edge TTS narration, Pillow illustrated forest scenes, FFmpeg motion/concatenation, Persian subtitle burn-in.
- Follow-up fixes: use ASCII scene labels to avoid Pillow RTL/font-layout dependency; pass workflow topic through an environment variable.
- Android application files and existing Android workflow were not changed.
- Commit sequence: `bab9e81573fbde75e943b74b0004c3a7dce37222`, `3799628a821610e4cafb7de411393f25b7da590c`, `c777416162be4aafe5a7a3047569a7c3134eca04`, `cea53a2018dddb16720bd24576439d5fb755583b`, with follow-up fixes recorded in subsequent commits.
- CI/runtime status: not yet run; workflow requires manual dispatch. No video artifact has been produced or device-tested.
- Limitation: visuals are illustrated 2D motion graphics, not generated 3D animation. LLM7 anonymous access and Edge TTS availability are not guaranteed.
