"""Derive adaptive-icon monochrome layers from the full-color foreground layers.

Controller body/shading stay opaque, details (sticks, d-pad, buttons) become holes.
Variant banner stays opaque with its label cut out.

Needed because Icon Kitchen copies raster foregrounds verbatim into the monochrome layer. Colors are tuned to the
current Flaticon controller clipart: revisit SOLID/HOLES when the artwork changes. Requires Pillow.

Usage: python scripts/gen_monochrome_icons.py androidApp/src/main/res androidApp/src/alpha/res ...
"""
import math
import sys
from pathlib import Path

from PIL import Image

SOLID = [(215, 230, 240), (165, 195, 220)]
HOLES = [(85, 90, 110), (70, 60, 75), (255, 110, 145), (0, 210, 210)]


def nearest(rgb: tuple[int, int, int], palette: list[tuple[int, int, int]]) -> float:
    return min(math.dist(rgb, p) for p in palette)


def banner_top(im: Image.Image) -> int:
    w, h = im.size
    for y in range(h // 2, h):
        if all(im.getpixel((x, y))[3] > 200 for x in range(0, w, max(1, w // 54))):
            return y
    return h


def monochrome(fg: Image.Image) -> Image.Image:
    w, h = fg.size
    top = banner_top(fg)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    src, dst = fg.load(), out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            if y >= top:
                keep = 1 - (0.299 * r + 0.587 * g + 0.114 * b) / 255
            else:
                ds, dh = nearest((r, g, b), SOLID), nearest((r, g, b), HOLES)
                t = dh / (ds + dh) if ds + dh else 1
                keep = min(1, max(0, (t - 0.25) / 0.5))
            dst[x, y] = (0, 0, 0, round(a * keep))
    return out


for res in map(Path, sys.argv[1:]):
    for fg_path in sorted(res.glob("mipmap-*dpi/ic_launcher*_foreground.png")):
        mono_path = fg_path.with_name(fg_path.name.replace("_foreground", "_monochrome"))
        monochrome(Image.open(fg_path).convert("RGBA")).save(mono_path, optimize=True)
        print(mono_path)
