# Free Claude Code (FCC) integration

The first animation prototype can use an externally hosted Free Claude Code server
for Persian story and scene-script generation. The existing Qwen Image and Wan 2.2
image/video generation in `AI-Animation-Studio-v2` stays on its current Hugging
Face Spaces endpoints.

## Important architecture

FCC is an OpenAI-compatible model router, not an image/video renderer. It must be
running on a persistent machine or hosted service that the GitHub Actions runner
can reach over HTTPS. A local address such as `http://127.0.0.1:8082/v1` on your
phone or personal computer is NOT reachable from GitHub Actions.

Do not expose the FCC Admin UI publicly. Enable FCC Proxy Authentication and use
the proxy URL only for `/v1` API requests.

## GitHub configuration

In the MPlayer repository, open **Settings → Secrets and variables → Actions**.

Create these repository secrets:

- `FCC_BASE_URL`: the reachable FCC proxy base URL, for example
  `https://YOUR-HOST.example/v1` (do not include `/chat/completions`).
- `FCC_API_KEY`: the FCC Proxy Authentication token. If proxy authentication
  is disabled, this may be left empty, but authentication is strongly recommended.

Create this repository variable:

- `FCC_MODEL`: the exact model identifier shown by FCC, for example
  `llm7/default` only if that provider/model is configured and available in FCC.

The existing `LLM7_API_KEY` secret remains an optional fallback. If FCC is
configured, the story generator tries FCC first. If FCC is unavailable or the
request fails, it tries the existing direct LLM7 endpoint. If neither works,
the existing sample-story fallback remains available.

## Workflow

Run **Actions → AI Animation Studio → Run workflow**. The log identifies the
selected text provider with `STORY_PROVIDER`.

The workflow does not start an FCC server inside GitHub Actions: the FCC service
needs provider configuration and a stable endpoint. The animation workflow only
acts as an authenticated client of that service.

## Current scope

- FCC: Persian story/script text generation in `AI-Animation-Studio/generate.py`.
- Qwen Image + Wan 2.2: image and image-to-video generation in
  `AI-Animation-Studio-v2/test_video.py`.
- Persian narration and subtitles: remain in the first prototype pipeline.

No FCC endpoint or provider credentials are included in the repository. Add them
as GitHub Actions secrets/variables; never commit API keys or proxy tokens.
