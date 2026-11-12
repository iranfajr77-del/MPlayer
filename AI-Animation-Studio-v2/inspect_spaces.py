import inspect
import json
import os
import traceback
from contextlib import redirect_stdout, redirect_stderr
from pathlib import Path
from io import StringIO

from gradio_client import Client

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
OUT.mkdir(parents=True, exist_ok=True)

SPACES = {
    "qwen_image": "mcp-tools/Qwen-Image",
    "wan_video": "zerogpu-aoti/wan2-2-fp8da-aoti-faster",
}


def make_client(space_id, token):
    """Support both Gradio Client APIs: older hf_token and newer token."""
    parameters = inspect.signature(Client).parameters
    kwargs = {"verbose": True}

    if token:
        if "token" in parameters:
            kwargs["token"] = token
        elif "hf_token" in parameters:
            kwargs["hf_token"] = token

    return Client(space_id, **kwargs)


def inspect_space(label, space_id, token):
    buffer = StringIO()
    result = {"label": label, "space": space_id, "ok": False}
    try:
        with redirect_stdout(buffer), redirect_stderr(buffer):
            client = make_client(space_id, token)
            client.view_api()
        result["ok"] = True
    except Exception as exc:
        result["error"] = repr(exc)
        result["traceback"] = traceback.format_exc()
    result["api_report"] = buffer.getvalue()
    return result


def main():
    token = os.environ.get("HF_TOKEN", "").strip()
    reports = [
        inspect_space(label, space_id, token)
        for label, space_id in SPACES.items()
    ]
    path = OUT / "huggingface-api-report.json"
    path.write_text(
        json.dumps(reports, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    for report in reports:
        print(f"SPACE {report['space']} OK={report['ok']}")
        if report.get("error"):
            print(report["error"])
        print(report["api_report"])

    if not all(report["ok"] for report in reports):
        raise SystemExit(
            "One or more Spaces could not be inspected; see the report artifact."
        )

    print(f"REPORT_READY: {path}")


if __name__ == "__main__":
    main()
