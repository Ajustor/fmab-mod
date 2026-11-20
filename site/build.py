"""Assemble le site GitHub Pages : l'accueil (site/index.html) et les pages Releases, Changelog et
Installation, en français et en anglais (sélecteur de langue comme sur l'accueil).

Usage : python3 site/build.py <dossier de sortie> [releases.json]

releases.json est la réponse de l'API GitHub (liste des releases) ; les changelogs viennent de
CHANGELOG.fr.md et CHANGELOG.md, recommités par le workflow de release. Sans eux, les pages le disent.
Bibliothèque standard uniquement.
"""
import html
import json
import math
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SITE = os.path.join(ROOT, "site")
REPO = "https://github.com/Ajustor/fmab-mod"

NAV = [("index.html", "Accueil", "Home"), ("releases.html", "Versions", "Releases"),
       ("changelog.html", "Changelog", "Changelog"), ("installation.html", "Installation", "Installation"),
       ("editor/", "Éditeur", "Editor")]


def template():
    """La tête et le pied de l'accueil : même style, même script de langue."""
    page = open(os.path.join(SITE, "index.html"), encoding="utf-8").read()
    head = page[:page.index("<main>")]
    tail = page[page.index("<script>"):]
    footer = page[page.index("<footer>"):page.index("</footer>") + len("</footer>")]
    return head, footer, tail


def nav(current):
    links = []
    for href, fr, en in NAV:
        mark = ' aria-current="page"' if href == current else ""
        links.append(f'<a href="{href}"{mark}><span data-lang="fr">{fr}</span><span data-lang="en">{en}</span></a>')
    return '<nav class="site-nav">' + "".join(links) + "</nav>"


def topbar(current, brand):
    """Le bandeau commun : la marque (un petit cercle du mod), le menu et le choix de langue."""
    lang_buttons = ('<div class="lang"><button type="button" data-set-lang="fr">FR</button>'
                    '<button type="button" data-set-lang="en">EN</button></div>')
    return (f'<header>\n<a class="brand" href="index.html">{brand}FMAB</a>\n{nav(current)}\n{lang_buttons}\n'
            '</header>')


def page(head, footer, tail, current, title_fr, title_en, body_fr, body_en, brand=""):
    """Une page intérieure : le bandeau, le titre, puis le contenu sur une feuille du grimoire."""
    head = re.sub(r"<title>.*?</title>", f"<title>FMAB — {html.escape(title_en)}</title>", head, count=1)
    return (head + "<main>\n" + topbar(current, brand) + "\n"
            f'<div class="page-title"><h1 data-lang="fr">{html.escape(title_fr)}</h1>'
            f'<h1 data-lang="en">{html.escape(title_en)}</h1></div>\n'
            f'<article class="sheet"><section data-lang="fr">{body_fr}</section>\n'
            f'<section data-lang="en">{body_en}</section></article>\n'
            f"{footer}\n</main>\n{tail}")


def inline(text):
    text = html.escape(text)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    return re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<a href="\2">\1</a>', text)


def markdown(md):
    """Le peu de Markdown que produit git-cliff : titres, listes, paragraphes."""
    out, in_list = [], False
    for line in md.splitlines():
        if line.startswith("- "):
            if not in_list:
                out.append("<ul>")
                in_list = True
            out.append(f"<li>{inline(line[2:])}</li>")
            continue
        if in_list:
            out.append("</ul>")
            in_list = False
        m = re.match(r"^(#{1,4}) (.*)", line)
        if m:
            level = min(4, len(m.group(1)) + 1)
            out.append(f"<h{level}>{inline(m.group(2))}</h{level}>")
        elif line.strip():
            out.append(f"<p>{inline(line)}</p>")
    if in_list:
        out.append("</ul>")
    return "\n".join(out)


