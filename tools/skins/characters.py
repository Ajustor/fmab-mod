"""Les peaux des personnages du mod. `python tools/skins/characters.py [nom ...]` les régénère dans
src/main/resources/assets/fmab/textures/entity (toutes, sans argument).

Deux squelettes : le modèle de joueur (avec sa surcouche : veste, manches, jambières, chapeau) et
celui des boss (BossModels), qui n'a que le chapeau en surcouche et réserve la bande y 32 à 48 à ses
volumes propres (ventre, chevelure, col...). Pour ces derniers, on déclare la boîte de chaque volume
avec son texOffs et sa taille, tels qu'ils sont dans BossModels.
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from skinlib import SIDES, Skin, eyes, mix, shade, skin_face  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "fmab", "textures",
                   "entity")

SKINS = {}


def skin(name, **kw):
    def register(fn):
        SKINS[name] = (fn, kw)
        return fn
    return register


# --- teintes communes ---------------------------------------------------------------------------
PALE = "#f0d4b8"
FAIR = "#e8c29e"
TAN = "#d6a57c"
ISHVAL = "#8a5634"
AMESTRIS_BLUE = "#2f4f93"
GOLD = "#d9b440"
BOOT = "#1c1b20"
BLACK_CLOTH = "#1e1c22"
HOMUNCULUS_EYE = "#b0123a"
OUROBOROS = "#c0182c"


# --- pièces de costume réutilisables --------------------------------------------------------------
def hands(s, tone, rows=2, glove=None):
    """Le bas des bras : mains nues ou gantées, dessous compris."""
    c = glove or tone
    mat = "cloth" if glove else "skin"
    for arm in ("rarm", "larm"):
        for f in SIDES:
            w, h = s.size(arm, f)
            s.material(arm, f, c, mat, y=h - rows, h=rows)
        s.material(arm, "bottom", c, mat)
        # Le pouce, un pixel plus sombre sur le devant de la main.
        s.tint(arm, "front", 0 if arm == "larm" else s.size(arm, "front")[0] - 1, 12 - rows, 1, 1, 0.9)


def bare_arms(s, tone):
    for arm in ("rarm", "larm"):
        s.fill(arm, tone, "skin")
        s.tint(arm, "front", 0, 4, s.size(arm, "front")[0], 1, 0.95)


def boots(s, color=BOOT, rows=4, sole="#0e0d10", cuff=None, mat="leather"):
    for leg in ("rleg", "lleg"):
        s.band(leg, 12 - rows, rows, color, mat)
        s.material(leg, "bottom", sole, "leather")
        if cuff:
            s.band(leg, 12 - rows, 1, cuff, "leather")
        # La pointe du pied, un peu plus claire, et le talon dans l'ombre.
        s.tint(leg, "front", 0, 11, 4, 1, 1.15)
        s.tint(leg, "back", 0, 11, 4, 1, 0.8)


def belt(s, y=8, color="#1a1a1e", buckle="#c9c9cf", part="body"):
    s.band(part, y, 1, color, "leather")
    s.px(part, "front", 3, y, buckle)
    s.px(part, "front", 4, y, shade(buckle, 0.75))


def short_hair(s, color, fringe=1, sideburn=3, back=4, part="head"):
    """Des cheveux courts : le dessus, une frange, les tempes et la nuque."""
    s.material(part, "top", color, "hair")
    if fringe:
        s.material(part, "front", color, "hair", h=fringe)
    s.material(part, "right", color, "hair", h=sideburn)
    s.material(part, "left", color, "hair", h=sideburn)
    # Sur les côtés, la nuque est vers le dos : x bas à droite, x haut à gauche.
    s.material(part, "right", color, "hair", x=0, w=4, h=back)
    s.material(part, "left", color, "hair", x=4, w=4, h=back)
    s.material(part, "back", color, "hair", h=back + 2)
    s.tint(part, "back", 0, back + 1, 8, 1, 0.85)


def fringe(s, color, heights, part="head"):
    """Une frange mèche par mèche : la hauteur de chaque colonne du front."""
    for x, h in enumerate(heights):
        if h:
            s.material(part, "front", color, "hair", x=x, w=1, h=h)
            s.tint(part, "front", x, h - 1, 1, 1, 0.82)


def hair_volume(s, color, sides=3, back=6, front=0, part="hat"):
    """De la masse dans la surcouche du chapeau : les cheveux décollent un peu du crâne."""
    s.material(part, "top", color, "hair")
    s.material(part, "right", color, "hair", h=sides)
    s.material(part, "left", color, "hair", h=sides)
    s.material(part, "right", color, "hair", x=0, w=4, h=back)
    s.material(part, "left", color, "hair", x=4, w=4, h=back)
    s.material(part, "back", color, "hair", h=back)
    if front:
        s.material(part, "front", color, "hair", h=front)


def long_hair_back(s, color, rows=6, part="jacket", width=None):
    """Des cheveux longs qui tombent dans le dos, peints sur la surcouche de la veste."""
    w = width or 8
    x = (8 - w) // 2
    s.material(part, "back", color, "hair", x=x, w=w, h=rows)
    for i in range(w):
        # Des pointes irrégulières.
        if (i + rows) % 3 == 0:
            s.px(part, "back", x + i, rows, shade(color, 0.8))


def ouroboros(s, part, f, x, y, color=OUROBOROS):
    """Le tatouage des homoncules : un anneau de 3x3 (le serpent qui se mord la queue)."""
    for dx, dy in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2)):
        s.px(part, f, x + dx, y + dy, color)
    s.px(part, f, x + 2, y + 2, shade(color, 0.6))


def amestris_uniform(s, blue=AMESTRIS_BLUE, officer=False, gloves=None, tone=FAIR, legs=True):
    """L'uniforme bleu de l'armée d'Amestris : vareuse boutonnée, col droit, ceinturon, bottes."""
    dark = shade(blue, 0.62)
    for part in ("body", "rarm", "larm") + (("rleg", "lleg") if legs else ()):
        s.fill(part, blue, "cloth")
    # Le col droit, plus sombre, et l'ouverture boutonnée de la vareuse.
    s.material("body", "front", dark, "cloth", y=0, h=1)
    s.material("body", "back", dark, "cloth", y=0, h=1)
    s.px("body", "front", 3, 0, shade(blue, 0.45))
    s.px("body", "front", 4, 0, shade(blue, 0.45))
    s.seam("body", "front", 4, 1, 11, None, k=0.78)
    for y in (2, 4, 6, 10):
        s.px("body", "front", 3, y, GOLD)
    # Les poches de poitrine : un rabat et une ombre.
    for x in (1, 5):
        s.rect("body", "front", x, 2, 2, 1, shade(blue, 0.8))
        s.tint("body", "front", x, 3, 2, 1, 0.93)
    if officer:
        # L'aiguillette dorée sur l'épaule droite et les pattes d'épaule.
        for arm in ("rarm", "larm"):
            s.material(arm, "top", GOLD, "metal")
            s.band(arm, 0, 1, shade(GOLD, 0.85), "metal")
        s.px("body", "front", 0, 1, GOLD)
        s.px("body", "front", 1, 2, GOLD)
        s.px("body", "front", 2, 3, shade(GOLD, 0.8))
    belt(s, y=8)
    # Les pans de la vareuse tombent sur les cuisses : un ourlet sombre.
    s.band("body", 11, 1, shade(blue, 0.75))
    for arm in ("rarm", "larm"):
        if not officer:
            s.material(arm, "top", shade(blue, 1.05), "cloth")
        s.band(arm, 8, 1, dark)
        s.outline_edges(arm, 0.9)
    hands(s, tone, rows=2, glove=gloves)
    if legs:
        for leg in ("rleg", "lleg"):
            s.seam(leg, "front", 2 if leg == "rleg" else 1, 0, 8, None, k=0.88)
            s.material(leg, "top", shade(blue, 0.8), "cloth")
        boots(s)


def army_cap(s, blue=AMESTRIS_BLUE, band="#151519", visor="#0d0d10", badge=GOLD):
    """La casquette d'Amestris, en surcouche : calotte bleue, bandeau noir, visière."""
    s.material("hat", "top", blue, "cloth")
    for f in SIDES:
        s.material("hat", f, blue, "cloth", y=0, h=2)
        s.material("hat", f, band, "leather", y=2, h=1)
    s.rect("hat", "front", 1, 3, 6, 1, visor)
    s.px("hat", "front", 3, 1, badge)
    s.px("hat", "front", 4, 1, shade(badge, 0.8))


def coat_tails(s, color, rows=4, open_front=True):
    """Les pans d'un manteau long, sur la surcouche des jambes ; ouvert devant si besoin."""
    for leg in ("rpants", "lpants"):
        for f in SIDES:
            w, _ = s.size(leg, f)
            s.material(leg, f, color, "cloth", y=0, h=rows)
            s.tint(leg, f, 0, rows - 1, w, 1, 0.82)
        if open_front:
            # L'ouverture au milieu : la colonne intérieure du devant reste vide.
            s.rect(leg, "front", 3 if leg == "rpants" else 0, 0, 1, rows, None)


def pants(s, color, mat="cloth"):
    for leg in ("rleg", "lleg"):
        s.fill(leg, color, mat)
        s.material(leg, "top", shade(color, 0.8), mat)


# Les volumes que FigureModels ajoute au squelette de joueur, dans les coins libres de la peau.
BUST = {"bust": (24, 0, 6, 3, 2)}
VISOR = {"visor": (24, 0, 6, 1, 2)}
# Les peaux élargies à 128x64 : la moitié droite reçoit les grands volumes.
WIDE = (128, 64)


def bust(s, cloth, mat="cloth", skin=None, neckline=0, shadow=6):
    """La poitrine (le volume « bust ») : l'étoffe qui la couvre, le décolleté en haut, le sillon au
    milieu, un reflet dessus et l'ombre qu'elle porte dessous."""
    s.fill("bust", cloth, mat)
    w, h = s.size("bust", "front")
    if skin:
        s.material("bust", "top", skin, "skin")
        if neckline:
            s.material("bust", "front", skin, "skin", h=neckline)
    s.seam("bust", "front", w // 2, 0, neckline + 1 if skin else 2, None, k=0.72)
    s.tint("bust", "front", 0, h - 1, w, 1, 0.85)
    s.tint("bust", "top", 0, 0, w, 1, 1.08)
    s.fill("bust", shade(cloth, 0.62), mat, faces=("bottom",))
    for f in ("right", "left"):
        s.tint("bust", f, 0, 0, 2, h, 0.9)
    # L'ombre qu'elle porte sur le buste, juste dessous.
    s.tint("body", "front", 1, shadow, 6, 1, 0.72)


def visor(s, color="#0d0d10"):
    """La visière de la casquette : du cuir verni, un reflet sur le bord."""
    s.fill("visor", color, "leather")
    s.tint("visor", "top", 0, 0, 6, 1, 1.6)


# --- l'armée ------------------------------------------------------------------------------------
@skin("amestrian_soldier", parts=VISOR)
def amestrian_soldier(s):
    skin_face(s, FAIR, mouth="#a0645a")
    short_hair(s, "#3b2a1e", fringe=1, back=5)
    eyes(s, "#3c4a5e", brow="#2a1d14")
    amestris_uniform(s)
    army_cap(s)
    visor(s)


@skin("state_examiner", parts=VISOR)
def state_examiner(s):
    """L'examinateur d'État : un officier grisonnant, moustache, gants blancs, galons dorés."""
    skin_face(s, FAIR, mouth=None)
    short_hair(s, "#8e8a86", fringe=0, back=5)
    eyes(s, "#2e3a4a", brow="#6f6a66")
    s.rect("head", "front", 2, 6, 4, 1, "#77726d")
    s.px("head", "front", 1, 6, shade("#77726d", 0.8))
    s.px("head", "front", 6, 6, shade("#77726d", 0.8))
    s.tint("head", "front", 2, 7, 4, 1, 0.9)
    amestris_uniform(s, officer=True, gloves="#f2f0ea")
    army_cap(s, badge="#e8c650")
    visor(s)
    # Un dossier sous le bras : non, des galons sur les manches.
    for arm in ("rarm", "larm"):
        s.band(arm, 7, 1, GOLD, "metal")


@skin("briggs_soldier", size=WIDE, parts={"collar": (64, 0, 10, 2, 6), "goggles": (64, 8, 7, 2, 1),
                                          "pack": (64, 12, 6, 7, 3)})
def briggs_soldier(s):
    """Briggs : la parka d'hiver grise, la capuche bordée de fourrure, les lunettes de neige."""
    coat, coat_dark, fur = "#c9d0d6", "#9aa3ab", "#eee9df"
    skin_face(s, FAIR, mouth=None)
    eyes(s, "#2b3644", brow="#3a2d22")
    # L'écharpe qui remonte sur le bas du visage.
    s.material("head", "front", "#6b7480", "cloth", y=6, h=2)
    s.material("head", "right", "#6b7480", "cloth", y=6, h=2)
    s.material("head", "left", "#6b7480", "cloth", y=6, h=2)
    s.material("head", "back", "#6b7480", "cloth", y=6, h=2)
    short_hair(s, "#2c2620", fringe=2, back=6)
    # La capuche : tissu gris sur le crâne, fourrure autour du visage, lunettes sur le front.
    s.material("hat", "top", coat, "cloth")
    for f in ("right", "left", "back"):
        s.material("hat", f, coat, "cloth")
    s.rect("hat", "right", 0, 0, 8, 8, None)
    s.rect("hat", "left", 0, 0, 8, 8, None)
    s.material("hat", "right", coat, "cloth", x=0, w=6)
    s.material("hat", "left", coat, "cloth", x=2, w=6)
    s.material("hat", "right", fur, "fur", x=6, w=2)
    s.material("hat", "left", fur, "fur", x=0, w=2)
    s.material("hat", "front", fur, "fur", h=2)
    s.material("hat", "front", fur, "fur", w=1)
    s.material("hat", "front", fur, "fur", x=7, w=1)
    s.rect("hat", "front", 1, 2, 6, 1, "#2a2a30")
    s.rect("hat", "front", 1, 2, 2, 1, "#5d7d93")
    s.rect("hat", "front", 5, 2, 2, 1, "#5d7d93")
    s.px("hat", "front", 1, 2, "#a9c6d8")
    s.px("hat", "front", 5, 2, "#a9c6d8")
    # La parka : boutonnage croisé, ceinture, poches basses.
    for part in ("body", "rarm", "larm"):
        s.fill(part, coat, "cloth")
    s.seam("body", "front", 4, 0, 12, None, k=0.8)
    for y in (2, 4, 6):
        s.px("body", "front", 2, y, "#3a3f46")
        s.px("body", "front", 5, y, "#3a3f46")
    belt(s, y=7, color="#3f3730", buckle="#a7a7ad")
    for x in (1, 5):
        s.rect("body", "front", x, 9, 2, 1, coat_dark)
    s.material("jacket", "front", fur, "fur", h=1)
    s.material("jacket", "back", fur, "fur", h=1)
    s.material("jacket", "right", fur, "fur", h=1)
    s.material("jacket", "left", fur, "fur", h=1)
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 1, coat_dark)
        s.outline_edges(arm, 0.88)
    hands(s, FAIR, rows=2, glove="#3b3e45")
    coat_tails(s, coat, rows=3)
    pants(s, "#5f6a78")
    boots(s, color="#26252a", rows=5)
    for leg in ("rpants", "lpants"):
        for f in SIDES:
            w, _ = s.size(leg, f)
            s.material(leg, f, fur, "fur", y=7, h=1)
    # Le col de fourrure, les lunettes de neige relevées sur la capuche, le sac et ses bretelles.
    s.fill("collar", fur, "fur")
    s.material("collar", "top", shade(fur, 1.05), "fur")
    s.fill("goggles", "#2a2a30", "leather")
    for x in (1, 4):
        s.rect("goggles", "front", x, 0, 2, 2, "#5d7d93")
        s.px("goggles", "front", x, 0, "#a9c6d8")
    s.fill("pack", "#6b6a52", "cloth")
    s.material("pack", "back", "#5c5b45", "cloth", y=0, h=3)
    s.tint("pack", "back", 0, 3, 6, 1, 0.75)
    s.rect("pack", "back", 2, 4, 2, 1, "#a7a7ad")
    for x in (1, 6):
        s.seam("body", "front", x, 0, 7, "#3f3730")


