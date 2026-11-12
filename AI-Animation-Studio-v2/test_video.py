import json
import os
import shutil
from pathlib import Path

from gradio_client import Client

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
OUT.mkdir(parents=True, exist_ok=True)

IMAGE_SPACE = "Qwen/Qwen-Image"
VIDEO_SPACE = "zerogpu-aoti/wan2-2-fp8da-aoti-faster"

IMAGE_PROMPT = """
A charming small anthropomorphic white rabbit child, large expressive amber eyes,
soft detailed white fur, wearing a tiny deep teal scarf, standing in a mysterious
moonlit enchanted forest. Premium 3D animated feature-film character design,
stylized high-end CGI, beautiful expressive face, detailed fur groom, cinematic
volumetric moonlight, soft mist, rich forest depth, dramatic composition,
professional animation movie still, vertical 9:16 framing. One single rabbit,
full body, visually appealing and emotionally expressive. No text, no logo,
no watermark, no extra limbs, no distorted anatomy.
""".strip()

MOTION_PROMPT = """
The same small white rabbit slowly turns its head toward a strange sound,
its ears twitch naturally, eyes widen with fear, and it takes one cautious
step backward. Leaves and soft mist move gently in the night breeze.
The camera makes a subtle cinematic push-in. Preserve the exact rabbit design,
fur, scarf, proportions and forest setting from the input image.
Fluid natural character animation, cinematic 3D animated feature-film quality.
""".strip()

NEGATIVE_PROMPT = (
    "static image, frozen motion, low quality, blurry, oversaturated, "
    "bad anatomy, deformed face, extra limbs, extra fingers, distorted eyes, "
    "flicker, jitter, inconsistent character, text, subtitles, watermark"
)


def main():
    token = os.environ.get("HF_TOKEN", "").strip()
    if not token:
        raise RuntimeError(
            "Missing HF_TOKEN. Add a Hugging Face read token as a GitHub Actions secret named HF_TOKEN."
        )

    image_client = Client(IMAGE_SPACE, token=token, verbose=True)
    try:
        image_result = image_client.predict(
            IMAGE_PROMPT,
            42,
            True,
            "9:16",
            4.0,
            24,
            False,
            api_name="/infer",
        )
    except Exception:
        print("Qwen Image API discovery:")
        image_client.view_api()
        raise

    image_value = image_result[0] if isinstance(image_result, (tuple, list)) else image_result
    image_path = Path(str(image_value))
    if not image_path.is_file():
        raise RuntimeError(f"Qwen Image did not return a local image file: {image_value}")

    saved_image = OUT / "character-reference.png"
    shutil.copy2(image_path, saved_image)
    print(f"IMAGE_READY: {saved_image}")

    video_client = Client(VIDEO_SPACE, token=token, verbose=True)
    try:
        video_result = video_client.predict(
            str(saved_image),
            MOTION_PROMPT,
            6,
            NEGATIVE_PROMPT,
            3.5,
            1.0,
            1.0,
            42,
            True,
            api_name="/generate_video",
        )
    except Exception:
        print("Wan 2.2 API discovery:")
        video_client.view_api()
        raise

    video_value = video_result[0] if isinstance(video_result, (tuple, list)) else video_result
    video_path = Path(str(video_value))
    if not video_path.is_file():
        raise RuntimeError(f"Wan 2.2 did not return a local video file: {video_value}")

    final_video = OUT / "ai-animation-v2-test.mp4"
    shutil.copy2(video_path, final_video)

    metadata = {
        "image_model": IMAGE_SPACE,
        "video_model": VIDEO_SPACE,
        "duration_target_seconds": 3.5,
        "image_prompt": IMAGE_PROMPT,
        "motion_prompt": MOTION_PROMPT,
        "note": "Visual-only proof of concept. Persian narration and subtitles are not included yet."
    }
    (OUT / "test-metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"VIDEO_READY: {final_video}")


if __name__ == "__main__":
    main()
