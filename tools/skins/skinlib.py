"""Une petite boîte à outils pour peindre des peaux 64x64 (disposition « joueur ») en code.

Chaque partie du corps est une boîte dépliée selon l'UV de boîte de Minecraft : dessus, dessous,
puis la ceinture des quatre côtés (droite, devant, gauche, dos) d'un seul tenant. On peint par
partie et par face, en coordonnées locales ; les matériaux ajoutent grain et ombrage pour que les
aplats ne restent pas plats.
"""
from __future__ import annotations

import random
from PIL import Image

FACES = ("top", "bottom", "right", "front", "left", "back")
SIDES = ("right", "front", "left", "back")


def rgb(c):
    if isinstance(c, tuple):
        return c if len(c) == 4 else (*c, 255)
    c = c.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255 if len(c) == 6 else int(c[6:8], 16))


def shade(c, f):
    """Éclaircit (f > 1) ou assombrit (f < 1) une couleur."""
    r, g, b, a = rgb(c)
    if f >= 1:
        # Un reflet ne fait pas que multiplier : les tons sombres doivent aussi pouvoir s'éclaircir.
        lift = (f - 1) * 40
        return (min(255, round(r * f + lift)), min(255, round(g * f + lift)), min(255, round(b * f + lift)), a)
    return (round(r * f), round(g * f), round(b * f), a)


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def hue_shift_dark(c, f):
    """Une ombre plus froide qu'un simple noircissement : on tire vers le bleu-violet."""
    return mix(shade(c, f), (40, 30, 70, rgb(c)[3]), 0.12)


# Les matériaux : amplitude du grain, gradient vertical (haut, bas), assombrissement des arêtes.
MATERIALS = {
    "cloth": dict(noise=0.04, top=1.06, bottom=0.88, edge=0.92),
    "skin": dict(noise=0.025, top=1.04, bottom=0.94, edge=0.97),
    "hair": dict(noise=0.08, top=1.12, bottom=0.82, edge=0.9, streak=True),
    "metal": dict(noise=0.04, top=1.18, bottom=0.78, edge=0.8),
    "leather": dict(noise=0.07, top=1.05, bottom=0.85, edge=0.88),
    "fur": dict(noise=0.14, top=1.1, bottom=0.85, edge=0.95, streak=True),
    "flat": dict(noise=0.0, top=1.0, bottom=1.0, edge=1.0),
    "stone": dict(noise=0.12, top=1.08, bottom=0.85, edge=0.85),
}