@skin("drachma_soldier", size=WIDE, parts={"chapka": (64, 0, 10, 2, 10), "flaps": (64, 12, 1, 5, 5),
                                           "skirt": (64, 24, 9, 5, 5)})
def drachma_soldier(s):
    """Drachma : la capote brun-vert, la chapka de fourrure frappée d'une étoile rouge."""
    coat, fur = "#545239", "#5b4331"
    skin_face(s, "#e3bea0", mouth="#93554d")
    short_hair(s, "#6a4d33", fringe=1, back=5)
    eyes(s, "#4a5a3a", brow="#4b3626")
    # La chapka : dessus et rabats en fourrure, étoile rouge.
    s.material("hat", "top", fur, "fur")
    for f in SIDES:
        s.material("hat", f, fur, "fur", h=3)
    s.material("hat", "right", fur, "fur", x=1, w=5, h=6)
    s.material("hat", "left", fur, "fur", x=2, w=5, h=6)
    s.material("hat", "back", fur, "fur", h=5)
    s.px("hat", "front", 3, 1, "#c8202c")
    s.px("hat", "front", 4, 1, "#9e1620")
    # La capote : col de fourrure, boutons de cuivre, ceinturon.
    for part in ("body", "rarm", "larm"):
        s.fill(part, coat, "cloth")
    s.material("body", "front", fur, "fur", h=1)
    s.material("jacket", "front", fur, "fur", h=1)
    s.material("jacket", "right", fur, "fur", h=1)
    s.material("jacket", "left", fur, "fur", h=1)
    s.material("jacket", "back", fur, "fur", h=1)
    s.seam("body", "front", 3, 1, 11, None, k=0.78)
    for y in (2, 4, 6, 10):
        s.px("body", "front", 4, y, "#b87a3a")
    belt(s, y=8, color="#3b2a1c", buckle="#b88a3a")
    s.rect("body", "front", 6, 8, 1, 3, "#3b2a1c")
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 1, fur, "fur")
    hands(s, "#e3bea0", rows=2, glove="#2f2a24")
    coat_tails(s, coat, rows=5)
    pants(s, "#4a4835")
    boots(s, rows=5)
    # La chapka bouffante, son étoile rouge, ses rabats ; les pans de la capote.
    s.fill("chapka", fur, "fur")
    s.material("chapka", "top", shade(fur, 1.1), "fur")
    s.rect("chapka", "front", 4, 0, 2, 2, "#c8202c")
    s.px("chapka", "front", 4, 0, "#e8484e")
    s.fill("flaps", fur, "fur")
    s.fill("skirt", coat, "cloth")
    s.seam("skirt", "front", 4, 0, 5, None, k=0.75)
    s.band("skirt", 4, 1, shade(coat, 0.7))


@skin("immortal_soldier")
def immortal_soldier(s):
    """Les soldats immortels : des corps de mannequin, une bouche fendue jusqu'aux oreilles, des
    sutures partout, un œil unique."""
    flesh, stitch = "#cbbfae", "#5a2a24"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, flesh, "skin")
    # Le visage : un œil rouge, la grande bouche aux dents serrées.
    s.rect("head", "front", 2, 2, 4, 2, shade(flesh, 0.9))
    s.rect("head", "front", 3, 2, 2, 2, "#1d0d0c")
    s.px("head", "front", 4, 3, "#d0242c")
    s.rect("head", "front", 0, 5, 8, 2, "#2a0d0d")
    for x in range(0, 8, 2):
        s.px("head", "front", x, 5, "#eee6d4")
        s.px("head", "front", x + 1, 6, "#eee6d4")
    s.px("head", "right", 7, 5, "#2a0d0d")
    s.px("head", "left", 0, 5, "#2a0d0d")
    # Les sutures : autour du cou, au milieu du torse, aux épaules et aux cuisses.
    for x in range(0, 8, 2):
        s.px("body", "front", x, 0, stitch)
        s.px("body", "back", x + 1, 0, stitch)
    for y in range(1, 12, 2):
        s.px("body", "front", 4, y, stitch)
        s.px("body", "front", 3 if y % 4 == 1 else 5, y, stitch)
    for arm in ("rarm", "larm"):
        s.band(arm, 3, 1, shade(flesh, 0.8))
        for x in range(0, 4, 2):
            s.px(arm, "front", x, 3, stitch)
    # Un pagne de toile sombre.
    s.material("body", "front", "#4a3e33", "cloth", y=9, h=3)
    s.material("body", "back", "#4a3e33", "cloth", y=9, h=3)
    s.material("body", "right", "#4a3e33", "cloth", y=9, h=3)
    s.material("body", "left", "#4a3e33", "cloth", y=9, h=3)
    for leg in ("rleg", "lleg"):
        s.band(leg, 0, 2, "#4a3e33")
        for x in range(4):
            s.px(leg, "front", x, 6, stitch if x % 2 == 0 else None)
        s.tint(leg, "front", 0, 6, 4, 1, 1.0)
        s.rect(leg, "front", 0, 6, 4, 1, lambda i, j: stitch if i % 2 == 0 else shade(flesh, 0.85))
        s.material(leg, "bottom", shade(flesh, 0.7), "skin")


# --- alliés et habitants --------------------------------------------------------------------------
@skin("hohenheim", parts={"ponytail": (56, 16, 2, 7, 2)})
def hohenheim(s):
    """Van Hohenheim : cheveux blonds noués, barbe, lunettes rondes, long manteau brun."""
    blond, coat = "#e1c26a", "#7a5a3a"
    skin_face(s, FAIR, mouth=None)
    eyes(s, "#c99a2a", brow=shade(blond, 0.8))
    # Les lunettes rondes : la monture au-dessus des yeux et le pont sur le nez.
    frame = "#4a3f30"
    s.rect("head", "front", 1, 3, 2, 1, frame)
    s.rect("head", "front", 5, 3, 2, 1, frame)
    s.rect("head", "front", 3, 4, 2, 1, frame)
    s.px("head", "front", 0, 4, frame)
    s.px("head", "front", 7, 4, frame)
    # Une barbe courte qui suit la mâchoire, la moustache autour de la bouche.
    beard = "#c7a656"
    s.rect("head", "front", 0, 7, 8, 1, beard)
    s.px("head", "front", 0, 6, beard)
    s.px("head", "front", 7, 6, beard)
    s.rect("head", "front", 2, 6, 4, 1, beard)
    s.rect("head", "front", 3, 6, 2, 1, "#8a5a46")
    s.tint("head", "front", 0, 7, 8, 1, 0.9)
    for f, x in (("right", 5), ("left", 0)):
        s.material("head", f, beard, "hair", x=x, w=3, y=6, h=2)
    short_hair(s, blond, fringe=0, back=8)
    fringe(s, blond, [3, 2, 1, 1, 0, 1, 2, 3])
    hair_volume(s, blond, sides=3, back=6)
    s.rect("hat", "back", 3, 5, 2, 3, shade(blond, 0.9))
    long_hair_back(s, blond, rows=5, width=2)
    s.fill("ponytail", blond, "hair")
    s.band("ponytail", 0, 1, "#4a3f30", "leather")
    # Le long manteau brun, ouvert sur une chemise claire et un gilet.
    for part in ("body", "rarm", "larm"):
        s.fill(part, coat, "cloth")
    s.material("body", "front", "#e9e3d4", "cloth", x=3, w=2, h=12)
    s.material("body", "front", "#4e5546", "cloth", x=3, w=2, y=3, h=6)
    s.px("body", "front", 3, 0, "#f5f0e4")
    s.px("body", "front", 4, 0, "#f5f0e4")
    s.outline_edges("body", 0.9, faces=("front",))
    s.material("jacket", "front", coat, "cloth", x=0, w=2, h=12)
    s.material("jacket", "front", coat, "cloth", x=6, w=2, h=12)
    s.material("jacket", "right", coat, "cloth", y=8, h=4)
    s.material("jacket", "left", coat, "cloth", y=8, h=4)
    s.material("jacket", "back", coat, "cloth", y=8, h=4)
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 1, shade(coat, 0.8))
    hands(s, FAIR, rows=2)
    coat_tails(s, coat, rows=8)
    pants(s, "#3d3a36")
    boots(s, color="#4a3426", rows=3)


@skin("marcoh", size=WIDE, parts={"satchel": (64, 0, 4, 3, 2)})
def marcoh(s):
    """Le docteur Marcoh : crâne dégarni, tempes grises, la brûlure sur le visage, chemise et gilet."""
    grey = "#8f8a80"
    skin_face(s, "#e2bd99", mouth="#8f5a4f")
    eyes(s, "#3f3a33", brow="#6c665d", lid=shade("#e2bd99", 0.85))
    s.rect("head", "front", 0, 4, 8, 1, lambda i, j: s.pget("head", "front", i, 4))
    # La brûlure sur la joue gauche.
    for (x, y) in ((5, 5), (6, 5), (6, 6), (5, 6), (7, 5), (6, 3)):
        s.px("head", "front", x, y, "#c88a7a")
    s.px("head", "front", 6, 6, "#a8665a")
    # Les tempes grises sur un crâne dégarni.
    s.material("head", "right", grey, "hair", h=5)
    s.material("head", "left", grey, "hair", h=5)
    s.material("head", "back", grey, "hair", h=6)
    s.material("head", "top", "#d9b391", "skin")
    s.material("head", "top", grey, "hair", y=6, h=2)
    for part in ("body", "rarm", "larm"):
        s.fill(part, "#e8e3d6", "cloth")
    s.material("body", "front", "#6b4b33", "cloth", x=0, w=3, y=1, h=10)
    s.material("body", "front", "#6b4b33", "cloth", x=5, w=3, y=1, h=10)
    s.material("body", "back", "#6b4b33", "cloth", y=1, h=10)
    s.material("body", "right", "#6b4b33", "cloth", y=1, h=10)
    s.material("body", "left", "#6b4b33", "cloth", y=1, h=10)
    s.px("body", "front", 2, 4, "#c2a46a")
    s.px("body", "front", 2, 7, "#c2a46a")
    for arm in ("rarm", "larm"):
        s.band(arm, 6, 1, shade("#e8e3d6", 0.85))
    hands(s, "#e2bd99", rows=3)
    pants(s, "#8a7a62")
    boots(s, color="#3e2c20", rows=2)
    # La sacoche de médecin et sa bandoulière, en travers du buste.
    leather = "#5a3a24"
    s.fill("satchel", leather, "leather")
    s.material("satchel", "back", shade(leather, 0.8), "leather", h=1)
    s.px("satchel", "back", 1, 1, "#c2a46a")
    strap = "#2a1a10"
    for i in range(8):
        s.px("body", "front", i, min(11, i + 1), strap)
        s.px("body", "back", 7 - i, min(11, i + 1), strap)


