# app_usage_tracker.py
"""
Track real desktop application usage by sampling the foreground app and
persisting aggregated metrics to `app_usage.json`.
"""
import argparse
import json
import time
from collections import defaultdict
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional

try:
    # PyObjC bridges macOS APIs into Python.
    from AppKit import NSWorkspace  # type: ignore
except ImportError as exc:  # pragma: no cover - dependency notice
    raise SystemExit(
        "PyObjC is required for app usage tracking. "
        "Install it via `pip install pyobjc`."
    ) from exc


DEFAULT_OUTPUT = Path("app_usage.json")
DEFAULT_DURATION = 300  # seconds
DEFAULT_INTERVAL = 1.0  # seconds


@dataclass
class AppUsage:
    """Aggregated usage metrics for a single app."""

    usage_seconds: float = 0.0
    sample_count: int = 0
    last_seen: Optional[str] = None


def get_frontmost_app_name() -> Optional[str]:
    """Return the localized name of the frontmost macOS application."""
    workspace = NSWorkspace.sharedWorkspace()
    app = workspace.frontmostApplication()
    if app is None:
        return None
    return app.localizedName()


def load_existing_usage(file_path: Path) -> Dict[str, AppUsage]:
    """Load existing usage stats from disk for incremental updates."""
    if not file_path.exists():
        return {}

    try:
        with file_path.open("r", encoding="utf-8") as handle:
            payload = json.load(handle)
    except (json.JSONDecodeError, OSError) as err:
        print(f"⚠️  Unable to read existing usage data ({err}); starting fresh.")
        return {}

    usage: Dict[str, AppUsage] = {}
    for app in payload.get("apps", []):
        name = app.get("name")
        if not name:
            continue
        usage_seconds = float(app.get("usage_count", 0))
        has_sample_count = "sample_count" in app
        sample_count = int(app.get("sample_count", 0))
        last_seen = app.get("last_seen")

        if not has_sample_count:
            # Legacy files tracked per-process presence without real sampling data.
            usage_seconds = 0.0
            sample_count = 0
            last_seen = None

        usage[name] = AppUsage(
            usage_seconds=usage_seconds,
            sample_count=sample_count,
            last_seen=last_seen,
        )
    return usage


def write_usage(file_path: Path, usage: Dict[str, AppUsage], interval: float) -> None:
    """Persist usage metrics in a JSON schema compatible with the Java app."""
    apps = []
    for name, stats in sorted(
        usage.items(), key=lambda item: item[1].usage_seconds, reverse=True
    ):
        if stats.sample_count == 0:
            continue
        apps.append(
            {
                "name": name,
                # Maintain compatibility: `usage_count` now reflects seconds of focus time.
                "usage_count": round(stats.usage_seconds),
                "sample_count": stats.sample_count,
                "last_seen": stats.last_seen,
            }
        )

    output = {
        "timestamp": datetime.now().isoformat(),
        "sample_interval_seconds": interval,
        "apps": apps,
    }

    with file_path.open("w", encoding="utf-8") as handle:
        json.dump(output, handle, indent=4)


def track_usage(duration: float, interval: float, destination: Path) -> None:
    """Sample the active app at the chosen interval for the specified duration."""
    usage = load_existing_usage(destination)
    start_time = time.time()

    while True:
        now = time.time()
        elapsed = now - start_time
        if elapsed >= duration:
            break

        step = min(interval, duration - elapsed)
        active_app = get_frontmost_app_name()
        if active_app:
            stats = usage.setdefault(active_app, AppUsage())
            stats.usage_seconds += step
            stats.sample_count += 1
            stats.last_seen = datetime.now().isoformat()

        time.sleep(step)

    write_usage(destination, usage, interval)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Track foreground macOS app usage and update app_usage.json."
    )
    parser.add_argument(
        "--duration",
        type=float,
        default=DEFAULT_DURATION,
        help="Tracking duration in seconds (default: %(default)s).",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=DEFAULT_INTERVAL,
        help="Sampling interval in seconds (default: %(default)s).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Path to the usage JSON file (default: %(default)s).",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    print(
        f"📊 Tracking app usage for {args.duration} seconds "
        f"at {args.interval}-second intervals…"
    )
    track_usage(duration=args.duration, interval=args.interval, destination=args.output)
    print(f"✅ Usage data written to {args.output}")


if __name__ == "__main__":
    main()
