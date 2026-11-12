# AI Animation Studio V2 — visual test

This is a separate proof-of-concept. It does not modify the Android M Player app or the first prototype.

## What this test does

1. Uses the public Qwen Image Hugging Face Space to create one vertical 3D-cartoon character reference.
2. Sends that image to the Wan 2.2 14B Image-to-Video Space to animate one 3.5-second shot.
3. Uploads the MP4, reference image, and metadata as a GitHub Actions artifact.

## Required setup

Create a Hugging Face access token with read access and add it to this GitHub repository:

- Repository → Settings → Secrets and variables → Actions → New repository secret
- Name: `HF_TOKEN`
- Value: your Hugging Face token

No credit card or paid API key is required for this test. Hugging Face ZeroGPU free accounts have a limited daily GPU quota (currently about five minutes); generation may queue, fail when quota is exhausted, or be temporarily unavailable.

## Run

Open **Actions → AI Animation Studio V2 Visual Test → Run workflow**.

Download the artifact named `ai-animation-v2-visual-test`.

## Important limits

- This is only a visual test of one short shot; it does not yet include Persian narration or subtitles.
- The current free GPU allowance is not enough to promise 2–3 minutes of generated AI footage every day.
- Public Spaces can change, queue, or become unavailable. A successful GitHub workflow is not guaranteed.
- The image/video models are third-party Hugging Face Spaces, not owned or controlled by this repository.
