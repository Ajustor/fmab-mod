"""Les icônes d'objets dessinées en code : sprites en ASCII (un caractère par pixel) et lames
tracées en diagonale. Une passe d'ombrage finale assombrit les bords tournés vers le bas et la
droite, éclaircit ceux du haut et de la gauche : la lumière vanilla vient d'en haut à gauche.
`python tools/skins/items.py [nom ...]` les régénère dans textures/item.
"""
from __future__ import annotations

import json
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


# --- les fournitures de l'alchimiste --------------------------------------------------------------
GLASS = dict(G="#a9c6d6", H="#f2fbff", g="#6f8fa2")


@item("alchemical_ink")
def alchemical_ink():
    """L'encre alchimique : un flacon trapu de verre, bouché de liège, l'encre violette où passe un reflet,
    une étiquette marquée d'un cercle."""
    return light(sprite([
        "................",
        "......kKKk......",
        "......cKKc......",
        "......cccc......",
        ".......GH.......",
        ".......Gg.......",
        "....GGGGHGGG....",
        "...GHIIIIIIIg...",
        "...GHIVIIIIIg...",
        "...GLLLLLLLLg...",
        "...GLLLrrLLLg...",
        "...GLLLLLLLLg...",
        "...GIIIIIIIdg...",
        "...GIIIIIIddg...",
        "....gggggggg....",
        "................",
    ], dict(GLASS, k="#8a5c34", K="#c8986a", c="#6a4426", I="#2c1a4c", V="#8a68d0", d="#160c28",
            L="#ece2c8", r="#b0162e")))


@item("alchemical_paint")
def alchemical_paint():
    """La peinture alchimique : un pot de fer-blanc ouvert, plein de rouge, un pinceau planté dedans."""
    return light(sprite([
        "...........WW...",
        "..........WWb...",
        ".........WWb....",
        "........WWb.....",
        ".......FFb......",
        "......FF........",
        "...TRRRrRRRRT...",
        "..TRRrrRRRRRRT..",
        "..TTRRRRRRRRTT..",
        "..TATTTTTTTTDT..",
        "..TALLLLLLLLDT..",
        "..TALLLrrLLLDT..",
        "..TALLLrrLLLDT..",
        "..TALLLLLLLLDT..",
        "..TATTTTTTTTDT..",
        "...TTTTTTTTTT...",
    ], dict(W="#c49a5e", b="#7a5430", F="#c8ccd2", R="#c8202c", r="#f05a50", T="#8a9098", A="#c8ccd4",
            D="#5a6068", L="#e8dcc0")))


@item("alchemical_thread")
def alchemical_thread():
    """Le fil alchimique : une bobine de bois, le fil rouge enroulé en spires, le bout qui pend et son
    aiguille."""
    img = sprite([
        "................",
        "...WWWWWWWWWW...",
        "...wwwwwwwwww...",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "....RRRRRRRR....",
        "...WWWWWWWWWW...",
        "...wwwwwwwwwwR..",
        ".............R..",
        "............R...",
        "...........N....",
        "..........N.....",
    ], dict(W="#c49a5e", w="#7a5430", R="#b0162e", N="#d8dde2"))
    # Les spires : des traits obliques plus clairs et plus sombres, enroulés autour de la bobine.
    for y in range(3, 10):
        for x in range(4, 12):
            k = (x + 2 * y) % 4
            if k == 0:
                img.putpixel((x, y), rgb("#e84a5a"))
            elif k == 2:
                img.putpixel((x, y), rgb("#7a0c1c"))
    return light(img)


def book(cover, spine, emblem, accent, pages="#ece2c8", clasp=None, ribbon=None):
    """Un livre relié vu de face : le dos à gauche, la tranche des pages à droite, des coins de métal,
    un emblème au centre du plat."""
    img = sprite([
        "................",
        "..SCCCCCCCCCCP..",
        "..SCKCCCCCCKCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCCCCCCCCCCPp.",
        "..SCKCCCCCCKCPp.",
        "..SCCCCCCCCCCPp.",
        "..sSSSSSSSSSSSs.",
        "................",
    ], dict(S=spine, s=shade(spine, 0.7), C=cover, K=accent, P=pages, p=shade(pages, 0.75)))
    for y in range(2, 14):
        if y % 2 == 0:
            img.putpixel((14, y), rgb(shade(pages, 0.9)))
    # Le dos : des nerfs dorés.
    for y in (3, 7, 11):
        img.putpixel((2, y), rgb(accent))
    emblem(img)
    if clasp:
        for y in (7, 8):
            img.putpixel((13, y), rgb(clasp))
            img.putpixel((14, y), rgb(shade(clasp, 0.8)))
    if ribbon:
        img.putpixel((9, 14), rgb(ribbon))
        img.putpixel((9, 15), rgb(ribbon))
        img.putpixel((10, 15), rgb(shade(ribbon, 0.7)))
    return light(img)