def changelog(name, missing):
    path = os.path.join(ROOT, name)
    if not os.path.exists(path):
        return f"<p>{missing}</p>"
    md = open(path, encoding="utf-8").read()
    # Le titre du fichier fait doublon avec celui de la page.
    md = re.sub(r"^# .*\n", "", md, count=1)
    return markdown(md)


def minecraft_of(jar):
    """La version de Minecraft d'un jar du mod : ce qui suit le « + » de son nom."""
    return jar.rsplit("+", 1)[-1].removesuffix(".jar") if "+" in jar else ""


def releases(data, fr):
    if not data:
        return "<p>" + ("Aucune version publiée pour l'instant." if fr else "No release published yet.") + "</p>"
    out = []
    for r in data:
        if r.get("draft"):
            continue
        tag = html.escape(r.get("tag_name", ""))
        date = (r.get("published_at") or "")[:10]
        pre = r.get("prerelease")
        badge = ('<span class="badge pre">' + ("préversion" if fr else "pre-release") + "</span>") if pre \
            else ('<span class="badge">' + ("stable" if fr else "stable") + "</span>")
        # Un jar par version de Minecraft (fmab-fabric-1.7.0+26.3.jar ; fmab-1.6.0+26.2.jar avant).
        jars = mod_jars(r)
        link = " ".join(f'<a class="button" href="{html.escape(a["browser_download_url"])}">'
                        + ("Télécharger" if fr else "Download") + f" {html.escape(a['name'])}</a>" for a in jars) \
            or f'<a href="{html.escape(r.get("html_url", REPO))}">GitHub</a>'
        versions = ", ".join(minecraft_of(a["name"]) for a in jars) or "26.2"
        details = f"Minecraft {versions} · Fabric Loader ≥ 0.19.3 · Fabric API · Java 25"
        out.append(f'<div class="release"><h2>{tag} {badge}</h2><p>{date} · {details}</p><p>{link}</p></div>')
    return "\n".join(out)


INSTALL_FR = """
<ol>
<li>Installez <strong>Java 25</strong> ou plus récent (le lanceur officiel le fournit).</li>
<li>Installez <a href="https://fabricmc.net/use/installer/">Fabric Loader</a> (0.19.3 ou plus) pour <strong>Minecraft 26.2 ou 26.3</strong>.</li>
<li>Téléchargez <a href="https://modrinth.com/mod/fabric-api">Fabric API</a> pour votre version de Minecraft, et le jar du mod de la même version (<code>fmab-fabric-…+26.3.jar</code> pour 26.3) depuis la page <a href="releases.html">Versions</a>.</li>
<li>Placez les deux jars dans le dossier <code>mods</code> de votre installation, puis lancez le profil Fabric.</li>
</ol>
<h2>Problèmes fréquents</h2>
<ul>
<li><strong>Le jeu refuse de démarrer, « Java 25 requis »</strong> : le profil utilise un Java trop ancien ; choisissez Java 25 dans les réglages du profil.</li>
<li><strong>« Fabric API manquant »</strong> : le mod en dépend ; ajoutez-le au dossier <code>mods</code>.</li>
<li><strong>Mauvaise version de Minecraft</strong> : chaque jar du mod ne marche qu'avec la version de Minecraft écrite après le « + » de son nom.</li>
<li><strong>Serveur</strong> : le même jar s'installe sur le serveur ; chaque joueur doit aussi l'avoir.</li>
</ul>
"""
INSTALL_EN = """
<ol>
<li>Install <strong>Java 25</strong> or newer (the official launcher ships it).</li>
<li>Install <a href="https://fabricmc.net/use/installer/">Fabric Loader</a> (0.19.3 or newer) for <strong>Minecraft 26.2 or 26.3</strong>.</li>
<li>Download <a href="https://modrinth.com/mod/fabric-api">Fabric API</a> for your Minecraft version, and the mod jar for that same version (<code>fmab-fabric-…+26.3.jar</code> for 26.3) from the <a href="releases.html">Releases</a> page.</li>
<li>Put both jars in your installation's <code>mods</code> folder, then launch the Fabric profile.</li>
</ol>
<h2>Common problems</h2>
<ul>
<li><strong>The game won't start, "Java 25 required"</strong>: the profile uses an old Java; pick Java 25 in the profile settings.</li>
<li><strong>"Missing Fabric API"</strong>: the mod depends on it; add it to the <code>mods</code> folder.</li>
<li><strong>Wrong Minecraft version</strong>: each mod jar only works with the Minecraft version written after the "+" in its name.</li>
<li><strong>Server</strong>: the same jar goes on the server; every player needs it too.</li>
</ul>
"""


