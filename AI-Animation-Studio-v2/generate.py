import argparse
import json
import os
import shutil
import sys
import urllib.error
import urllib.request
from pathlib import Path

from gradio_client import Client

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
OUT.mkdir(parents=True, exist_ok=True)

QWEN_SPACE = "mcp-tools/Qwen-Image"
WAN_SPACE = "zerogpu-aoti/wan2-2-fp8da-aoti-faster"


def gemini_refine_prompt(prompt: str) -> str:
    """Use Gemini as the creative director when GEMINI_API_KEY is configured."""
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        print("Gemini: skipped (GEMINI_API_KEY is not configured).")
        return prompt

    model = os.getenv("GEMINI_TEXT_MODEL", "gemini-2.5-flash").strip()
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    instruction = (
        "You are a cinematic animation director. Rewrite the user's idea as one "
        "detailed English image-generation prompt for a high-quality vertical "
        "3D animated shot. Specify subject, environment, lighting, materials, "
        "camera, composition and mood. Preserve the user's intended events. "
        "Do not add text, subtitles, logos or watermarks. Return only the prompt.\n\n"
        f"User idea: {prompt}"
    )
    payload = {
        "contents": [{"parts": [{"text": instruction}]}],
        "generationConfig": {"temperature": 0.7, "maxOutputTokens": 700},
    }
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            data = json.loads(response.read().decode("utf-8"))
        refined = data["candidates"][0]["content"]["parts"][0]["text"].strip()
        if not refined:
            raise RuntimeError("Gemini returned an empty prompt.")
        print(f"Gemini prompt refinement: OK ({model})")
        return refined
    except (urllib.error.URLError, KeyError, IndexError, ValueError) as exc:
        raise RuntimeError(
            "Gemini prompt refinement failed. Check GEMINI_API_KEY/model and retry. "
            f"Details: {exc}"
        ) from exc


def make_client(space_id: str, token: str):
    kwargs = {"verbose": False}
    if token:
        kwargs["token"] = token
    return Client(space_id, **kwargs)


def first_file(value):
    if isinstance(value, str):
        return value
    if isinstance(value, dict):
        return value.get("path") or value.get("video") or value.get("name")
    if isinstance(value, (list, tuple)) and value:
        return first_file(value[0])
    return None


def main():
    parser = argparse.ArgumentParser(description="Generate a vertical AI animation clip.")
    parser.add_argument("--prompt", required=True, help="Describe the shot to generate.")
    parser.add_argument("--duration", type=float, default=3.5, help="Video duration in seconds (Space limit applies).")
    args = parser.parse_args()

    token = os.getenv("HF_TOKEN", "").strip()
    prompt = gemini_refine_prompt(args.prompt)

    print("1/2 Generating a 9:16 image with Qwen Image...")
    qwen = make_client(QWEN_SPACE, token)
    image_result = qwen.predict(
        prompt=prompt,
        seed=0,
        randomize_seed=True,
        aspect_ratio="9:16",
        negative_prompt="text, subtitles, watermark, logo, blurry, low resolution, deformed face, extra fingers",
        guidance_scale=4.0,
        num_inference_steps=24,
        api_name="/generate_image",
    )
    image_path = first_file(image_result)
    if not image_path or not Path(image_path).is_file():
        raise RuntimeError(f"Qwen did not return a readable image file: {image_result!r}")
    image_out = OUT / "generated-image.png"
    shutil.copy2(image_path, image_out)
    print(f"Image saved: {image_out}")

    print("2/2 Animating the image with Wan 2.2...")
    wan = make_client(WAN_SPACE, token)
    video_result = wan.predict(
        input_image={"path": str(image_out)},
        prompt=(
            "Animate the scene naturally with smooth cinematic camera movement, "
            "realistic motion, consistent character identity and detailed 3D rendering. "
            "No cuts, no subtitles, no text, no watermark."
        ),
        steps=6,
        negative_prompt=(
            "static, blurry, low quality, distorted face, deformed body, extra limbs, "
            "flicker, text, subtitles, watermark"
        ),
        duration_seconds=args.duration,
        guidance_scale=1,
        guidance_scale_2=1,
        seed=42,
        randomize_seed=True,
        api_name="/generate_video",
    )
    video_path = first_file(video_result)
    if not video_path or not Path(video_path).is_file():
        raise RuntimeError(f"Wan did not return a readable video file: {video_result!r}")
    video_out = OUT / "mplayer-ai-animation.mp4"
    shutil.copy2(video_path, video_out)

    metadata = {
        "original_prompt": args.prompt,
        "image_prompt": prompt,
        "image_model": QWEN_SPACE,
        "video_model": WAN_SPACE,
        "video_duration_seconds_requested": args.duration,
        "gemini_used": bool(os.getenv("GEMINI_API_KEY", "").strip()),
        "video_file": video_out.name,
    }
    (OUT / "generation.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"SUCCESS: {video_out}")
    print(f"Metadata: {OUT / 'generation.json'}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"GENERATION_FAILED: {exc}", file=sys.stderr)
        raise
