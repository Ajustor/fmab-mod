"""Les icônes d'objets dessinées en code : sprites en ASCII (un caractère par pixel) et lames
tracées en diagonale. Une passe d'ombrage finale assombrit les bords tournés vers le bas et la
droite, éclaircit ceux du haut et de la gauche : la lumière vanilla vient d'en haut à gauche.
`python tools/skins/items.py [nom ...]` les régénère dans textures/item.
"""
from __future__ import annotations

import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from skinlib import mix, rgb, shade  # noqa: E402
from eggs import OUT  # noqa: E402

ITEMS = {}


def item(name):
    def register(fn):
        ITEMS[name] = fn
        return fn
    return register


def sprite(rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), rgb(palette[ch]))
    return img


def light(img, dark=0.72, bright=1.12):
    """Ombre les bords : en bas et à droite plus sombres, en haut et à gauche plus clairs."""
    out = img.copy()
    op = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3] > 0
    for y in range(16):
        for x in range(16):
            c = img.getpixel((x, y))
            if not c[3]:
                continue
            if not op(x + 1, y) or not op(x, y + 1):
                out.putpixel((x, y), mix(shade(c, dark), (30, 20, 50, 255), 0.15))
            elif not op(x - 1, y) or not op(x, y - 1):
                out.putpixel((x, y), shade(c, bright))
    return out


# --- automails ------------------------------------------------------------------------------------
AUTOMAIL = {
    "iron": dict(M="#a9afb6", L="#e1e5ea", D="#6c727a", J="#2c2e33", A="#c3c8ce"),
    "briggs": dict(M="#5d636b", L="#8b929b", D="#3a3e44", J="#16171a", A="#2e3238"),
    "rush_valley": dict(M="#c8ccd2", L="#f2f4f6", D="#8a8e96", J="#34302a", A="#c8a24a"),
}

ARM = [
    "................",
    ".....L.L.L.L....",
    ".....MDMDMDM....",
    "....LMDMDMDM....",
    "....MMMMMMMM....",
    ".....MLLMMMD....",
    ".....JJJJJJJ....",
    ".....AMLMMDA....",
    ".....AMLMMDA....",
    ".....AMLMMDA....",
    ".....AALAAAA....",
    ".....JJJJJJJ....",
    "......MLMMD.....",
    "......MLMMD.....",
    ".....DJJJJJD....",
    "................",
]

LEG = [
    "................",
    ".....DJJJJJD....",
    "......MLMMD.....",
    "......MLMMD.....",
    "......MLMMD.....",
    "......JJJJJ.....",
    "......AMLMA.....",
    "......AMLMA.....",
    "......AMLMA.....",
    "......AALAA.....",
    "......AMLMA.....",
    "......JJJJJ.....",
    "......MLMMMMM...",
    "......MMMMMMMD..",
    "......DDDDDDDD..",
    "................",
]

for _kind, _pal in AUTOMAIL.items():
    ITEMS[_kind + "_automail_arm"] = (lambda p: lambda: light(sprite(ARM, p)))(_pal)
    ITEMS[_kind + "_automail_leg"] = (lambda p: lambda: light(sprite(LEG, p)))(_pal)


# --- armes à feu ----------------------------------------------------------------------------------
@item("pistol")
def pistol():
    """Le pistolet réglementaire d'Amestris : culasse bronzée, plaquettes de bois."""
    return light(sprite([
        "................",
        "................",
        "................",
        "................",
        "..LLLLLLLLLLLL..",
        "..SSSSSSSSSSSSL.",
        "..SSSSSSSSSSSSD.",
        "..DSSSSSDDDDDD..",
        "...GGGD.D.......",
        "...GGgDD........",
        "..GGGGg.........",
        "..GGGGg.........",
        ".GGGGg..........",
        ".GgGGg..........",
        ".DDDDD..........",
        "................",
    ], dict(L="#80858f", S="#3e424a", D="#23252a", G="#7a4e2c", g="#5a3820")))


@item("rifle")
def rifle():
    """Le fusil à verrou de l'armée : crosse de noyer, canon bleui, levier de culasse."""
    return light(sprite([
        "..............L.",
        ".............SD.",
        "............SD..",
        "...........SW...",
        "..........SW....",
        ".........SW.....",
        "........SW......",
        ".......MMW......",
        "......MMMb......",
        ".....WMM..b.....",
        "....WWWT........",
        "...WWWWT........",
        "..WWWWg.........",
        ".WWWWg..........",
        ".DWWg...........",
        "..DD............",
    ], dict(L="#9aa0aa", S="#3e424a", D="#202228", W="#8a5a32", g="#5e3c22", M="#55595f", b="#a8adb5",
            T="#2a2c30")))


@item("cartridge")
def cartridge():
    """Deux cartouches : douille de laiton, balle de cuivre."""
    return light(sprite([
        "................",
        "................",
        "....C.....C.....",
        "...CCc...CCc....",
        "...CCc...CCc....",
        "...BBb...BBb....",
        "...BYb...BYb....",
        "...BYb...BYb....",
        "...BYb...BYb....",
        "...BYb...BYb....",
        "...BYb...BYb....",
        "...BYb...BYb....",
        "..RBBbR.RBBbR...",
        "...RRR...RRR....",
        "................",
        "................",
    ], dict(C="#c87a4a", c="#8e4e2c", B="#c9a24a", Y="#efd27a", b="#8e6e2a", R="#9a7a32")))