FORBIDDEN = os.path.join(ROOT, "src", "main", "java", "com", "ajustor", "fmab", "alchemy", "ForbiddenCircles.java")


def svg(primitives, box="0 0 32 32", cls=None, dot=0.35):
    """Un dessin du mod (y vers le bas) en SVG, un trait après l'autre : les cercles interdits et ceux
    du Traité sur une grille de 0 à 32, les glyphes autour de l'origine (de -1 à 1)."""
    def n(v):
        return f"{round(v, 3):g}"

    def points(pts):
        return " ".join(f"{n(x)},{n(y)}" for x, y in pts)

    parts = []
    for i, p in enumerate(primitives):
        kind = p["type"]
        if kind == "circle":
            (cx, cy), r = p["center"], p["radius"]
            shape = f'<circle cx="{n(cx)}" cy="{n(cy)}" r="{n(r)}"'
        elif kind in ("polygon", "polyline"):
            shape = f'<{kind} points="{points(p["points"])}"'
        elif kind == "line":
            (x1, y1), (x2, y2) = p["from"], p["to"]
            shape = f'<line x1="{n(x1)}" y1="{n(y1)}" x2="{n(x2)}" y2="{n(y2)}"'
        elif kind == "arc":
            (cx, cy), r = p["center"], p["radius"]
            start, sweep = p["start_deg"], p["sweep_deg"]
            if abs(sweep) >= 360:
                shape = f'<circle cx="{n(cx)}" cy="{n(cy)}" r="{n(r)}"'
            else:
                a0, a1 = math.radians(start), math.radians(start + sweep)
                shape = (f'<path d="M{n(cx + r * math.cos(a0))} {n(cy + r * math.sin(a0))} '
                         f'A{n(r)} {n(r)} 0 {1 if abs(sweep) > 180 else 0} {1 if sweep > 0 else 0} '
                         f'{n(cx + r * math.cos(a1))} {n(cy + r * math.sin(a1))}"')
        elif kind == "dot":
            x, y = p["at"]
            shape = f'<circle class="dot" cx="{n(x)}" cy="{n(y)}" r="{n(dot)}"'
        else:
            raise ValueError(f"trait inconnu : {kind}")
        parts.append(f'{shape} pathLength="1" style="--i:{i}"/>')
    group = "".join(parts)
    if cls:
        group = f'<g class="{cls}">{group}</g>'
    return f'<svg viewBox="{box}" aria-hidden="true">' + group + "</svg>"


TREATISE = os.path.join(ROOT, "src", "main", "resources", "assets", "fmab", "treatise", "treatise.json")
GLYPHS = os.path.join(ROOT, "src", "main", "resources", "data", "fmab", "fmab", "glyph")
STONECUTTER = os.path.join(ROOT, "stonecutter.properties.toml")
# Le cercle d'ouverture de l'accueil et celui de la marque, tirés du Traité.
HERO_CIRCLE = "treatise.fmab.stages.bristling_rampart"
BRAND_CIRCLE = "treatise.fmab.everyday.river"
EDITOR_CIRCLE = "treatise.fmab.ch5.series_spikes"


def treatise_circle(text_key):
    treatise = json.load(open(TREATISE, encoding="utf-8"))
    for chapter in treatise["chapters"]:
        for p in chapter["pages"]:
            if p.get("text") == text_key and p.get("example"):
                return p["example"]
    raise ValueError(f"cercle {text_key} introuvable dans le Traité")


