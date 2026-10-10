"""Les textures des créatures et objets en volume qui ne sont pas des humanoïdes : chimères, golem,
lézard d'Envy, mains de la Porte, fauteuil roulant, automails.

Les boîtes reprennent les modèles vanilla (ours polaire, araignée, golem de fer, poisson d'argent)
ou ceux du mod (GateHandRenderer, WheelchairRenderer) : (u, v, largeur, hauteur, profondeur).
`python tools/skins/creatures.py [nom ...]`.
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from skinlib import FACES, SIDES, Skin, mix, shade  # noqa: E402
from characters import OUT  # noqa: E402

CREATURES = {}


def creature(name, size, parts):
    def register(fn):
        CREATURES[name] = (fn, size, parts)
        return fn
    return register


def stitches(s, part, f, x, y, length, vertical=True, color="#5a1e1e"):
    """Une couture de chimère : des points sombres, un sur deux."""
    for n in range(length):
        xx, yy = (x, y + n) if vertical else (x + n, y)
        if n % 2 == 0:
            s.px(part, f, xx, yy, color)
        else:
            s.tint(part, f, xx, yy, 1, 1, 0.8)


# --- chimères -----------------------------------------------------------------------------------
@creature("chimera_beast", (128, 64), {
    "head": (0, 0, 7, 7, 7), "mouth": (0, 44, 5, 3, 3), "ear": (26, 0, 2, 2, 1),
    "body": (0, 19, 14, 14, 11), "upper": (39, 0, 12, 12, 10),
    "hind": (50, 22, 4, 10, 8), "fore": (50, 40, 4, 10, 6)})
def chimera_beast(s):
    """Une chimère de Tucker : avant-train et crinière de lion, arrière-train de loup gris, cousus."""
    lion, mane, wolf = "#c08a44", "#6e4424", "#7d7a74"
    s.fill("head", lion, "fur")
    s.fill("upper", mane, "fur")
    s.fill("ear", mane, "fur")
    s.fill("fore", lion, "fur")
    s.fill("body", lion, "fur")
    s.fill("hind", wolf, "fur")
    # Le corps est couché dans le modèle (pivoté d'un quart de tour) : le haut de la texture des
    # côtés est vers la tête, le « dessus » de la boîte est le poitrail, le « dessous » la croupe.
    # L'arrière passe au pelage de loup ; la couture marque la jointure.
    s.material("body", "bottom", wolf, "fur")
    s.material("body", "front", mix(lion, "#f0dcb0", 0.35), "fur")
    for f in SIDES:
        w, h = s.size("body", f)
        s.material("body", f, wolf, "fur", y=h // 2, h=h - h // 2)
        stitches(s, "body", f, 0, h // 2, w, vertical=False)
    # La crinière déborde sur le crâne, la tête garde un ventre plus clair sous la gueule.
    s.material("head", "top", mane, "fur")
    for f in ("right", "left", "back"):
        s.material("head", f, mane, "fur", h=3)
    s.material("head", "front", mane, "fur", h=1)
    # Les yeux jaunes à pupille fendue, l'arcade sombre.
    s.rect("head", "front", 1, 2, 2, 1, "#e8c020")
    s.rect("head", "front", 4, 2, 2, 1, "#e8c020")
    s.px("head", "front", 2, 2, "#1a1208")
    s.px("head", "front", 4, 2, "#1a1208")
    s.tint("head", "front", 0, 1, 7, 1, 0.75)
    s.fill("mouth", "#dcb27a", "fur")
    s.rect("mouth", "top", 1, 0, 3, 1, "#2a1a14")
    s.rect("mouth", "front", 1, 0, 3, 1, "#2a1a14")
    s.rect("mouth", "front", 0, 2, 5, 1, "#3a1a18")
    s.px("mouth", "front", 0, 2, "#f0eadc")
    s.px("mouth", "front", 4, 2, "#f0eadc")
    # Une couture en travers du museau et une autre au cou : on voit qu'il a été assemblé.
    stitches(s, "head", "right", 0, 3, 7, vertical=False)
    stitches(s, "head", "left", 0, 3, 7, vertical=False)
    for f in SIDES:
        w, h = s.size("upper", f)
        stitches(s, "upper", f, 0, h - 1, w, vertical=False)
    # Les pattes : des griffes claires.
    for leg in ("hind", "fore"):
        s.band(leg, 9, 1, shade(s.pget(leg, "front", 1, 5), 0.75), "fur")
        for x in (0, 2):
            s.px(leg, "front", x, 9, "#e6dccb")
        s.material(leg, "bottom", "#3a2a22", "leather")


@creature("chimera_crawler", (64, 32), {
    "head": (32, 4, 8, 8, 8), "neck": (0, 0, 6, 6, 6), "body": (0, 12, 10, 8, 12), "leg": (18, 0, 16, 2, 2)})
def chimera_crawler(s):
    """La chimère rampante : un pelage de chien pelé sur un corps d'araignée, des pattes de chitine."""
    fur, skin, chitin = "#7a5a3e", "#b88a72", "#2a2420"
    s.fill("head", fur, "fur")
    s.fill("neck", fur, "fur")
    s.fill("body", fur, "fur")
    # Des plaques de peau nue là où le pelage est tombé.
    for part, f, x, y, w, h in (("body", "top", 2, 3, 3, 4), ("body", "top", 6, 7, 2, 3), ("body", "right", 3, 2, 3, 2),
                                ("body", "left", 6, 4, 2, 2), ("head", "top", 1, 2, 2, 2), ("body", "back", 4, 1, 3, 2)):
        s.material(part, f, skin, "skin", x=x, y=y, w=w, h=h)
    for f in SIDES:
        w, h = s.size("body", f)
        stitches(s, "body", f, 0, 3, w, vertical=False)
    # Le visage : un museau de chien et les orbites où la couche des yeux allume le rouge.
    s.material("head", "front", mix(fur, skin, 0.4), "fur")
    for x, y in ((1, 0), (6, 0), (2, 1), (5, 1), (0, 2), (7, 2), (0, 3), (7, 3), (2, 3), (3, 3), (4, 3), (5, 3),
                 (2, 4), (3, 4), (4, 4), (5, 4)):
        s.px("head", "front", x, y, "#1a0e0c")
    s.rect("head", "front", 2, 5, 4, 2, "#d8b8a0")
    s.rect("head", "front", 3, 5, 2, 1, "#1a1214")
    s.rect("head", "front", 2, 7, 4, 1, "#3a1416")
    s.px("head", "front", 2, 7, "#ece2d0")
    s.px("head", "front", 5, 7, "#ece2d0")
    # Les oreilles tombantes sur les côtés.
    s.material("head", "right", shade(fur, 0.7), "fur", x=3, w=3, y=0, h=4)
    s.material("head", "left", shade(fur, 0.7), "fur", x=2, w=3, y=0, h=4)
    # Les pattes : chitine luisante, articulations plus claires, pointes acérées.
    s.fill("leg", chitin, "metal")
    for f in ("front", "back", "top", "bottom"):
        w, h = s.size("leg", f)
        for x in (4, 9):
            if x < w:
                s.tint("leg", f, x, 0, 1, h, 1.6)
        s.tint("leg", f, w - 2, 0, 2, h, 0.6)


