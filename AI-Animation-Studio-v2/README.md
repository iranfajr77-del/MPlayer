# M Player — AI Animation Studio V2

An isolated cloud-assisted video workflow for creating short vertical animation clips. It does not change the Android music-player code.

## Engines

- **Gemini** — creative director: turns your idea into a detailed cinematic prompt. Configure `GEMINI_API_KEY` to enable it. If the key is absent, the original prompt is passed through unchanged.
- **Qwen Image** — creates the 9:16 keyframe image.
- **Wan 2.2** — animates that image into a short MP4 clip.

The Qwen and Wan Gradio endpoints were inspected successfully in GitHub Actions. Their exact endpoint names and parameter order are used in `generate.py`.

## Run from GitHub Actions

1. Open **Settings → Secrets and variables → Actions** in this repository.
2. Add repository secret `GEMINI_API_KEY` using a Google AI Studio API key. This is optional; without it the Qwen → Wan path still runs.
3. Optionally add `HF_TOKEN` (a Hugging Face read token) to improve access/rate limits.
4. Open **Actions → AI Animation Studio V2 Generate**.
5. Choose **Run workflow**, enter a short shot description, and start it.
6. Download the `mplayer-ai-animation` artifact when the run completes. It contains the MP4, keyframe image, and generation metadata.

No key is stored in the repository. Do not paste API keys into source files or chat.

## Local command

Install `requirements.txt`, then run:

```bash
python AI-Animation-Studio-v2/generate.py --prompt "A small robot walks through a neon-lit rainy city"
```

Set `GEMINI_API_KEY` and optionally `HF_TOKEN` in the environment first.

## Free usage and limitations

Public Hugging Face Spaces may queue, sleep, change, or exhaust their shared ZeroGPU quota. A successful API inspection does not guarantee a successful generation every time. Gemini API usage and model availability depend on Google's current account, region, and pricing terms. This workflow does not enable billing or promise unlimited free generation.

The first generation is intentionally a single short vertical shot. Voice, Persian subtitles, multi-shot editing, and final reel assembly are later stages.
