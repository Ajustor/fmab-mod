"""Régénère toutes les textures dessinées en code : `python tools/skins/all.py`.

Chaque générateur de ce dossier tourne à son tour, dans son propre processus (ils partagent des
modules et des variables globales). L'ordre connu d'abord, puis tout nouveau générateur ajouté au
dossier, par ordre alphabétique. Les textures sont déterministes : relancer ne doit rien changer aux
PNG commités (la CI le vérifie).
"""
from __future__ import annotations

import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
# Les modules partagés, qui ne génèrent rien eux-mêmes.
LIBRARIES = {"skinlib", "all"}
ORDER = ["characters", "creatures", "eggs", "items", "blocks", "cinematic"]


def generators():
    found = sorted(name[:-3] for name in os.listdir(HERE)
                   if name.endswith(".py") and name[:-3] not in LIBRARIES)
    return [g for g in ORDER if g in found] + [g for g in found if g not in ORDER]


def main():
    failed = []
    for name in generators():
        print(f"== {name}", flush=True)
        result = subprocess.run([sys.executable, os.path.join(HERE, name + ".py")], cwd=HERE)
        if result.returncode != 0:
            failed.append(name)
    if failed:
        print("échec : " + ", ".join(failed), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
