# AI Animation Studio (isolated from M Player)

This folder contains a first working automation prototype. It does not modify the Android app.

## What it currently does
- Creates a six-scene Persian animal story. It tries the anonymous LLM7 OpenAI-compatible endpoint first and falls back to a bundled sample story if that endpoint is unavailable.
- Generates Persian narration with Microsoft Edge TTS voice `fa-IR-DilaraNeural`.
- Creates original illustrated forest scene cards with subtle camera movement.
- Produces a vertical 720x1280 MP4 with burned-in Persian subtitles and a separate SRT file.

## What it does NOT do yet
- It does not generate true 3D characters or AI text-to-video footage. The first visual renderer is 2D illustrated motion graphics.
- Free-tier/API availability is not guaranteed. LLM7 is attempted without a key; a `LLM7_API_KEY` repository secret can be added if anonymous requests stop working.
- Edge TTS is an unofficial online endpoint and may be rate-limited or unavailable. This workflow is not fully offline.

## Run it
In GitHub, open **Actions → AI Animation Studio → Run workflow**. Enter a story topic in Persian. When complete, download the `ai-animation-video` artifact.

No secret is required for the fallback story and narration. If the LLM endpoint is available, it will generate a new story.

## Output
- `output/ai-animation.mp4`
- `output/subtitles.srt`
- `output/story.json`

The first run is a technical prototype. Review the artifact before treating it as publish-ready.
