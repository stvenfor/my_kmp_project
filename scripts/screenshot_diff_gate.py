#!/usr/bin/env python3
"""Android Screenshot Diff Gate for Full Parity Phase (issue #2).

Compares Flutter Android SoT vs KMP Android screenshots and reports
mse_pct / mad_pct against the UI Parity Bar (default mse_pct <= 2.0).

Usage:
  python3 scripts/screenshot_diff_gate.py \\
    --flutter path/to/flutter.png \\
    --kmp path/to/kmp.png \\
    --out-dir openspec/changes/parity-flutter-to-kmp/notes/evidence/<route>/Android \\
    [--route home] [--bar 2.0] [--top 0.04] [--bot 0.90] [--width 540] [--height 960]

Exit 0 = pass, 1 = fail, 2 = usage/IO error.
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
from PIL import Image


def _crop_roi(arr: np.ndarray, top: float, bot: float) -> np.ndarray:
    h = arr.shape[0]
    y0 = int(h * top)
    y1 = int(h * bot)
    if y1 <= y0:
        raise ValueError(f"invalid ROI top={top} bot={bot}")
    return arr[y0:y1, :, :]


def _load_rgb(path: Path, size: tuple[int, int], top: float, bot: float) -> np.ndarray:
    img = Image.open(path).convert("RGB")
    arr = np.asarray(img, dtype=np.float32)
    arr = _crop_roi(arr, top, bot)
    pil = Image.fromarray(arr.astype(np.uint8), mode="RGB")
    pil = pil.resize(size, Image.Resampling.BILINEAR)
    return np.asarray(pil, dtype=np.float32)


def compare(
    flutter: Path,
    kmp: Path,
    *,
    top: float,
    bot: float,
    width: int,
    height: int,
) -> dict:
    a = _load_rgb(flutter, (width, height), top, bot)
    b = _load_rgb(kmp, (width, height), top, bot)
    diff = a - b
    mse = float(np.mean(diff * diff))
    mad = float(np.mean(np.abs(diff)))
    # Normalize to 0–100 scale relative to 255^2 / 255 (matches Phase-1 parity-report)
    mse_pct = mse / (255.0 * 255.0) * 100.0
    mad_pct = mad / 255.0 * 100.0
    heat = np.clip(np.mean(np.abs(diff), axis=2) / 255.0 * 255.0 * 4.0, 0, 255).astype(np.uint8)
    heat_rgb = np.stack([heat, np.zeros_like(heat), heat], axis=-1)
    return {
        "mse_pct": round(mse_pct, 4),
        "mad_pct": round(mad_pct, 4),
        "heat": heat_rgb,
        "size": [width, height],
    }


def main() -> int:
    p = argparse.ArgumentParser(description="Android Screenshot Diff Gate (Full Parity)")
    p.add_argument("--flutter", required=True, type=Path)
    p.add_argument("--kmp", required=True, type=Path)
    p.add_argument("--out-dir", required=True, type=Path)
    p.add_argument("--route", default="sample")
    p.add_argument("--bar", type=float, default=2.0, help="UI Parity Bar: max mse_pct")
    p.add_argument("--top", type=float, default=0.04)
    p.add_argument("--bot", type=float, default=0.90)
    p.add_argument("--width", type=int, default=540)
    p.add_argument("--height", type=int, default=960)
    args = p.parse_args()

    if not args.flutter.is_file() or not args.kmp.is_file():
        print("error: flutter/kmp image missing", file=sys.stderr)
        return 2

    args.out_dir.mkdir(parents=True, exist_ok=True)
    result = compare(
        args.flutter,
        args.kmp,
        top=args.top,
        bot=args.bot,
        width=args.width,
        height=args.height,
    )
    passed = result["mse_pct"] <= args.bar

    # Copy/normalize inputs for evidence packet
    flutter_out = args.out_dir / f"{args.route}.flutter.png"
    kmp_out = args.out_dir / f"{args.route}.kmp.png"
    heat_out = args.out_dir / f"{args.route}.heat.png"
    Image.open(args.flutter).convert("RGB").save(flutter_out)
    Image.open(args.kmp).convert("RGB").save(kmp_out)
    Image.fromarray(result["heat"], mode="RGB").save(heat_out)

    report = {
        args.route: {
            "mse_pct": result["mse_pct"],
            "mad_pct": result["mad_pct"],
            "pass_": passed,
            "bar_mse_pct": args.bar,
            "flutter": str(flutter_out),
            "kmp": str(kmp_out),
            "heat": str(heat_out),
        },
        "meta": {
            "gate": f"mse_pct<={args.bar}",
            "roi": f"top={args.top},bot={args.bot},resize={args.width}x{args.height}",
            "measured_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%MZ"),
            "script": "scripts/screenshot_diff_gate.py",
            "ios_ohos": "open-path + sampling only (not hard gate per ADR 0003)",
        },
    }
    report_path = args.out_dir / f"{args.route}.diff.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    status = "PASS" if passed else "FAIL"
    print(
        f"{status} route={args.route} mse_pct={result['mse_pct']} "
        f"mad_pct={result['mad_pct']} bar={args.bar} → {report_path}"
    )
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
