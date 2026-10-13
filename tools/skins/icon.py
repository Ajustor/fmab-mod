"""L'icône du mod (128x128), celle des listes de mods : un cercle de transmutation tracé à l'encre
sur un parchemin, et au centre la croix de Flamel (le serpent enroulé sur la croix, couronné et ailé)
en rouge, celle qu'Edward porte dans le dos.
`python tools/skins/icon.py` la régénère dans assets/fmab/icon.png.
"""
from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "fmab", "icon.png")
SIZE = 128
# Dessinée en grand puis réduite : les traits fins restent lisses.
SCALE = 4
PAPER = (239, 227, 196, 255)
PAPER_EDGE = (205, 184, 140, 255)
INK = (52, 40, 34, 255)
RED = (176, 32, 30, 255)
RED_DARK = (120, 18, 20, 255)


def s(v):
    return v * SCALE


def w(v):
    """Une épaisseur de trait, en pixels entiers de l'image en grand."""
    return round(v * SCALE)


def circle(d, cx, cy, r, width, color):
    d.ellipse((s(cx - r), s(cy - r), s(cx + r), s(cy + r)), outline=color, width=w(width))


def polygon(d, points, width, color):
    pts = [(s(x), s(y)) for x, y in points]
    d.line(pts + [pts[0]], fill=color, width=w(width), joint="curve")


def ring_points(cx, cy, r, n, start):
    return [(cx + r * math.cos(start + 2 * math.pi * i / n), cy + r * math.sin(start + 2 * math.pi * i / n))
            for i in range(n)]


def transmutation_circle(d, c):
    """Double anneau, graduations entre les deux, hexagramme et petits cercles à ses pointes."""
    circle(d, c, c, 58, 2.5, INK)
    circle(d, c, c, 50, 1.5, INK)
    for i in range(48):
        a = 2 * math.pi * i / 48
        r0, r1 = (50, 58) if i % 4 == 0 else (50, 54)
        d.line((s(c + r0 * math.cos(a)), s(c + r0 * math.sin(a)), s(c + r1 * math.cos(a)), s(c + r1 * math.sin(a))),
               fill=INK, width=w(1))
    up = ring_points(c, c, 50, 3, -math.pi / 2)
    down = ring_points(c, c, 50, 3, math.pi / 2)
    polygon(d, up, 1.5, INK)
    polygon(d, down, 1.5, INK)
    for x, y in up + down:
        d.ellipse((s(x - 4), s(y - 4), s(x + 4), s(y + 4)), fill=PAPER, outline=INK, width=w(1.5))
    circle(d, c, c, 30, 1.5, INK)


def flamel(d, c):
    """La croix de Flamel : la croix, le serpent qui s'y enroule, la couronne et les ailes."""
    # Le cœur du cercle reste vierge : la croix s'y détache.
    d.ellipse((s(c - 29), s(c - 29), s(c + 29), s(c + 29)), fill=PAPER)
    top = c - 22
    # Les ailes, sous la couronne : déployées vers le haut, le bord inférieur en trois plumes.
    for side in (-1, 1):
        wing = [(c + side * 3, top + 5), (c + side * 12, top - 4), (c + side * 24, top - 6),
                (c + side * 21, top + 1), (c + side * 23, top + 3), (c + side * 17, top + 6),
                (c + side * 18, top + 9), (c + side * 11, top + 10), (c + side * 3, top + 11)]
        d.polygon([(s(x), s(y)) for x, y in wing], fill=RED)
        for k in (0, 1):
            fx, fy = (c + side * 21, top + 1) if k == 0 else (c + side * 17, top + 6)
            d.line((s(c + side * 6), s(top + 4 + k * 3), s(fx), s(fy)), fill=RED_DARK, width=w(1))
    # La croix.
    d.rectangle((s(c - 2.5), s(top), s(c + 2.5), s(c + 27)), fill=RED)
    d.rectangle((s(c - 11), s(c - 4), s(c + 11), s(c + 1)), fill=RED)
    # La couronne, posée en haut de la croix.
    crown = [(c - 7, top), (c - 8, top - 7), (c - 4, top - 3), (c, top - 9), (c + 4, top - 3), (c + 8, top - 7),
             (c + 7, top)]
    d.polygon([(s(x), s(y)) for x, y in crown], fill=RED)
    # Le serpent : une sinusoïde qui s'enroule sur la croix, de la tête (en haut) à la queue.
    pts = []
    for i in range(81):
        t = i / 80
        y = c - 9 + t * 35
        x = c + 9 * math.sin(t * 3 * math.pi + 0.5) * (1 - 0.45 * t)
        pts.append((s(x), s(y)))
    d.line(pts, fill=RED_DARK, width=w(4.5), joint="curve")
    d.line(pts, fill=RED, width=w(2.6), joint="curve")
    hx, hy = pts[0]
    d.ellipse((hx - s(3.5), hy - s(2.8), hx + s(3.5), hy + s(2.8)), fill=RED_DARK)


def icon():
    big = SIZE * SCALE
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, big - 1, big - 1), radius=w(18), fill=PAPER_EDGE)
    d.rounded_rectangle((s(3), s(3), big - 1 - s(3), big - 1 - s(3)), radius=w(15), fill=PAPER)
    c = SIZE / 2
    transmutation_circle(d, c)
    flamel(d, c)
    return img.resize((SIZE, SIZE), Image.LANCZOS)


def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    icon().save(OUT)
    print("icon.png")


if __name__ == "__main__":
    main()