def ring_emblem(img, cx, cy, r, color, inner=None):
    for y in range(16):
        for x in range(16):
            if abs(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 - r) < 0.5:
                img.putpixel((x, y), rgb(color))
    if inner:
        inner(img)


@item("alchemy_tome")
def alchemy_tome():
    """Le tome d'alchimie : un gros volume de cuir rouge, coins et fermoir de laiton, un cercle de
    transmutation doré sur le plat."""
    def emblem(img):
        ring_emblem(img, 7.5, 7.5, 3.6, "#e8c050")
        for x, y in ((7, 5), (8, 5), (6, 7), (9, 7), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9),
                     (6, 8), (9, 8)):
            img.putpixel((x, y), rgb("#d9b440"))
        img.putpixel((7, 7), rgb("#fff0a0"))
    return book("#7a1a1e", "#4e0e12", emblem, "#d9b440", clasp="#d9b440")


@item("alchemy_treatise")
def alchemy_treatise():
    """Le traité d'alchimie : un ouvrage de toile bleu nuit, une pièce de titre en cuir, la croix de
    Flamel dorée, un signet rouge qui dépasse."""
    def emblem(img):
        for y in range(3, 6):
            for x in range(5, 11):
                img.putpixel((x, y), rgb("#5a3a22"))
        for x in range(6, 10):
            img.putpixel((x, 4), rgb("#d9b440"))
        # La croix fleurie et le serpent.
        for y in range(7, 13):
            img.putpixel((7, y), rgb("#e8c050"))
        for x in range(5, 10):
            img.putpixel((x, 8), rgb("#e8c050"))
        for x, y in ((6, 10), (8, 11), (6, 12), (8, 9)):
            img.putpixel((x, y), rgb("#3a9a5a"))
        img.putpixel((7, 6), rgb("#fff0a0"))
    return book("#22305e", "#141c3a", emblem, "#c8a040", ribbon="#c0182c")


@item("chalk")
def chalk():
    """Un bâton de craie usé en biseau, et le trait qu'il vient de laisser."""
    img = diagonal([("core", "#00000000")] * 3 +
                   [("blade", ("#ffffff", "#ece8de", "#b8b2a4"))] * 6 + [("thin", ("#ffffff", "#d0cabc"))])
    # Le trait de craie, tiré sous le bâton, qui s'efface au bout.
    for x in range(1, 13):
        if x % 4 != 3:
            img.putpixel((x, 15), rgb("#f4f2ec" if x < 8 else "#cfcac0"))
    for x, y in ((2, 13), (4, 14)):
        img.putpixel((x, y), rgb("#e8e4da"))
    return light(img)


# --- les gants -------------------------------------------------------------------------------------
GLOVE = [
    "................",
    "......L.L.L.....",
    "......M.M.M.L...",
    "......MDMDMDM...",
    "......MDMDMDM...",
    "......MDMDMDM...",
    "......MMMMMMM...",
    "...LL.MMMMMMM...",
    "...MMMMMMMMMD...",
    "....MMMMMMMMD...",
    ".....MMMMMMMD...",
    "......MMMMMMD...",
    "......MMMMMMD...",
    ".....CCCCCCCC...",
    ".....CCCCCCCC...",
    ".....cccccccc...",
]


def glove(colors, back=None, rows=GLOVE):
    """Le gant de la main droite, paume vers soi, doigts dressés : la même silhouette pour tous."""
    img = sprite(rows, colors)
    if back:
        back(img)
    return light(img)


def put(img, pts, color):
    for x, y in pts:
        img.putpixel((x, y), rgb(color))


@item("cloth_gloves")
def cloth_gloves():
    """Des gants de coton blanc, à teindre : des tons gris que la teinture colore, une couture au dos,
    un poignet côtelé."""
    def back(img):
        put(img, ((8, 8), (9, 9), (10, 10)), "#c8c8c8")
        put(img, ((6, 13), (8, 13), (10, 13), (12, 13), (6, 14), (8, 14), (10, 14), (12, 14)), "#d0d0d0")
    return glove(dict(L="#ffffff", M="#ececec", D="#b8b8b8", C="#f6f6f6", c="#a8a8a8"), back)


