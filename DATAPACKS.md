# Étendre l'alchimie par datapack

Tout ce qu'un cercle sait faire se décrit en données : un datapack ajoute des glyphes, des
combinaisons et des effets sans toucher au mod. *English summary at the end.*

## Les briques

| Brique | Fichier | Rôle |
|---|---|---|
| Glyphe | `data/<ns>/fmab/glyph/<id>.json` | Un tracé reconnu dans le cercle : élément, action ou modificateur. |
| Combinaison | `data/<ns>/fmab/combination/<id>.json` | Tels éléments et telle action donnent tel effet. |
| Matière d'un élément | tags `fmab:element/<élément>` (objets et blocs) | Ce que vise l'élément. |

## Combinaisons

```json
{ "elements": ["wood"], "action": "decompose", "effect": "fmab:fell", "range": 4, "school": "earth" }
```

- `effect` : le nom de l'effet (sa traduction : `effect.<ns>.<nom>`). Un effet codé du mod, ou
  n'importe quel nom si `spec` décrit l'effet.
- `range` : la portée de base, que les points d'Intensité, la taille du cercle et la Pierre
  philosophale multiplient.
- `requires`, `passive`, `formula` : voir les combinaisons du mod.

### Jokers

`"elements": ["*"]` vaut pour tout élément seul qui n'a pas sa propre combinaison avec cette action ;
`except` en écarte certains. Un élément ajouté par un datapack (un glyphe et ses tags) profite aussitôt
des jokers du mod : Fixer élève un mur de sa matière, Projeter en fait jaillir une pique, Réparer
répare ce qui se répare avec elle.

```json
{ "elements": ["*"], "action": "fix", "effect": "fmab:matter_wall", "range": 2,
  "except": ["air", "fire", "gold", "human"], "spec": { "zone": "wall", "operation": "place", "channel": "grow" } }
```

## Effets en données (`spec`)

Une étape, ou une suite d'étapes (`"steps": [ … ]`). Chaque étape a une **cible**, une **zone**, un
**filtre** et une **opération**.

| Clé | Valeurs |
|---|---|
| `target` | `blocks` (par défaut), `entities`, `items`, `effect` (un effet codé : `"effect": "fmab:smelt"`), `function` |
| `zone` (blocs) | `sphere` (par défaut), `cube`, `below` (le sol sous le cercle), `wall` (un mur devant), `column` (une colonne à portée, dans la direction), `area` (la ligne, le cône ou le disque des glyphes de forme) |
| `filter` | `element` (la matière de l'élément du cercle, par défaut), `#ns:tag`, `ns:id`, `any` ; créatures : `living` (par défaut), `hostile`, `animal`, `#ns:tag`, `ns:id` |
| `only_aimed` | `true` : l'étape n'a lieu que si le glyphe de cible est tracé (Créature pour `entities`) |
| `channel` | `once` (par défaut), `grow` (l'ouvrage grandit tant que l'alchimiste garde la main sur le cercle), `repeat` |

**Opérations sur les blocs**
- `break` : brise ce que retient le filtre ; la matière passe dans le cercle.
- `place` : pose des blocs dans les cases libres, pris sur le cercle, dans l'étage précédent ou dans le
  sac (`material` : `element`, `#tag` ou un objet) ; `damage` blesse ce qui s'y trouve.
- `freeze` : l'eau devient glace, la lave obsidienne.
- `drain` : retire les liquides.

**Opérations sur les créatures**
- `damage` (`amount`), `effect` (`effect`, `duration` en secondes, `amplifier`), `push` (`strength`),
  `ignite` (`seconds`), `heal` (`amount`), `grow_up` (les petits deviennent adultes). Ce qui blesse
  épargne l'alchimiste et ses alliés.

**Opérations sur les objets**
- `dismantle` : défait les objets de l'élément ; `collect` : les objets au sol à portée viennent sur le
  cercle.

**Fonctions de datapack**

```json
"spec": { "target": "function", "function": "monpack:cercles/foudre" }
```

La fonction est lancée au cercle, avec les droits du serveur et l'alchimiste comme entité, et reçoit en
paramètres de macro `range`, `dx`, `dz` (la direction) et `element`. Tout ce que les commandes savent
faire devient un effet d'alchimie.

### L'échange équivalent

Les opérations le font respecter d'elles-mêmes : ce qu'on brise passe dans la matière du cercle (puis
dans l'étage suivant en série), ce qu'on pose se prend dans la matière disponible. Une donnée mal écrite
ne fait rien (le cercle répond qu'il n'y a rien à portée) et laisse un avertissement dans le journal du
serveur.

### Exemple : dégeler un lac

```json
{ "elements": ["fire"], "action": "fix", "effect": "monpack:thaw", "range": 4,
  "spec": { "zone": "sphere", "filter": "#minecraft:ice", "operation": "break" } }
```

## Glyphes de cible

Posés sur l'anneau : **Objet** (le cercle ne touche que les objets), **Bloc** (que les blocs du monde),
**Créature** (ajoute les étapes `only_aimed` sur les créatures). Sans glyphe de cible, un cercle agit
comme il l'a toujours fait.

---

## English summary

Glyphs (`data/<ns>/fmab/glyph`), combinations (`data/<ns>/fmab/combination`) and element tags
(`fmab:element/*`) are data. A combination may describe its effect in data with `spec`: a `target`
(`blocks`, `entities`, `items`, `effect`, `function`), a `zone`, a `filter` and an `operation` (see the
tables above), or a list of `steps`. `"elements": ["*"]` makes a wildcard that applies to any single
element without its own combination for that action. `"target": "function"` runs a datapack function
at the circle with `range`, `dx`, `dz` and `element` as macro arguments. Operations enforce equivalent
exchange: what is broken goes into the circle's matter, what is placed is taken from it.
