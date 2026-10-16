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
# Les scripts qui ne génèrent rien eux-mêmes : la boîte à outils, ce lanceur et la vérification.
NOT_GENERATORS = {"skinlib", "all", "check"}
# Posée pour les générateurs lancés : un script qui relancerait all.py s'arrête au lieu de boucler.
GUARD = "FMAB_SKINS_ALL"
ORDER = ["characters", "creatures", "eggs", "items", "blocks", "cinematic"]


def generators():
    found = sorted(name[:-3] for name in os.listdir(HERE)
                   if name.endswith(".py") and name[:-3] not in NOT_GENERATORS)
    return [g for g in ORDER if g in found] + [g for g in found if g not in ORDER]


def main():
    if os.environ.get(GUARD):
        sys.exit("all.py lancé depuis un générateur : arrêt, pour ne pas boucler.")
    env = dict(os.environ, **{GUARD: "1"})
    failed = []
    for name in generators():
        print(f"== {name}", flush=True)
        result = subprocess.run([sys.executable, os.path.join(HERE, name + ".py")], cwd=HERE, env=env)
        if result.returncode != 0:
            failed.append(name)
    if failed:
        print("échec : " + ", ".join(failed), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