# --- le golem de pierre ---------------------------------------------------------------------------
GLOW = "#7fd8ff"


@creature("stone_golem", (128, 128), {
    "head": (0, 0, 8, 10, 8), "nose": (24, 0, 2, 4, 2), "body": (0, 40, 18, 12, 11), "waist": (0, 70, 9, 5, 6),
    "rarm": (60, 21, 4, 30, 6), "larm": (60, 58, 4, 30, 6), "rleg": (37, 0, 6, 16, 5), "lleg": (60, 0, 6, 16, 5)})
def stone_golem(s):
    """Un golem levé par l'alchimie : des blocs de pierre appareillés, un cercle gravé sur le torse,
    des lignes de transmutation qui luisent encore."""
    stone = "#8a8780"
    for part in s.parts:
        s.fill(part, stone, "stone")

    def masonry(part, f, course=4, offset=0):
        """Un appareil de pierres : des joints horizontaux, des joints verticaux décalés."""
        w, h = s.size(part, f)
        for y in range(course - 1, h, course):
            s.tint(part, f, 0, y, w, 1, 0.72)
        for row, y0 in enumerate(range(0, h, course)):
            for x in range((row * 3 + offset) % 5, w, 5):
                s.tint(part, f, x, y0, 1, min(course - 1, h - y0), 0.78)

    for part in ("body", "waist", "rarm", "larm", "rleg", "lleg"):
        for f in SIDES:
            masonry(part, f)
    # La tête : arcade lourde, deux yeux de lumière, le nez taillé.
    masonry("head", "back")
    s.tint("head", "front", 0, 3, 8, 1, 0.65)
    for x in (1, 5):
        s.rect("head", "front", x, 4, 2, 1, GLOW)
        s.px("head", "front", x, 5, shade(GLOW, 0.55))
    s.tint("head", "front", 0, 8, 8, 2, 0.85)
    s.fill("nose", shade(stone, 0.95), "stone")
    # Le cercle de transmutation qui l'a animé, gravé au milieu du torse.
    cx, cy, r = 8.5, 5.5, 4.6
    w, h = s.size("body", "front")
    for y in range(h):
        for x in range(w):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            on_ring = abs(d - r) < 0.55
            on_tri = abs((y - (cy - r + 1)) - abs(x - cx) * 1.7) < 0.6 and y <= cy + 2.6
            on_base = abs(y - (cy + 2.5)) < 0.5 and abs(x - cx) <= 3.6
            if on_ring or on_tri or on_base:
                s.px("body", "front", x, y, GLOW if on_ring else mix(GLOW, stone, 0.25))
    s.px("body", "front", 8, 5, "#ffffff")
    # Les lignes d'alchimie descendent le long des bras jusqu'aux poings.
    for arm in ("rarm", "larm"):
        w, h = s.size(arm, "front")
        for y in range(4, h - 4):
            if y % 7 != 3:
                s.px(arm, "front", w // 2, y, mix(GLOW, stone, 0.35))
        s.band(arm, h - 4, 4, shade(stone, 0.85), "stone")
    # De la mousse et des cailloux dans les jointures, en bas des jambes.
    for leg in ("rleg", "lleg"):
        s.band(leg, 13, 3, shade(stone, 0.78), "stone")
        for f in SIDES:
            s.px(leg, f, 1, 12, "#5d7340")
            s.px(leg, f, 2, 12, "#4a5e33")


# --- le lézard d'Envy -----------------------------------------------------------------------------
@creature("envy_lizard", (64, 32), {
    "s0": (0, 0, 3, 2, 2), "s1": (0, 4, 4, 3, 2), "s2": (0, 9, 6, 4, 3), "s3": (0, 16, 3, 3, 3), "s4": (0, 22, 2, 2, 3),
    "s5": (11, 0, 2, 1, 2), "s6": (13, 4, 1, 1, 2), "l0": (20, 0, 10, 8, 3), "l1": (20, 11, 6, 4, 3),
    "l2": (20, 18, 6, 5, 2)})
def envy_lizard(s):
    """Envy réduit à sa vraie taille : un petit lézard vert sombre, les yeux rouges, l'ouroboros."""
    green, belly = "#3c6a34", "#a8b86a"
    for part in s.parts:
        s.fill(part, green, "stone")
        s.material(part, "bottom", belly, "skin")
        w, h = s.size(part, "top")
        for x in range(0, w, 2):
            s.tint(part, "top", x, 0, 1, h, 0.75)
    for part in ("l0", "l1", "l2"):
        s.fill(part, shade(green, 0.8), "stone")
        s.material(part, "top", shade(green, 0.7), "stone")
    # La tête : deux yeux rouges, l'ouroboros minuscule sur le dos.
    s.px("s0", "front", 0, 0, "#e0202a")
    s.px("s0", "front", 2, 0, "#e0202a")
    s.px("s0", "front", 1, 1, "#1a1010")
    s.px("s2", "top", 2, 1, "#c0182c")
    s.px("s2", "top", 3, 1, "#c0182c")
    s.px("s2", "top", 2, 2, "#c0182c")


# --- la Porte -------------------------------------------------------------------------------------
@creature("gate_hand", (64, 64), {"arm": (0, 0, 3, 3, 16), "hand": (0, 20, 5, 2, 4), "finger": (20, 20, 1, 1, 5)})
def gate_hand(s):
    """Les bras noirs de la Porte : une encre épaisse, des reflets violacés, des doigts effilés."""
    ink = "#0d0b12"
    for part in s.parts:
        s.fill(part, ink, "stone")
    # Un lustre humide qui court le long du bras.
    for f in ("top", "right", "left"):
        w, h = s.size("arm", f)
        for i in range(max(w, h)):
            s.px("arm", f, (i * 3) % w if w < h else i % w, i % h if w < h else (i * 3) % h, "#2a1d3a")
    for f in FACES:
        w, h = s.size("finger", f)
        s.tint("finger", f, 0, 0, w, h, 1.0)
    # Les bouts des doigts se dissolvent en fumée : un peu plus clairs.
    s.px("finger", "front", 0, 0, "#3a2c4e")


# --- le fauteuil roulant --------------------------------------------------------------------------
@creature("wheelchair", (64, 64), {
    "seat": (0, 0, 10, 2, 10), "back": (0, 12, 10, 10, 1), "armrest": (24, 12, 1, 1, 8), "handle": (44, 12, 1, 1, 3),
    "caster": (44, 18, 1, 3, 3), "footrest": (24, 24, 8, 1, 3), "frame": (48, 24, 1, 6, 1), "wheel": (0, 24, 1, 10, 10)})
def wheelchair(s):
    """Un fauteuil d'hôpital d'Amestris : cadre d'acier, assise de cuir vert, roues à rayons."""
    steel, leather, wood = "#9aa0a8", "#3e5a44", "#7a5434"
    s.fill("seat", steel, "metal")
    s.material("seat", "top", leather, "leather")
    s.fill("back", steel, "metal")
    s.material("back", "front", leather, "leather")
    s.material("back", "back", leather, "leather")
    for y in (3, 6):
        s.tint("back", "front", 1, y, 8, 1, 0.82)
    for x, y in ((2, 2), (7, 2), (2, 7), (7, 7)):
        s.px("back", "front", x, y, "#c8c8c0")
    s.fill("armrest", wood, "leather")
    s.fill("handle", "#1c1a1c", "leather")
    s.fill("caster", "#2a2a2e", "metal")
    s.fill("footrest", steel, "metal")
    s.fill("frame", steel, "metal")
    # La roue : un pneu noir, une jante d'acier, des rayons et un moyeu.
    s.fill("wheel", "#1e1d22", "leather")
    for f in ("right", "left"):
        w, h = s.size("wheel", f)
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for y in range(h):
            for x in range(w):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d > 5.2:
                    s.px("wheel", f, x, y, None)
                elif d > 4.2:
                    s.px("wheel", f, x, y, "#1e1d22")
                elif d > 3.4:
                    s.px("wheel", f, x, y, shade(steel, 1.1))
                elif x in (4, 5) or y in (4, 5) or abs(x - y) < 0.6 or abs(x + y - 9) < 0.6:
                    s.px("wheel", f, x, y, steel)
                else:
                    s.px("wheel", f, x, y, None)
        s.rect("wheel", f, 4, 4, 2, 2, "#5a5e66")


# --- automails ------------------------------------------------------------------------------------
LIMBS = {"rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4), "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4),
         "rsleeve": (40, 32, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
         "lpants": (0, 48, 4, 12, 4)}


def automail(s, metal, joint, accent, bolts, heavy=False):
    """Un membre mécanique : plaques, articulations à nu, rivets ; la surcouche porte les blindages."""
    for part in ("rarm", "larm", "rleg", "lleg"):
        s.fill(part, metal, "metal")
        # L'épaule ou la hanche, le coude ou le genou : des joints sombres et des pistons.
        s.band(part, 0, 1, shade(metal, 0.7), "metal")
        s.band(part, 5, 1, joint, "metal")
        s.tint(part, "front", 1, 5, 2, 1, 1.6)
        # Le piston qui relie les deux segments, à l'arrière du coude ou du genou.
        s.rect(part, "back", 1, 4, 2, 3, shade(joint, 1.8))
        s.px(part, "back", 1, 5, shade(metal, 1.15))
        for f in SIDES:
            w, _ = s.size(part, f)
            s.px(part, f, 0, 2, bolts)
            s.px(part, f, w - 1, 9, bolts)
            s.seam(part, f, w // 2, 7, 4, None, k=0.75)
        s.material(part, "top", shade(metal, 0.6), "metal")
    for arm in ("rarm", "larm"):
        # La main articulée : des phalanges séparées par des lignes sombres.
        s.band(arm, 10, 2, shade(metal, 0.92), "metal")
        for x in range(0, 4):
            s.tint(arm, "front", x, 11, 1, 1, 0.7 if x % 2 else 1.1)
        s.material(arm, "bottom", joint, "metal")
    for leg in ("rleg", "lleg"):
        s.band(leg, 10, 2, shade(metal, 0.8), "metal")
        s.material(leg, "bottom", joint, "metal")
    # La surcouche : une plaque d'avant-bras ou de tibia, aux couleurs du fabricant.
    for over, rows in (("rsleeve", (7, 10)), ("lsleeve", (7, 10)), ("rpants", (7, 10)), ("lpants", (7, 10))):
        y0, y1 = rows
        s.material(over, "front", accent, "metal", y=y0, h=y1 - y0)
        s.material(over, "right" if over[0] == "r" else "left", accent, "metal", y=y0, h=y1 - y0)
        s.px(over, "front", 0, y0, bolts)
        s.px(over, "front", 3, y0, bolts)
        if heavy:
            for f in SIDES:
                s.material(over, f, accent, "metal", y=0, h=4)
                s.tint(over, f, 0, 3, 4, 1, 0.7)


@creature("automail/iron", (64, 64), LIMBS)
def automail_iron(s):
    """L'automail de série : acier brut, comme celui d'Edward."""
    automail(s, "#a9afb6", "#2c2e33", "#c3c8ce", "#5a5e66")


@creature("automail/briggs", (64, 64), LIMBS)
def automail_briggs(s):
    """L'automail de Briggs : alliage noirci contre le gel, plaques épaisses aux épaules et aux hanches."""
    automail(s, "#5d636b", "#16171a", "#3a3e44", "#b8bcc2", heavy=True)


@creature("automail/rush_valley", (64, 64), LIMBS)
def automail_rush_valley(s):
    """L'automail de Rush Valley : acier poli, liserés de laiton, la fierté des artisans."""
    automail(s, "#c8ccd2", "#34302a", "#c8a24a", "#e8d080")


def build(names=None):
    for name in names or CREATURES:
        fn, size, parts = CREATURES[name]
        s = Skin(name, size=size)
        s.parts = dict(parts)
        fn(s)
        path = os.path.join(OUT, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s.save(path)
        print("texture", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
