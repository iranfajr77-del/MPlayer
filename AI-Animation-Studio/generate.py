import argparse
import asyncio
import json
import math
import os
import re
import shutil
import subprocess
import textwrap
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
import edge_tts

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
WIDTH, HEIGHT, FPS = 720, 1280, 25
VOICE = "fa-IR-DilaraNeural"

SAMPLE_STORY = {
    "title": "آخرین حیوان جنگل",
    "scenes": [
        {"narration": "نیمه‌شب بود. خرگوش کوچکی در جنگل تاریک صدای کمک شنید.", "visual": "rabbit"},
        {"narration": "روباه از پشت درخت بیرون آمد و گفت: وارد جنگل نشو! آن صدا واقعی نیست.", "visual": "fox"},
        {"narration": "اما خرگوش صدای مادرش را شنید که نام او را صدا می‌زد.", "visual": "rabbit"},
        {"narration": "او میان مه جلو رفت و سایه‌ای بزرگ را پشت درخت‌ها دید.", "visual": "shadow"},
        {"narration": "ناگهان فهمید تمام حیوانات جنگل ناپدید شده‌اند؛ حتی روباه.", "visual": "empty"},
        {"narration": "درست وقتی برگشت، صدایی آرام پشت گوشش گفت: بالاخره پیدات کردم!", "visual": "shadow"}
    ]
}