@skin("scar", size=WIDE, parts={"rshoulder": (64, 0, 5, 3, 5), "lshoulder": (64, 8, 5, 3, 5)})
def scar(s):
    """Scar : la peau mate d'Ishval, la cicatrice en X, les yeux rouges, le bras tatoué."""
    tone = ISHVAL
    skin_face(s, tone, mouth="#4a2416")
    eyes(s, "#c4202a", white="#e6d8c6", brow="#d0ccc4")
    # La cicatrice en croix, sur le front, entre les yeux.
    for (x, y) in ((2, 1), (3, 2), (4, 3), (5, 1), (4, 2), (3, 3)):
        s.px("head", "front", x, y, "#c48e7a")
    short_hair(s, "#e3dfd6", fringe=1, back=5)
    fringe(s, "#e3dfd6", [2, 1, 0, 0, 0, 0, 1, 2])
    hair_volume(s, "#e3dfd6", sides=2, back=4)
    # La veste jaune ouverte sur un maillot noir.
    jacket = "#c7a24a"
    s.fill("body", BLACK_CLOTH, "cloth")
    s.fill("larm", jacket, "cloth")
    s.material("body", "front", jacket, "cloth", x=0, w=2)
    s.material("body", "front", jacket, "cloth", x=6, w=2)
    for f in ("right", "left", "back"):
        s.material("body", f, jacket, "cloth")
    s.material("jacket", "front", jacket, "cloth", x=0, w=2)
    s.material("jacket", "front", jacket, "cloth", x=6, w=2)
    s.material("jacket", "back", jacket, "cloth", y=9, h=3)
    s.material("jacket", "right", jacket, "cloth", y=9, h=3)
    s.material("jacket", "left", jacket, "cloth", y=9, h=3)
    s.material("jacket", "front", shade(jacket, 0.75), "cloth", x=0, w=8, y=0, h=1)
    s.rect("jacket", "front", 2, 0, 4, 1, None)
    belt(s, y=9, color="#3b2d22", buckle="#a39074")
    # Le bras droit : nu, couvert du tatouage d'alchimie de son frère.
    s.fill("rarm", tone, "skin")
    ink = "#22283a"
    for f in SIDES:
        w, _ = s.size("rarm", f)
        for y in range(0, 10):
            for x in range(w):
                if (x + y) % 3 == 0 or (f == "front" and y in (2, 6)):
                    s.px("rarm", f, x, y, ink)
    s.band("rarm", 0, 1, shade(jacket, 0.9))
    hands(s, tone, rows=2)
    s.material("larm", "bottom", tone, "skin")
    pants(s, "#2e2f36")
    boots(s, color="#3b2a20", rows=3)
    # Les épaules de lutteur : le deltoïde nu et tatoué à droite, sous la veste à gauche.
    s.fill("rshoulder", tone, "skin")
    for f in SIDES:
        w, _ = s.size("rshoulder", f)
        for x in range(0, w, 3):
            s.px("rshoulder", f, x, 2, ink)
    s.material("rshoulder", "top", shade(jacket, 0.9), "cloth")
    s.fill("lshoulder", jacket, "cloth")
    s.material("lshoulder", "top", shade(jacket, 1.08), "cloth")
    # Un torse plat et sec : l'ombre sous les pectoraux, la ligne du sternum.
    s.tint("body", "front", 2, 3, 4, 1, 0.7)
    s.seam("body", "front", 4, 0, 3, None, k=0.75)


@skin("cornello", size=WIDE, parts={"medallion": (64, 0, 4, 4, 1), "robe": (64, 8, 10, 6, 6)})
def cornello(s):
    """Le père Cornello, prophète de Léto : crâne rasé, robe crème, soleil d'or, bague rouge."""
    robe, trim = "#ddd1aa", "#b88a2c"
    skin_face(s, "#e7c2a0", mouth="#8e5248")
    eyes(s, "#5a4128", brow="#5a3e28", lid=shade("#e7c2a0", 0.88))
    s.material("head", "top", "#d8b08e", "skin")
    s.material("head", "right", "#5b4130", "hair", x=0, w=4, y=2, h=3)
    s.material("head", "left", "#5b4130", "hair", x=4, w=4, y=2, h=3)
    s.material("head", "back", "#5b4130", "hair", y=2, h=3)
    s.rect("head", "front", 3, 7, 2, 1, "#5b4130")
    for part in ("body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, robe, "cloth")
    # Les galons dorés au col et sur le devant, le soleil de Léto sur la poitrine.
    s.band("body", 0, 1, trim, "metal")
    s.material("body", "front", trim, "metal", x=3, w=2, y=6, h=6)
    s.rect("body", "front", 2, 2, 4, 3, lambda i, j: GOLD if (i in (1, 2) or j == 1) else None)
    s.px("body", "front", 1, 3, GOLD)
    s.px("body", "front", 6, 3, GOLD)
    s.px("body", "front", 3, 3, "#f0d870")
    s.material("jacket", "front", robe, "cloth", y=8, h=4)
    s.material("jacket", "back", robe, "cloth", y=6, h=6)
    for arm in ("rarm", "larm"):
        s.band(arm, 8, 1, trim, "metal")
    hands(s, "#e7c2a0", rows=2)
    s.px("larm", "front", 1, 10, "#c8102e")
    for leg in ("rleg", "lleg"):
        s.band(leg, 11, 1, shade(robe, 0.8))
        s.material(leg, "bottom", "#6b4a2a", "leather")
    coat_tails(s, robe, rows=10, open_front=False)
    for leg in ("rpants", "lpants"):
        s.rect(leg, "front", 3 if leg == "rpants" else 0, 0, 1, 10,
               lambda i, j: trim if j < 10 else None)
    # Le soleil de Léto en médaillon : un disque d'or rayonnant, le cœur clair.
    s.fill("medallion", GOLD, "metal")
    for f in ("front", "back"):
        for x, y in ((0, 0), (3, 0), (0, 3), (3, 3)):
            s.px("medallion", f, x, y, None)
    s.rect("medallion", "front", 1, 1, 2, 2, "#f0d870")
    s.px("medallion", "front", 1, 1, "#fff4b0")
    # La robe qui s'évase sous la ceinture, galonnée devant et à l'ourlet.
    s.fill("robe", robe, "cloth")
    s.material("robe", "front", trim, "metal", x=4, w=2)
    s.band("robe", 5, 1, trim, "metal")
    s.material("robe", "top", shade(robe, 0.9), "cloth")


@skin("izumi", slim=True, size=WIDE, parts={**BUST, "dreads": (64, 0, 3, 13, 2), "lock": (78, 0, 1, 6, 1)})
def izumi(s):
    """Izumi Curtis : les dreadlocks noires nouées en une grosse queue dans le dos et deux mèches devant
    les épaules, les anneaux d'or aux oreilles, la chemise blanche, le pantalon sombre, les sandales."""
    tone = "#e4bc98"
    skin_face(s, tone, mouth="#9a4e48")
    eyes(s, "#2c2420", brow="#18120e", lid="#18120e")
    hair = "#1c1716"
    short_hair(s, hair, fringe=0, back=8)
    fringe(s, hair, [3, 2, 1, 0, 0, 1, 2, 3])
    hair_volume(s, hair, sides=6, back=8)
    for f in ("right", "left", "back"):
        w, _ = s.size("hat", f)
        for x in range(0, w, 2):
            s.tint("hat", f, x, 0, 1, 8, 0.75)
    long_hair_back(s, hair, rows=4)
    s.px("head", "right", 4, 6, GOLD)
    s.px("head", "left", 3, 6, GOLD)
    # La chemise blanche, manches retroussées.
    for part in ("body", "rarm", "larm"):
        s.fill(part, "#f0ece2", "cloth")
    s.rect("body", "front", 3, 0, 2, 2, tone)
    bust(s, "#f0ece2")
    s.seam("body", "front", 4, 2, 8, None, k=0.88)
    s.band("body", 8, 1, "#3b3330", "leather")
    for arm in ("rarm", "larm"):
        s.material(arm, "front", tone, "skin", y=6)
        for f in SIDES:
            s.material(arm, f, tone, "skin", y=6)
        s.band(arm, 5, 1, "#d9d3c4")
    hands(s, tone, rows=1)
    pants(s, "#3a3b46")
    boots(s, color="#6b4b32", rows=1)
    for leg in ("rleg", "lleg"):
        s.band(leg, 10, 1, tone, "skin")
    # Les dreadlocks en volume : la queue dans le dos, ses mèches séparées, le lien qui la noue.
    s.fill("dreads", hair, "hair")
    for f in SIDES:
        w, h = s.size("dreads", f)
        for x in range(1, w, 2):
            s.seam("dreads", f, x, 0, h, None, k=0.7)
        s.rect("dreads", f, 0, 1, w, 1, "#6b4b32")
    s.fill("lock", hair, "hair")


@skin("may_chang", slim=True, parts={"buns": (24, 0, 3, 3, 3), "braids": (56, 16, 1, 7, 1)})
def may_chang(s):
    """May Chang : la petite princesse de Xing, nattes et chignons noirs, tunique rose à revers blancs."""
    tone = "#f0d0b4"
    skin_face(s, tone, mouth="#c06a6a", mouth_w=2, cheeks="#f0a8a0")
    eyes(s, "#2a2230", brow="#1a1418")
    hair = "#1d1820"
    short_hair(s, hair, fringe=0, back=8)
    fringe(s, hair, [2, 2, 1, 2, 2, 1, 2, 2])
    hair_volume(s, hair, sides=4, back=7)
    # Les deux chignons, en haut et sur les côtés de la surcouche.
    for f, x in (("right", 1), ("left", 4)):
        s.material("hat", f, shade(hair, 1.2), "hair", x=x, w=3, h=3)
        s.px("hat", f, x + 1, 1, "#e85a80")
    long_hair_back(s, hair, rows=8, width=2)
    s.fill("buns", shade(hair, 1.15), "hair")
    s.px("buns", "front", 1, 1, "#e85a80")
    s.fill("braids", hair, "hair")
    s.band("braids", 5, 1, "#e85a80")
    tunic, trim = "#d9608c", "#f4ecef"
    for part in ("body", "rarm", "larm"):
        s.fill(part, tunic, "cloth")
    # Le col croisé de Xing.
    for y in range(0, 6):
        s.px("body", "front", 2 + y // 2 if y < 4 else 4, y, trim)
    s.rect("body", "front", 3, 0, 2, 1, tone)
    s.band("body", 7, 1, "#8a2a52")
    s.material("jacket", "front", tunic, "cloth", y=8, h=4)
    s.material("jacket", "right", tunic, "cloth", y=8, h=4)
    s.material("jacket", "left", tunic, "cloth", y=8, h=4)
    s.material("jacket", "back", tunic, "cloth", y=8, h=4)
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 2, trim)
    hands(s, tone, rows=1)
    coat_tails(s, tunic, rows=3, open_front=False)
    pants(s, "#3a2c3c")
    boots(s, color="#1e1a22", rows=3)


@skin("olivier", slim=True, size=WIDE, parts={**BUST, "hair": (64, 0, 8, 11, 1), "collar": (82, 0, 14, 2, 6),
                                              "sabre": (122, 0, 1, 10, 1)})
def olivier(s):
    """Olivier Mira Armstrong : la longue chevelure blonde qui cache un œil, l'uniforme de général, le
    col de fourrure du manteau de Briggs et le sabre à la hanche."""
    tone, blond = "#f0d4b8", "#ecd27a"
    skin_face(s, tone, mouth="#b05a5a")
    eyes(s, "#3d6aa8", brow=shade(blond, 0.75))
    short_hair(s, blond, fringe=1, back=8)
    # La mèche qui tombe sur l'œil droit.
    fringe(s, blond, [6, 5, 4, 1, 1, 1, 2, 3])
    hair_volume(s, blond, sides=8, back=8, front=0)
    s.material("hat", "right", blond, "hair")
    s.material("hat", "left", blond, "hair")
    long_hair_back(s, blond, rows=10)
    for f in ("right", "left"):
        s.material("jacket", f, blond, "hair", h=3)
    amestris_uniform(s, officer=True, gloves="#20202a", tone=tone)
    bust(s, AMESTRIS_BLUE)
    s.px("bust", "front", 2, 1, GOLD)
    # La chevelure qui tombe jusqu'aux reins, en mèches, les pointes irrégulières.
    s.fill("hair", blond, "hair")
    s.material("hair", "back", shade(blond, 1.08), "hair")
    for x in range(8):
        if x % 3 == 1:
            s.px("hair", "back", x, 10, None)
            s.px("hair", "front", x, 10, None)
        if x % 2 == 0:
            s.seam("hair", "back", x, 2, 6, None, k=0.88)
    # Le long manteau d'officier de Briggs.
    s.band("body", 11, 1, AMESTRIS_BLUE)
    coat_tails(s, shade(AMESTRIS_BLUE, 0.85), rows=7)
    boots(s, color="#16161b", rows=6)
    # Le col de fourrure blanche du manteau de Briggs.
    fur = "#e4e0d8"
    s.fill("collar", fur, "fur")
    s.material("collar", "top", shade(fur, 1.05), "fur")
    s.fill("collar", shade(fur, 0.7), "fur", faces=("bottom",))
    # Le sabre au fourreau noir, sa garde dorée en haut.
    s.fill("sabre", "#1a1a20", "leather")
    for f in SIDES:
        s.px("sabre", f, 0, 0, GOLD)
        s.px("sabre", f, 0, 1, shade(GOLD, 0.8))
    s.material("sabre", "top", GOLD, "metal")


@skin("winry", slim=True, parts={**BUST, "ponytail": (56, 16, 2, 11, 2)})
def winry(s):
    """Winry Rockbell : queue de cheval blonde, débardeur noir, combinaison bleue nouée à la taille."""
    tone, blond = "#f2d6ba", "#efd27e"
    skin_face(s, tone, mouth="#c46c64", cheeks="#f2b8a8")
    eyes(s, "#4c86c8", brow=shade(blond, 0.75))
    short_hair(s, blond, fringe=0, back=8)
    fringe(s, blond, [3, 2, 1, 2, 1, 2, 2, 3])
    hair_volume(s, blond, sides=5, back=8)
    # Le bandana et la queue de cheval.
    s.band("hat", 1, 1, "#e8e8f0")
    s.material("hat", "top", "#e8e8f0", "cloth")
    long_hair_back(s, blond, rows=7, width=2)
    s.fill("ponytail", blond, "hair")
    s.band("ponytail", 0, 1, "#e8e8f0")
    s.px("head", "left", 3, 5, "#c0c0c8")
    s.px("head", "left", 4, 5, "#c0c0c8")
    # Le débardeur noir, les épaules nues, la combinaison nouée par les manches.
    overall = "#4d79b8"
    s.fill("body", BLACK_CLOTH, "cloth")
    s.material("body", "front", tone, "skin", h=2)
    s.material("body", "back", tone, "skin", h=2)
    s.material("body", "right", tone, "skin", h=2)
    s.material("body", "left", tone, "skin", h=2)
    bust(s, BLACK_CLOTH, skin=tone)
    s.band("body", 8, 4, overall)
    s.band("body", 8, 1, shade(overall, 0.75))
    s.rect("body", "front", 2, 8, 4, 1, shade(overall, 0.6))
    bare_arms(s, tone)
    hands(s, tone, rows=1)
    pants(s, overall)
    for leg in ("rleg", "lleg"):
        s.seam(leg, "front", 1, 2, 6, None, k=0.85)
    boots(s, color="#3a3036", rows=3)
    # Les manches nouées qui pendent devant.
    s.material("jacket", "front", overall, "cloth", x=2, w=4, y=8, h=2)
    s.material("jacket", "front", overall, "cloth", x=3, w=2, y=10, h=2)


# --- armures et êtres à part ----------------------------------------------------------------------
def armor(s, steel, trim, eye="#ff4a3a", dented=False):
    """Une armure d'acier vide, à la manière d'Alphonse : plaques, rivets, fente du heaume."""
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, steel, "metal")
    # Le heaume : arête centrale, fente de vision et deux lueurs.
    s.rect("head", "front", 0, 3, 8, 2, shade(steel, 0.35))
    s.px("head", "front", 2, 3, eye)
    s.px("head", "front", 5, 3, eye)
    s.px("head", "front", 2, 4, shade(eye, 0.6))
    s.px("head", "front", 5, 4, shade(eye, 0.6))
    s.seam("head", "front", 3, 5, 3, None, k=0.75)
    s.seam("head", "front", 4, 5, 3, None, k=1.2)
    for x in range(0, 8, 2):
        s.px("head", "front", x, 6, shade(steel, 0.6))
    s.material("head", "top", shade(steel, 1.08), "metal")
    # Le plastron : un renflement, des rivets, la ceinture de plaques.
    s.tint("body", "front", 2, 1, 4, 4, 1.15)
    s.seam("body", "front", 4, 0, 7, None, k=0.75)
    for x, y in ((1, 1), (6, 1), (1, 6), (6, 6)):
        s.px("body", "front", x, y, shade(trim, 1.1))
    s.band("body", 7, 1, trim, "metal")
    s.band("body", 8, 4, shade(steel, 0.85), "metal")
    for x in range(0, 8, 2):
        s.tint("body", "front", x, 9, 1, 3, 0.75)
    for arm in ("rarm", "larm"):
        s.band(arm, 0, 4, shade(steel, 1.1), "metal")
        s.band(arm, 4, 1, trim, "metal")
        s.band(arm, 9, 1, trim, "metal")
        s.material(arm, "top", shade(steel, 1.15), "metal")
    for leg in ("rleg", "lleg"):
        s.band(leg, 5, 1, trim, "metal")
        s.tint(leg, "front", 0, 4, 4, 1, 1.2)
        s.band(leg, 9, 3, shade(steel, 0.8), "metal")
    if dented:
        for part, f, x, y in (("body", "front", 2, 3), ("head", "front", 6, 1), ("rarm", "front", 1, 6)):
            s.px(part, f, x, y, shade(steel, 0.55))
            s.px(part, f, x + 1, y + 1, shade(steel, 1.3))