class Skin:
    def __init__(self, name, slim=False, size=(64, 64), parts=None):
        self.name = name
        self.img = Image.new("RGBA", size, (0, 0, 0, 0))
        self.rng = random.Random(name)
        aw = 3 if slim else 4
        self.parts = {
            "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
            "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
            "rarm": (40, 16, aw, 12, 4), "rsleeve": (40, 32, aw, 12, 4),
            "larm": (32, 48, aw, 12, 4), "lsleeve": (48, 48, aw, 12, 4),
            "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
            "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
        }
        if parts:
            self.parts.update(parts)

    # --- géométrie -----------------------------------------------------------------------------
    def face(self, part, f):
        u, v, w, h, d = self.parts[part]
        return {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }[f]

    def size(self, part, f):
        return self.face(part, f)[2:]

    # --- pixels --------------------------------------------------------------------------------
    def put(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            if c is None:
                self.img.putpixel((x, y), (0, 0, 0, 0))
            else:
                self.img.putpixel((x, y), rgb(c))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def px(self, part, f, x, y, c):
        fx, fy, fw, fh = self.face(part, f)
        if x < 0:
            x += fw
        if y < 0:
            y += fh
        if 0 <= x < fw and 0 <= y < fh:
            self.put(fx + x, fy + y, c)

    def pget(self, part, f, x, y):
        fx, fy, fw, fh = self.face(part, f)
        return self.get(fx + x, fy + y)

    def rect(self, part, f, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.px(part, f, x + i, y + j, c(i, j) if callable(c) else c)

    def tint(self, part, f, x, y, w, h, k):
        """Multiplie ce qui est déjà peint (ombre ou reflet locaux)."""
        fx, fy, fw, fh = self.face(part, f)
        for j in range(y, y + h):
            for i in range(x, x + w):
                if 0 <= i < fw and 0 <= j < fh:
                    c = self.get(fx + i, fy + j)
                    if c[3]:
                        self.put(fx + i, fy + j, shade(c, k))

    # --- ceinture des côtés --------------------------------------------------------------------
    def band_cells(self, part):
        """Les colonnes de la ceinture (droite, devant, gauche, dos), en (face, x local)."""
        cells = []
        for f in SIDES:
            w = self.size(part, f)[0]
            cells += [(f, x) for x in range(w)]
        return cells

    def band(self, part, y, h, c, mat="cloth", faces=SIDES):
        """Une bande horizontale sur tout le tour de la partie (ceinture, revers, bottes)."""
        for f in faces:
            w, fh = self.size(part, f)
            self.material(part, f, c, mat, x=0, y=y, w=w, h=h)

    # --- matériaux -----------------------------------------------------------------------------
    def material(self, part, f, c, mat="cloth", x=0, y=0, w=None, h=None, edges=True):
        """Remplit (une zone d') une face d'un matériau : grain, dégradé vertical, arêtes."""
        m = MATERIALS[mat]
        fx, fy, fw, fh = self.face(part, f)
        w = fw - x if w is None else w
        h = fh - y if h is None else h
        base = rgb(c)
        streak = [self.rng.uniform(-1, 1) for _ in range(fw)] if m.get("streak") else None
        # Un grain à deux échelles : des taches de 2x2 (les plis, l'usure) et un léger bruit au pixel.
        blocks = {}
        for j in range(y, y + h):
            for i in range(x, x + w):
                if not (0 <= i < fw and 0 <= j < fh):
                    continue
                b = blocks.setdefault((i // 2, j // 2), self.rng.uniform(-1, 1))
                k = 1 + m["noise"] * (0.65 * b + 0.45 * self.rng.uniform(-1, 1))
                if streak:
                    k += streak[i] * m["noise"] * 0.8
                if f in SIDES:
                    t = j / max(1, fh - 1)
                    k *= m["top"] + (m["bottom"] - m["top"]) * t
                elif f == "top":
                    k *= m["top"] * 1.04
                else:
                    k *= m["bottom"] * 0.95
                if edges and m["edge"] < 1 and f in ("top", "bottom") and (i in (0, fw - 1) or j in (0, fh - 1)):
                    k *= m["edge"]
                col = shade(base, k) if k >= 1 else hue_shift_dark(base, k)
                self.put(fx + i, fy + j, (*col[:3], base[3]))

    def fill(self, part, c, mat="cloth", faces=FACES, top=None, bottom=None):
        for f in faces:
            col = top if (f == "top" and top) else bottom if (f == "bottom" and bottom) else c
            self.material(part, f, col, mat)

    def clear(self, part, faces=FACES):
        for f in faces:
            fx, fy, fw, fh = self.face(part, f)
            for j in range(fh):
                for i in range(fw):
                    self.put(fx + i, fy + j, None)

    def seam(self, part, f, x, y, length, c, vertical=True, k=0.8):
        """Une couture ou un pli : une ligne plus sombre (ou de couleur) sur une face."""
        for n in range(length):
            xx, yy = (x, y + n) if vertical else (x + n, y)
            if c is None:
                self.tint(part, f, xx, yy, 1, 1, k)
            else:
                self.px(part, f, xx, yy, c)

    def outline_edges(self, part, k=0.82, faces=SIDES, sides=("left", "right")):
        """Assombrit les arêtes verticales de la ceinture : un léger contour de volume."""
        for f in faces:
            w, h = self.size(part, f)
            if "left" in sides:
                self.tint(part, f, 0, 0, 1, h, k)
            if "right" in sides:
                self.tint(part, f, w - 1, 0, 1, h, k)

    def volume(self, overlays=True):
        """L'ombre portée du corps sur lui-même : l'intérieur des membres, sous les bras, l'entrejambe.
        À appeler en dernier, une fois tout peint."""
        for part, inner in (("rleg", "left"), ("lleg", "right"), ("rarm", "left"), ("larm", "right")):
            w, h = self.size(part, inner)
            self.tint(part, inner, 0, 0, w, h, 0.84)
        # L'entrejambe : la colonne intérieure du devant et du dos des jambes.
        for part, x in (("rleg", -1), ("lleg", 0)):
            for f in ("front", "back"):
                xx = x if x >= 0 else self.size(part, f)[0] - 1
                xx = xx if f == "front" else self.size(part, f)[0] - 1 - xx
                self.tint(part, f, xx, 0, 1, 3, 0.88)
        # Le haut des jambes, sous le buste.
        for part in ("rleg", "lleg"):
            self.band_tint(part, 0, 1, 0.86)

    def band_tint(self, part, y, h, k, faces=SIDES):
        for f in faces:
            w, _ = self.size(part, f)
            self.tint(part, f, 0, y, w, h, k)

    def save(self, path):
        self.img.save(path)


# --- visages ---------------------------------------------------------------------------------
def eyes(s, iris, white="#f4f1ec", y=4, gap=2, x0=1, brow=None, brow_y=None, lid=None, pupil=None,
         part="head"):
    """Deux yeux de deux pixels (blanc vers l'extérieur, iris vers le nez), sourcils optionnels."""
    left_x = x0                # œil à gauche de l'image (droit du personnage)
    right_x = 8 - x0 - 2
    s.rect(part, "front", left_x, y, 1, 1, white)
    s.rect(part, "front", left_x + 1, y, 1, 1, iris)
    s.rect(part, "front", right_x, y, 1, 1, iris)
    s.rect(part, "front", right_x + 1, y, 1, 1, white)
    if pupil:
        s.px(part, "front", left_x + 1, y, pupil)
        s.px(part, "front", right_x, y, pupil)
    if lid:
        s.rect(part, "front", left_x, y - 1, 2, 1, lid)
        s.rect(part, "front", right_x, y - 1, 2, 1, lid)
    if brow:
        by = y - 1 if brow_y is None else brow_y
        s.rect(part, "front", left_x, by, 2, 1, brow)
        s.rect(part, "front", right_x, by, 2, 1, brow)


def skin_face(s, tone, part="head", nose=True, mouth="#9a5a52", mouth_w=2, mouth_y=6, cheeks=None):
    """La peau de la tête, avec un nez en ombre et une bouche."""
    s.fill(part, tone, "skin")
    if nose:
        s.tint(part, "front", 3, 5, 2, 1, 0.9)
        s.px(part, "front", 3, 5, shade(tone, 0.86))
    if mouth:
        s.rect(part, "front", 4 - mouth_w // 2, mouth_y, mouth_w, 1, mouth)
    if cheeks:
        s.px(part, "front", 1, 5, cheeks)
        s.px(part, "front", 6, 5, cheeks)
    # Les oreilles, au milieu des côtés.
    for f, x in (("right", 3), ("left", 3)):
        s.rect(part, f, x, 4, 2, 2, shade(tone, 0.92))
        s.px(part, f, x + (1 if f == "right" else 0), 5, shade(tone, 0.8))
    # Le dessous du menton et la nuque, dans l'ombre.
    s.tint(part, "front", 0, 7, 8, 1, 0.93)