def salamander(img, color, center="#f4f0e8"):
    """Le cercle de Mustang au dos de la main : un anneau, le triangle de la flamme, la salamandre."""
    put(img, ((8, 7), (9, 7), (10, 8), (11, 9), (10, 10), (9, 11), (8, 11), (7, 10), (6, 9), (7, 8)), color)
    put(img, ((8, 9), (9, 9)), color)
    put(img, ((8, 8), (9, 10)), shade(color, 0.7))
    put(img, ((9, 8),), center)


@item("spark_gloves")
def spark_gloves():
    """Les gants à étincelles de Mustang : coton blanc, le cercle de la salamandre brodé en rouge, le
    tissu d'allumage sur le bout des doigts."""
    def back(img):
        salamander(img, "#c8202c")
        put(img, ((6, 1), (8, 1), (10, 1)), "#9aa0a8")
        put(img, ((6, 13), (12, 13)), "#d9b440")
    return glove(dict(L="#ffffff", M="#ece8e0", D="#b8b2a8", C="#f6f2ea", c="#a8a29a"), back)


@item("state_gloves")
def state_gloves():
    """Les gants d'Alchimiste d'État : cuir bleu nuit, l'emblème doré au dos, le poignet galonné d'or."""
    def back(img):
        put(img, ((8, 8), (9, 8), (7, 9), (10, 9), (8, 10), (9, 10)), "#d9b440")
        put(img, ((8, 9), (9, 9)), "#fff0a0")
    return glove(dict(L="#3a5090", M="#26386c", D="#141e40", C="#d9b440", c="#8e6e2a"), back)


@item("iron_gauntlets")
def iron_gauntlets():
    """Des gantelets de fer : doigts articulés en écailles, plaque des jointures rivetée, manchette."""
    def back(img):
        for x in range(6, 13):
            img.putpixel((x, 7), rgb("#e1e5ea"))
            img.putpixel((x, 8), rgb("#6c727a"))
        put(img, ((7, 10), (11, 10), (7, 12), (11, 12)), "#f4f6f8")
        put(img, ((6, 14), (9, 14), (12, 14)), "#e1e5ea")
        for y in (2, 4):
            put(img, ((6, y), (8, y), (10, y), (12, y + 1)), "#4a5058")
    return glove(dict(L="#e1e5ea", M="#a9afb6", D="#4a5058", C="#7a8088", c="#3a3e44"), back)


# --- les notes et les cercles ----------------------------------------------------------------------
@item("ishval_tattoo")
def ishval_tattoo():
    """Les notes du frère de Scar : un feuillet jauni aux bords rongés, le dessin du tatouage du bras,
    des colonnes d'écriture d'Ishval."""
    img = sprite([
        "................",
        "...PPPPPPPPPP...",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "...PPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPP...",
        "..PPPPPPPPPPPp..",
        "...PPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPPp..",
        "..PPPPPPPPPPP...",
        "...pppppppppp...",
        "................",
    ], dict(P="#e6d6b0", p="#b89c6a"))
    # Le bras tatoué : une bande de traits croisés, et deux cercles à ses bouts.
    for y in range(3, 13):
        for x in (5, 6, 7):
            if (x + y) % 2 == 0:
                img.putpixel((x, y), rgb("#2a2440"))
        img.putpixel((4, y), rgb("#c8b48a"))
        img.putpixel((8, y), rgb("#c8b48a"))
    put(img, ((5, 2), (6, 2), (7, 2), (5, 13), (6, 13), (7, 13)), "#8a1a2a")
    # L'écriture en colonnes.
    for x in (10, 12):
        for y in range(3, 13):
            if (y * 3 + x) % 5 != 0:
                img.putpixel((x, y), rgb("#5a4a3a"))
    put(img, ((3, 4), (12, 11), (11, 2)), "#c8ac78")
    return light(img)


