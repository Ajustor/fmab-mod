"""Les œufs d'apparition, à la manière des œufs vanilla récents : chaque œuf est un petit portrait
de son personnage (coiffe en haut, visage au milieu, tenue en bas), avec un vrai volume.
`python tools/skins/eggs.py [nom ...]` les régénère dans textures/item.
"""
from __future__ import annotations

import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from skinlib import mix, rgb, shade  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "fmab", "textures",
                   "item")

# La silhouette : pour chaque ligne, la première et la dernière colonne de l'œuf.
PROFILE = {1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (2, 13), 9: (2, 13),
           10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (3, 12), 14: (4, 11), 15: (6, 9)}


def inside(x, y):
    return y in PROFILE and PROFILE[y][0] <= x <= PROFILE[y][1]


class Egg:
    def __init__(self, name):
        self.name = name
        self.rng = random.Random(name)
        self.base = {}

    def paint(self, y0, y1, color):
        """Une bande horizontale de l'œuf (bornes incluses)."""
        for y in range(y0, y1 + 1):
            if y in PROFILE:
                for x in range(PROFILE[y][0], PROFILE[y][1] + 1):
                    self.base[(x, y)] = rgb(color)

    def px(self, x, y, color):
        if inside(x, y):
            self.base[(x, y)] = rgb(color)

    def row(self, y, x0, x1, color):
        for x in range(x0, x1 + 1):
            self.px(x, y, color)

    def render(self):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.base.items():
            # Une lumière venue d'en haut à gauche : reflet, ombre propre, puis le contour.
            lx, ly = (x - 5.5) / 6, (y - 5) / 8
            k = 1.12 - 0.22 * (lx * 0.6 + ly * 0.8) + self.rng.uniform(-0.025, 0.025)
            col = shade(c, k) if k >= 1 else mix(shade(c, k), (40, 30, 70, 255), 0.1)
            edge = not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                col = mix(shade(c, 0.55), (30, 20, 50, 255), 0.25)
            img.putpixel((x, y), col)
        # Le reflet : deux pixels clairs en haut à gauche.
        for x, y in ((5, 3), (4, 4)):
            c = img.getpixel((x, y))
            if c[3]:
                img.putpixel((x, y), mix(c, (255, 255, 255, 255), 0.45))
        return img


def portrait(e, hair, skin, cloth, eye="#20181c", hair_to=5, face_to=9, mouth=None, fringe=True):
    """Le schéma commun : cheveux jusqu'à hair_to, visage jusqu'à face_to, puis la tenue."""
    e.paint(1, hair_to, hair)
    e.paint(hair_to + 1, face_to, skin)
    e.paint(face_to + 1, 15, cloth)
    if fringe:
        # Une frange irrégulière qui mord sur le front.
        for x in (4, 6, 9, 11):
            e.px(x, hair_to + 1, hair)
    ey = hair_to + 2
    if eye:
        e.px(5, ey, eye)
        e.px(10, ey, eye)
    if mouth:
        e.row(face_to, 7, 8, mouth)


EGGS = {}


def egg(name):
    def register(fn):
        EGGS[name] = fn
        return fn
    return register


BLUE = "#2f4f93"
GOLD = "#d9b440"
FAIR = "#e8c29e"


def uniform_buttons(e, color=GOLD, x=7):
    for y in (11, 13):
        e.px(x, y, color)


@egg("amestrian_soldier")
def _(e):
    portrait(e, BLUE, FAIR, BLUE, hair_to=4)
    e.row(4, 3, 12, "#151519")
    e.px(7, 2, GOLD)
    e.px(8, 2, GOLD)
    e.row(10, 2, 13, "#1d2f5c")
    uniform_buttons(e)


@egg("state_examiner")
def _(e):
    portrait(e, BLUE, FAIR, BLUE, hair_to=4)
    e.row(4, 3, 12, "#151519")
    e.px(7, 2, GOLD)
    e.row(9, 5, 10, "#8e8a86")
    e.row(10, 2, 13, GOLD)
    uniform_buttons(e)


