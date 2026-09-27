#!/usr/bin/env python3
"""Seam test for scripts/screenshot_diff_gate.py (Full Parity #2)."""
from __future__ import annotations

import json
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "screenshot_diff_gate.py"


def _solid(path: Path, rgb: tuple[int, int, int], size=(200, 400)) -> None:
    Image.fromarray(np.full((size[1], size[0], 3), rgb, dtype=np.uint8), "RGB").save(path)


def main() -> int:
    with tempfile.TemporaryDirectory() as td:
        td_path = Path(td)
        a = td_path / "a.png"
        b = td_path / "b.png"
        out = td_path / "out"
        _solid(a, (10, 20, 30))
        _solid(b, (10, 20, 30))
        r = subprocess.run(
            [
                sys.executable,
                str(SCRIPT),
                "--flutter",
                str(a),
                "--kmp",
                str(b),
                "--out-dir",
                str(out),
                "--route",
                "identical",
                "--bar",
                "2.0",
            ],
            capture_output=True,
            text=True,
        )
        assert r.returncode == 0, r.stderr + r.stdout
        report = json.loads((out / "identical.diff.json").read_text())
        assert report["identical"]["pass_"] is True
        assert report["identical"]["mse_pct"] == 0.0

        _solid(b, (200, 0, 0))
        r2 = subprocess.run(
            [
                sys.executable,
                str(SCRIPT),
                "--flutter",
                str(a),
                "--kmp",
                str(b),
                "--out-dir",
                str(out),
                "--route",
                "divergent",
                "--bar",
                "0.01",
            ],
            capture_output=True,
            text=True,
        )
        assert r2.returncode == 1, r2.stdout
        report2 = json.loads((out / "divergent.diff.json").read_text())
        assert report2["divergent"]["pass_"] is False
        assert report2["divergent"]["mse_pct"] > 0.01
    print("test_screenshot_diff_gate: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