def call_llm(topic):
    prompt = (
        "Write a suspenseful family-friendly Persian animal story for a short vertical animated video. "
        "Return ONLY valid JSON with keys title and scenes. scenes must be an array of exactly 6 objects; "
        "each object has narration (one or two short Persian sentences, suitable for spoken audio) and "
        "visual (one of rabbit, fox, shadow, empty). No markdown. Topic: " + topic
    )
    body = json.dumps({
        "model": "gpt-oss:20b",
        "messages": [
            {"role": "system", "content": "You are a creative Persian short-film writer. Output valid JSON only."},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.85,
        "max_tokens": 1200
    }).encode("utf-8")
    req = urllib.request.Request(
        "https://api.llm7.io/v1/chat/completions",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    token = os.getenv("LLM7_API_KEY", "").strip()
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=45) as response:
        payload = json.loads(response.read().decode("utf-8"))
    raw = payload["choices"][0]["message"]["content"]
    match = re.search(r"\{.*\}", raw, re.S)
    story = json.loads(match.group(0) if match else raw)
    scenes = story.get("scenes", [])
    if len(scenes) != 6:
        raise ValueError("The story model did not return exactly six scenes.")
    for scene in scenes:
        if not isinstance(scene.get("narration"), str) or not scene["narration"].strip():
            raise ValueError("A scene is missing narration.")
        if scene.get("visual") not in {"rabbit", "fox", "shadow", "empty"}:
            scene["visual"] = "rabbit"
    return {"title": str(story.get("title") or "داستان کوتاه"), "scenes": scenes}

def get_font(size):
    candidates = [
        "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()

def draw_tree(draw, x, y, scale, color):
    draw.rectangle((x - 9*scale, y, x + 9*scale, HEIGHT), fill=(30, 37, 34))
    for dx, dy, r in [(-35, -10, 55), (15, -45, 70), (55, 5, 52), (-5, 35, 62)]:
        draw.ellipse((x + (dx-r)*scale, y + (dy-r)*scale,
                      x + (dx+r)*scale, y + (dy+r)*scale), fill=color)

def draw_character(draw, kind, x, y, scale=1.0):
    if kind == "empty":
        return
    if kind == "shadow":
        draw.ellipse((x-95*scale, y-210*scale, x+95*scale, y+80*scale), fill=(8, 10, 12))
        draw.ellipse((x-55*scale, y-260*scale, x+55*scale, y-150*scale), fill=(5, 7, 9))
        draw.ellipse((x-34*scale, y-215*scale, x-12*scale, y-195*scale), fill=(255, 40, 35))
        draw.ellipse((x+12*scale, y-215*scale, x+34*scale, y-195*scale), fill=(255, 40, 35))
        return
    body = (225, 225, 220) if kind == "rabbit" else (195, 105, 55)
    draw.ellipse((x-70*scale, y-95*scale, x+70*scale, y+85*scale), fill=body)
    draw.ellipse((x-48*scale, y-190*scale, x+48*scale, y-95*scale), fill=body)
    if kind == "rabbit":
        draw.ellipse((x-43*scale, y-330*scale, x-4*scale, y-165*scale), fill=body)
        draw.ellipse((x+4*scale, y-330*scale, x+43*scale, y-165*scale), fill=body)
    else:
        draw.polygon([(x-40*scale,y-160*scale),(x-75*scale,y-245*scale),(x-5*scale,y-185*scale)], fill=body)
        draw.polygon([(x+40*scale,y-160*scale),(x+75*scale,y-245*scale),(x+5*scale,y-185*scale)], fill=body)
        draw.polygon([(x+45*scale,y+10*scale),(x+180*scale,y-10*scale),(x+80*scale,y+55*scale)], fill=body)
    draw.ellipse((x-22*scale,y-150*scale,x-10*scale,y-138*scale), fill=(15,20,24))
    draw.ellipse((x+10*scale,y-150*scale,x+22*scale,y-138*scale), fill=(15,20,24))
    draw.ellipse((x-5*scale,y-122*scale,x+5*scale,y-114*scale), fill=(35,20,20))

def make_scene(index, visual, title):
    im = Image.new("RGB", (WIDTH, HEIGHT))
    pix = im.load()
    top = (8, 18, 25)
    bottom = (20, 43, 36)
    for y in range(HEIGHT):
        t = y / HEIGHT
        c = tuple(int(top[i]*(1-t) + bottom[i]*t) for i in range(3))
        for x in range(WIDTH):
            pix[x, y] = c
    d = ImageDraw.Draw(im, "RGBA")
    # Moon and mist
    d.ellipse((490, 105, 620, 235), fill=(225, 231, 206, 230))
    for k in range(5):
        yy = 690 + k*95
        d.ellipse((-120-k*20, yy, 900+k*20, yy+150), fill=(170, 195, 185, 17))
    # Distant trees
    for x, y, s in [(-10,520,1.2),(150,570,.8),(690,540,1.15),(560,610,.8),(330,560,.65)]:
        draw_tree(d, x, y, s, (17, 35, 31, 255))
    # Fireflies
    for j in range(18):
        x = (j*137 + index*53) % WIDTH
        y = 340 + (j*83) % 650
        d.ellipse((x-3,y-3,x+3,y+3), fill=(213, 235, 135, 180))
    if visual == "rabbit":
        draw_character(d, "rabbit", 340, 1040, 1.0)
    elif visual == "fox":
        draw_character(d, "fox", 350, 1040, 1.0)
        draw_character(d, "rabbit", 145, 1080, .55)
    elif visual == "shadow":
        draw_character(d, "rabbit", 175, 1080, .62)
        draw_character(d, "shadow", 500, 970, 1.15)
    else:
        draw_character(d, "rabbit", 360, 1070, .7)
    # Scene marker/title
    d.rounded_rectangle((35, 42, 685, 125), radius=24, fill=(3, 9, 12, 155), outline=(130, 190, 120, 100), width=2)
    d.text((WIDTH//2, 83), f"AI ANIMATION  •  SCENE {index:02}", font=get_font(28), fill=(240, 246, 230), anchor="mm")
    return im

async def make_voice(text, path):
    talk = edge_tts.Communicate(text=text, voice=VOICE, rate="+0%", volume="+0%")
    await talk.save(str(path))

def probe_duration(path):
    result = subprocess.check_output([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1", str(path)
    ], text=True).strip()
    return float(result)

def srt_time(seconds):
    ms = int(round(seconds * 1000))
    h, ms = divmod(ms, 3600000)
    m, ms = divmod(ms, 60000)
    s, ms = divmod(ms, 1000)
    return f"{h:02}:{m:02}:{s:02},{ms:03}"

def run():
    parser = argparse.ArgumentParser()
    parser.add_argument("--topic", default="داستان عجیب و ترسناک حیوانات در جنگل")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    for p in OUT.iterdir():
        if p.is_file():
            p.unlink()
    try:
        story = call_llm(args.topic)
        source = "LLM7"
    except Exception as exc:
        print("LLM7 unavailable; using bundled sample story:", str(exc))
        story = SAMPLE_STORY
        source = "sample-fallback"

    (OUT / "story.json").write_text(
        json.dumps({"source": source, **story}, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    concat_file = OUT / "clips.txt"
    subtitle_lines = []
    current = 0.0
    clip_paths = []
    for i, scene in enumerate(story["scenes"], start=1):
        narration = scene["narration"].strip()
        audio = OUT / f"voice_{i:02}.mp3"
        image = OUT / f"scene_{i:02}.png"
        clip = OUT / f"clip_{i:02}.mp4"
        asyncio.run(make_voice(narration, audio))
        duration = probe_duration(audio)
        make_scene(i, scene.get("visual", "rabbit"), story["title"]).save(image)
        frames = max(1, math.ceil(duration * FPS))
        vf = f"zoompan=z='min(zoom+0.0007,1.10)':d={frames}:s={WIDTH}x{HEIGHT}:fps={FPS},format=yuv420p"
        subprocess.run([
            "ffmpeg", "-y", "-loglevel", "error",
            "-loop", "1", "-framerate", str(FPS), "-i", str(image),
            "-i", str(audio), "-vf", vf, "-t", f"{duration:.3f}",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "25",
            "-c:a", "aac", "-b:a", "128k", "-shortest", str(clip)
        ], check=True)
        clip_paths.append(clip)
        subtitle_lines.extend([
            str(i),
            f"{srt_time(current)} --> {srt_time(current + duration)}",
            narration,
            ""
        ])
        current += duration
        print(f"Rendered scene {i}/6 ({duration:.1f}s)")

    concat_file.write_text(
        "".join(f"file '{p.name}'\n" for p in clip_paths), encoding="utf-8"
    )
    raw_video = OUT / "joined.mp4"
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0",
        "-i", str(concat_file), "-c", "copy", str(raw_video)
    ], check=True)
    srt = OUT / "subtitles.srt"
    srt.write_text("\n".join(subtitle_lines), encoding="utf-8")
    final_video = OUT / "ai-animation.mp4"
    # Burn Persian subtitles into the final video; keep the standalone SRT too.
    srt_filter_path = str(srt.resolve()).replace("\\", "/").replace(":", r"\:")
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(raw_video),
        "-vf", f"subtitles='{srt_filter_path}':force_style='FontName=Noto Sans,FontSize=24,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=1,Alignment=2,MarginV=55'",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "24",
        "-c:a", "copy", "-movflags", "+faststart", str(final_video)
    ], check=True)
    raw_video.unlink(missing_ok=True)
    concat_file.unlink(missing_ok=True)
    print("DONE:", final_video)
    print("STORY SOURCE:", source)

if __name__ == "__main__":
    run()
