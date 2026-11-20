/*
 * Le rituel du pacte alchimique : ce qui se joue à l'écran quand on tape le code Konami.
 *
 * L'écran s'assombrit, le cercle de la transmutation humaine se trace trait après trait en
 * tournant, des éclairs rouges crépitent autour, l'œil de la Vérité s'ouvre au centre, puis un
 * éclair blanc emporte tout et le site réapparaît passé au rouge. Un clic ou Échap abrège.
 *
 * Partagé tel quel par le site (site/build.py le publie à la racine) et par l'éditeur de cercles
 * (web/scripts/sync-data.mjs le recopie dans web/public). Autonome : il porte son propre style.
 *
 *   window.fmabPactRitual({ primitives, title, motto, onSealed })
 *     primitives : le cercle à tracer, au format des dessins du mod (grille de 0 à 32)
 *     onSealed   : appelé au plus fort de l'éclair, pour passer la page au rouge
 */
(function () {
  "use strict";

  var NS = "http://www.w3.org/2000/svg";
  var RED = "#ff4a30";
  var STYLE_ID = "fmab-ritual-style";
  var running = null;

  var CSS = [
    ".fmab-ritual{position:fixed;inset:0;z-index:2147483000;display:grid;place-items:center;overflow:hidden;cursor:pointer;",
    "background:radial-gradient(circle at 50% 50%,#3a0806 0%,#140203 45%,#000 100%);animation:fr-in .45s ease-out both}",
    ".fmab-ritual.fr-out{animation:fr-out .7s ease-in forwards}",
    ".fr-stage{position:relative;width:min(64vmin,580px);aspect-ratio:1;margin-top:-10vmin}",
    ".fr-stage.fr-shake{animation:fr-shake .09s linear 7}",
    ".fr-stage svg{position:absolute;inset:0;width:100%;height:100%;overflow:visible}",
    ".fr-circle{fill:none;stroke:" + RED + ";stroke-width:.2;stroke-linecap:round;stroke-linejoin:round;",
    "filter:drop-shadow(0 0 .25px #ffd2b0) drop-shadow(0 0 1.2px " + RED + ") drop-shadow(0 0 3px rgba(255,40,20,.6));",
    "transform-origin:16px 16px;animation:fr-turn 3.2s cubic-bezier(.2,.7,.2,1) both}",
    ".fr-circle>*{stroke-dasharray:1.05;stroke-dashoffset:1.05;animation:fr-draw .55s cubic-bezier(.6,.05,.3,1) forwards;",
    "animation-delay:calc(.25s + var(--i) * 28ms)}",
    ".fr-circle .fr-dot{fill:" + RED + ";stroke:none;opacity:0;animation:fr-appear .3s forwards;animation-delay:calc(.25s + var(--i) * 28ms)}",
    ".fr-bolts{fill:none;stroke-linecap:round;stroke-linejoin:round}",
    ".fr-bolts .fr-glow{stroke:" + RED + ";stroke-width:.55;opacity:.55;filter:blur(.35px)}",
    ".fr-bolts .fr-core{stroke:#fff1e0;stroke-width:.13}",
    ".fr-eye{transform-origin:16px 16px;transform:scaleY(0);animation:fr-open .5s cubic-bezier(.3,1.5,.5,1) 1.35s forwards}",
    ".fr-eye .fr-white{fill:#f6e6d0;stroke:#2a0504;stroke-width:.18}",
    ".fr-eye .fr-iris{fill:#b3160b;stroke:#ff6a3c;stroke-width:.08}",
    ".fr-eye .fr-pupil{fill:#120101}",
    ".fr-eye g{animation:fr-look 1.1s ease-in-out 1.9s both}",
    ".fr-text{position:absolute;left:50%;top:calc(100% + 2vmin);transform:translateX(-50%);width:90vw;text-align:center;",
    "color:#f2dcc8;pointer-events:none}",
    ".fr-title{margin:0;font-family:var(--display,'IM Fell English SC',Georgia,serif);font-weight:400;",
    "font-size:clamp(1.6rem,5vmin,3rem);letter-spacing:.5em;opacity:0;text-shadow:0 0 18px rgba(255,74,48,.8);",
    "animation:fr-title 1.2s ease-out 1.55s forwards}",
    ".fr-motto{margin:.6em 0 0;font-family:var(--body,'EB Garamond',Georgia,serif);font-style:italic;color:#d79a84;",
    "font-size:clamp(.95rem,2.4vmin,1.25rem);opacity:0;animation:fr-appear .8s ease-out 1.9s forwards}",
    ".fr-flash{position:absolute;inset:0;pointer-events:none;opacity:0;",
    "background:radial-gradient(circle at 50% 50%,#fff 0%,#fff3e0 25%,#ff6a3c 55%,rgba(120,10,10,0) 80%)}",
    ".fr-flash.fr-go{animation:fr-flash .9s ease-out forwards}",
    "@keyframes fr-in{from{opacity:0}to{opacity:1}}",
    "@keyframes fr-out{to{opacity:0}}",
    "@keyframes fr-draw{to{stroke-dashoffset:0}}",
    "@keyframes fr-appear{to{opacity:1}}",
    "@keyframes fr-turn{from{transform:rotate(-140deg) scale(.6);opacity:.2}to{transform:rotate(0) scale(1);opacity:1}}",
    "@keyframes fr-open{to{transform:scaleY(1)}}",
    "@keyframes fr-look{0%,100%{transform:translateX(0)}30%{transform:translateX(-.9px)}65%{transform:translateX(.9px)}}",
    "@keyframes fr-title{from{opacity:0;letter-spacing:.9em;filter:blur(6px)}to{opacity:1;letter-spacing:.18em;filter:blur(0)}}",
    "@keyframes fr-flash{0%{opacity:0;transform:scale(.3)}25%{opacity:1;transform:scale(1.6)}100%{opacity:0;transform:scale(2.4)}}",
    "@keyframes fr-shake{0%{transform:translate(0,0)}25%{transform:translate(-4px,2px)}50%{transform:translate(3px,-3px)}",
    "75%{transform:translate(-2px,-2px)}100%{transform:translate(0,0)}}",
    "@media (prefers-reduced-motion:reduce){.fmab-ritual *{animation-duration:.01s!important;animation-delay:0s!important}}"
  ].join("");

  function el(name, attrs, parent) {
    var node = document.createElementNS(NS, name);
    for (var k in attrs) node.setAttribute(k, attrs[k]);
    if (parent) parent.appendChild(node);
    return node;
  }

  /** Un dessin du mod en SVG, comme site/build.py : chaque trait se trace l'un après l'autre. */
  function drawing(primitives, group) {
    primitives.forEach(function (p, i) {
      var node;
      if (p.type === "circle") {
        node = el("circle", { cx: p.center[0], cy: p.center[1], r: p.radius });
      } else if (p.type === "polygon" || p.type === "polyline") {
        node = el(p.type, { points: p.points.map(function (q) { return q[0] + "," + q[1]; }).join(" ") });
      } else if (p.type === "line") {
        node = el("line", { x1: p.from[0], y1: p.from[1], x2: p.to[0], y2: p.to[1] });
      } else if (p.type === "arc") {
        var a0 = p.start_deg * Math.PI / 180, a1 = (p.start_deg + p.sweep_deg) * Math.PI / 180, r = p.radius;
        node = el("path", {
          d: "M" + (p.center[0] + r * Math.cos(a0)) + " " + (p.center[1] + r * Math.sin(a0)) +
            " A" + r + " " + r + " 0 " + (Math.abs(p.sweep_deg) > 180 ? 1 : 0) + " " + (p.sweep_deg > 0 ? 1 : 0) + " " +
            (p.center[0] + r * Math.cos(a1)) + " " + (p.center[1] + r * Math.sin(a1))
        });
      } else if (p.type === "dot") {
        node = el("circle", { cx: p.at[0], cy: p.at[1], r: 0.35, "class": "fr-dot" });
      } else {
        return;
      }
      node.setAttribute("pathLength", "1");
      node.style.setProperty("--i", String(i));
      group.appendChild(node);
    });
  }

  /** Un éclair : la droite de a à b brisée par déplacements successifs du milieu. */
  function bolt(a, b, roughness, depth) {
    if (depth === 0) return [a, b];
    var mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2;
    var dx = b[0] - a[0], dy = b[1] - a[1], len = Math.sqrt(dx * dx + dy * dy) || 1;
    var off = (Math.random() - 0.5) * roughness * len;
    var mid = [mx - dy / len * off, my + dx / len * off];
    var left = bolt(a, mid, roughness, depth - 1);
    return left.slice(0, -1).concat(bolt(mid, b, roughness, depth - 1));
  }

  function onRing(radius) {
    var t = Math.random() * Math.PI * 2;
    return [16 + Math.cos(t) * radius, 16 + Math.sin(t) * radius];
  }

  /** Une volée d'éclairs : du bord vers l'intérieur, avec quelques branches. */
  function strike(group) {
    while (group.firstChild) group.removeChild(group.firstChild);
    var count = 2 + Math.floor(Math.random() * 3);
    for (var n = 0; n < count; n++) {
      var from = onRing(15 + Math.random() * 3), to = onRing(4 + Math.random() * 9);
      var paths = [bolt(from, to, 0.45, 5)];
      var trunk = paths[0];
      if (Math.random() < 0.7) {
        var fork = trunk[Math.floor(trunk.length * (0.3 + Math.random() * 0.4))];
        paths.push(bolt(fork, onRing(9 + Math.random() * 8), 0.5, 4));
      }
      paths.forEach(function (pts) {
        var d = pts.map(function (q) { return q[0].toFixed(2) + "," + q[1].toFixed(2); }).join(" ");
        el("polyline", { points: d, "class": "fr-glow" }, group);
        el("polyline", { points: d, "class": "fr-core" }, group);
      });
    }
  }

  /** L'œil de la Vérité, fermé, qui s'ouvre au centre du cercle. */
  function eye(svg) {
    var g = el("g", { "class": "fr-eye" }, svg);
    el("path", { d: "M11.2 16 Q16 11.6 20.8 16 Q16 20.4 11.2 16 Z", "class": "fr-white" }, g);
    var look = el("g", {}, g);
    el("circle", { cx: 16, cy: 16, r: 1.75, "class": "fr-iris" }, look);
    el("ellipse", { cx: 16, cy: 16, rx: 0.42, ry: 1.35, "class": "fr-pupil" }, look);
  }

  function ritual(options) {
    if (running) return;
    var done = false, timers = [], boltTimer = 0;
    if (!document.getElementById(STYLE_ID)) {
      var style = document.createElement("style");
      style.id = STYLE_ID;
      style.textContent = CSS;
      document.head.appendChild(style);
    }
    var reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    var overlay = document.createElement("div");
    overlay.className = "fmab-ritual";
    overlay.setAttribute("role", "presentation");
    var stage = document.createElement("div");
    stage.className = "fr-stage";
    overlay.appendChild(stage);

    var svg = el("svg", { viewBox: "0 0 32 32", "aria-hidden": "true" }, stage);
    drawing(options.primitives || [], el("g", { "class": "fr-circle" }, svg));
    var bolts = el("g", { "class": "fr-bolts" }, svg);
    eye(svg);

    var text = document.createElement("div");
    text.className = "fr-text";
    var title = document.createElement("p");
    title.className = "fr-title";
    title.textContent = options.title || "";
    var motto = document.createElement("p");
    motto.className = "fr-motto";
    motto.textContent = options.motto || "";
    text.appendChild(title);
    text.appendChild(motto);
    stage.appendChild(text);

    var flash = document.createElement("div");
    flash.className = "fr-flash";
    overlay.appendChild(flash);
    document.body.appendChild(overlay);

    function later(fn, ms) { timers.push(setTimeout(fn, reduced ? Math.min(ms, 400) : ms)); }

    function seal() {
      if (done) return;
      done = true;
      clearInterval(boltTimer);
      while (bolts.firstChild) bolts.removeChild(bolts.firstChild);
      if (options.onSealed) options.onSealed();
      overlay.classList.add("fr-out");
      setTimeout(finish, 700);
    }

    function finish() {
      timers.forEach(clearTimeout);
      clearInterval(boltTimer);
      document.removeEventListener("keydown", onKey, true);
      if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
      running = null;
    }

    function onKey(e) {
      if (e.key === "Escape" || e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        seal();
      }
    }

    overlay.addEventListener("click", seal);
    document.addEventListener("keydown", onKey, true);

    if (!reduced) {
      // Les éclairs crépitent pendant que le cercle se trace ; l'écran tremble à leur plus fort.
      later(function () {
        strike(bolts);
        boltTimer = setInterval(function () { strike(bolts); }, 85);
      }, 650);
      later(function () { stage.classList.add("fr-shake"); }, 900);
      later(function () { clearInterval(boltTimer); while (bolts.firstChild) bolts.removeChild(bolts.firstChild); }, 1700);
    }
    later(function () { flash.classList.add("fr-go"); }, 2550);
    later(seal, 2800);
    running = { seal: seal };
  }

  window.fmabPactRitual = ritual;
})();
