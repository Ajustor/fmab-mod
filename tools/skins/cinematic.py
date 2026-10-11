"""Les images des cinématiques de la Porte : l'œil qui s'ouvre derrière la Porte, et le bras d'ombre
qui rampe depuis les bords de l'écran quand le cercle happe l'alchimiste.
`python tools/skins/cinematic.py` les régénère dans textures/gui/cinematic.
"""
from __future__ import annotations

import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from skinlib import mix, rgb, shade  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "fmab", "textures",
                   "gui", "cinematic")


def eye():
    """L'œil de la Porte, 128x64 : une amande blanche ombrée sous la paupière, des veinules, un iris
    sombre cerclé de rouge, la pupille au centre exact de l'image (on zoome dedans)."""
    w, h = 128, 64
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rng = random.Random("eye")
    cx, cy = w / 2 - 0.5, h / 2 - 0.5
    rx, ry = 60, 28
    iris_r, pupil_r = 19, 8
    for y in range(h):
        for x in range(w):
            # L'amande : deux arcs qui se rejoignent en pointe aux coins.
            nx = (x - cx) / rx
            if abs(nx) >= 1:
                continue
            half = ry * (1 - nx * nx) ** 0.8
            dy = y - cy
            if abs(dy) > half:
                continue
            edge = half - abs(dy)
            d = math.hypot(x - cx, y - cy)
            if d <= pupil_r:
                c = rgb("#030204")
            elif d <= iris_r:
                # L'iris : du noir près de la pupille au rouge sombre au bord, strié.
                t = (d - pupil_r) / (iris_r - pupil_r)
                ang = math.atan2(y - cy, x - cx)
                streak = 0.85 + 0.15 * math.sin(ang * 23)
                c = shade(mix("#140a10", "#7a121c", t ** 1.5), streak)
                if d > iris_r - 1.5:
                    c = rgb("#1a0c10")
            else:
                # Le blanc : ombré sous la paupière du haut et vers les coins.
                k = 1 - 0.35 * max(0, 1 - (dy + half) / (half * 0.9)) - 0.25 * abs(nx) ** 3
                c = shade("#ece8de", max(0.45, k))
            if edge < 1.6 and d > iris_r:
                c = rgb("#1c1418")
            img.putpixel((x, y), c)
    # Des veinules rouges qui partent des coins vers l'iris.
    for side in (-1, 1):
        for _ in range(4):
            x, y = cx + side * (rx - 6), cy + rng.uniform(-6, 6)
            for _ in range(rng.randint(10, 22)):
                x -= side * rng.uniform(0.6, 1.4)
                y += rng.uniform(-0.9, 0.9)
                xi, yi = int(round(x)), int(round(y))
                if 0 <= xi < w and 0 <= yi < h and img.getpixel((xi, yi))[3] and \
                        math.hypot(xi - cx, yi - cy) > iris_r + 1:
                    img.putpixel((xi, yi), mix(img.getpixel((xi, yi)), (150, 20, 30, 255), 0.6))
    # Le reflet, en haut à gauche de la pupille.
    for x, y in ((cx - 5, cy - 5), (cx - 4, cy - 5), (cx - 5, cy - 4), (cx - 4, cy - 4), (cx - 2, cy - 6)):
        img.putpixel((int(x), int(y)), rgb("#ffffff"))
    return img


def arm():
    """Un bras d'ombre, 32x128, la main en haut : il part large du bord de l'écran et s'effile, les
    doigts écartés. Des bords flous, un reflet violacé sur un flanc."""
    w, h = 32, 128
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ink = rgb("#08060c")
    sheen = rgb("#2e2244")

    def put(x, y, a, c=ink):
        if 0 <= x < w and 0 <= y < h:
            old = img.getpixel((x, y))
            if a > old[3]:
                img.putpixel((x, y), (*c[:3], a))

    def blob(x, y, r, c=ink):
        for yy in range(int(y - r - 1), int(y + r + 2)):
            for xx in range(int(x - r - 1), int(x + r + 2)):
                d = math.hypot(xx - x, yy - y)
                if d <= r + 0.8:
                    put(xx, yy, int(255 * min(1, r + 0.8 - d)), c)

    # Le bras : une coulée ondulante, de 7 pixels de rayon en bas à 3 sous le poignet.
    for y in range(h - 1, 28, -1):
        t = (h - y) / (h - 28)
        x = w / 2 + math.sin(t * 5.5) * 3 * (1 - t * 0.5)
        r = 7 - 4 * t
        blob(x, y, r)
        if y % 2 == 0:
            put(int(x - r + 1.5), y, 200, sheen)
    # La paume, puis quatre doigts écartés et un pouce.
    px, py = w / 2 + math.sin(5.5) * 1.5, 24
    for k in range(5):
        blob(px, py - k * 0.8, 4.2)
    fingers = [(-0.55, 13), (-0.18, 16), (0.15, 15), (0.48, 12)]
    for angle, length in fingers:
        for s in range(length):
            x = px + math.sin(angle) * (s + 3) + math.sin(s * 0.5 + angle * 4) * 0.4
            y = py - 3 - math.cos(angle) * (s + 3)
            blob(x, y, 1.3 - s / length * 0.5)
    for s in range(9):
        blob(px - 4 - s * 0.85, py + 1 - s * 0.55, 1.2)
    return img


def build():
    os.makedirs(OUT, exist_ok=True)
    eye().save(os.path.join(OUT, "eye.png"))
    arm().save(os.path.join(OUT, "arm.png"))
    print("cinématique : eye, arm")


if __name__ == "__main__":
    build()