@item("crimson_seals")
def crimson_seals():
    """Les cercles écarlates de Kimblee : la paume ouverte, tatouée du cercle du soleil (celui de l'autre
    main porte la lune, dont le croissant mord le bord)."""
    def back(img):
        ink = "#a8101e"
        cx, cy = 9.0, 9.5
        for y in range(16):
            for x in range(16):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if abs(d - 2.7) < 0.55 and img.getpixel((x, y))[3]:
                    img.putpixel((x, y), rgb(ink))
        put(img, ((9, 9), (8, 9), (9, 10), (8, 10)), "#e0302c")
        put(img, ((9, 6), (12, 9), (9, 13), (6, 10)), "#e0302c")
        put(img, ((12, 12), (12, 11)), "#3a1a22")
    return glove(dict(L="#f6d8bc", M="#e8c4a4", D="#b88a6a", C="#e8c4a4", c="#b88a6a"), back)


# --- la Pierre philosophale ------------------------------------------------------------------------
def gem(core, mid, dark, rim, glow):
    """Une gemme rouge à facettes : une table claire en haut, des pans qui s'assombrissent vers le bas,
    le reflet dur d'une arête."""
    img = sprite([
        "................",
        "................",
        ".....RRRRRR.....",
        "....RTTTTTTR....",
        "...RTTHHTTTTR...",
        "..RMTTHTTTTTMR..",
        "..RMMTTTTTTMMR..",
        "..RMMMMTTMMMMR..",
        "..RDMMMMMMMMDR..",
        "...RDMMMMMMDR...",
        "....RDDMMDDR....",
        ".....RDDDDR.....",
        "......RDDR......",
        ".......RR.......",
        "................",
        "................",
    ], dict(R=rim, T=core, H=glow, M=mid, D=dark))
    # Les arêtes des facettes : des lignes claires qui partent de la table.
    put(img, ((4, 5), (5, 6), (6, 7), (11, 5), (10, 6), (9, 7)), shade(mid, 1.35))
    return light(img)


@item("philosopher_stone")
def philosopher_stone():
    """La Pierre philosophale : une gemme de sang, brillante, qui bat comme un cœur (l'éclat pulse)."""
    frames = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for k, boost in enumerate((1.0, 1.12, 1.25, 1.12)):
        g = gem(shade("#f04050", boost), shade("#c01828", boost), "#6a0612", "#3a0208", "#ffd0d4")
        frames.paste(g, (0, 16 * k))
    return frames, {"animation": {"frametime": 4}}


@item("philosopher_stone_core")
def philosopher_stone_core():
    """Le noyau d'une Pierre : une gemme plus petite et plus sombre, enchâssée dans une griffe de fer."""
    img = gem("#d0283a", "#8e101e", "#4a040c", "#240106", "#ffb0b8")
    claw = "#6a7078"
    put(img, ((2, 6), (2, 7), (13, 6), (13, 7), (7, 14), (8, 14), (3, 4), (12, 4)), claw)
    put(img, ((1, 7), (14, 7), (7, 15), (8, 15)), shade(claw, 0.7))
    return img


# --- l'armure d'âme et la lance de pierre ---------------------------------------------------------
@item("soul_chestplate")
def soul_chestplate():
    """Le plastron d'âme, à la manière de l'armure d'Alphonse : acier bleuté, épaulières rondes, la
    plaque du torse à arête, des rivets ; dans l'ouverture du col, le sceau de sang."""
    img = sprite([
        "................",
        ".SSSS.....SSSS..",
        "SLLLSS...SSLLLS.",
        "SLMMMSKKKSMMMDS.",
        ".SMMMSkBkSMMMDS.",
        ".SSMMMMLMMMMMSS.",
        "...MMMMLMMMMD...",
        "...MLMMLMMMMD...",
        "...MMMMLMMMMD...",
        "...PPPPPPPPPP...",
        "...MMMMLMMMMD...",
        "...PPPPPPPPPP...",
        "....MMMLMMMD....",
        ".....MMMMMD.....",
        "................",
        "................",
    ], dict(S="#5a6676", L="#d8e0ea", M="#9aa6b6", D="#5e6a7a", P="#6e7a8a", K="#1c1418", k="#2a1a1e",
            B="#c0182c"))
    put(img, ((4, 6), (11, 6), (4, 10), (11, 10)), "#eef2f6")
    return light(img)


LANCE = dict(H="#e2ddd0", L="#bdb8ac", M="#8e897e", D="#4e4b46", S="#9a958a", s="#6e6a62")


