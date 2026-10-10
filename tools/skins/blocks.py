"""Les textures des blocs du mod, en 16x16. Chaque bloc est peint en code : appareils de briques
ombrés brique par brique, bruits qui se raccordent d'un bloc à l'autre, gemmes et métaux à facettes.
`python tools/skins/blocks.py [nom ...]` les régénère dans textures/block.
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
                   "block")

BLOCKS = {}


def block(name):
    def register(fn):
        BLOCKS[name] = fn
        return fn
    return register


class Tile:
    def __init__(self, name, base="#000000"):
        self.img = Image.new("RGBA", (16, 16), rgb(base))
        self.rng = random.Random(name)

    def put(self, x, y, c):
        self.img.putpixel((x % 16, y % 16), rgb(c))

    def get(self, x, y):
        return self.img.getpixel((x % 16, y % 16))

    def tint(self, x, y, k):
        self.put(x, y, shade(self.get(x, y), k))

    def rect(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.put(x + i, y + j, c(i, j) if callable(c) else c)

    def line(self, x0, y0, x1, y1, c, wrap=True):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            if wrap or (0 <= x < 16 and 0 <= y < 16):
                self.put(x, y, c(x, y) if callable(c) else c)


def noise(seed, scale=4):
    """Un bruit de valeur qui se raccorde sur les bords : un bloc posé à côté d'un autre continue le motif."""
    rng = random.Random(seed)
    n = 16 // scale
    grid = [[rng.random() for _ in range(n)] for _ in range(n)]

    def at(x, y):
        gx, gy = x / scale, y / scale
        x0, y0 = int(gx) % n, int(gy) % n
        x1, y1 = (x0 + 1) % n, (y0 + 1) % n
        fx, fy = gx - int(gx), gy - int(gy)
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = grid[y0][x0] * (1 - fx) + grid[y0][x1] * fx
        bottom = grid[y1][x0] * (1 - fx) + grid[y1][x1] * fx
        return top * (1 - fy) + bottom * fy
    return at


def ramp(colors, t):
    """Une couleur prise le long d'un dégradé de plusieurs teintes, t de 0 à 1."""
    t = max(0.0, min(1.0, t))
    i = min(len(colors) - 2, int(t * (len(colors) - 1)))
    return mix(colors[i], colors[i + 1], t * (len(colors) - 1) - i)


