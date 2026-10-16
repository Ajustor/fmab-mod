"""Vérifie que les textures commitées sont celles des générateurs : `python tools/skins/check.py`.

Régénère tout (all.py), puis compare chaque fichier modifié à sa version commitée. Une PNG compte
comme inchangée si ses pixels le sont : l'encodage (compression zlib) varie d'une machine à l'autre
sans rien changer à l'image. Une PNG nouvelle, une image différente ou un .mcmeta changé font échouer
la vérification.
"""
from __future__ import annotations

import io
import os
import subprocess
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
WATCHED = "src/main/resources"


def git(*args: str, binary: bool = False):
    out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, check=True)
    return out.stdout if binary else out.stdout.decode("utf-8")


def pixels(data: bytes):
    img = Image.open(io.BytesIO(data))
    img.load()
    return img.mode, img.size, img.tobytes()


def main():
    subprocess.run([sys.executable, os.path.join(HERE, "all.py")], check=True)
    wrong = []
    for line in git("status", "--porcelain", "--", WATCHED).splitlines():
        status, path = line[:2], line[3:]
        if status.strip() != "M" or not path.endswith(".png"):
            wrong.append(line)
            continue
        before = git("show", "HEAD:" + path, binary=True)
        with open(os.path.join(ROOT, path), "rb") as f:
            after = f.read()
        if pixels(before) != pixels(after):
            wrong.append(line)
    if wrong:
        print("Ces textures ne correspondent pas aux générateurs (lancer python tools/skins/all.py et committer) :",
              file=sys.stderr)
        for line in wrong:
            print("  " + line, file=sys.stderr)
        sys.exit(1)
    print("Textures conformes aux générateurs.")


if __name__ == "__main__":
    main()