@item("stone_lance")
def stone_lance():
    """La lance de pierre tirée du sol : un fût brut et noueux qui part d'en bas à gauche, une ligature,
    puis une pointe de roche en losange, effilée, son arête claire au milieu."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def px(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), rgb(LANCE[c]))
    # Le fût, deux pixels de large, des nœuds çà et là.
    for k in range(7):
        x, y = k, 15 - k
        px(x, y, "S")
        px(x + 1, y, "s" if k % 3 else "D")
    # La ligature, sombre, à la jonction.
    for x, y in ((6, 8), (7, 9), (8, 9), (7, 8)):
        px(x, y, "D")
    # La pointe : plus large près de la base, effilée vers le haut.
    widths = (1, 2, 2, 2, 1, 1, 1, 0)
    for k, w in enumerate(widths):
        x, y = 8 + k, 7 - k
        for j in range(-w, w + 1):
            c = "H" if j == 0 else "L" if j < 0 else "M" if j < w else "D"
            px(x + j, y + j, c)
            if j < w:
                px(x + j + 1, y + j, c if j >= 0 else "L")
    return light(img)


@item("stone_lance_in_hand")
def stone_lance_in_hand():
    """La même lance pour la main, en 32x32 comme les lances vanilla : un long fût de roche de trois
    pixels, noueux, et la pointe en losange en haut à gauche."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def px(x, y, c):
        if 0 <= x < 32 and 0 <= y < 32:
            img.putpixel((x, y), rgb(LANCE[c] if c in LANCE else c))
    # Le fût : trois pixels, éclairé en haut à droite, des nœuds tous les cinq pas.
    for r in range(10, 32):
        knot = r % 5 == 0
        px(r - 1, r, "D" if knot else "s")
        px(r, r, "M" if knot else "S")
        px(r + 1, r, "L" if not knot else "S")
    # La ligature de cuir.
    for r in (9, 10):
        for c in range(r - 2, r + 3):
            px(c, r, "#4a3424" if (c + r) % 2 else "#6a4a30")
    # La pointe : un losange taillé, l'arête claire au milieu.
    widths = (0, 1, 1, 2, 2, 3, 3, 3, 2)
    for i, w in enumerate(widths):
        for j in range(-w, w + 1):
            c = "H" if j == 0 else "L" if j > 0 else "M" if j > -w else "D"
            px(i + j, i - j, c)
            px(i + j + 1, i - j, c if j < w else "D")
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


def blinking(eyes, open_ticks):
    """Les yeux seuls, sur fond transparent : le modèle les pose en pleine lumière par-dessus le noir.
    Trois images empilées (ouverts, mi-clos, clos) que l'animation fait cligner de temps en temps."""
    frames = Image.new("RGBA", (16, 48), (0, 0, 0, 0))
    for k in range(3):
        for kind, x0, y0 in eyes:
            rows = EYES[kind]
            shut = (len(rows) * k + 1) // 2
            for dy, row in enumerate(rows):
                for dx, ch in enumerate(row):
                    if ch == ".":
                        continue
                    if dy < shut and ch != "s":
                        ch = "L"
                    elif k == 2 and ch != "s":
                        ch = "L"
                    frames.putpixel((x0 + dx, 16 * k + y0 + dy), rgb(EYE_COLORS[ch]))
    meta = {"animation": {"frametime": 2, "frames": [{"index": 0, "time": open_ticks}, 1, 2, 2, 1]}}
    return frames, meta


GATE_EYES = {
    "gate_darkness_eye": ([("big", 4, 5)], 70),
    "gate_darkness_lidded": ([("lidded", 2, 10)], 45),
    "gate_darkness_eyes": ([("small", 1, 2), ("small", 9, 10)], 110),
}
BLOCKS["gate_darkness"] = lambda: darkness([])
for _name, (_eyes, _ticks) in GATE_EYES.items():
    BLOCKS[_name] = lambda: darkness([])
    BLOCKS[_name + "_glow"] = (lambda e, n: lambda: blinking(e, n))(_eyes, _ticks)


def build(names=None):
    for name in names or list(ITEMS) + list(BLOCKS):
        if name in BLOCKS:
            img = BLOCKS[name]()
            path = os.path.join(OUT, "..", "block", name + ".png")
            if isinstance(img, tuple):
                img, meta = img
                with open(path + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
                    json.dump(meta, f, indent=2)
                    f.write("\n")
            img.save(path)
        else:
            img = ITEMS[name]()
            path = os.path.join(OUT, name + ".png")
            if isinstance(img, tuple):
                img, meta = img
                with open(path + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
                    json.dump(meta, f, indent=2)
                    f.write("\n")
            img.save(path)
        print("texture", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