def glyph_svg(name):
    glyph = json.load(open(os.path.join(GLYPHS, name + ".json"), encoding="utf-8"))
    return svg(glyph["primitives"], box="-1.15 -1.15 2.3 2.3", cls="chalk", dot=0.07).replace(
        "<svg ", '<svg class="glyph" ', 1)


MONTHS_FR = ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre",
             "octobre", "novembre", "décembre"]
MONTHS_EN = ["January", "February", "March", "April", "May", "June", "July", "August", "September",
             "October", "November", "December"]


def long_date(iso, fr):
    m = re.match(r"(\d{4})-(\d{2})-(\d{2})", iso or "")
    if not m:
        return ""
    y, mo, d = int(m.group(1)), int(m.group(2)), int(m.group(3))
    return f"{d} {MONTHS_FR[mo - 1]} {y}" if fr else f"{MONTHS_EN[mo - 1]} {d}, {y}"


def minecraft_versions():
    """Les versions de Minecraft prises en charge : une section par version dans Stonecutter."""
    return re.findall(r'^\["([^"]+)"\]', open(STONECUTTER, encoding="utf-8").read(), re.M)


def mod_jars(release):
    """Les jars du mod d'une release, un par version de Minecraft, dans l'ordre des versions."""
    return sorted((a for a in release.get("assets", []) if a.get("name", "").endswith(".jar")
                   and not a["name"].endswith("-sources.jar")), key=lambda a: minecraft_of(a["name"]))


def current_version(data):
    """La version actuelle : la dernière release stable ; à défaut, le haut du changelog."""
    for r in data or []:
        if r.get("draft") or r.get("prerelease"):
            continue
        jars = [(minecraft_of(a["name"]), a["browser_download_url"]) for a in mod_jars(r)]
        return {"version": r.get("tag_name", "").removeprefix("v"), "date": (r.get("published_at") or "")[:10],
                "jars": jars, "minecraft": [mc for mc, _ in jars if mc] or minecraft_versions()}
    path = os.path.join(ROOT, "CHANGELOG.fr.md")
    if os.path.exists(path):
        m = re.search(r"^## (\S+) — (\S+)", open(path, encoding="utf-8").read(), re.M)
        if m:
            return {"version": m.group(1), "date": m.group(2), "jars": [], "minecraft": minecraft_versions()}
    return None


EDITOR_BUTTON = ('<a class="button ghost" href="editor/"><span data-lang="fr">Ouvrir l\'éditeur</span>'
                 '<span data-lang="en">Open the editor</span></a>')


def version_html(current):
    """Le sceau de la version actuelle, et un lien de téléchargement par version de Minecraft."""
    if not current:
        return ('<div class="actions"><a class="button" href="releases.html"><span data-lang="fr">Télécharger</span>'
                '<span data-lang="en">Download</span></a>' + EDITOR_BUTTON + '</div>')
    v = html.escape(current["version"])
    mc = " · ".join(html.escape(x) for x in current["minecraft"])
    seal = (f'<p class="seal"><span class="tag">v{v}</span><span class="meta">'
            f'<span data-lang="fr">Publiée le {long_date(current["date"], True)}</span>'
            f'<span data-lang="en">Released {long_date(current["date"], False)}</span>'
            f' · Minecraft {mc}</span></p>')
    actions = ('<div class="actions"><a class="button" href="releases.html">'
               f'<span data-lang="fr">Télécharger la v{v}</span><span data-lang="en">Download v{v}</span></a>'
               + EDITOR_BUTTON + '</div>')
    direct = ""
    if current["jars"]:
        links = " · ".join(f'<a href="{html.escape(url)}">Minecraft {html.escape(mc)}</a>' for mc, url in current["jars"])
        direct = (f'<p class="downloads"><span data-lang="fr">Jar direct :</span>'
                  f'<span data-lang="en">Direct jar:</span> {links}</p>')
    return seal + actions + direct