@skin("haunted_armor")
def haunted_armor(s):
    """Une armure habitée : l'acier bleuté d'Alphonse, la houppe du cimier, la lueur des yeux."""
    armor(s, "#8f9aa6", "#5d6670", eye="#f0f4ff")
    # Le pic du heaume et sa houppe blanche, dans la surcouche.
    s.rect("hat", "top", 3, 3, 2, 2, "#e8e8e0")
    s.material("hat", "back", "#e8e8e0", "hair", x=3, w=2, h=7)
    s.tint("hat", "back", 3, 6, 2, 1, 0.8)
    # Le pagne de mailles.
    for leg in ("rpants", "lpants"):
        for f in SIDES:
            w, _ = s.size(leg, f)
            s.material(leg, f, "#6d5f4c", "cloth", y=0, h=3)


@skin("barry", parts={"crest": (0, 32, 2, 3, 9)})
def barry(s):
    """Barry le Boucher : une armure brune et cabossée, maculée de sang, un cimier sur le heaume."""
    armor(s, "#7c6c5a", "#4a3e32", eye="#ff3b2b", dented=True)
    blood = "#7a1414"
    for part, f, x, y in (("body", "front", 5, 2), ("body", "front", 6, 3), ("body", "front", 5, 4),
                          ("rarm", "front", 2, 9), ("rarm", "front", 1, 10), ("head", "front", 1, 6),
                          ("rleg", "front", 1, 7), ("larm", "front", 0, 10)):
        s.px(part, f, x, y, blood)
    s.fill("crest", "#5a2a22", "fur")
    s.material("crest", "top", "#7a3a2c", "fur")


@skin("truth", volume=False)
def truth(s):
    """La Vérité : une silhouette blanche, sans autre trait que son sourire."""
    white = "#f2f1ee"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, white, "flat")
    # Un voile d'ombre très léger sous les bras et entre les jambes, sans plus.
    for part, f in (("rarm", "left"), ("larm", "right"), ("rleg", "left"), ("lleg", "right")):
        s.tint(part, f, 0, 0, 4, 12, 0.93)
    # Le sourire, d'une oreille à l'autre : la bouche noire et les dents blanches.
    ink = "#141416"
    s.px("head", "front", 0, 4, ink)
    s.px("head", "front", 7, 4, ink)
    s.rect("head", "front", 0, 5, 8, 1, ink)
    s.rect("head", "front", 1, 6, 6, 1, ink)
    for x in range(1, 7):
        s.px("head", "front", x, 5, "#fbfbfa" if x % 2 else ink)
    for x in (2, 4):
        s.px("head", "front", x, 6, "#fbfbfa")


# --- les homoncules (squelette BossModels) --------------------------------------------------------
@skin("lust", slim=True, parts={"hair_back": (0, 32, 9, 14, 2), "bust": (24, 32, 7, 3, 3), "hips": (24, 38, 9, 3, 5),
                                       "lance": (52, 38, 1, 4, 1)})
def lust(s):
    """Lust : la longue chevelure noire, les yeux violets, la robe noire, l'ouroboros sur la poitrine."""
    tone, hair = "#f2ddd0", "#17141c"
    skin_face(s, tone, mouth="#7a2a5a")
    eyes(s, "#9a3aa8", brow=hair, lid=hair)
    s.px("head", "front", 0, 3, hair)
    s.px("head", "front", 7, 3, hair)
    short_hair(s, hair, fringe=0, back=8)
    fringe(s, hair, [5, 3, 2, 1, 1, 2, 2, 3])
    hair_volume(s, hair, sides=6, back=8)
    s.fill("hair_back", hair, "hair")
    s.material("hair_back", "front", shade(hair, 1.3), "hair")
    # La robe noire, le décolleté et l'ouroboros juste dessous.
    s.fill("body", BLACK_CLOTH, "cloth")
    s.rect("body", "front", 2, 0, 4, 3, tone)
    s.rect("body", "front", 3, 3, 2, 1, tone)
    ouroboros(s, "body", "front", 3, 0)
    s.px("body", "front", 2, 0, shade(tone, 0.9))
    bust(s, BLACK_CLOTH, skin=tone, neckline=1, shadow=7)
    # Les ongles qui deviennent lames : un noir laqué, reflet violet.
    s.rect("lance", "front", 0, 0, 1, 4, "#1a0a1e")
    s.fill("lance", "#1a0a1e", "metal")
    s.px("lance", "front", 0, 0, "#6a3a7a")
    # Les hanches dans la robe, et la fente de la jupe sur la cuisse gauche.
    s.fill("hips", BLACK_CLOTH, "cloth")
    s.tint("hips", "top", 0, 0, 9, 5, 1.1)
    s.tint("hips", "front", 0, 2, 9, 1, 0.82)
    s.px("hips", "front", 6, 1, tone)
    s.px("hips", "front", 6, 2, tone)
    # La taille fine : la robe s'ombre sur les flancs, sous la poitrine.
    s.tint("body", "front", 0, 6, 1, 4, 0.7)
    s.tint("body", "front", 7, 6, 1, 4, 0.7)
    for arm in ("rarm", "larm"):
        s.fill(arm, tone, "skin")
        s.band(arm, 3, 9, BLACK_CLOTH)
        s.material(arm, "bottom", BLACK_CLOTH, "cloth")
        # Les doigts-lames : des pointes sombres au bout des gants.
        s.band(arm, 11, 1, "#2a2236", "metal")
    for leg in ("rleg", "lleg"):
        s.fill(leg, BLACK_CLOTH, "cloth")
        s.band(leg, 11, 1, "#0f0e12", "leather")
    s.seam("lleg", "front", 0, 4, 8, tone)


@skin("gluttony", parts={"belly": (0, 32, 10, 8, 4)})
def gluttony(s):
    """Gluttony : le crâne chauve et rond, les petits yeux vides, la bouche baveuse, le pull noir."""
    tone = "#efe2d2"
    skin_face(s, tone, nose=False, mouth=None)
    s.rect("head", "front", 2, 3, 1, 1, "#141016")
    s.rect("head", "front", 5, 3, 1, 1, "#141016")
    s.rect("head", "front", 2, 5, 4, 2, "#5a1a22")
    s.rect("head", "front", 3, 5, 2, 1, "#d6566a")
    s.px("head", "front", 2, 7, "#c8d4e0")
    s.tint("head", "front", 1, 4, 1, 1, 0.92)
    s.tint("head", "front", 6, 4, 1, 1, 0.92)
    s.material("head", "top", shade(tone, 0.97), "skin")
    for part in ("body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, BLACK_CLOTH, "cloth")
    s.band("body", 0, 1, "#2c2a32")
    hands(s, tone, rows=2)
    s.fill("belly", BLACK_CLOTH, "cloth")
    s.material("belly", "front", "#2a2830", "cloth")
    # Le pull tendu sur le ventre : un reflet en haut, l'ombre qui tombe dessous.
    s.tint("belly", "front", 1, 1, 8, 1, 1.25)
    s.tint("belly", "front", 2, 2, 6, 1, 1.12)
    s.tint("belly", "front", 0, 6, 10, 2, 0.8)
    s.tint("belly", "front", 0, 0, 1, 8, 0.85)
    s.tint("belly", "front", 9, 0, 1, 8, 0.85)
    boots(s, color="#0f0e12", rows=2)


@skin("sloth", parts={"shoulders": (0, 32, 12, 5, 6)})
def sloth(s):
    """Sloth : un colosse pâle et las, les cheveux noués en arrière, les fers aux poignets."""
    tone, hair = "#ddcfc0", "#3a3028"
    skin_face(s, tone, mouth="#7a5a50", mouth_w=4)
    eyes(s, "#4a3a32", lid=shade(tone, 0.7), brow=hair)
    short_hair(s, hair, fringe=1, back=8)
    hair_volume(s, hair, sides=2, back=8)
    s.rect("hat", "back", 3, 4, 2, 4, shade(hair, 0.85))
    for part in ("body", "rarm", "larm"):
        s.fill(part, tone, "skin")
    # Les pectoraux, les abdominaux, l'ouroboros sur le cœur.
    s.tint("body", "front", 0, 3, 8, 1, 0.85)
    s.seam("body", "front", 4, 0, 8, None, k=0.88)
    for y in (5, 7):
        s.tint("body", "front", 2, y, 4, 1, 0.9)
    ouroboros(s, "body", "front", 5, 1)
    s.band("body", 9, 3, "#4a4036")
    belt(s, y=9, color="#2c241e", buckle="#8a8a90")
    # Les fers aux poignets, avec un bout de chaîne.
    for arm in ("rarm", "larm"):
        s.band(arm, 7, 2, "#5e6066", "metal")
        s.px(arm, "front", 1, 9, "#8a8c92")
    s.fill("shoulders", tone, "skin")
    s.tint("shoulders", "front", 0, 4, 12, 1, 0.85)
    s.material("shoulders", "back", "#4a4036", "cloth", y=2, h=3)
    pants(s, "#4a4036")
    for leg in ("rleg", "lleg"):
        s.band(leg, 9, 3, tone, "skin")
        s.material(leg, "bottom", shade(tone, 0.8), "skin")


def envy_body(s):
    tone, hair = "#efdcc8", "#1e3226"
    skin_face(s, tone, mouth="#8a4a4a")
    eyes(s, "#6a3a8a", brow=hair, lid=hair)
    s.px("head", "front", 4, 6, "#f2f2f2")
    short_hair(s, hair, fringe=0, back=8)
    fringe(s, hair, [6, 4, 2, 1, 1, 2, 4, 6])
    hair_volume(s, hair, sides=8, back=8)
    # Le bandeau noir orné d'un triangle rouge.
    s.band("head", 0, 1, BLACK_CLOTH)
    s.rect("head", "front", 0, 0, 8, 1, BLACK_CLOTH)
    s.px("head", "front", 3, 0, "#c0182c")
    s.px("head", "front", 4, 0, "#c0182c")
    s.fill("spikes", hair, "hair")
    s.material("spikes", "top", shade(hair, 1.3), "hair")
    # Le haut court et la jupe-short noirs ; le ventre et les bras nus.
    s.fill("body", tone, "skin")
    s.band("body", 0, 6, BLACK_CLOTH)
    s.band("body", 9, 3, BLACK_CLOTH)
    for arm in ("rarm", "larm"):
        s.fill(arm, tone, "skin")
        s.band(arm, 8, 3, BLACK_CLOTH)
    hands(s, tone, rows=1)
    pants(s, tone)
    for leg in ("rleg", "lleg"):
        s.material(leg, "front", tone, "skin")
        s.band(leg, 0, 4, BLACK_CLOTH)
        s.band(leg, 7, 4, BLACK_CLOTH)
        s.material(leg, "bottom", shade(tone, 0.8), "skin")
    ouroboros(s, "lleg", "front", 0, 4)


@skin("envy", parts={"spikes": (0, 32, 9, 3, 9)})
def envy(s):
    """Envy : la tignasse en palmier, le bandeau, le haut noir et l'ouroboros sur la cuisse."""
    envy_body(s)


@skin("envy_giant", parts={"spikes": (0, 32, 9, 3, 9)})
def envy_giant(s):
    """La vraie forme d'Envy : une bête verte faite des visages de ceux qu'il a absorbés."""
    flesh = "#4f6b3a"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg", "spikes"):
        s.fill(part, flesh, "stone")
    s.material("spikes", "top", shade(flesh, 0.8), "stone")
    # Des visages et des mains humaines prisonniers de la chair.
    faces = [("body", "front", 1, 2), ("body", "front", 5, 6), ("body", "back", 2, 4), ("rarm", "front", 0, 3),
             ("larm", "front", 1, 7), ("rleg", "front", 0, 2), ("lleg", "front", 1, 5), ("head", "right", 2, 3),
             ("head", "left", 3, 2), ("body", "right", 0, 7), ("body", "left", 1, 1), ("head", "back", 2, 2)]
    for part, f, x, y in faces:
        s.rect(part, f, x, y, 3, 3, "#d7b49a")
        s.px(part, f, x, y + 1, "#1a1014")
        s.px(part, f, x + 2, y + 1, "#1a1014")
        s.px(part, f, x + 1, y + 2, "#6a2a2a")
        s.px(part, f, x + 1, y, shade("#d7b49a", 1.1))
    # La tête de la bête : des yeux rouges, une gueule de dents.
    s.rect("head", "front", 0, 2, 8, 2, shade(flesh, 0.6))
    s.px("head", "front", 1, 2, "#e0202a")
    s.px("head", "front", 6, 2, "#e0202a")
    s.rect("head", "front", 0, 5, 8, 3, "#240c10")
    for x in range(8):
        s.px("head", "front", x, 5 if x % 2 else 7, "#e8e0c8")


def greed_head(s, tone):
    skin_face(s, tone, mouth=None)
    # Le sourire carnassier, dents pointues.
    s.rect("head", "front", 2, 6, 4, 1, "#3a1414")
    s.px("head", "front", 2, 6, "#f4f0e8")
    s.px("head", "front", 5, 6, "#f4f0e8")
    s.px("head", "front", 1, 5, "#3a1414")
    s.px("head", "front", 6, 5, "#3a1414")
    # Les petites lunettes rondes teintées.
    s.rect("head", "front", 1, 4, 2, 1, "#1d2a3a")
    s.rect("head", "front", 5, 4, 2, 1, "#1d2a3a")
    s.px("head", "front", 1, 4, "#5a7a9a")
    s.px("head", "front", 5, 4, "#5a7a9a")
    s.rect("head", "front", 3, 4, 2, 1, "#28282e")
    s.px("head", "front", 0, 4, "#28282e")
    s.px("head", "front", 7, 4, "#28282e")
    hair = "#16141a"
    short_hair(s, hair, fringe=0, back=7)
    fringe(s, hair, [3, 2, 2, 1, 1, 2, 2, 3])
    hair_volume(s, hair, sides=3, back=6)
    for x in range(0, 8, 2):
        s.px("hat", "front", x, 0, hair)


@skin("greed", parts={"fur": (0, 32, 10, 3, 6)})
def greed(s):
    """Greed : cheveux noirs en épis, lunettes teintées, gilet sans manches à col de fourrure."""
    tone = "#ecd2b8"
    greed_head(s, tone)
    s.fill("body", "#1a181e", "leather")
    s.rect("body", "front", 3, 0, 2, 6, tone)
    s.seam("body", "front", 3, 6, 6, None, k=0.75)
    belt(s, y=8, color="#3a2a22", buckle="#c4c4c8")
    bare_arms(s, tone)
    s.band("larm", 10, 2, tone, "skin")
    ouroboros(s, "larm", "back", 0, 9)
    for arm in ("rarm", "larm"):
        s.band(arm, 6, 1, "#1a181e", "leather")
    pants(s, "#1d1b22", mat="leather")
    boots(s, color="#100f14", rows=3)
    s.fill("fur", "#c6bcb0", "fur")
    s.material("fur", "top", "#ddd4c8", "fur")


@skin("greed_shield", parts={"fur": (0, 32, 10, 3, 6)})
def greed_shield(s):
    """Le bouclier ultime : une carapace de carbone noir, facettée, et la mâchoire de requin."""
    shell = "#2a2a33"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, shell, "metal")
    for part in ("body", "rarm", "larm", "rleg", "lleg"):
        w, h = s.size(part, "front")
        for y in range(0, h, 3):
            s.tint(part, "front", 0, y, w, 1, 1.35)
    s.rect("head", "front", 1, 2, 2, 1, "#c41a2a")
    s.rect("head", "front", 5, 2, 2, 1, "#c41a2a")
    s.rect("head", "front", 0, 5, 8, 2, "#0c0c10")
    for x in range(8):
        s.px("head", "front", x, 5 if x % 2 else 6, "#e8e4dc")
    ouroboros(s, "larm", "back", 0, 9)
    s.fill("fur", "#3a3a44", "fur")


