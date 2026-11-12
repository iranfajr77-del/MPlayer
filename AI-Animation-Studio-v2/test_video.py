import json
import os
import shutil
import inspect
from pathlib import Path
from urllib.request import urlretrieve

from gradio_client import Client
from PIL import Image

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
OUT.mkdir(parents=True, exist_ok=True)

IMAGE_SPACE = "mcp-tools/Qwen-Image"
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


def make_client(space_id, token):
    params = inspect.signature(Client).parameters
    kwargs = {"verbose": True}
    if token:
        if "token" in params:
            kwargs["token"] = token
        elif "hf_token" in params:
            kwargs["hf_token"] = token
    return Client(space_id, **kwargs)


def save_gradio_file(value, destination):
    # Gradio image outputs are commonly dictionaries with path/url;
    # video outputs are commonly local filepath strings.
    if isinstance(value, dict):
        local_path = value.get("path")
        remote_url = value.get("url")
        if local_path and Path(local_path).is_file():
            shutil.copy2(local_path, destination)
            return
        if remote_url:
            urlretrieve(remote_url, destination)
            return
        raise RuntimeError(f"Gradio returned no usable file path or URL: {value}")

    path = Path(str(value))
    if path.is_file():
        shutil.copy2(path, destination)
        return

    raise RuntimeError(f"Gradio returned an unavailable file: {value!r}")


def main():
    token = os.environ.get("HF_TOKEN", "").strip()

    print("Connecting to Qwen Image Space...")
    image_client = make_client(IMAGE_SPACE, token)
    image_result = image_client.predict(
        IMAGE_PROMPT,
        42,
        True,
        "9:16",
        NEGATIVE_PROMPT,
        4.0,
        24,
        api_name="/generate_image",
    )
    image_value = image_result[0] if isinstance(image_result, (tuple, list)) else image_result
    saved_image = OUT / "character-reference.png"
    save_gradio_file(image_value, saved_image)
    # Normalize possible WebP bytes returned by Qwen into a real PNG.
    with Image.open(saved_image) as generated:
        generated.convert("RGB").save(saved_image, format="PNG", optimize=True)
    print(f"IMAGE_READY: {saved_image} ({saved_image.stat().st_size} bytes, PNG)")

    print("Connecting to Wan 2.2 video Space...")
    video_client = make_client(VIDEO_SPACE, token)
    video_result = video_client.predict(
        {"path": str(saved_image)},
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
    video_value = video_result[0] if isinstance(video_result, (tuple, list)) else video_result
    final_video = OUT / "ai-animation-v2-test.mp4"
    save_gradio_file(video_value, final_video)

    metadata = {
        "image_model": IMAGE_SPACE,
        "video_model": VIDEO_SPACE,
        "duration_seconds_requested": 3.5,
        "aspect_ratio": "9:16",
        "image_prompt": IMAGE_PROMPT,
        "motion_prompt": MOTION_PROMPT,
        "note": "Visual-only proof of concept. Persian narration and subtitles are not included yet."
    }
    (OUT / "test-metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"VIDEO_READY: {final_video}")
    print(f"VIDEO_BYTES: {final_video.stat().st_size}")


if __name__ == "__main__":
    main()