# --- lames ----------------------------------------------------------------------------------------
def diagonal(steps):
    """Trace un objet en diagonale, du pommeau (en bas à gauche) à la pointe (en haut à droite).
    `steps` : une liste de segments (kind, couleurs) ; chaque pas avance d'un pixel en x et en y.
    - ("blade", (clair, milieu, sombre)) : une bande de trois pixels ;
    - ("thin", (clair, sombre)) : deux pixels ;
    - ("core", couleur) : un seul pixel ;
    - ("guard", (couleur, demi-largeur)) : une garde perpendiculaire.
    """
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), rgb(c))

    for k, (kind, col) in enumerate(steps):
        x, y = 1 + k, 14 - k
        if kind == "blade":
            put(x, y - 1, col[0])
            put(x, y, col[1])
            put(x + 1, y, col[2])
        elif kind == "thin":
            put(x, y, col[0])
            put(x + 1, y, col[1])
        elif kind == "core":
            put(x, y, col)
        elif kind == "guard":
            c, half = col
            for i in range(-half, half + 1):
                put(x + i, y + i, c)
    return img


STEEL = ("#eef1f4", "#b8bec6", "#6e747c")


def blade(n, colors=STEEL, tip=True):
    seg = [("blade", colors)] * (n - 1)
    return seg + ([("core", colors[0])] if tip else [("blade", colors)])


@item("xing_sword")
def xing_sword():
    """Le sabre droit de Xing : garde dorée, poignée rouge, un pompon qui pend au pommeau."""
    img = diagonal([("core", "#e8c650"), ("thin", ("#c02a2a", "#7a1414")), ("thin", ("#c02a2a", "#7a1414")),
                    ("thin", ("#c02a2a", "#7a1414")), ("guard", ("#d9b440", 2))] + blade(9))
    for x, y in ((0, 15), (1, 15), (0, 14)):
        img.putpixel((x, y), rgb("#c0182c"))
    return light(img)


@item("briggs_sabre")
def briggs_sabre():
    """Le sabre d'officier de Briggs : garde de laiton, poignée de cuir noir, lame longue."""
    return light(diagonal([("core", "#b88a3a"), ("thin", ("#2a2a30", "#141418")), ("thin", ("#2a2a30", "#141418")),
                           ("guard", ("#c9a24a", 2))] + blade(10)))


@item("arm_blade")
def arm_blade():
    """La lame qu'Edward fait jaillir de son automail : le poignet d'acier, puis la lame large."""
    wrist = ("#c3c8ce", "#8a9098", "#4a4e56")
    return light(diagonal([("blade", wrist), ("blade", wrist), ("blade", ("#2c2e33",) * 3)] +
                          blade(11, ("#f4f6f8", "#c4cad2", "#7a8088"))))


@item("throwing_knife")
def throwing_knife():
    """Un couteau de lancer : anneau, manche gainé, lame courte."""
    return light(diagonal([("core", "#8a8e96"), ("thin", ("#3a2e28", "#221a16")), ("thin", ("#3a2e28", "#221a16")),
                           ("thin", ("#3a2e28", "#221a16")), ("guard", ("#5a5e66", 1))] + blade(6)))


@item("kunai")
def kunai():
    """Le kunai de May Chang : anneau, manche enrubanné de rouge, lame noire en feuille."""
    dark = ("#6a6e78", "#33363d", "#16171b")
    return light(diagonal([("core", "#8a8e96"), ("thin", ("#a02a2a", "#5a1414")), ("thin", ("#2a2a30", "#141418")),
                           ("thin", ("#a02a2a", "#5a1414")), ("thin", ("#2a2a30", "#141418")),
                           ("guard", ("#33363d", 1))] + blade(6, dark)))


@item("alchemist_chisel")
def alchemist_chisel():
    """Le ciseau de l'alchimiste : manche de bois, virole de laiton, tige d'acier au bout plat."""
    return light(diagonal([("thin", ("#a0703c", "#6a4424"))] * 5 + [("thin", ("#d9b440", "#8e6e2a"))] +
                          [("thin", ("#d8dde2", "#7a8088"))] * 6 + [("core", "#f4f6f8")]))


@item("chalk")
def chalk():
    """Un bâton de craie, usé en biseau, avec un peu de poudre."""
    chalk = ("#ffffff", "#ece8de", "#bdb8aa")
    img = diagonal([("core", "#00000000")] * 4 + [("blade", chalk)] * 5 + [("thin", chalk[:2])])
    # La poudre tombée sous le bâton.
    for x, y in ((3, 14), (5, 14), (2, 13)):
        img.putpixel((x, y), rgb("#e0dcd2"))
    return light(img)


def build(names=None):
    for name in names or ITEMS:
        ITEMS[name]().save(os.path.join(OUT, name + ".png"))
        print("objet", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