@skin("wrath", parts={"coat": (0, 32, 9, 5, 5)})
def wrath(s):
    """King Bradley : le bandeau sur l'œil gauche, la moustache, l'uniforme de Généralissime."""
    tone = "#e4c0a0"
    skin_face(s, tone, mouth=None)
    eyes(s, "#2a2a30", brow="#141218")
    # Le bandeau noir sur l'œil gauche (à droite de l'image) et sa lanière en biais.
    s.rect("head", "front", 5, 3, 2, 2, "#0e0e12")
    s.px("head", "front", 5, 3, "#26262e")
    s.px("head", "front", 7, 3, "#0e0e12")
    s.px("head", "front", 4, 2, "#0e0e12")
    s.px("head", "front", 3, 1, "#0e0e12")
    s.px("head", "front", 0, 2, "#0e0e12")
    s.material("head", "left", "#0e0e12", "leather", y=2, h=1)
    s.material("head", "right", "#0e0e12", "leather", y=2, h=1)
    s.material("head", "back", "#0e0e12", "leather", y=2, h=1)
    s.rect("head", "front", 2, 6, 4, 1, "#1a1618")
    s.px("head", "front", 1, 6, "#1a1618")
    short_hair(s, "#16141a", fringe=1, back=5)
    s.px("head", "front", 2, 1, "#16141a")
    amestris_uniform(s, blue="#24396e", officer=True, gloves="#f2f0ea", tone=tone)
    # Les décorations sur la poitrine.
    s.rect("body", "front", 5, 2, 2, 1, "#b8202a")
    s.rect("body", "front", 5, 3, 2, 1, GOLD)
    s.fill("coat", "#24396e", "cloth")
    s.material("coat", "top", shade("#24396e", 0.7), "cloth")
    s.tint("coat", "front", 0, 4, 9, 1, 0.8)
    s.rect("coat", "front", 4, 0, 1, 5, shade("#24396e", 0.6))


@skin("pride")
def pride(s):
    """Selim / Pride : un enfant sage aux cheveux noirs, et ses ombres pleines d'yeux et de crocs."""
    tone = "#f2d8c0"
    skin_face(s, tone, mouth="#b0646a", cheeks="#f0b8a8")
    eyes(s, "#3a3036", brow="#141018")
    short_hair(s, "#141018", fringe=0, back=6)
    fringe(s, "#141018", [3, 2, 2, 2, 2, 2, 2, 3])
    hair_volume(s, "#141018", sides=3, back=5)
    # Le costume d'écolier : chemise blanche à col marin, short bleu, chaussettes hautes.
    for part in ("body", "rarm", "larm"):
        s.fill(part, "#f2f0ea", "cloth")
    s.rect("body", "front", 2, 0, 4, 2, "#2c3e74")
    s.rect("body", "front", 3, 1, 2, 2, "#2c3e74")
    s.px("body", "front", 3, 3, "#b8202a")
    s.material("body", "back", "#2c3e74", "cloth", h=3)
    s.band("body", 9, 3, "#2c3e74")
    for arm in ("rarm", "larm"):
        s.band(arm, 5, 1, "#2c3e74")
    hands(s, tone, rows=6)
    for arm in ("rarm", "larm"):
        s.band(arm, 4, 1, "#f2f0ea")
        s.band(arm, 5, 1, "#2c3e74")
    pants(s, tone)
    for leg in ("rleg", "lleg"):
        s.band(leg, 0, 3, "#2c3e74")
        s.band(leg, 6, 4, "#f2f0ea")
        s.band(leg, 10, 2, "#2a1c16", "leather")
        s.material(leg, "bottom", "#140e0c", "leather")
    # Les ombres : cinq lames noires, parsemées d'yeux et de dents.
    for u, w, h in ((0, 2, 14), (6, 2, 16), (12, 2, 13), (18, 2, 15), (24, 2, 12)):
        s.parts["shadow%d" % u] = (u, 32, w, h, 1)
        s.fill("shadow%d" % u, "#0a080c", "flat")
        for y in range(1, h, 4):
            s.px("shadow%d" % u, "front", 0, y, "#f0e6e0")
            s.px("shadow%d" % u, "front", 1, y, "#c0101e")
            s.px("shadow%d" % u, "back", 1, y + 2, "#c0101e")
        s.px("shadow%d" % u, "front", 0, h - 2, "#e8e0d8")
        s.px("shadow%d" % u, "front", 1, h - 3, "#e8e0d8")
    # Le masque d'ombre qui lui mange le visage une fois révélé : des yeux ronds, un sourire de crocs.
    s.parts["mask"] = (32, 32, 8, 8, 0)
    s.rect("mask", "front", 0, 0, 8, 8, "#060408")
    for x0 in (1, 5):
        s.rect("mask", "front", x0, 2, 2, 2, "#f4ece4")
        s.px("mask", "front", x0 + (1 if x0 == 1 else 0), 3, "#d0101e")
    s.rect("mask", "front", 1, 5, 6, 2, "#2a0408")
    for x in range(1, 7):
        s.px("mask", "front", x, 5 if x % 2 else 6, "#f0e8dc")
    s.px("mask", "front", 0, 4, "#2a0408")
    s.px("mask", "front", 7, 4, "#2a0408")


def father_mane(s, hair, beard):
    s.fill("beard", beard, "hair")
    s.fill("mane", hair, "hair")
    s.material("mane", "front", shade(hair, 0.85), "hair")


@skin("father_faceless", parts={"beard": (0, 32, 7, 7, 1), "mane": (16, 32, 9, 11, 2)})
def father_faceless(s):
    """Père sans visage : un corps d'ombre où ne brillent qu'un œil rouge et une bouche, sous la
    crinière et la barbe d'or de Hohenheim."""
    void = "#141218"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, void, "stone")
    s.rect("head", "front", 3, 3, 2, 1, "#f2f0ea")
    s.px("head", "front", 4, 3, "#d0101e")
    s.rect("head", "front", 2, 5, 4, 1, "#d0101e")
    # Des veines rouges, la Pierre qui bat dans ce corps.
    for part, f, pts in (("body", "front", ((3, 2), (4, 3), (4, 4), (5, 5), (3, 6), (2, 7))),
                         ("rarm", "front", ((1, 2), (2, 3), (2, 5))), ("larm", "front", ((1, 4), (0, 5), (1, 7)))):
        for x, y in pts:
            s.px(part, f, x, y, "#7a0c18")
    s.material("head", "top", "#d9b860", "hair")
    s.material("head", "back", "#d9b860", "hair", h=6)
    s.material("head", "right", "#d9b860", "hair", x=0, w=5, h=4)
    s.material("head", "left", "#d9b860", "hair", x=3, w=5, h=4)
    father_mane(s, "#d9b860", "#c9a650")