def news_html(limit=5):
    """Les dernières lignes du changelog, en français et en anglais, avec leur version."""
    out = []
    for name, lang in (("CHANGELOG.fr.md", "fr"), ("CHANGELOG.md", "en")):
        path = os.path.join(ROOT, name)
        if not os.path.exists(path):
            continue
        items, version = [], None
        for line in open(path, encoding="utf-8").read().splitlines():
            m = re.match(r"^## (\S+)", line)
            if m:
                version = m.group(1)
            elif line.startswith("- ") and version:
                items.append(f'<li><span class="v">v{html.escape(version)}</span>{inline(line[2:])}</li>')
            if len(items) >= limit:
                break
        if items:
            out.append(f'<ul class="news" data-lang="{lang}">' + "".join(items) + "</ul>")
    return "".join(out)


def pact_circles():
    """Les deux cercles interdits, tels que le jeu les dessine (ForbiddenCircles.java)."""
    source = open(FORBIDDEN, encoding="utf-8").read()
    circles = {}
    for key, name in (("stone", "PHILOSOPHER_STONE"), ("human", "HUMAN_TRANSMUTATION")):
        m = re.search(name + r' = parse\("""(.*?)"""\);', source, re.S)
        if not m:
            raise ValueError(f"cercle {name} introuvable dans {FORBIDDEN}")
        circles[key] = svg(json.loads(m.group(1)))
    # « </ » dans un script fermerait la balise : on l'échappe.
    return json.dumps(circles).replace("</", "<\\/")


def with_pact(text, circles):
    return text.replace("/*pact-circles*/{}", circles, 1)


def main(out, releases_json=None):
    if os.path.exists(out):
        shutil.rmtree(out)
    shutil.copytree(SITE, out, ignore=shutil.ignore_patterns("build.py", "__pycache__"))
    head, footer, tail = template()
    # Le pacte alchimique (code Konami) montre les cercles interdits, sur toutes les pages.
    circles = pact_circles()
    tail = with_pact(tail, circles)
    data = []
    if releases_json and os.path.exists(releases_json):
        data = json.load(open(releases_json, encoding="utf-8"))
    brand = svg(treatise_circle(BRAND_CIRCLE), cls="chalk")
    # L'accueil : le menu, le cercle d'ouverture, les glyphes, la version actuelle et les nouveautés.
    index = os.path.join(out, "index.html")
    home = with_pact(open(index, encoding="utf-8").read(), circles)
    home = home.replace("<!--nav-->", nav("index.html"), 1).replace("<!--brand-circle-->", brand, 1)
    home = home.replace("<!--hero-circle-->", svg(treatise_circle(HERO_CIRCLE), cls="chalk"), 1)
    home = home.replace("<!--version-->", version_html(current_version(data)), 1)
    home = home.replace("<!--news-->", news_html(), 1)
    home = home.replace("<!--editor-circle-->", svg(treatise_circle(EDITOR_CIRCLE), cls="chalk"), 1)
    home = re.sub(r"<!--glyph:(\w+)-->", lambda m: glyph_svg(m.group(1)), home)
    open(index, "w", encoding="utf-8", newline="\n").write(home)
    pages = {
        "releases.html": ("Versions", "Releases", releases(data, True), releases(data, False)),
        "changelog.html": ("Journal des modifications", "Changelog",
                           changelog("CHANGELOG.fr.md", "Le journal apparaîtra avec la première version publiée."),
                           changelog("CHANGELOG.md", "The changelog will appear with the first published release.")),
        "installation.html": ("Installation", "Installation", INSTALL_FR, INSTALL_EN),
    }
    for name, (tfr, ten, bfr, ben) in pages.items():
        with open(os.path.join(out, name), "w", encoding="utf-8", newline="\n") as f:
            f.write(page(head, footer, tail, name, tfr, ten, bfr, ben, brand))
    print(f"site assemblé dans {out} ({len(data)} release(s))")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