@egg("briggs_soldier")
def _(e):
    portrait(e, "#c9d0d6", FAIR, "#c9d0d6", hair_to=5, face_to=8)
    e.row(6, 3, 12, "#2a2a30")
    e.row(6, 4, 6, "#5d7d93")
    e.row(6, 9, 11, "#5d7d93")
    for y in range(5, 10):
        e.px(3 if y < 8 else 2, y, "#eee9df")
        e.px(12 if y < 8 else 13, y, "#eee9df")
    e.paint(9, 9, "#6b7480")
    e.row(12, 2, 13, "#3f3730")


@egg("drachma_soldier")
def _(e):
    portrait(e, "#5b4331", "#e3bea0", "#545239", hair_to=5)
    e.px(7, 3, "#c8202c")
    e.px(8, 3, "#9e1620")
    e.row(10, 2, 13, "#5b4331")
    e.row(13, 2, 13, "#3b2a1c")


@egg("immortal_soldier")
def _(e):
    portrait(e, "#cbbfae", "#cbbfae", "#4a3e33", eye=None, hair_to=3, face_to=11, fringe=False)
    e.px(7, 5, "#1d0d0c")
    e.px(8, 5, "#d0242c")
    e.row(8, 3, 12, "#2a0d0d")
    for x in range(3, 13, 2):
        e.px(x, 8, "#eee6d4")
    e.row(10, 4, 11, "#5a2a24")


@egg("hohenheim")
def _(e):
    portrait(e, "#e1c26a", FAIR, "#7a5a3a")
    e.row(7, 4, 6, "#4a3f30")
    e.row(7, 9, 11, "#4a3f30")
    e.row(9, 4, 11, "#c7a656")
    e.row(10, 7, 8, "#e9e3d4")
    e.row(11, 7, 8, "#4e5546")


@egg("marcoh")
def _(e):
    portrait(e, "#e2bd99", "#e2bd99", "#6b4b33", hair_to=3, fringe=False)
    e.row(4, 3, 4, "#8f8a80")
    e.row(4, 11, 12, "#8f8a80")
    e.px(5, 6, "#3f3a33")
    e.px(10, 6, "#3f3a33")
    e.px(10, 8, "#c88a7a")
    e.px(11, 7, "#c88a7a")
    e.row(10, 6, 9, "#e8e3d6")


@egg("scar")
def _(e):
    portrait(e, "#e3dfd6", "#8a5634", "#c7a24a", eye="#c4202a")
    e.px(7, 6, "#c48e7a")
    e.px(8, 7, "#c48e7a")
    e.px(8, 6, "#c48e7a")
    e.px(7, 7, "#c48e7a")
    for y in range(10, 15):
        e.row(y, 6, 9, "#1e1c22")
    e.row(12, 2, 4, "#22283a")


@egg("cornello")
def _(e):
    portrait(e, "#d8b08e", "#e7c2a0", "#ddd1aa", eye="#5a4128", hair_to=3, fringe=False)
    e.row(5, 3, 4, "#5b4130")
    e.row(5, 11, 12, "#5b4130")
    e.row(10, 2, 13, "#b88a2c")
    e.row(12, 7, 8, GOLD)
    e.px(6, 12, GOLD)
    e.px(9, 12, GOLD)
    e.row(11, 7, 8, GOLD)
    e.row(13, 7, 8, GOLD)


@egg("izumi")
def _(e):
    portrait(e, "#1c1716", "#e4bc98", "#f0ece2")
    for y in range(6, 11):
        e.px(3 if y < 8 else 2, y, "#1c1716")
        e.px(12 if y < 8 else 13, y, "#1c1716")
    e.row(9, 7, 8, "#9a4e48")
    e.row(13, 2, 13, "#3b3330")


@egg("may_chang")
def _(e):
    portrait(e, "#1d1820", "#f0d0b4", "#d9608c")
    e.px(3, 2, "#1d1820")
    e.px(12, 2, "#1d1820")
    e.px(4, 8, "#f0a8a0")
    e.px(11, 8, "#f0a8a0")
    e.row(10, 6, 9, "#f4ecef")
    e.px(7, 11, "#f4ecef")
    e.px(8, 12, "#f4ecef")