@skin("father_divine", parts={"beard": (0, 32, 7, 7, 1), "mane": (16, 32, 9, 11, 2)})
def father_divine(s):
    """Père divin, après avoir avalé Dieu : une peau de marbre, une toge blanche, des yeux d'or."""
    tone = "#f4ead8"
    skin_face(s, tone, mouth="#9a6a5a")
    eyes(s, "#d8a020", white="#ffffff", brow="#d9c070")
    s.material("head", "top", "#e9d48a", "hair")
    s.material("head", "back", "#e9d48a", "hair", h=6)
    s.material("head", "right", "#e9d48a", "hair", x=0, w=5, h=4)
    s.material("head", "left", "#e9d48a", "hair", x=3, w=5, h=4)
    fringe(s, "#e9d48a", [2, 1, 0, 0, 0, 0, 1, 2])
    for part in ("body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, "#f6f2e8", "cloth")
    # La toge drapée en diagonale, l'épaule droite nue.
    s.rect("body", "front", 0, 0, 3, 4, lambda i, j: tone if i + j < 4 else None)
    for y in range(12):
        s.tint("body", "front", min(7, y // 2 + 1), y, 1, 1, 0.85)
    s.material("rarm", "top", tone, "skin")
    s.band("rarm", 0, 12, tone, "skin")
    hands(s, tone, rows=2)
    # Les âmes de la Pierre, encore visibles : des points rouges sous la peau.
    for part, f, x, y in (("body", "front", 5, 2), ("rarm", "front", 2, 5), ("body", "back", 3, 6),
                          ("larm", "front", 1, 8), ("lleg", "front", 2, 3)):
        s.px(part, f, x, y, "#c8202e")
    for leg in ("rleg", "lleg"):
        s.band(leg, 11, 1, "#d8c49a", "leather")
        s.material(leg, "bottom", "#c8b48a", "leather")
    father_mane(s, "#e9d48a", "#e0c87a")


# --- l'armure d'âme, portée -------------------------------------------------------------------------
@skin("equipment/humanoid/soul", size=(64, 32), volume=False)
def soul_armor(s):
    """Le plastron d'âme porté, à la manière de l'armure d'Alphonse : acier bleuté, épaulières rondes
    rivetées, la plaque du torse à arête, des lames sur le ventre, des gantelets plus sombres."""
    steel, dark, light_ = "#9aa6b6", "#5e6a7a", "#d8e0ea"
    for part in ("body", "rarm"):
        s.fill(part, steel, "metal")
    # Le col : un anneau sombre. L'arête du torse, claire, et les deux pans qui s'en écartent.
    s.band("body", 0, 1, dark, "metal")
    s.seam("body", "front", 4, 1, 6, light_)
    s.seam("body", "front", 3, 1, 6, None, k=1.12)
    s.seam("body", "front", 5, 1, 6, None, k=0.82)
    # Les lames du ventre, superposées.
    for y in (7, 9):
        s.band("body", y, 1, dark, "metal")
        s.band_tint("body", y + 1, 1, 1.12)
    s.band("body", 11, 1, shade(dark, 0.85), "metal")
    for x, y in ((1, 2), (6, 2), (1, 5), (6, 5)):
        s.px("body", "front", x, y, "#eef2f6")
    for x, y in ((1, 2), (6, 2), (3, 6), (4, 6)):
        s.px("body", "back", x, y, "#eef2f6")
    # L'épaulière ronde : le haut du bras plus clair, bordé, un rivet ; puis le brassard et le gantelet.
    s.material("rarm", "top", light_, "metal")
    for f in SIDES:
        s.material("rarm", f, shade(steel, 1.1), "metal", h=4)
    s.band("rarm", 4, 1, dark, "metal")
    s.px("rarm", "front", 1, 1, "#eef2f6")
    s.px("rarm", "right", 2, 1, "#eef2f6")
    s.band("rarm", 8, 1, dark, "metal")
    for f in SIDES:
        s.material("rarm", f, shade(steel, 0.82), "metal", y=9, h=3)


# --- les tenues, portées ----------------------------------------------------------------------------
# Une armure se peint sur le gabarit 64x32 : la tête et sa surcouche, le torse, le bras et la jambe
# droits (le gauche en est le miroir). La couche des jambières ne montre que la taille et les jambes ;
# celle de l'armure ne montre des jambes que les bottes. Les volumes en plus (capuche, pans,
# épaulières, plumet, visière) ont leur propre texture, au gabarit d'OutfitLayer.
ARMOR = (64, 32)
# Pour les peintres pensés pour une peau entière : les membres gauches retombent sur ceux de droite.
MIRRORED = {"larm": (40, 16, 4, 12, 4), "lleg": (0, 16, 4, 12, 4)}
EXTRAS = {"hood": (0, 0, 8, 4, 2), "visor": (24, 0, 8, 1, 3), "spike": (48, 0, 2, 3, 2),
          "pauldron": (0, 8, 5, 3, 7), "plume": (48, 8, 2, 7, 3), "tails": (0, 20, 4, 9, 4),
          "long_tails": (16, 20, 4, 11, 4)}
EDWARD_RED = "#b3242b"
GREATCOAT = "#262a33"
ALPHONSE_STEEL, ALPHONSE_TRIM = "#8f9aa6", "#5d6670"


def clear_rows(s, part, y, h):
    """Rend transparentes ces rangées tout autour de la partie : la peau ou le vêtement dessous se voit."""
    for f in SIDES:
        w, _ = s.size(part, f)
        s.rect(part, f, 0, y, w, h, None)


def flamel(s, part, f, x, y, color="#1a1416"):
    """La croix de Flamel, en 5x8 : la couronne, les ailes, la croix et le serpent enroulé."""
    for dx, dy in ((1, 0), (3, 0), (2, 1), (0, 2), (4, 2), (2, 2), (1, 3), (2, 3), (3, 3), (2, 4),
                   (1, 5), (2, 5), (3, 6), (2, 6), (1, 7), (2, 7)):
        s.px(part, f, x + dx, y + dy, color)


def coat_sleeves(s, color, cuff, mat="cloth"):
    """Des manches de manteau jusqu'au poignet ; la main reste nue (ou gantée, par les gants du mod)."""
    s.fill("rarm", color, mat)
    s.band("rarm", 9, 1, cuff, mat)
    s.outline_edges("rarm", 0.9)
    clear_rows(s, "rarm", 10, 2)
    s.clear("rarm", faces=("bottom",))


def armor_boots(s, color, rows, band, mat="leather", sole="#0e0d10"):
    """Les bottes, seules visibles aux jambes de l'armure."""
    clear_rows(s, "rleg", 0, 12 - rows)
    for f in SIDES:
        s.material("rleg", f, color, mat, y=12 - rows, h=rows)
    s.band("rleg", 12 - rows, 1, band, mat)
    s.material("rleg", "bottom", sole, mat)
    s.clear("rleg", faces=("top",))
    s.tint("rleg", "front", 0, 11, 4, 1, 1.15)


@skin("equipment/humanoid/alchemist", size=ARMOR, volume=False)
def alchemist_outfit(s):
    """Le manteau rouge d'Edward, ouvert sur la veste noire, la croix de Flamel dans le dos ; et ses
    bottes noires."""
    red, deep = EDWARD_RED, shade(EDWARD_RED, 0.7)
    s.fill("body", red, "cloth")
    # Devant, le manteau s'ouvre sur la veste noire, bordée de blanc, et son col montant.
    s.material("body", "front", BLACK_CLOTH, "cloth", x=2, w=4)
    s.seam("body", "front", 2, 0, 12, "#e8e4dc")
    s.seam("body", "front", 5, 0, 12, "#e8e4dc")
    s.rect("body", "front", 2, 0, 4, 1, "#2a282e")
    s.tint("body", "front", 1, 0, 1, 12, 0.85)
    s.tint("body", "front", 6, 0, 1, 12, 0.85)
    s.band("body", 0, 1, deep, "cloth", faces=("right", "left", "back"))
    flamel(s, "body", "back", 2, 3)
    coat_sleeves(s, red, deep)
    armor_boots(s, BOOT, 4, "#2c2a30")


@skin("equipment/humanoid_leggings/alchemist", size=ARMOR, volume=False)
def alchemist_trousers(s):
    """Le pantalon de cuir noir d'Edward, sa ceinture et sa boucle d'argent."""
    pants(s, "#232127", "leather")
    s.seam("rleg", "front", 2, 0, 12, None, k=0.85)
    for f in SIDES:
        s.material("body", f, "#232127", "leather", y=8, h=4)
    belt(s, y=8, color="#141216", buckle="#d0d4da")


@skin("outfit/alchemist", size=(64, 64), parts=EXTRAS, volume=False)
def alchemist_extras(s):
    """La capuche rabattue et les pans du manteau d'Edward, jusqu'aux genoux, ouverts devant."""
    red, deep = EDWARD_RED, shade(EDWARD_RED, 0.7)
    s.fill("hood", red, "cloth")
    s.material("hood", "front", deep, "cloth")
    s.material("hood", "top", shade(red, 0.85), "cloth")
    s.tint("hood", "back", 0, 3, 8, 1, 0.8)
    s.fill("tails", red, "cloth")
    s.band("tails", 8, 1, deep, "cloth")
    # L'ouverture devant : le pan s'écarte de la jambe intérieure.
    s.rect("tails", "front", 2, 0, 2, 9, None)
    s.tint("tails", "front", 1, 0, 1, 9, 0.85)
    s.fill("tails", deep, "cloth", faces=("top",))


@skin("equipment/humanoid/steel_armor", size=ARMOR, volume=False, parts=MIRRORED)
def steel_armor(s):
    """L'armure d'Alphonse, pour un vivant : le heaume à fente, le plastron à arête, les lames du
    ventre, les gantelets ; et des solerets aux jambes."""
    armor(s, ALPHONSE_STEEL, ALPHONSE_TRIM, eye="#1a1c22")
    light_ = "#d8e0ea"
    s.seam("body", "front", 4, 1, 6, light_)
    for y in (8, 10):
        s.band_tint("body", y, 1, 1.12)
    # Le heaume : la corne du front, au-dessus de la fente sombre (il y a un visage derrière).
    s.px("head", "front", 3, 1, light_)
    s.px("head", "front", 4, 1, light_)
    s.px("head", "front", 3, 2, shade(ALPHONSE_STEEL, 0.75))
    s.px("head", "front", 4, 2, shade(ALPHONSE_STEEL, 0.75))
    # Les jambes ne portent ici que les solerets.
    clear_rows(s, "rleg", 0, 7)
    s.clear("rleg", faces=("top",))
    s.band("rleg", 7, 1, ALPHONSE_TRIM, "metal")


@skin("equipment/humanoid_leggings/steel_armor", size=ARMOR, volume=False)
def steel_leggings(s):
    """Les cuissards : des plaques sur les cuisses, les genouillères, et le pagne de tissu à la taille."""
    for f in SIDES:
        s.material("rleg", f, ALPHONSE_STEEL, "metal", y=0, h=9)
    s.material("rleg", "top", ALPHONSE_STEEL, "metal")
    s.band("rleg", 4, 1, ALPHONSE_TRIM, "metal")
    s.tint("rleg", "front", 0, 5, 4, 2, 1.2)
    s.band("rleg", 8, 1, shade(ALPHONSE_STEEL, 0.8), "metal")
    for f in SIDES:
        s.material("body", f, shade(ALPHONSE_STEEL, 0.85), "metal", y=8, h=4)
    s.band("body", 8, 1, ALPHONSE_TRIM, "metal")
    # Le pagne, devant et derrière.
    s.material("body", "front", "#6d5f4c", "cloth", x=2, y=9, w=4, h=3)
    s.material("body", "back", "#6d5f4c", "cloth", x=2, y=9, w=4, h=3)


@skin("outfit/steel", size=(64, 64), parts=EXTRAS, volume=False)
def steel_extras(s):
    """Les épaulières rondes et rivetées, la pointe du heaume et son plumet blanc."""
    steel, light_ = ALPHONSE_STEEL, "#d8e0ea"
    s.fill("pauldron", shade(steel, 1.08), "metal")
    s.material("pauldron", "top", light_, "metal")
    s.band("pauldron", 2, 1, ALPHONSE_TRIM, "metal")
    for f, x in (("front", 2), ("back", 2), ("right", 3), ("left", 3)):
        s.px("pauldron", f, x, 1, "#eef2f6")
    s.fill("pauldron", shade(steel, 0.6), "metal", faces=("bottom",))
    s.fill("spike", light_, "metal")
    s.fill("plume", "#ecebe2", "hair")
    s.tint("plume", "back", 0, 5, 2, 2, 0.85)


@skin("equipment/humanoid/officer", size=ARMOR, volume=False, parts=MIRRORED)
def officer_uniform(s):
    """L'uniforme bleu d'officier d'Amestris : casquette, vareuse aux boutons d'or et aiguillette,
    bottes de cuir noir."""
    amestris_uniform(s, officer=True, legs=False)
    clear_rows(s, "rarm", 10, 2)
    s.clear("rarm", faces=("bottom",))
    # La casquette, sur la tête de l'armure : calotte, bandeau noir, insigne d'or ; le visage reste nu.
    s.material("head", "top", AMESTRIS_BLUE, "cloth")
    for f in SIDES:
        s.material("head", f, AMESTRIS_BLUE, "cloth", y=0, h=2)
        s.material("head", f, "#151519", "leather", y=2, h=1)
    s.px("head", "front", 3, 1, GOLD)
    s.px("head", "front", 4, 1, shade(GOLD, 0.8))
    armor_boots(s, BOOT, 5, "#2c2a30")


@skin("equipment/humanoid_leggings/officer", size=ARMOR, volume=False)
def officer_trousers(s):
    """Le pantalon bleu, son passepoil sombre sur le côté."""
    pants(s, AMESTRIS_BLUE)
    s.seam("rleg", "right", 2, 0, 12, shade(AMESTRIS_BLUE, 0.55))
    for f in SIDES:
        s.material("body", f, AMESTRIS_BLUE, "cloth", y=9, h=3)


@skin("outfit/officer", size=(64, 64), parts=EXTRAS, volume=False)
def officer_extras(s):
    """La visière de la casquette : du cuir verni, un reflet sur le bord."""
    s.fill("visor", "#0d0d10", "leather")
    s.tint("visor", "top", 0, 2, 8, 1, 1.8)


@skin("equipment/humanoid/officer_greatcoat", size=ARMOR, volume=False)
def officer_greatcoat(s):
    """Le long manteau d'officier, à la Mustang : drap sombre croisé, deux rangs de boutons d'argent,
    le grand col rabattu sur le col bleu de la vareuse."""
    coat, collar = GREATCOAT, shade(GREATCOAT, 1.35)
    s.fill("body", coat, "cloth")
    s.material("body", "front", collar, "cloth", x=1, w=6, h=2)
    s.rect("body", "front", 3, 0, 2, 2, AMESTRIS_BLUE)
    s.band("body", 0, 1, collar, "cloth", faces=("right", "left", "back"))
    for y in (3, 5, 7, 9):
        s.px("body", "front", 2, y, "#c9ccd2")
        s.px("body", "front", 5, y, "#c9ccd2")
    s.seam("body", "front", 4, 2, 10, None, k=0.75)
    coat_sleeves(s, coat, shade(coat, 0.75))


@skin("outfit/greatcoat", size=(64, 64), parts=EXTRAS, volume=False)
def greatcoat_extras(s):
    """Les pans du manteau d'officier, jusqu'aux chevilles, la fente dans le dos."""
    coat = GREATCOAT
    s.fill("long_tails", coat, "cloth")
    s.band("long_tails", 10, 1, shade(coat, 0.75), "cloth")
    s.rect("long_tails", "front", 3, 0, 1, 11, shade(coat, 0.7))
    s.rect("long_tails", "back", 3, 4, 1, 7, shade(coat, 0.6))
    s.fill("long_tails", shade(coat, 0.7), "cloth", faces=("top",))


# --- les personnages du BRIEF (FMAB-59) ---------------------------------------------------------
def glasses(s, frame, y=4):
    """Des lunettes rondes : la monture autour des yeux et le pont sur le nez."""
    s.rect("head", "front", 0, y - 1, 3, 1, frame)
    s.rect("head", "front", 5, y - 1, 3, 1, frame)
    s.px("head", "front", 0, y, frame)
    s.px("head", "front", 7, y, frame)
    s.rect("head", "front", 3, y, 2, 1, frame)


def mustache(s, color, wide=False):
    """Une moustache sous le nez ; large, elle déborde jusqu'aux joues."""
    x0, w = (0, 8) if wide else (2, 4)
    s.rect("head", "front", x0, 6, w, 1, color)
    s.tint("head", "front", x0, 6, w, 1, 0.9)
    if wide:
        s.px("head", "front", 0, 5, color)
        s.px("head", "front", 7, 5, color)


def apron(s, color, mat="leather", y=2):
    """Un tablier sur le devant du buste et des cuisses."""
    s.material("body", "front", color, mat, x=1, w=6, y=y, h=12 - y)
    s.px("body", "front", 0, y, shade(color, 0.7))
    s.px("body", "front", 7, y, shade(color, 0.7))
    for leg in ("rleg", "lleg"):
        s.material(leg, "front", color, mat, h=6)
        s.tint(leg, "front", 0, 5, 4, 1, 0.8)


@skin("mustang")
def mustang(s):
    """Roy Mustang, l'Alchimiste de flamme : cheveux noirs, uniforme d'officier, gants blancs au
    cercle rouge."""
    skin_face(s, FAIR, mouth="#9a5a52")
    eyes(s, "#1c1a1e", brow="#141216")
    hair = "#16141a"
    short_hair(s, hair, fringe=0, back=5)
    fringe(s, hair, [2, 3, 1, 2, 1, 3, 2, 2])
    hair_volume(s, hair, sides=3, back=5)
    amestris_uniform(s, officer=True, gloves="#f4f2ec")
    # Le cercle de transmutation, rouge, sur le dos de chaque gant.
    for arm in ("rarm", "larm"):
        w, _ = s.size(arm, "front")
        s.px(arm, "front", w // 2 - 1, 10, "#c0182c")
        s.px(arm, "front", w // 2, 11, "#c0182c")
        s.px(arm, "back", w // 2 - 1, 10, "#c0182c")
        s.px(arm, "back", w // 2, 11, "#c0182c")


@skin("hawkeye", slim=True)
def hawkeye(s):
    """Riza Hawkeye : les cheveux blonds relevés en chignon, l'uniforme, le regard brun et droit."""
    blond = "#e8cf7a"
    skin_face(s, PALE, mouth="#b0645c")
    eyes(s, "#5a3a22", brow=shade(blond, 0.75), lid="#3a2a20")
    short_hair(s, blond, fringe=0, back=6)
    fringe(s, blond, [3, 2, 1, 1, 2, 1, 2, 3])
    hair_volume(s, blond, sides=3, back=6)
    # Le chignon, épinglé à l'arrière de la tête.
    s.rect("hat", "back", 2, 2, 4, 3, shade(blond, 0.9))
    s.px("hat", "back", 3, 3, "#3a3036")
    amestris_uniform(s, tone=PALE)


# Les volumes d'Armstrong (FigureModels.armstrong) : la boucle, la moustache en guidon, les épaules.
ARMSTRONG_PARTS = {"curl": (64, 0, 2, 3, 2), "stache": (72, 0, 10, 1, 1), "tip": (72, 4, 1, 2, 1),
                   "rshoulder": (64, 8, 5, 3, 5),
                   "lshoulder": (64, 16, 5, 3, 5)}


@skin("armstrong", size=WIDE, parts=ARMSTRONG_PARTS)
def armstrong(s):
    """Alex Louis Armstrong, l'Alchimiste au bras puissant : un colosse au crâne nu, une seule boucle
    blonde dressée sur le front, la grosse moustache blonde en guidon, de petits yeux bleus, les
    épaules énormes sous l'uniforme, gantelets à pointes."""
    blond = "#ecd27a"
    skin_face(s, PALE, mouth=None)
    eyes(s, "#5a7ab0", brow=blond)
    s.material("head", "top", shade(PALE, 0.96), "skin")
    # La racine de la boucle sur le front, la moustache peinte sous le nez.
    s.px("head", "front", 3, 0, blond)
    s.px("head", "front", 4, 0, blond)
    mustache(s, blond, wide=True)
    # La boucle et la moustache en volume.
    s.fill("curl", blond, "hair")
    s.tint("curl", "front", 0, 2, 2, 1, 0.8)
    s.fill("stache", blond, "hair")
    s.tint("stache", "front", 4, 0, 2, 1, 0.85)
    s.fill("tip", shade(blond, 0.95), "hair")
    amestris_uniform(s, officer=True, tone=PALE, gloves="#3a3d44")
    # Les épaules de colosse, sous le drap bleu de l'uniforme, l'épaulette dorée.
    for shoulder in ("rshoulder", "lshoulder"):
        s.fill(shoulder, AMESTRIS_BLUE, "cloth")
        s.material(shoulder, "top", shade(AMESTRIS_BLUE, 1.1), "cloth")
        s.rect(shoulder, "top", 0, 0, 5, 1, GOLD)
    # Les pointes des gantelets.
    for arm in ("rarm", "larm"):
        w, _ = s.size(arm, "front")
        for x in range(0, w, 2):
            s.px(arm, "front", x, 10, "#9aa2ad")
        s.band(arm, 9, 1, "#22252b", "metal")


@skin("hughes")
def hughes(s):
    """Maes Hughes : cheveux noirs courts, lunettes, une barbe de quelques jours, l'uniforme."""
    skin_face(s, FAIR, mouth="#9a5a52")
    eyes(s, "#4a8a50", brow="#1e1a1c")
    glasses(s, "#2a2a30")
    short_hair(s, "#1e1a1c", fringe=1, back=5)
    for x in range(1, 7):
        s.tint("head", "front", x, 7, 1, 1, 0.82)
    s.tint("head", "front", 0, 6, 1, 2, 0.88)
    s.tint("head", "front", 7, 6, 1, 2, 0.88)
    amestris_uniform(s, officer=True)


@skin("ling")
def ling(s):
    """Ling Yao, prince de Xing : cheveux noirs noués, yeux étirés, tenue blanche à bandes bleu nuit."""
    tone = "#f0d4b4"
    skin_face(s, tone, mouth="#a8645a")
    s.rect("head", "front", 1, 4, 2, 1, "#2a2224")
    s.rect("head", "front", 5, 4, 2, 1, "#2a2224")
    s.rect("head", "front", 1, 3, 2, 1, "#141016")
    s.rect("head", "front", 5, 3, 2, 1, "#141016")
    hair = "#141016"
    short_hair(s, hair, fringe=0, back=7)
    fringe(s, hair, [3, 2, 2, 1, 1, 2, 2, 3])
    hair_volume(s, hair, sides=4, back=7)
    long_hair_back(s, hair, rows=5, width=2)
    white, navy = "#f0eee6", "#1e2a4a"
    for part in ("body", "rarm", "larm"):
        s.fill(part, white, "cloth")
    s.material("body", "front", navy, "cloth", x=0, w=2)
    s.material("body", "front", navy, "cloth", x=6, w=2)
    s.band("body", 8, 1, navy)
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 1, navy)
    hands(s, tone, rows=2)
    pants(s, navy)
    boots(s, color="#1a1a20", rows=3)


@skin("lan_fan", slim=True)
def lan_fan(s):
    """Lan Fan, garde du corps de Ling : tenue noire, masque blanc aux traits rouges, bras gauche
    d'automail."""
    tone = "#f0d4b4"
    skin_face(s, tone, mouth=None)
    hair = "#141016"
    short_hair(s, hair, fringe=0, back=7)
    hair_volume(s, hair, sides=3, back=7)
    long_hair_back(s, hair, rows=4, width=2)
    # Le masque blanc, ses fentes et ses marques rouges.
    s.rect("hat", "front", 0, 1, 8, 6, "#f4f2ee")
    s.rect("hat", "front", 1, 3, 2, 1, "#141016")
    s.rect("hat", "front", 5, 3, 2, 1, "#141016")
    s.rect("hat", "front", 3, 5, 2, 1, "#c0182c")
    s.px("hat", "front", 2, 2, "#c0182c")
    s.px("hat", "front", 5, 2, "#c0182c")
    black = "#1c1a20"
    for part in ("body", "rarm", "larm"):
        s.fill(part, black, "cloth")
    s.band("body", 8, 1, "#3a3036", "leather")
    hands(s, tone, rows=1)
    # L'automail du bras gauche, à nu.
    s.fill("larm", "#9aa2ad", "metal")
    for y in (3, 7):
        s.band("larm", y, 1, "#5d6670", "metal")
    pants(s, black)
    boots(s, color="#101014", rows=4)


# Les volumes d'Alphonse (FigureModels.alphonse) : épaulières, corne, houppe, pans du pagne.
ALPHONSE_PARTS = {"rpauldron": (64, 0, 7, 5, 7), "lpauldron": (64, 12, 7, 5, 7), "spike": (92, 0, 1, 2, 1),
                  "horn": (96, 0, 2, 3, 2), "plume": (96, 8, 2, 9, 1), "flap": (106, 0, 4, 6, 1),
                  "back_flap": (106, 8, 4, 6, 1)}


@skin("alphonse", size=WIDE, parts=ALPHONSE_PARTS)
def alphonse(s):
    """Alphonse Elric, comme dans Brotherhood : la grande armure d'acier gris bleuté, vide ; deux points
    de lumière pâle au fond de la fente du heaume, la corne du front et la longue houppe blanche, les
    épaulières rondes (la gauche à pointe), le ceinturon de cuir et le pagne rouge sombre."""
    steel, dark, light = "#6f7889", "#3e4450", "#b8c2d2"
    void, glow, cloth, leather = "#121318", "#fff3c4", "#8a2a2c", "#5a3a22"
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        s.fill(part, steel, "metal")
    # Le heaume : le cimier, l'arcade, la fente où brillent les yeux, la grille de la bouche.
    s.material("head", "top", shade(steel, 1.1), "metal")
    s.rect("head", "front", 0, 0, 8, 1, shade(steel, 1.1))
    s.rect("head", "front", 3, 0, 2, 2, light)
    s.rect("head", "front", 1, 2, 6, 1, dark)
    s.rect("head", "front", 1, 3, 6, 2, void)
    s.px("head", "front", 2, 3, glow)
    s.px("head", "front", 5, 3, glow)
    s.px("head", "front", 2, 4, shade(glow, 0.55))
    s.px("head", "front", 5, 4, shade(glow, 0.55))
    s.rect("head", "front", 2, 5, 4, 2, dark)
    for x in (2, 4):
        s.rect("head", "front", x, 5, 1, 2, void)
    s.rect("head", "front", 0, 7, 8, 1, shade(steel, 0.8))
    for f in ("left", "right"):
        s.rect("head", f, 3, 3, 2, 2, dark)
        s.px("head", f, 3, 3, light)
    s.seam("head", "back", 4, 1, 6, None, k=0.75)
    # La corne et la houppe (volumes à part).
    s.fill("horn", light, "metal")
    s.material("horn", "top", shade(light, 1.1), "metal")
    s.fill("plume", "#ecebe4", "hair")
    s.tint("plume", "front", 0, 0, 2, 1, 0.8)
    # Le buste : le col, le plastron bombé et son arête, les lames du ventre, le ceinturon, les tassettes.
    s.band("body", 0, 1, dark, "metal")
    s.tint("body", "front", 1, 1, 6, 4, 1.12)
    s.seam("body", "front", 4, 1, 5, light)
    s.seam("body", "front", 3, 1, 5, None, k=0.8)
    s.band("body", 6, 1, shade(steel, 0.9), "metal")
    s.band("body", 7, 1, shade(steel, 0.82), "metal")
    s.band("body", 8, 1, leather, "leather")
    s.rect("body", "front", 3, 8, 2, 1, "#b0a070")
    s.band("body", 9, 3, shade(steel, 0.78), "metal")
    for f in ("front", "back"):
        s.material("body", f, cloth, "cloth", x=2, w=4, y=9, h=3)
    # Les pans du pagne, devant et derrière, qui pendent sous le ceinturon.
    for flap in ("flap", "back_flap"):
        s.fill(flap, cloth, "cloth")
        s.tint(flap, "front", 0, 5, 4, 1, 0.8)
        s.tint(flap, "back", 0, 5, 4, 1, 0.8)
    # Les épaulières rondes : un rebord clair en haut, sombre en bas.
    for pauldron in ("rpauldron", "lpauldron"):
        s.fill(pauldron, shade(steel, 1.15), "metal")
        s.material(pauldron, "top", shade(steel, 1.25), "metal")
        for f in SIDES:
            w, h = s.size(pauldron, f)
            s.rect(pauldron, f, 0, h - 1, w, 1, dark)
    # La pointe de l'épaulière gauche.
    s.fill("spike", light, "metal")
    # Les bras : le coude, le brassard, le gantelet et ses jointures.
    for arm in ("rarm", "larm"):
        s.band(arm, 5, 1, dark, "metal")
        s.band(arm, 6, 3, shade(steel, 1.05), "metal")
        s.band(arm, 9, 3, shade(steel, 0.72), "metal")
        s.rect(arm, "front", 0, 9, 4, 1, light)
    # Les jambes : la genouillère pointue, la grève, les solerets.
    for leg in ("rleg", "lleg"):
        s.band(leg, 5, 1, dark, "metal")
        s.rect(leg, "front", 1, 4, 2, 2, light)
        s.band(leg, 8, 1, dark, "metal")
        s.band(leg, 9, 3, shade(steel, 0.75), "metal")
        s.rect(leg, "front", 0, 11, 4, 1, shade(steel, 0.6))


@skin("pinako", slim=True, parts={"pipe": (24, 0, 1, 1, 3), "bowl": (56, 16, 1, 2, 1), "bun": (56, 20, 2, 2, 2)})
def pinako(s):
    """Pinako Rockbell : la vieille mécanicienne, toute petite, chignon gris sur le crâne, lunettes
    rondes, tenue sombre et tablier, et sa longue pipe."""
    tone = "#e6c4a4"
    skin_face(s, tone, mouth="#8a5a52")
    eyes(s, "#5a4636", brow="#b8b4ae")
    glasses(s, "#a8a29a")
    grey = "#a8a49e"
    short_hair(s, grey, fringe=1, back=6)
    hair_volume(s, grey, sides=3, back=6)
    # Le chignon dressé sur le dessus de la tête.
    s.rect("hat", "top", 2, 2, 4, 4, shade(grey, 1.05))
    s.rect("hat", "front", 2, 0, 4, 1, shade(grey, 0.9))
    for x in (1, 6):
        s.tint("head", "front", x, 6, 1, 1, 0.85)
    dark = "#3a3036"
    for part in ("body", "rarm", "larm"):
        s.fill(part, dark, "cloth")
    apron(s, "#c8b8a0", "cloth")
    hands(s, tone, rows=2)
    pants(s, "#4a3e3a")
    boots(s, color="#2a2226", rows=2)
    # Le chignon, en volume sur le sommet du crâne.
    s.fill("bun", shade(grey, 1.05), "hair")
    # La pipe : un tuyau sombre, le fourneau de bois et sa braise.
    s.fill("pipe", "#2a2226", "leather")
    s.fill("bowl", "#6a4a2e", "leather")
    s.material("bowl", "top", "#e06a2a", "flat")


@skin("basque_grand")
def basque_grand(s):
    """Basque Grand, l'Alchimiste au sang de fer : mâchoire carrée, grosse moustache brune, l'uniforme."""
    skin_face(s, FAIR, mouth=None)
    eyes(s, "#3a2a20", brow="#4a3020")
    short_hair(s, "#4a3020", fringe=1, back=4)
    mustache(s, "#5a3a24", wide=True)
    s.tint("head", "front", 0, 7, 8, 1, 0.85)
    amestris_uniform(s, officer=True)


@skin("mcdougal")
def mcdougal(s):
    """Isaac McDougal, l'Alchimiste de glace : peau pâle, cheveux gris-bleu, long manteau sombre aux
    reflets de givre."""
    skin_face(s, PALE, mouth="#8a5a5a")
    eyes(s, "#8ab8d8", brow="#6a7a8a")
    short_hair(s, "#7a8a9a", fringe=2, back=5)
    coat, frost = "#26303a", "#a8d8f0"
    for part in ("body", "rarm", "larm"):
        s.fill(part, coat, "cloth")
    s.seam("body", "front", 4, 1, 11, frost, k=0.8)
    s.band("body", 0, 1, frost)
    for arm in ("rarm", "larm"):
        s.band(arm, 9, 1, frost)
    hands(s, PALE, rows=2, glove="#d8e8f0")
    coat_tails(s, coat, rows=6)
    pants(s, "#1c2228")
    boots(s, rows=4)


@skin("tattooist")
def tattooist(s):
    """Le tatoueur de Rush Valley : peau tannée, crâne rasé, bouc, avant-bras couverts d'encre et
    tablier de cuir."""
    tone = "#b07a52"
    skin_face(s, tone, mouth="#6a3a2a")
    eyes(s, "#2a2018", brow="#2a2018")
    s.material("head", "top", shade(tone, 0.95), "skin")
    s.rect("head", "front", 3, 7, 2, 1, "#2a2018")
    s.px("head", "front", 3, 6, "#2a2018")
    s.px("head", "front", 4, 6, "#2a2018")
    s.fill("body", "#e0d6c4", "cloth")
    bare_arms(s, tone)
    # Les motifs d'encre sur les avant-bras.
    ink = "#1c2430"
    for arm in ("rarm", "larm"):
        w, _ = s.size(arm, "front")
        for f in ("front", "back"):
            for y in range(5, 11, 2):
                for x in range((y // 2) % 2, w, 2):
                    s.px(arm, f, x, y, ink)
    s.material("body", "front", "#e0d6c4", "cloth", h=2)
    apron(s, "#6a4a2e")
    pants(s, "#3a3430")
    boots(s, color="#4a3426", rows=3)


@skin("kimblee")
def kimblee(s):
    """Solf J. Kimblee, l'Alchimiste écarlate : costume et chapeau blancs, longs cheveux noirs noués
    bas, yeux d'or, un cercle tatoué dans chaque paume."""
    skin_face(s, FAIR, mouth="#8a4a4a")
    eyes(s, "#d8a820", brow="#141216", pupil="#6a4a10")
    hair = "#141216"
    short_hair(s, hair, fringe=1, back=8)
    hair_volume(s, hair, sides=4, back=8)
    long_hair_back(s, hair, rows=6, width=2)
    white = "#ecebe6"
    # Le chapeau blanc, son ruban sombre.
    s.material("hat", "top", white, "cloth")
    for f in SIDES:
        s.material("hat", f, white, "cloth", y=0, h=2)
        s.material("hat", f, "#2a2a30", "cloth", y=2, h=1)
    for part in ("body", "rarm", "larm"):
        s.fill(part, white, "cloth")
    s.material("body", "front", "#2a2a30", "cloth", x=3, w=2, h=8)
    s.px("body", "front", 3, 1, "#c0182c")
    s.px("body", "front", 4, 1, "#c0182c")
    hands(s, FAIR, rows=2)
    # Le soleil dans une paume, la lune dans l'autre, sur le dessous des mains.
    s.px("rarm", "bottom", 1, 1, "#c0182c")
    s.px("rarm", "bottom", 2, 2, "#c0182c")
    s.px("larm", "bottom", 1, 1, "#141216")
    s.px("larm", "bottom", 2, 2, "#141216")
    for arm in ("rarm", "larm"):
        s.px(arm, "front", 1, 11, "#c0182c" if arm == "rarm" else "#141216")
    pants(s, white)
    boots(s, color="#e8e6e0", rows=2, sole="#8a8680")


@skin("slicer")
def slicer(s):
    """Les frères Slicer, deux âmes dans une armure rouillée et cabossée, le 48 sur le plastron."""
    armor(s, "#7a6a5c", "#4a3a2e", eye="#ff3b2b", dented=True)
    rust = "#8a4a22"
    for part, f, x, y in (("body", "front", 1, 4), ("body", "front", 6, 9), ("head", "front", 1, 1),
                          ("larm", "front", 1, 6), ("rleg", "front", 2, 3), ("lleg", "front", 1, 8)):
        s.px(part, f, x, y, rust)
    # Le « 48 » gravé sur le plastron.
    num = "#2a2420"
    for x, y in ((1, 2), (1, 3), (2, 3), (2, 2), (2, 4)):
        s.px("body", "front", x, y, num)
    for x, y in ((5, 2), (6, 2), (5, 3), (6, 3), (5, 4), (6, 4)):
        s.px("body", "front", x, y, num)
    s.px("body", "front", 6, 3, "#7a6a5c")


@skin("tucker")
def tucker(s):
    """Shou Tucker, l'Alchimiste des liens vitaux : maigre, cheveux bruns, lunettes rondes, gilet beige
    et pantalon vert."""
    tone = "#e6c8a8"
    skin_face(s, tone, mouth="#8a5a52")
    eyes(s, "#4a3a2a", brow="#5a4030")
    glasses(s, "#5a5048")
    short_hair(s, "#5a4030", fringe=2, back=5)
    hair_volume(s, "#5a4030", sides=3, back=5)
    for part in ("body", "rarm", "larm"):
        s.fill(part, "#d8c8a0", "cloth")
    s.material("body", "front", "#f0ece0", "cloth", x=3, w=2, h=6)
    s.seam("body", "front", 4, 2, 10, None, k=0.8)
    for y in (4, 7, 10):
        s.px("body", "front", 3, y, "#8a7a5a")
    hands(s, tone, rows=2)
    pants(s, "#4a6a3a")
    boots(s, color="#3a2a20", rows=2)


@skin("ishvalan")
def ishvalan(s):
    """Un Ishvalien : la peau mate, les yeux rouges, les cheveux blancs, des étoffes couleur de sable
    nouées d'une ceinture rouge sombre, les sandales."""
    tone = ISHVAL
    skin_face(s, tone, mouth="#5a2a20")
    eyes(s, "#b0202a", brow="#d8d4cc")
    short_hair(s, "#e4e0d8", fringe=1, back=5)
    hair_volume(s, "#e4e0d8", sides=3, back=5)
    sand = "#c8a46a"
    for part in ("body", "rarm", "larm"):
        s.fill(part, sand, "cloth")
    # L'étoffe drapée en travers du buste, plus claire.
    for i in range(8):
        s.px("body", "front", i, min(11, 2 + i), "#e0c890")
        s.px("body", "back", 7 - i, min(11, 2 + i), "#e0c890")
    belt(s, y=8, color="#7a1c1c", buckle="#7a1c1c")
    hands(s, tone, rows=3)
    pants(s, "#b08a52")
    # Des sandales : la peau nue, des lanières de cuir.
    boots(s, color=tone, rows=2, sole="#4a2e1a", mat="skin")
    for leg in ("rleg", "lleg"):
        s.band(leg, 10, 1, "#5a3a20")


@skin("drachma_general")
def drachma_general(s):
    """Un général de Drachma : la haute chapka de fourrure à l'étoile rouge, la barbe grise, la longue
    capote vert sombre aux épaulettes et aux boutons d'or."""
    tone = "#e3bea0"
    fur = "#3e2e22"
    coat = "#2f3d2a"
    skin_face(s, tone, mouth="#93554d")
    eyes(s, "#4a5a6a", brow="#9a9a94")
    short_hair(s, "#8a8a84", fringe=0, back=5)
    # La barbe grise, du menton aux joues.
    s.rect("head", "front", 1, 6, 6, 2, "#a8a8a2")
    s.rect("head", "front", 0, 5, 1, 3, "#a8a8a2")
    s.rect("head", "front", 7, 5, 1, 3, "#a8a8a2")
    mustache(s, "#9a9a94", wide=True)
    # La chapka haute, sur la couche du chapeau : fourrure tout autour, étoile rouge devant.
    s.material("hat", "top", shade(fur, 1.1), "fur")
    for f in SIDES:
        s.material("hat", f, fur, "fur", h=4)
    s.material("hat", "back", fur, "fur", h=6)
    s.rect("hat", "front", 3, 1, 2, 2, "#c8202c")
    s.px("hat", "front", 3, 1, "#e8484e")
    for part in ("body", "rarm", "larm"):
        s.fill(part, coat, "cloth")
    s.material("jacket", "front", fur, "fur", h=1)
    s.seam("body", "front", 3, 1, 11, None, k=0.75)
    for y in (2, 4, 6, 8, 10):
        s.px("body", "front", 4, y, GOLD)
    # Les épaulettes d'or.
    for arm in ("rarm", "larm"):
        s.material(arm, "top", GOLD, "metal")
        s.band(arm, 0, 1, GOLD)
        s.band(arm, 9, 1, fur, "fur")
    belt(s, y=8, color="#2a1e14", buckle=GOLD)
    hands(s, tone, rows=2, glove="#1e1a16")
    coat_tails(s, coat, rows=6)
    pants(s, "#2a3424")
    boots(s, rows=5)


# --- ce qu'on porte sur soi : gants et tatouages, dessinés sur le corps du joueur ----------------------
# Des calques au gabarit d'une peau de joueur, transparents sauf là où ils se posent (voir HandsLayer).
GLOVE_COLORS = {"cloth": "#ececec", "leather": "#7a4a26", "spark": "#f7f7f7", "gauntlet": "#4f545b",
                "state": "#f4f4f4"}


def glove_layer(kind):
    def draw(s):
        color = GLOVE_COLORS[kind]
        hands(s, color, rows=4, glove=color)
        for arm in ("rarm", "larm"):
            if kind == "spark":
                # Le cercle rouge brodé sur le dos de la main.
                ink_ring(s, arm, "back", 1.5, 9.5, 1.2, "#c0202a")
            if kind == "state":
                # Le revers doré des gants d'État.
                s.band(arm, 8, 1, "#c9a23a")
            if kind == "gauntlet":
                s.band(arm, 8, 1, "#2e3238")
        # Le même gant sur la couche des manches : une peau aux manches longues ne le cache plus.
        for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
            for f in SIDES + ("bottom",):
                w, h = s.size(arm, f)
                for y in range(h):
                    for x in range(w):
                        c = s.pget(arm, f, x, y)
                        if c[3]:
                            s.px(sleeve, f, x, y, "#%02x%02x%02x" % c[:3])
    return draw


def ink_ring(s, part, f, cx, cy, r, color):
    """Un cercle d'encre sur une face : les pixels à un demi-pixel du rayon."""
    w, h = s.size(part, f)
    for y in range(h):
        for x in range(w):
            if abs(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 - r) < 0.6:
                s.px(part, f, x, y, color)


TATTOO_INK = "#1b1626"
TATTOO_PLACES = {
    "palm_right": ("rarm", "front", 1.5, 9.5, 1.3),
    "palm_left": ("larm", "front", 1.5, 9.5, 1.3),
    "forearm_right": ("rarm", "right", 1.5, 5.5, 1.4),
    "forearm_left": ("larm", "left", 1.5, 5.5, 1.4),
    "back": ("body", "back", 3.5, 5.5, 3.0),
    "chest": ("body", "front", 3.5, 4.5, 2.6),
}


def tattoo_layer(slot):
    def draw(s):
        part, f, cx, cy, r = TATTOO_PLACES[slot]
        ink_ring(s, part, f, cx, cy, r, TATTOO_INK)
        if r > 2:
            # Sur le dos et la poitrine, un second anneau et le point du centre.
            ink_ring(s, part, f, cx, cy, r - 1.5, TATTOO_INK)
            s.px(part, f, int(cx + 0.5), int(cy + 0.5), TATTOO_INK)
    return draw


for _kind in GLOVE_COLORS:
    skin("worn/glove_" + _kind, volume=False)(glove_layer(_kind))
    # Les bras fins (Alex) n'ont pas le même gabarit : leur calque à part (voir HandsLayer).
    skin("worn/glove_" + _kind + "_slim", volume=False, slim=True)(glove_layer(_kind))
for _slot in TATTOO_PLACES:
    skin("worn/tattoo_" + _slot, volume=False)(tattoo_layer(_slot))
    if TATTOO_PLACES[_slot][0] != "body":
        skin("worn/tattoo_" + _slot + "_slim", volume=False, slim=True)(tattoo_layer(_slot))


def build(names=None):
    os.makedirs(OUT, exist_ok=True)
    for name in names or SKINS:
        fn, kw = SKINS[name]
        kw = dict(kw)
        volume = kw.pop("volume", True)
        s = Skin(name, **kw)
        fn(s)
        if volume:
            s.volume()
        path = os.path.join(OUT, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        s.save(path)
        print("peau", name)


if __name__ == "__main__":
    build(sys.argv[1:] or None)
