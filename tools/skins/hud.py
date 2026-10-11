"""Les textures du HUD : le voile de la vue perdue et l'icône de la jauge de concentration.
`python tools/skins/hud.py` les régénère dans textures/gui/hud.
"""
from __future__ import annotations

import math
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "fmab", "textures",
                   "gui", "hud")


def lost_sight(size=256):
    """Le voile noir de la vue que la Vérité a prise : un trou clair au centre, qui se brouille en noir
    vers les bords. Un dégradé doux (étiré à l'écran) et un léger grain pour qu'il ne paraisse pas lisse."""
    rng = random.Random("lost_sight")
    img = Image.new("RGBA", (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot((x - c) / c, (y - c) / c)
            t = min(1.0, max(0.0, (d - 0.28) / 0.62))
            t = t * t * (3 - 2 * t)
            t = min(1.0, max(0.0, t + rng.uniform(-0.03, 0.03) * t))
            img.putpixel((x, y), (6, 4, 8, int(t * 250)))
    return img


def concentration_icon():
    """L'icône de la jauge : un petit cercle de transmutation bleu, un triangle inscrit."""
    img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    ring = (79, 163, 255, 255)
    for y in range(9):
        for x in range(9):
            d = math.hypot(x - 4, y - 4)
            if abs(d - 3.6) < 0.6:
                img.putpixel((x, y), ring)
    for x, y in ((4, 1), (3, 3), (5, 3), (2, 5), (6, 5), (3, 6), (4, 6), (5, 6)):
        img.putpixel((x, y), (200, 228, 255, 255))
    return img


def build():
    os.makedirs(OUT, exist_ok=True)
    lost_sight().save(os.path.join(OUT, "lost_sight.png"))
    concentration_icon().save(os.path.join(OUT, "concentration.png"))
    print("hud: lost_sight, concentration")


if __name__ == "__main__":
    build()
