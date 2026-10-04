#!/usr/bin/env bash
# Regenerate every raster icon from kst4contest.svg.
#
# The rasters are committed, so nothing in CI runs this — it exists so the
# icons can be rebuilt reproducibly whenever the SVG changes. Requires
# rsvg-convert (librsvg) and Python Pillow.
#
#   packaging/icons/generate-icons.sh
#
# Outputs, all regenerated in place and mirrored into src/main/resources/icons
# so the in-app window icon matches what the launcher shows:
#   kst4contest.png   256px, the size jpackage --icon feeds to every Linux
#                     package and the size hicolor/256x256/apps expects
#   kst4contest.ico   16/24/32/48/64 as BMP, 128/256 as PNG (Windows convention)
#   kst4contest.icns  ic07-ic14, the modern macOS set up to 1024px

set -euo pipefail

ICON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${ICON_DIR}/../.." && pwd)"
cd "$REPO_ROOT"

for tool in rsvg-convert python3; do
    command -v "$tool" >/dev/null || { echo "Missing: $tool" >&2; exit 1; }
done
python3 -c "import PIL" 2>/dev/null || { echo "Missing: Python Pillow" >&2; exit 1; }

# The SVG asks for 'Space Grotesk' and falls back to Helvetica Neue, then Arial.
# Whichever is installed is baked into the raster, so a machine with a different
# font set produces visibly different icons. Warn rather than fail — Arial is
# the author's own third choice and renders acceptably.
if ! fc-list 2>/dev/null | grep -qi "space grotesk"; then
    echo "WARNING: 'Space Grotesk' is not installed; the KST wordmark will be"
    echo "         rendered with a fallback font (checked: $(fc-match 'Space Grotesk' 2>/dev/null | head -1))."
fi

python3 - <<'PY'
import struct, subprocess, os
from PIL import Image

SRC, OUT = "packaging/icons/kst4contest.svg", "packaging/icons"
_cache = {}

def render(size):
    if size not in _cache:
        p = f"/tmp/kst4contest-icon-{size}.png"
        subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), SRC, "-o", p], check=True)
        _cache[size] = p
    return _cache[size]

# --- Linux -------------------------------------------------------------
Image.open(render(256)).convert("RGBA").save(f"{OUT}/kst4contest.png", optimize=True)

# --- Windows -----------------------------------------------------------
def bmp_entry(im):
    w, h = im.size
    px = im.load()
    hdr = struct.pack("<IiiHHIIiiII", 40, w, h * 2, 1, 32, 0, 0, 0, 0, 0, 0)
    rows = [bytes(bytearray().join(
        bytes((px[x, y][2], px[x, y][1], px[x, y][0], px[x, y][3])) for x in range(w)
    )) for y in range(h - 1, -1, -1)]
    mask = bytes(((w + 31) // 32) * 4 * h)
    return hdr + b"".join(rows) + mask

entries = []
for size in [16, 24, 32, 48, 64]:
    entries.append((size, bmp_entry(Image.open(render(size)).convert("RGBA"))))
for size in [128, 256]:
    entries.append((size, open(render(size), "rb").read()))

offset = 6 + 16 * len(entries)
dir_, blobs = b"", b""
for size, data in entries:
    dim = 0 if size == 256 else size
    dir_ += struct.pack("<BBBBHHII", dim, dim, 0, 0, 1, 32, len(data), offset)
    blobs += data
    offset += len(data)
open(f"{OUT}/kst4contest.ico", "wb").write(struct.pack("<HHH", 0, 1, len(entries)) + dir_ + blobs)

# --- macOS -------------------------------------------------------------
chunks = b""
for t, size in [("ic11", 32), ("ic12", 64), ("ic07", 128), ("ic13", 256),
                ("ic08", 256), ("ic14", 512), ("ic09", 512), ("ic10", 1024)]:
    data = open(render(size), "rb").read()
    chunks += t.encode("ascii") + struct.pack(">I", len(data) + 8) + data
open(f"{OUT}/kst4contest.icns", "wb").write(b"icns" + struct.pack(">I", len(chunks) + 8) + chunks)
PY

cp packaging/icons/kst4contest.{png,ico,icns} src/main/resources/icons/
echo "Regenerated packaging/icons and src/main/resources/icons from kst4contest.svg"
