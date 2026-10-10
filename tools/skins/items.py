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


# --- objets du quotidien de l'alchimiste -------------------------------------------------------------
@item("leather_gloves")
def leather_gloves():
    """Un gant de cuir fauve : doigts séparés, une couture sur le dos, un poignet plus sombre à boucle."""
    return light(sprite([
        "................",
        "......L.L.L.....",
        "......M.M.M.L...",
        "......MDMDMDM...",
        "......MDMDMDM...",
        "......MDMDMDM...",
        "......MMMMMMM...",
        "...LL.MMMMMMM...",
        "...MMMMSMMMMD...",
        "....MMMMSMMMD...",
        ".....MMMMSMMD...",
        "......MMMMMMD...",
        "......MMMMMMD...",
        ".....CCCCCCCC...",
        ".....CKCCCCCC...",
        ".....CCCCCCCC...",
    ], dict(L="#c48a52", M="#a06a3a", D="#6e4524", S="#d8b080", C="#5a361c", K="#d9b440")))


@item("red_stone_shard")
def red_stone_shard():
    """Un éclat de pierre rouge : un cristal à facettes, translucide sur l'arête, presque noir au cœur."""
    img = sprite([
        "................",
        "..........H.....",
        ".........HLR....",
        "........HLLRD...",
        ".......HLLRRD...",
        "......HLLRRRD...",
        ".....HLLRRRDD...",
        ".....LLRRRRD....",
        "....HLRRRRDD....",
        "....LLRRRDD.....",
        "....LRRRDD......",
        "...HLRRDD.......",
        "...LRRDD........",
        "...RRDD.........",
        "...DD...........",
        "................",
    ], dict(H="#ffc8d0", L="#e8506a", R="#b0162e", D="#5a0814"))
    out = light(img)
    for x, y in ((12, 3), (6, 11)):
        out.putpixel((x, y), rgb("#fff4f6"))
    return out


@item("state_watch")
def state_watch():
    """La montre d'Alchimiste d'État : boîtier d'argent fermé, l'emblème d'Amestris gravé sur le
    couvercle, la bélière et un bout de chaîne."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 9.5, 6.0
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > r + 0.4:
                continue
            if d > r - 0.6:
                img.putpixel((x, y), rgb("#4a4f58"))
                continue
            # L'argent poli : clair en haut à gauche, sombre en bas à droite, un liseré près du bord.
            t = ((x - cx) + (y - cy)) / (2 * r)
            base = mix(rgb("#f2f4f7"), rgb("#7c838d"), min(1.0, max(0.0, t + 0.5)))
            if r - 1.8 < d <= r - 0.6:
                base = shade(base, 0.86)
            img.putpixel((x, y), base)
    # L'emblème : un hexagramme stylisé, gravé.
    for dx, dy in ((0, -2), (-2, -1), (-1, -1), (0, -1), (1, -1), (2, -1), (-1, 0), (1, 0),
                   (-2, 1), (-1, 1), (0, 1), (1, 1), (2, 1), (0, 2)):
        img.putpixel((int(cx + 0.5) + dx, int(cy) + dy), rgb("#5a616b"))
    # La bélière (le petit pas de vis) et son anneau.
    for x, y, c in ((7, 3, "#9aa1aa"), (8, 3, "#c9ced6"), (7, 2, "#c9ced6"), (8, 2, "#8a9099"),
                    (6, 1, "#4a4f58"), (7, 0, "#c9ced6"), (8, 0, "#c9ced6"), (9, 1, "#4a4f58")):
        img.putpixel((x, y), rgb(c))
    # La chaîne, qui part vers la droite et retombe.
    for k, (x, y) in enumerate(((10, 1), (11, 1), (12, 2), (13, 3), (13, 4), (14, 5), (14, 6))):
        img.putpixel((x, y), rgb("#d8dde2" if k % 2 == 0 else "#7c838d"))
    return img


# --- les ténèbres derrière la Porte -----------------------------------------------------------------
BLOCKS = {}


EYES = {
    "big": ["...sss....",
            ".sWGRRWWs.",
            "sWWRPPRWWs",
            ".sWWRRWWs.",
            "...sss...."],
    "lidded": ["sLLLLLLLs",
               "sWWRPPRWs",
               ".ssWWWss."],
    "small": [".sWWs.",
              "sWRPWs",
              ".sWWs."],
}
EYE_COLORS = dict(s="#6a665e", W="#dcd8cc", R="#6a0f18", P="#050407", G="#ffffff", L="#2a2630")


def darkness(eyes):
    """Un bloc de ténèbres : un noir à peine grenu, et des yeux qui s'ouvrent dedans."""
    import random
    rng = random.Random(str(eyes))
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade("#0a080e", rng.uniform(0.85, 1.15)))
    for kind, x0, y0 in eyes:
        for dy, row in enumerate(EYES[kind]):
            for dx, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x0 + dx, y0 + dy), rgb(EYE_COLORS[ch]))
    return img


BLOCKS["gate_darkness"] = lambda: darkness([])
BLOCKS["gate_darkness_eye"] = lambda: darkness([("big", 4, 5)])
BLOCKS["gate_darkness_lidded"] = lambda: darkness([("lidded", 2, 10)])
BLOCKS["gate_darkness_eyes"] = lambda: darkness([("small", 1, 2), ("small", 9, 10)])


def build(names=None):
    for name in names or list(ITEMS) + list(BLOCKS):
        if name in BLOCKS:
            BLOCKS[name]().save(os.path.join(OUT, "..", "block", name + ".png"))
        else:
            ITEMS[name]().save(os.path.join(OUT, name + ".png"))
        print("texture", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
