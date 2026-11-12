# AI Animation Studio V2

This is an isolated AI image-to-video experiment. It does not change the Android M Player app or the first prototype.

## First step: inspect the real APIs

Hugging Face Spaces can expose Gradio APIs, but endpoint names and parameter order vary. This project first inspects the live API schemas instead of guessing them.

1. Open **Actions → AI Animation Studio V2 API Check**.
2. Press **Run workflow**.
3. Download the `huggingface-space-api-report` artifact.
4. The report contains the actual endpoints and parameter signatures for both models.

The public Spaces can be inspected without a token. Adding an optional repository secret named `HF_TOKEN` (a Hugging Face read token) can improve authenticated access and rate limits.

## Target models

- Image: `Qwen/Qwen-Image`
- Image-to-video: `zerogpu-aoti/wan2-2-fp8da-aoti-faster`

The intended first visual test is one short vertical character shot. Persian narration and subtitles will be added only after the visual generation works.

## Free-tier limits

Hugging Face ZeroGPU quotas are limited and shared. The official documentation currently lists 2 GPU minutes per day for unauthenticated use and 5 minutes per day for a free account. Queueing, quota exhaustion, or public Space downtime may prevent a run.

No daily 2–3 minute output guarantee is made. The first goal is one verifiable high-quality test clip.
