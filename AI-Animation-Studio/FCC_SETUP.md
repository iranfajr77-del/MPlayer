# Free Claude Code (FCC) integration

The first animation prototype can use an externally hosted Free Claude Code server
for Persian story and scene-script generation. The existing Qwen Image and Wan 2.2
image/video generation in `AI-Animation-Studio-v2` stays on its current Hugging
Face Spaces endpoints.

## Deploy FCC for free on Render

Render is the simplest practical free test host for this Python service. It is not
an always-on production host: free services sleep after 15 minutes without traffic,
and the first request after sleep can take about a minute. Free instances can also
restart and have no persistent disk. See Render's current free-tier limits:
https://render.com/docs/free

1. Open https://dashboard.render.com/ and sign in with GitHub.
2. Choose **New + → Web Service**.
3. Select the upstream repository **Alishahryar1/free-claude-code** (not MPlayer),
   branch **main**.
4. Choose the **Python** runtime.
5. Use these commands:
   - Build: `uv sync --locked --no-dev`
   - Start: `uv run uvicorn server:app --host 0.0.0.0 --port $PORT`
6. Add these environment variables in Render before deploying:
   - `PYTHON_VERSION` = `3.14.10`
   - `PROXY_AUTH_ENABLED` = `true`
   - `ANTHROPIC_AUTH_TOKEN` = a long, unique random secret (do not reuse a password)
   - `LLM7_API_KEY` = your LLM7 key, if using LLM7
   - `MODEL` = `llm7/default` (only when LLM7 is configured)
7. Deploy. When it succeeds, copy the service's HTTPS address.

The upstream project pins Python 3.14.10 and includes `uv.lock`. Render currently
supports Python 3.14.10 and its native uv workflow. The free service is suitable
for a connection test, but the cold start and resource limits may make animation
runs slow or unreliable.

## Connect Render to MPlayer

In the MPlayer repository, open **Settings → Secrets and variables → Actions**.

Create these repository secrets:

- `FCC_BASE_URL`: your Render HTTPS address followed by `/v1`, for example
  `https://YOUR-SERVICE.onrender.com/v1`.
- `FCC_API_KEY`: the exact value you set for `ANTHROPIC_AUTH_TOKEN`.

Create this repository variable:

- `FCC_MODEL`: `llm7/default` when the LLM7 provider is configured in FCC.

Keep the existing `LLM7_API_KEY` secret as a fallback for the animation workflow.
Never commit API keys or proxy tokens to GitHub files.

## Important architecture

FCC is an OpenAI-compatible model router, not an image/video renderer. It must be
reachable by GitHub Actions over HTTPS. A local address such as
`http://127.0.0.1:8082/v1` on a phone or personal computer cannot be reached by
GitHub Actions.

Do not expose FCC's Admin UI publicly. Use proxy authentication as described above.
FCC's role here is Persian story/script text generation only; Qwen Image and Wan 2.2
remain the image and image-to-video engines. Persian narration and subtitles remain
in the existing animation pipeline.

## Workflow

Run **Actions → AI Animation Studio → Run workflow**. The log identifies the
selected text provider with `STORY_PROVIDER`.

The workflow does not start FCC itself. It only sends requests to the hosted FCC
service. The actual FCC-to-MPlayer connection cannot be tested until the Render
service is deployed and the GitHub secrets/variable above are filled in.