def bricks(t, colors, mortar, course=4, length=8, offset=4, chip=0.15, top=1.08, bottom=0.8):
    """Un appareil en panneresse : chaque brique a sa teinte, un reflet en haut, une ombre en bas."""
    rng = t.rng
    for row in range(16 // course):
        y0 = row * course
        shift = (row % 2) * offset
        for b in range(16 // length + 1):
            x0 = b * length - shift
            base = rgb(rng.choice(colors))
            base = shade(base, rng.uniform(0.92, 1.06))
            for j in range(course - 1):
                for i in range(length - 1):
                    k = rng.uniform(0.94, 1.06)
                    if j == 0:
                        k *= top
                    elif j == course - 2:
                        k *= bottom
                    if i == length - 2:
                        k *= 0.88
                    t.put(x0 + i, y0 + j, shade(base, k))
            # Une brique ébréchée de temps en temps.
            if rng.random() < chip:
                cx = x0 + rng.randint(1, length - 3)
                t.put(cx, y0 + course - 2, shade(base, 0.62))
        # Les joints : l'horizontal sous la rangée, les verticaux entre les briques.
        for x in range(16):
            t.put(x, y0 + course - 1, shade(mortar, rng.uniform(0.92, 1.05)))
        for b in range(16 // length + 1):
            for j in range(course - 1):
                t.put(b * length - shift - 1, y0 + j, shade(mortar, rng.uniform(0.9, 1.0)))


# --- maçonneries ----------------------------------------------------------------------------------
@block("amestrian_bricks")
def amestrian_bricks(t):
    """Les briques de grès clair des villes d'Amestris."""
    bricks(t, ["#d8c296", "#cdb585", "#e0cca2", "#c9ae7c"], "#9e8b6a")


@block("xing_bricks")
def xing_bricks(t):
    """Les briques laquées de Xing : un rouge profond, des joints sombres, un liseré doré par endroits."""
    bricks(t, ["#9a2a22", "#a8322a", "#8a241e", "#b0382c"], "#2e1c1a", chip=0.05, top=1.12, bottom=0.74)
    # Un éclat de laque, comme une glaçure qui accroche la lumière.
    for x, y in ((2, 0), (11, 4), (6, 8), (14, 12)):
        t.put(x, y, "#e07a5a")
    for x in range(16):
        if x % 8 in (3, 4):
            t.put(x, 15, "#c9a24a")


@block("briggs_packed_snow")
def briggs_packed_snow(t):
    """Les blocs de neige tassée des remparts de Briggs : de grands pavés bleutés, du givre qui scintille."""
    n = noise("snow", 4)
    for y in range(16):
        for x in range(16):
            t.put(x, y, ramp(["#c6d4e2", "#dfe8f0", "#f4f8fb"], n(x, y) * 0.8 + 0.2))
    for row, y0 in enumerate((0, 8)):
        shift = 4 if row else 0
        for x in range(16):
            t.put(x, y0 + 7, "#9db0c4")
            t.tint(x, y0, 1.06)
        for x in (shift, shift + 8):
            for j in range(7):
                t.put(x - 1, y0 + j, "#a9bccf")
                t.tint(x, y0 + j, 1.04)
    for _ in range(7):
        t.put(t.rng.randrange(16), t.rng.randrange(16), "#ffffff")


@block("white_floor")
def white_floor(t):
    """Le sol de l'Espace blanc : un blanc presque pur, à peine quadrillé."""
    for y in range(16):
        for x in range(16):
            c = "#f6f6f4" if (x + y) % 2 else "#f3f3f1"
            t.put(x, y, c)
    for i in range(16):
        t.put(i, 15, "#e8e8e4")
        t.put(15, i, "#e8e8e4")


# --- la Porte -------------------------------------------------------------------------------------
@block("gate_stone")
def gate_stone(t):
    """La pierre de la Porte : un calcaire pâle où est gravé l'arbre des Séphiroth. Les sphères et
    leurs chemins vont d'un bord à l'autre : posés côte à côte, les blocs tissent un seul grand arbre."""
    n = noise("gate", 4)
    for y in range(16):
        for x in range(16):
            t.put(x, y, ramp(["#a29d92", "#b7b2a7", "#c8c3b8"], n(x, y) + t.rng.uniform(-0.08, 0.08)))
    groove, lit = "#6e6a62", "#dcd8ce"

    def carve(points):
        # Une gravure : le creux sombre, et la lèvre du bas éclairée.
        for x, y in points:
            t.put(x, y + 1, lit)
        for x, y in points:
            t.put(x, y, groove)

    path = []
    # Les chemins : vertical au centre, et les diagonales vers les coins (qui rejoignent les voisins).
    path += [(7, y) for y in range(16)]
    for i in range(16):
        path.append((i, i // 2 + 0))
        path.append((15 - i, i // 2 + 0))
    carve([(x, y) for x, y in path if not (4 <= x <= 10 and 5 <= y <= 11)])
    # La sphère au centre : un anneau creusé, un cœur poli.
    ring = [(x, y) for y in range(16) for x in range(16) if abs(math.hypot(x - 7, y - 8) - 3.2) < 0.6]
    carve(ring)
    for x, y in ((6, 7), (7, 7), (6, 8), (7, 8), (8, 8), (7, 9)):
        t.put(x, y, "#d4cfc4")
    t.put(6, 7, "#ece8de")


# --- chairs et cristaux ---------------------------------------------------------------------------
@block("belly_flesh")
def belly_flesh(t):
    """L'estomac de Gluttony : une chair plissée, humide, parcourue de veines."""
    n = noise("flesh", 4)
    m = noise("folds", 8)
    for y in range(16):
        for x in range(16):
            v = n(x, y) * 0.6 + m(x, y) * 0.4
            t.put(x, y, ramp(["#3e080e", "#6e1418", "#922026", "#b0343a"], v))
    # Les veines : des traînées sombres qui serpentent et se raccordent sur les bords.
    rng = t.rng
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(14):
            t.put(x, y, "#3a0a1a")
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1, 1))
    # Des reflets mouillés.
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        t.put(x, y, "#e07a80")
        t.put(x + 1, y, "#c04a52")


@block("crystallized_blood")
def crystallized_blood(t):
    """Du sang cristallisé : des facettes rouge sombre, chacune éclairée selon son orientation."""
    rng = t.rng
    seeds = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(0, math.tau)) for _ in range(7)]

    def nearest(x, y):
        best, second, idx = 1e9, 1e9, 0
        for i, (sx, sy, _) in enumerate(seeds):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = math.hypot(x - sx - ox, y - sy - oy)
                    if d < best:
                        best, second, idx = d, best, i
                    elif d < second:
                        second = d
        return idx, second - best

    for y in range(16):
        for x in range(16):
            i, edge = nearest(x + 0.5, y + 0.5)
            light = 0.5 + 0.5 * math.cos(seeds[i][2] - math.pi * 0.75)
            c = ramp(["#2a0308", "#5a0a12", "#8e1420", "#c0303a"], light * 0.85 + 0.1)
            if edge < 0.9:
                c = mix(c, "#e06a6a", 0.35) if light > 0.5 else mix(c, "#180104", 0.5)
            t.put(x, y, c)
    for x, y in ((3, 2), (11, 9), (6, 13)):
        t.put(x, y, "#ffd0d0")


@block("red_stone_ore")
def red_stone_ore(t):
    """Le grès de Liore où affleurent des pierres rouges : des cristaux sertis, qui luisent."""
    n = noise("sand", 4)
    for y in range(16):
        for x in range(16):
            band = 0.06 * math.sin(y * 1.2)
            t.put(x, y, ramp(["#c9ad78", "#dcc292", "#ead4a8"], n(x, y) * 0.7 + 0.15 + band))
    for y in (5, 11):
        for x in range(16):
            t.tint(x, y, 0.9)
    gem = ["..D..", ".DmD.", "DmLmD", ".DmD.", "..D.."]
    small = [".D.", "DLD", ".D."]
    colors = dict(D="#5a0810", m="#c01828", L="#ff7a7a")
    for pattern, x0, y0 in ((gem, 2, 2), (small, 10, 3), (gem, 8, 9), (small, 2, 11), (small, 13, 13)):
        for dy, row in enumerate(pattern):
            for dx, ch in enumerate(row):
                if ch != ".":
                    t.put(x0 + dx, y0 + dy, colors[ch])


# --- l'antre de Père ------------------------------------------------------------------------------
PIPE = ["#2a2a2e", "#4a4a50", "#76767c", "#9a9aa0", "#6a6a70", "#4a4a50", "#36363a", "#2a2a2e"]


@block("father_pipe")
def father_pipe(t):
    """Les conduites de l'antre de Père : un gros tuyau de fonte, éclairé de côté, cerclé de bagues rivetées."""
    for y in range(16):
        for x in range(16):
            c = rgb(PIPE[min(7, int(x / 2))])
            t.put(x, y, shade(c, t.rng.uniform(0.95, 1.05)))
    for y0 in (0, 8):
        for x in range(16):
            t.put(x, y0, shade(PIPE[min(7, int(x / 2))], 1.25))
            t.put(x, y0 + 1, shade(PIPE[min(7, int(x / 2))], 0.7))
        for x in (2, 7, 12):
            t.put(x, y0, "#b8b8be")
    # De la rouille qui coule sous les bagues.
    for x, y in ((5, 2), (5, 3), (10, 10), (10, 11), (10, 12), (3, 10)):
        t.put(x, y, "#6a3e24")


@block("father_pipe_top")
def father_pipe_top(t):
    """Le bout d'une conduite : une bride boulonnée, puis le noir du tuyau."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.2:
                c = "#2a2a2e"
            elif d > 5.6:
                c = shade("#6a6a70", 1.15 - 0.3 * ((x + y) / 30))
            elif d > 4.6:
                c = "#3a3a3e"
            else:
                c = mix("#050405", "#1a1418", d / 4.6)
            t.put(x, y, c)
    for a in range(8):
        x = round(7.5 + math.cos(a * math.pi / 4) * 6.4)
        y = round(7.5 + math.sin(a * math.pi / 4) * 6.4)
        t.put(x, y, "#b8b8be")


def seal(t, color, ring, inner):
    """Un sceau tracé : un double anneau et un motif intérieur, d'une couleur (sang, ou lumière rouge)."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if any(abs(d - r) < 0.55 for r in ring):
                t.put(x, y, shade(color, t.rng.uniform(0.85, 1.1)))
    for x, y in inner:
        t.put(x, y, shade(color, t.rng.uniform(0.85, 1.1)))


@block("father_seal")
def father_seal(t):
    """Le sceau de Père, gravé dans la roche sombre de son antre : un cercle rouge qui luit encore."""
    n = noise("basalt", 4)
    for y in range(16):
        for x in range(16):
            t.put(x, y, ramp(["#1c1a1e", "#2a282e", "#36343a"], n(x, y)))
    inner = []
    # Le triangle pointe en bas et l'œil au centre.
    for k in range(3):
        a0, a1 = math.pi / 2 + k * 2 * math.pi / 3, math.pi / 2 + (k + 1) * 2 * math.pi / 3
        x0, y0 = 7.5 + math.cos(a0) * 5.4, 7.5 + math.sin(a0) * 5.4
        x1, y1 = 7.5 + math.cos(a1) * 5.4, 7.5 + math.sin(a1) * 5.4
        for i in range(11):
            inner.append((round(x0 + (x1 - x0) * i / 10), round(y0 + (y1 - y0) * i / 10)))
    seal(t, "#c01828", (6.4,), inner)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        t.put(x, y, "#ff6050")
    # Le halo : la roche rougeoie autour des traits.
    for y in range(16):
        for x in range(16):
            c = t.get(x, y)
            if c[0] < 80 and any(t.get(x + dx, y + dy)[0] > 150 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.put(x, y, mix(c, "#5a1018", 0.6))


def deepslate_tiles(t):
    n = noise("tiles", 4)
    for y in range(16):
        for x in range(16):
            t.put(x, y, ramp(["#2c2c32", "#3a3a40", "#46464c"], n(x, y) * 0.8 + t.rng.uniform(-0.1, 0.1)))
    for y0 in (0, 8):
        for x in range(16):
            t.put(x, y0 + 7, "#1c1c20")
            t.tint(x, y0, 1.15)
        for x in ((0, 8) if y0 == 0 else (4, 12)):
            for j in range(7):
                t.put(x - 1, y0 + j, "#1c1c20")


BLOOD = "#8a0e14"


def blood_seal(t):
    """Le sceau de sang d'une armure habitée : un cercle, et la croix ancrée à l'intérieur."""
    inner = [(7, y) for y in range(3, 13)] + [(x, 7) for x in range(3, 13)]
    inner += [(5, 3), (9, 3), (5, 12), (9, 12), (3, 5), (3, 9), (12, 5), (12, 9)]
    seal(t, BLOOD, (6.3,), inner)
    # Des coulures et des éclaboussures.
    for x, y in ((12, 13), (12, 14), (2, 2), (13, 3), (4, 14)):
        t.put(x, y, shade(BLOOD, 0.8))


@block("blood_crest")
def blood_crest(t):
    deepslate_tiles(t)
    blood_seal(t)


@block("blood_crest_sealed")
def blood_crest_sealed(t):
    """Le sceau effacé : un trait de craie en travers, l'âme est libérée."""
    deepslate_tiles(t)
    blood_seal(t)
    for i in range(1, 15):
        t.put(i, i, "#e8e4da")
        t.put(i + 1, i, "#a8a49a")


@block("blood_crest_side")
def blood_crest_side(t):
    """Le flanc du sceau : la pierre, et le sang qui a coulé depuis le dessus."""
    deepslate_tiles(t)
    rng = t.rng
    for x in range(16):
        length = rng.choice((0, 1, 1, 2, 3, 5))
        for y in range(length):
            t.put(x, y, shade(BLOOD, 1.1 - y * 0.08))
        if length > 2:
            t.put(x, length, shade(BLOOD, 0.6))


# --- les établis ----------------------------------------------------------------------------------
OAK = ["#6a4a2a", "#8a6238", "#a07444", "#b4884e"]


def planks(t, horizontal=True):
    """Des planches de chêne : le fil du bois, un joint toutes les quatre lignes, des clous."""
    n = noise("grain", 2)
    for y in range(16):
        for x in range(16):
            u, v = (x, y) if horizontal else (y, x)
            grain = 0.5 + 0.35 * math.sin(u * 0.9 + n(u, v) * 4) * 0.5
            t.put(x, y, ramp(OAK, grain + (v // 4 % 2) * 0.08))
            if v % 4 == 3:
                t.put(x, y, OAK[0])


@block("alchemist_table_top")
def alchemist_table_top(t):
    """Le plateau de la table d'alchimiste : des planches, un cercle tracé à la craie, un encrier."""
    planks(t)
    chalk = "#ece8de"
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 6.2) < 0.5:
                t.put(x, y, chalk if t.rng.random() > 0.15 else "#c8c4b8")
    # Le triangle inscrit.
    for i in range(9):
        t.put(round(7.5 - 4 + i), 10, chalk)
        t.put(round(3.5 + i / 2), round(10 - i), chalk)
        t.put(round(11.5 - i / 2), round(10 - i), chalk)
    # L'encrier dans un coin, la plume à côté.
    t.rect(12, 12, 3, 3, "#1c1c2a")
    t.put(13, 12, "#4a4a6a")
    t.line(1, 14, 4, 11, "#e8e0c8")


@block("alchemist_table_side")
def alchemist_table_side(t):
    """Le flanc de la table : un cadre de chêne, un panneau sombre et un tiroir à poignée de laiton."""
    planks(t, horizontal=False)
    t.rect(0, 0, 16, 2, lambda i, j: shade(OAK[3], 1.05 if j == 0 else 0.8))
    t.rect(0, 14, 16, 2, lambda i, j: shade(OAK[1], 0.85))
    # Le tiroir.
    t.rect(2, 3, 12, 4, lambda i, j: shade(OAK[2], 1.1 if j == 0 else 0.95))
    t.rect(2, 6, 12, 1, OAK[0])
    t.rect(7, 4, 2, 1, "#d9b440")
    t.put(7, 5, "#8e6e2a")
    t.put(8, 5, "#8e6e2a")
    # L'étagère : des flacons et des livres.
    t.rect(2, 8, 12, 5, "#2a1c12")
    for x, c in ((3, "#3a6a9a"), (5, "#8a2a2a"), (7, "#2a6a3a"), (8, "#8a2a2a")):
        t.rect(x, 9, 1, 4, c)
        t.put(x, 9, shade(c, 1.3))
    t.rect(10, 10, 2, 3, "#c8dce8")
    t.put(10, 10, "#a02838")
    t.put(11, 9, "#8a6238")
    t.rect(12, 11, 1, 2, "#7a4ac0")
    t.rect(2, 13, 12, 1, OAK[0])


STEEL = ["#4e5258", "#6a6e76", "#868a92", "#a2a6ae"]


def steel_plate(t, seed):
    n = noise(seed, 4)
    for y in range(16):
        for x in range(16):
            t.put(x, y, ramp(STEEL, n(x, y) * 0.5 + 0.35 + t.rng.uniform(-0.04, 0.04)))
    # Quelques rayures.
    for _ in range(3):
        x, y = t.rng.randrange(16), t.rng.randrange(16)
        for i in range(4):
            t.put(x + i, y + i // 2, "#b4b8c0")


@block("automail_bench_top")
def automail_bench_top(t):
    """Le dessus de l'établi d'automail : une plaque d'acier boulonnée, une clé, un tournevis, des
    engrenages et une phalange en cours de montage."""
    steel_plate(t, "bench_top")
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        t.put(x, y, "#c8ccd2")
    # La clé plate.
    t.line(2, 4, 8, 4, "#c8ccd2")
    t.line(2, 5, 8, 5, "#7a7e86")
    t.rect(1, 3, 2, 4, "#c8ccd2")
    t.put(1, 4, "#4e5258")
    t.rect(8, 3, 2, 4, "#c8ccd2")
    t.put(9, 4, "#4e5258")
    # Le tournevis à manche jaune.
    t.rect(13, 2, 1, 5, "#d9b440")
    t.put(13, 2, "#f0d070")
    t.line(13, 7, 13, 11, "#c8ccd2")
    # Un engrenage de laiton.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 5, y - 11)
            a = math.atan2(y - 11, x - 5)
            r = 2.6 + (0.9 if math.cos(a * 8) > 0.3 else 0)
            if d <= r:
                t.put(x, y, "#b89040" if d > 1.2 else "#3a3020")
    t.put(4, 10, "#e8c870")
    # Une phalange d'automail.
    t.rect(10, 12, 3, 2, "#a9afb6")
    t.put(12, 12, "#2c2e33")
    t.rect(13, 12, 2, 2, "#c3c8ce")


@block("automail_bench_side")
def automail_bench_side(t):
    """Le flanc de l'établi : un caisson d'acier riveté, deux tiroirs à poignée, une tache d'huile."""
    steel_plate(t, "bench_side")
    t.rect(0, 0, 16, 2, lambda i, j: shade(STEEL[3], 1.1 if j == 0 else 0.8))
    for y0 in (3, 9):
        t.rect(2, y0, 12, 5, lambda i, j: shade(STEEL[2], 1.12 if j == 0 else 0.78 if j == 4 else 1.0))
        t.rect(6, y0 + 2, 4, 1, "#2c2e33")
        t.rect(6, y0 + 1, 4, 1, "#c8ccd2")
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        t.put(x, y, "#c8ccd2")
    t.rect(10, 13, 3, 2, "#2a2620")
    t.put(11, 15, "#2a2620")


@block("transmutation_circle")
def transmutation_circle(t):
    """La poussière de craie d'un cercle (la texture de ses particules) : des éclats blancs épars."""
    t.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for _ in range(26):
        x, y = t.rng.randrange(16), t.rng.randrange(16)
        t.put(x, y, mix("#f4f2ec", "#b8b4aa", t.rng.random()))


@block("alchemist_table_leg")
def alchemist_table_leg(t):
    """Les pieds tournés de la table : du chêne sombre, le fil vertical, une bague à mi-hauteur."""
    for y in range(16):
        for x in range(16):
            grain = 0.35 + 0.25 * math.sin(x * 2.1 + y * 0.15) + t.rng.uniform(-0.06, 0.06)
            t.put(x, y, ramp(["#3e2a16", "#5a3e22", "#6e4c2a"], grain))
    for x in range(16):
        t.put(x, 7, "#7e5a32")
        t.put(x, 8, "#2e1e10")


@block("alchemist_props")
def alchemist_props(t):
    """Ce qui traîne sur la table : un grimoire (reliure, pages), des fioles, une bougie, un encrier.
    Une planche de 16x16 où les modèles vont piocher leurs morceaux."""
    t.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # La reliure de cuir rouge (0..6, 0..4), dorée sur la tranche.
    t.rect(0, 0, 6, 4, lambda i, j: shade("#7a1a1e", 1.1 - j * 0.06 + t.rng.uniform(-0.05, 0.05)))
    t.rect(0, 0, 6, 1, "#c8a040")
    # Les pages ouvertes (0..6, 4..8) : deux pages, des lignes d'écriture et un cercle.
    t.rect(0, 4, 6, 4, "#efe6cc")
    t.rect(3, 4, 1, 4, "#c8bc9c")
    for y in (5, 6):
        t.put(1, y, "#6a5a4a")
        t.put(4, y, "#6a5a4a")
    t.put(5, 6, "#8a2a2a")
    t.put(1, 7, "#8a2a2a")
    # Le flanc des pages (0..6, 8..9).
    t.rect(0, 8, 6, 1, "#ddd2b2")
    # Une fiole verte (8..11, 0..6) et une rouge (12..15, 0..6) : du verre, le liquide en bas.
    for x0, liquid in ((8, "#3aa060"), (12, "#c02838")):
        t.rect(x0, 0, 3, 6, lambda i, j: (190, 220, 230, 140))
        t.rect(x0, 2, 3, 4, lambda i, j: shade(liquid, 1.15 if i == 0 else 0.9))
        t.put(x0, 0, (240, 250, 255, 200))
        t.put(x0 + 3, 0, "#8a6238")
    # La bougie (8..9, 7..12), sa mèche et sa flamme (10..11, 7..9).
    t.rect(8, 7, 2, 5, lambda i, j: "#f0e8d0" if i == 0 else "#d8ccac")
    t.put(10, 7, "#fff4a0")
    t.put(11, 7, "#fff4a0")
    t.put(10, 8, "#ffb030")
    t.put(11, 8, "#ffd060")
    t.put(10, 9, "#e06010")
    t.put(11, 9, "#202020")
    # L'encrier (12..15, 7..10) : du verre noir et un col de laiton.
    t.rect(12, 7, 3, 3, "#1c1c2a")
    t.put(12, 7, "#4a4a6a")
    t.rect(12, 10, 3, 1, "#b89040")
    # La plume (0..6, 10..11) : blanche, la pointe noire.
    t.rect(0, 10, 5, 1, "#f2eee2")
    t.put(5, 10, "#202020")
    t.rect(1, 11, 3, 1, "#d8d2c2")
    # Des rouleaux de parchemin (0..6, 12..14).
    t.rect(0, 12, 6, 2, lambda i, j: "#e8dcb8" if j == 0 else "#c8b890")
    t.put(0, 12, "#a02838")
    t.put(5, 13, "#a02838")


@block("automail_props")
def automail_props(t):
    """Les pièces de l'établi d'automail : un étau, un avant-bras d'acier en montage, des câbles,
    une boîte de boulons. Une planche que les modèles se partagent."""
    t.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # L'acier poli de l'avant-bras (0..8, 0..4) : clair dessus, un liseré, des rivets.
    t.rect(0, 0, 8, 4, lambda i, j: ramp(STEEL, 0.95 - j * 0.18))
    for x in (1, 4, 7):
        t.put(x, 1, "#e0e4ea")
    t.rect(0, 3, 8, 1, "#3e4248")
    # Les doigts articulés (0..8, 4..6) : phalanges et jointures sombres.
    t.rect(0, 4, 8, 2, "#a9afb6")
    for x in (1, 3, 5, 7):
        t.put(x, 4, "#2c2e33")
        t.put(x, 5, "#2c2e33")
    # Les câbles rouge et bleu (0..8, 6..7).
    t.rect(0, 6, 8, 1, "#b02828")
    t.rect(0, 7, 8, 1, "#2848a0")
    # L'étau (8..12, 0..6) : de la fonte peinte en vert, une mâchoire striée.
    t.rect(8, 0, 4, 6, lambda i, j: shade("#3a5a46", 1.1 - j * 0.07))
    t.rect(8, 0, 4, 1, "#6a7066")
    t.rect(8, 1, 4, 1, "#2a2e2a")
    # La vis de l'étau (12..16, 0..2) : du laiton.
    t.rect(12, 0, 4, 2, lambda i, j: "#d8b850" if j == 0 else "#987028")
    # La boîte de boulons (8..13, 8..12) : du bois, des têtes de boulons dessus.
    t.rect(8, 8, 5, 4, "#7a5430")
    t.rect(8, 8, 5, 1, "#9a6c3c")
    for x, y in ((9, 9), (11, 9), (10, 10), (12, 11), (9, 11)):
        t.put(x, y, "#b4b8c0")
    # L'engrenage de laiton (0..6, 8..14).
    for y in range(6):
        for x in range(6):
            d = math.hypot(x - 2.5, y - 2.5)
            a = math.atan2(y - 2.5, x - 2.5)
            if d <= 2.2 + (0.8 if math.cos(a * 6) > 0.3 else 0):
                t.put(x, 8 + y, "#b89040" if d > 1.0 else "#3a3020")
    t.put(1, 9, "#e8c870")
    # Le bois brut des pieds (13..16, 4..16) : du chêne foncé taché d'huile.
    t.rect(13, 4, 3, 12, lambda i, j: shade("#5a4028", 1.1 - i * 0.12 + t.rng.uniform(-0.05, 0.05)))
    t.put(14, 9, "#2a2620")


@block("father_pipe_flange")
def father_pipe_flange(t):
    """Le flanc d'une bride : de la fonte épaisse, une ligne de boulons, des coulures de rouille."""
    for y in range(16):
        for x in range(16):
            t.put(x, y, shade(PIPE[3] if y < 1 else PIPE[2] if y < 2 else PIPE[5], t.rng.uniform(0.93, 1.07)))
    for x in range(1, 16, 4):
        t.put(x, 0, "#c8c8ce")
        t.put(x, 1, "#36363a")
    t.put(6, 1, "#6a3e24")


def glow(base, keep):
    """Ce qui luit d'une texture : ses pixels retenus, le reste transparent. Le modèle le pose par-dessus
    le bloc avec une pleine lumière."""
    img = Image.open(os.path.join(OUT, base + ".png")).convert("RGBA")
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if keep(c):
                out.putpixel((x, y), c)
    return out


def reddish(min_r, ratio=2.0):
    return lambda c: c[0] >= min_r and c[0] > c[1] * ratio and c[0] > c[2] * ratio


GLOWS = {
    "father_seal_glow": ("father_seal", reddish(150)),
    "blood_crest_glow": ("blood_crest", reddish(100, 3.0)),
    "red_stone_ore_glow": ("red_stone_ore", reddish(170)),
}


def build(names=None):
    for name in names or BLOCKS:
        t = Tile(name)
        BLOCKS[name](t)
        t.img.save(os.path.join(OUT, name + ".png"))
        print("bloc", name)
    for name, (base, keep) in GLOWS.items():
        if not names or base in names or name in names:
            glow(base, keep).save(os.path.join(OUT, name + ".png"))
            print("lueur", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