@egg("olivier")
def _(e):
    portrait(e, "#ecd27a", "#f0d4b8", BLUE, eye="#3d6aa8")
    for y in range(6, 14):
        e.px(3 if y < 8 else 2, y, "#ecd27a")
        e.px(12 if y < 8 else 13, y, "#ecd27a")
    e.row(6, 4, 6, "#ecd27a")
    e.px(5, 7, "#ecd27a")
    e.row(10, 4, 11, GOLD)
    uniform_buttons(e)


@egg("winry")
def _(e):
    portrait(e, "#efd27e", "#f2d6ba", "#1e1c22", eye="#4c86c8")
    e.row(3, 4, 11, "#e8e8f0")
    e.paint(10, 10, "#f2d6ba")
    e.paint(13, 15, "#4d79b8")


@egg("haunted_armor")
def _(e):
    portrait(e, "#8f9aa6", "#8f9aa6", "#8f9aa6", eye=None, hair_to=4, fringe=False)
    e.row(6, 3, 12, "#2a2e34")
    e.px(5, 6, "#f0f4ff")
    e.px(10, 6, "#f0f4ff")
    e.px(7, 1, "#e8e8e0")
    e.px(8, 1, "#e8e8e0")
    e.row(10, 2, 13, "#5d6670")
    e.row(13, 2, 13, "#6d5f4c")


@egg("barry")
def _(e):
    portrait(e, "#7c6c5a", "#7c6c5a", "#7c6c5a", eye=None, hair_to=4, fringe=False)
    e.row(6, 3, 12, "#2a221c")
    e.px(5, 6, "#ff3b2b")
    e.px(10, 6, "#ff3b2b")
    for y in (1, 2, 3):
        e.px(7, y, "#5a2a22")
        e.px(8, y, "#5a2a22")
    e.px(9, 11, "#7a1414")
    e.px(10, 12, "#7a1414")
    e.px(4, 13, "#7a1414")


@egg("truth")
def _(e):
    portrait(e, "#f2f1ee", "#f2f1ee", "#f2f1ee", eye=None, fringe=False)
    e.px(3, 7, "#141416")
    e.px(12, 7, "#141416")
    e.row(8, 3, 12, "#141416")
    for x in range(4, 12, 2):
        e.px(x, 8, "#fbfbfa")
    e.row(9, 5, 10, "#141416")


@egg("lust")
def _(e):
    portrait(e, "#17141c", "#f2ddd0", "#1e1c22", eye="#9a3aa8")
    for y in range(6, 15):
        e.px(3 if y < 8 else 2, y, "#17141c")
        e.px(12 if y < 8 else 13, y, "#17141c")
    e.row(9, 7, 8, "#7a2a5a")
    e.row(11, 6, 9, "#f2ddd0")
    e.px(7, 11, "#c0182c")
    e.px(8, 11, "#c0182c")
    e.px(7, 12, "#c0182c")


@egg("gluttony")
def _(e):
    portrait(e, "#efe2d2", "#efe2d2", "#1e1c22", eye="#141016", hair_to=3, fringe=False)
    e.row(8, 6, 9, "#5a1a22")
    e.row(8, 7, 8, "#d6566a")
    e.px(6, 9, "#c8d4e0")


@egg("sloth")
def _(e):
    portrait(e, "#3a3028", "#ddcfc0", "#ddcfc0", eye="#4a3a32", hair_to=4)
    e.row(9, 6, 9, "#7a5a50")
    e.row(13, 2, 13, "#4a4036")
    e.row(14, 2, 13, "#4a4036")
    e.px(10, 11, "#c0182c")
    e.px(11, 11, "#c0182c")
    e.px(10, 12, "#c0182c")


@egg("envy")
def _(e):
    portrait(e, "#1e3226", "#efdcc8", "#1e1c22", eye="#6a3a8a", hair_to=5)
    e.row(5, 3, 12, "#1e1c22")
    e.px(7, 5, "#c0182c")
    e.px(8, 5, "#c0182c")
    for y in range(6, 12):
        e.px(3 if y < 8 else 2, y, "#1e3226")
        e.px(12 if y < 8 else 13, y, "#1e3226")
    e.paint(12, 12, "#efdcc8")


@egg("greed")
def _(e):
    portrait(e, "#16141a", "#ecd2b8", "#1a181e", eye=None)
    e.row(7, 4, 6, "#1d2a3a")
    e.row(7, 9, 11, "#1d2a3a")
    e.px(4, 7, "#5a7a9a")
    e.px(9, 7, "#5a7a9a")
    e.row(9, 6, 9, "#3a1414")
    e.px(6, 9, "#f4f0e8")
    e.px(9, 9, "#f4f0e8")
    e.row(10, 2, 13, "#c6bcb0")
    e.row(11, 7, 8, "#ecd2b8")


@egg("wrath")
def _(e):
    portrait(e, "#16141a", "#e4c0a0", "#24396e", eye="#2a2a30", hair_to=4)
    e.row(6, 9, 11, "#0e0e12")
    e.px(10, 7, "#0e0e12")
    e.px(8, 5, "#0e0e12")
    e.row(8, 6, 9, "#1a1618")
    e.row(10, 2, 13, GOLD)
    uniform_buttons(e)


@egg("pride")
def _(e):
    portrait(e, "#141018", "#f2d8c0", "#f2f0ea", eye="#3a3036")
    e.row(10, 5, 10, "#2c3e74")
    e.row(11, 6, 9, "#2c3e74")
    e.paint(13, 15, "#2c3e74")
    # Les ombres qui débordent derrière lui.
    e.px(2, 9, "#0a080c")
    e.px(13, 8, "#0a080c")
    e.px(2, 10, "#c0101e")


@egg("father")
def _(e):
    portrait(e, "#d9b860", "#141218", "#141218", eye=None)
    e.row(7, 7, 8, "#f2f0ea")
    e.px(8, 7, "#d0101e")
    e.row(9, 6, 9, "#d0101e")
    for y in range(6, 15):
        e.px(3 if y < 8 else 2, y, "#d9b860")
        e.px(12 if y < 8 else 13, y, "#d9b860")
    e.row(11, 5, 10, "#c9a650")
    e.row(12, 6, 9, "#c9a650")


@egg("chimera_beast")
def _(e):
    portrait(e, "#6e4424", "#c08a44", "#7d7a74", eye="#e8c020", hair_to=4, face_to=10)
    for y in range(5, 11):
        e.px(3 if y < 8 else 2, y, "#6e4424")
        e.px(12 if y < 8 else 13, y, "#6e4424")
    e.row(9, 7, 8, "#2a1a14")
    e.row(10, 6, 9, "#dcb27a")
    for x in range(2, 14, 2):
        e.px(x, 11, "#5a1e1e")


@egg("chimera_crawler")
def _(e):
    portrait(e, "#7a5a3e", "#9a7258", "#2a2420", eye="#d0202a", hair_to=4, face_to=10)
    e.px(6, 7, "#d0202a")
    e.px(9, 7, "#d0202a")
    e.row(9, 6, 9, "#3a1416")
    for y in (12, 14):
        e.row(y, 2, 13, "#7a5a3e")


EGG_NAMES = {"father": "father_spawn_egg"}


@egg("stone_golem")
def _(e):
    """Le golem de pierre : des blocs appareillés, deux yeux de lumière, le cercle gravé sur le torse."""
    stone, glow = "#8a8780", "#7fd8ff"
    e.paint(1, 15, stone)
    for y in (4, 8, 12):
        e.row(y, 0, 15, shade(stone, 0.72))
    for x, y in ((6, 2), (10, 3), (4, 6), (9, 6), (7, 10), (11, 10), (5, 14), (9, 14)):
        e.px(x, y, shade(stone, 0.78))
    e.row(5, 3, 12, shade(stone, 0.6))
    for x in (5, 10):
        e.px(x, 6, glow)
        e.px(x + 1, 6, glow)
        e.px(x, 7, shade(glow, 0.55))
    for x, y in ((7, 10), (8, 10), (6, 11), (9, 11), (6, 12), (9, 12), (7, 13), (8, 13)):
        e.px(x, y, glow)
    e.px(7, 11, "#ffffff")


def build(names=None):
    for name in names or EGGS:
        e = Egg(name)
        EGGS[name](e)
        e.render().save(os.path.join(OUT, EGG_NAMES.get(name, name + "_spawn_egg") + ".png"))
        print("œuf", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
