"""Build Capri's SVG (renderer/capri-art.js) from parameters.

Run from the capri/ folder:   python3 tools/build_capri.py

Canvas: viewBox 0 0 360 360. She sits on a window edge at y = 270, legs crossed,
shins dangling below it. The head is traced point-by-point from a zoomed anime
frame (an 840x560 crop) and mapped in with HEAD_SCALE; the hair is a fan of
seeded, procedurally generated blade strands so it can be tuned in one place.
Class names match renderer/avatar.css (animations) and avatar.js (behaviour).
"""
import math
import pathlib
import random

W = H = 360
EDGE = 270
BODY_X = 208                      # body centre line

# ---------------------------------------------------------------- helpers
def fmt(v):
    return f'{v:.1f}'.rstrip('0').rstrip('.')

def poly(pts, close=True):
    return 'M' + ' L'.join(f'{fmt(x)} {fmt(y)}' for x, y in pts) + (' Z' if close else '')

def mirror(pts):
    return [(2 * BODY_X - x, y) for x, y in pts]

# ---------------------------------------------------------------- head (traced)
HEAD_SCALE = 0.11
HEAD_X, HEAD_Y = BODY_X, 104      # where the reference visor centre (420, 250) lands

def hm(x, y):
    return (HEAD_X + (x - 420) * HEAD_SCALE, HEAD_Y + (y - 250) * HEAD_SCALE)

def hpoly(pts, close=True):
    return poly([hm(*p) for p in pts], close)

def hr(v):
    return v * HEAD_SCALE

HEAD = hpoly([(322, -88), (356, 106), (500, 70), (790, -280), (790, 20), (900, 60), (840, 200),
              (880, 420), (720, 640), (330, 720), (-40, 620), (-130, 470), (-110, 350), (-210, 300), (-90, 330)])
VISOR = hpoly([(35, 255), (288, 132), (318, 440), (356, 106), (775, 5), (800, 362), (115, 495)])
EYES = {'L': ((215, 325), 90, 55, 22, (218, 322)), 'R': ((640, 245), 85, 55, 22, (641, 240))}
ARMS = {
    'L': [((230, 232), (270, 250), (292, 128)), ((112, 318), (172, 232), (52, 206)),
          ((140, 388), (186, 414), (124, 474)), ((262, 404), (296, 372), (313, 424))],
    'R': [((572, 182), (602, 164), (512, 88)), ((688, 176), (716, 202), (772, 40)),
          ((562, 298), (590, 322), (502, 390)), ((698, 322), (722, 298), (788, 362)),
          ((726, 222), (726, 258), (790, 212))],
}

def widen(a, f=1.8):
    (x1, y1), (x2, y2), tip = a
    mx, my = (x1 + x2) / 2, (y1 + y2) / 2
    return ((mx + (x1 - mx) * f, my + (y1 - my) * f), (mx + (x2 - mx) * f, my + (y2 - my) * f), tip)

MOUTH = hpoly([(325, 545), (405, 490), (440, 520), (495, 480), (520, 500), (635, 425)], close=False)
GRIN = hpoly([(250, 468), (290, 500), (322, 462), (360, 494), (396, 452), (440, 480), (472, 442), (610, 410), (338, 625)])

def chevron(cx, cy, w, h):
    return hpoly([(cx - w, cy + h * .2), (cx, cy - h), (cx + w, cy + h * .2)], close=False)

HAPPY = chevron(215, 350, 85, 80) + ' ' + chevron(640, 270, 80, 80)
SLEEPY = hpoly([(130, 330), (215, 360), (300, 330)], False) + ' ' + hpoly([(560, 245), (640, 275), (720, 245)], False)

def eye_svg():
    sockets, rings, pupils = [], [], []
    for k, ((cx, cy), ring, blue, white, wc) in EYES.items():
        x, y = hm(cx, cy)
        wx, wy = hm(*wc)
        sockets.append(f'<path class="cp-void" data-boil=".25" d="{" ".join(hpoly(widen(a)) for a in ARMS[k])}"/>')
        rings.append(f'<circle cx="{fmt(x)}" cy="{fmt(y)}" r="{fmt(hr(ring + 9))}" fill="#07040c"/>'
                     f'<circle cx="{fmt(x)}" cy="{fmt(y)}" r="{fmt(hr(ring))}" fill="none" stroke="#39ff7a" stroke-width=".9"/>')
        pupils.append((f'<circle cx="{fmt(x)}" cy="{fmt(y)}" r="{fmt(hr(blue))}" fill="#2457ff"/>',
                       f'<circle cx="{fmt(wx)}" cy="{fmt(wy)}" r="{fmt(hr(white))}" fill="#fff"/>'))
    return ('\n        '.join(sockets + rings) +
            '\n        <g class="cp-pupils">' + ''.join(p[0] for p in pupils) + ''.join(p[1] for p in pupils) + '</g>')

# ---------------------------------------------------------------- hair (procedural blades)
rng = random.Random(2077)

def blade(ox, oy, ang, length, width, kink=0.18):
    """A long jagged hair blade from (ox, oy) pointing at `ang` degrees."""
    a = math.radians(ang)
    dx, dy = math.cos(a), math.sin(a)
    px, py = -dy, dx
    j = lambda s: rng.uniform(-s, s)
    pts = [
        (ox + px * width / 2, oy + py * width / 2),
        (ox + dx * length * .42 + px * width * (.42 + j(kink)), oy + dy * length * .42 + py * width * (.42 + j(kink))),
        (ox + dx * length * .55 + px * width * .55, oy + dy * length * .55 + py * width * .55),   # side notch
        (ox + dx * length * .62 + px * width * .2, oy + dy * length * .62 + py * width * .2),
        (ox + dx * length, oy + dy * length),                                                       # tip
        (ox + dx * length * .7 - px * width * (.18 + j(kink)), oy + dy * length * .7 - py * width * (.18 + j(kink))),
        (ox + dx * length * .35 - px * width * .45, oy + dy * length * .35 - py * width * .45),
        (ox - px * width / 2, oy - py * width / 2),
    ]
    return poly(pts)

# (origin x, origin y, angle, length, base width) — most stream left, some rise, a few flick right
HAIR_BLADES = [
    (236, 70, 198, 236, 44), (232, 92, 186, 250, 52), (226, 116, 176, 236, 50), (222, 138, 166, 220, 46),
    (218, 160, 156, 196, 40), (214, 178, 146, 160, 34), (240, 58, 214, 196, 40), (244, 50, 232, 150, 36),
    (232, 48, 252, 104, 30), (250, 52, 284, 84, 28), (258, 64, 310, 70, 28), (262, 90, 336, 50, 24),
    (204, 60, 228, 120, 30), (210, 100, 196, 210, 40),
]
DARK = [(230, 96, 184, 200, 12), (226, 128, 170, 196, 12), (236, 66, 204, 180, 10),
        (220, 158, 155, 160, 10), (246, 58, 238, 120, 9), (264, 100, 330, 60, 9)]
LIGHT = [(232, 80, 192, 190, 10), (226, 112, 178, 214, 9), (238, 60, 222, 150, 8),
         (220, 146, 160, 170, 8), (256, 56, 270, 70, 7)]

def hair_svg():
    out, uses = [], []
    for i, (ox, oy, ang, ln, w) in enumerate(HAIR_BLADES):
        hid = f'cpH{i}'
        uses.append(f'<use href="#{hid}"/>')
        delay = f'-{(i * 0.37) % 2:.2f}s'
        out.append(f'<path id="{hid}" class="cp-strand" style="animation-delay:{delay}" data-boil="2" fill="url(#cpHairFill)" d="{blade(ox, oy, ang, ln, w)}"/>')
    dark = ' '.join(blade(*b, kink=.1) for b in DARK)
    light = ' '.join(blade(*b, kink=.1) for b in LIGHT)
    out.append(f'<path class="cp-hair-dark" data-boil="1.6" d="{dark}"/>')
    out.append(f'<path class="cp-hair-light" data-boil="1.4" d="{light}"/>')
    return '\n      '.join(out), ''.join(uses)

# front strands falling over the shoulders
FRONT = [blade(196, 150, 116, 74, 18, .1), blade(232, 150, 64, 70, 18, .1)]

# ---------------------------------------------------------------- body
TORSO = [(186, 166), (230, 166), (238, 192), (226, 222), (238, 262), (178, 262), (190, 222), (178, 192)]
CHEST = [(190, 178), (228, 175), (220, 206), (198, 200)]
CHEST_NOTCH = 'M201 182 L212 182 L212 197 L201 197 M206 189.5 L212 189.5 M217 181 L217 195 L223 195'
HIP = [(184, 236), (222, 231), (216, 258), (182, 261)]
HIP_NOTCH = 'M190 241 L201 241 L201 253 L190 253 M195 247 L201 247 M207 238 L207 249 L213 249'
WAIST_SHARD = [(220, 212), (235, 218), (222, 236)]
COAT_L = [(186, 162), (150, 170), (128, 196), (138, 222), (124, 250), (150, 244), (162, 268), (168, 228), (166, 200), (190, 178)]
COAT_STRIPE = [(141, 202), (147, 199), (151, 234), (145, 238)]
COAT_R_SHARD = [(262, 178), (282, 196), (268, 202)]

ARM_L_UPPER = [(170, 186), (180, 192), (160, 226), (148, 220)]
ARM_L_GLOVE = [(146, 214), (166, 222), (158, 264), (130, 266), (135, 240)]
HAND_L = [(126, 264), (161, 264), (158, 276), (154, 290), (150, 277), (145, 294), (141, 278), (136, 291), (131, 277)]
ARM_R_UPPER = [(246, 186), (236, 192), (258, 226), (270, 220)]
ARM_R_FORE = [(256, 218), (272, 214), (283, 264), (262, 266)]
ARM_R_STRIPE = [(266, 226), (272, 225), (278, 254)]
HAND_R = [(258, 264), (290, 264), (290, 279), (285, 291), (282, 278), (277, 290), (274, 278), (266, 288)]

THIGH_UNDER = [(182, 248), (210, 252), (212, 286), (180, 287)]
THIGH_TOP = [(204, 244), (242, 244), (238, 266), (196, 289), (164, 287), (168, 268)]
THIGH_PANEL = [(206, 249), (236, 249), (233, 262), (196, 282), (172, 278), (176, 269)]
SHIN_UNDER = [(184, 284), (211, 284), (213, 318), (210, 340), (190, 356), (198, 336), (193, 313)]
SHIN_UNDER_PATCH = [(191, 300), (209, 300), (209, 317), (192, 317)]
SHIN_TOP = [(164, 284), (196, 288), (183, 316), (171, 336), (140, 348), (159, 328), (161, 307)]

SHARDS = [((26, 300), (42, 288), (36, 308), '#a24dff', 0), ((318, 40), (336, 30), (326, 54), '#a24dff', -1.2),
          ((332, 200), (346, 184), (343, 210), '#39ff7a', -2.1), ((40, 110), (58, 100), (50, 122), '#a24dff', -.6),
          ((108, 300), (122, 290), (120, 310), '#39ff7a', -2.8), ((306, 300), (320, 290), (316, 312), '#a24dff', -1.7)]

# ---------------------------------------------------------------- assemble
def build():
    hair, hair_uses = hair_svg()
    shards = '\n    '.join(
        f'<path class="cp-shard" style="animation-delay:{d}s" d="{poly([a, b, c])}" fill="{col}"/>' for a, b, c, col, d in SHARDS)
    return f'''<svg class="capri-svg capri-art" viewBox="0 0 {W} {H}" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Capri">
  <defs>
    <linearGradient id="cpHairFill" x1="1" y1=".2" x2="0" y2=".55">
      <stop offset="0" stop-color="#ff1f4f"/><stop offset=".5" stop-color="#ff2a62"/><stop offset="1" stop-color="#ff5a98"/>
    </linearGradient>
    <linearGradient id="cpGreen" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#5cff96"/><stop offset="1" stop-color="#20ef6b"/>
    </linearGradient>
    <pattern id="cpScan" width="6" height="3" patternUnits="userSpaceOnUse"><rect width="6" height="1" fill="#2b0a3d" opacity=".14"/></pattern>
    <clipPath id="cpHairClip">{hair_uses}</clipPath>
    <clipPath id="cpAboveEdge"><rect x="-40" y="-40" width="{W + 80}" height="{EDGE + 40}"/></clipPath>
  </defs>

  <ellipse class="cp-glow" cx="{BODY_X - 4}" cy="{EDGE + 1}" rx="104" ry="5"/>

  <!-- under leg: shin hangs from the knee -->
  <g class="cp-leg cp-leg-under">
    <path class="cp-ink" data-boil=".8" d="{poly(SHIN_UNDER)}"/>
    <path class="cp-red" data-boil=".6" d="{poly(SHIN_UNDER_PATCH)}"/>
  </g>

  <g class="cp-upper">
    <!-- the mane: windswept glittering blades; tucked behind the window edge -->
    <g class="cp-hair" clip-path="url(#cpAboveEdge)">
      {hair}
      <g class="cp-stars" clip-path="url(#cpHairClip)" data-w="{W - 20}" data-h="{EDGE}"></g>
      <rect x="0" y="0" width="{W}" height="{EDGE}" fill="url(#cpScan)" clip-path="url(#cpHairClip)"/>
    </g>

    <!-- arms: green glove on the left, green lightning stripe on the right; hands grip the ledge -->
    <g class="cp-arm-l">
      <path class="cp-ink" data-boil=".8" d="{poly(ARM_L_UPPER)}"/>
      <path class="cp-lime" data-boil=".9" d="{poly(ARM_L_GLOVE)}"/>
      <path d="M142 226 L146 258" stroke="#07040c" stroke-width="2.2"/>
      <path class="cp-ink" data-boil=".7" d="{poly(HAND_L)}"/>
    </g>
    <g class="cp-arm-r">
      <path class="cp-ink" data-boil=".8" d="{poly(ARM_R_UPPER)}"/>
      <path class="cp-ink" data-boil=".8" d="{poly(ARM_R_FORE)}"/>
      <path class="cp-lime" d="{poly(ARM_R_STRIPE)}"/>
      <path class="cp-ink" data-boil=".7" d="{poly(HAND_R)}"/>
    </g>

    <!-- torso: slim black bodysuit, red panels with black E-shaped circuit notches -->
    <path class="cp-ink" data-boil=".8" d="{poly(TORSO)}"/>
    <path class="cp-red" data-boil=".8" d="{poly(CHEST)}"/>
    <path class="cp-notch" d="{CHEST_NOTCH}"/>
    <path class="cp-red" data-boil=".8" d="{poly(HIP)}"/>
    <path class="cp-notch" d="{HIP_NOTCH}"/>
    <path class="cp-lime" data-boil=".7" d="{poly(WAIST_SHARD)}"/>
    <circle class="cp-led" cx="194" cy="216" r="3.6" fill="#39ff7a"/>
    <!-- big white angular coat shards with glowing violet edges -->
    <path class="cp-coat" data-boil=".9" d="{poly(COAT_L)}"/>
    <path class="cp-coat" data-boil=".9" d="{poly(mirror(COAT_L))}"/>
    <path class="cp-lime" data-boil=".6" d="{poly(COAT_STRIPE)}"/>
    <path class="cp-red" data-boil=".7" d="{poly(COAT_R_SHARD)}"/>
    <!-- crossed legs: under thigh, then the top thigh crossing over to the left knee -->
    <path class="cp-ink" data-boil=".7" d="{poly(THIGH_UNDER)}"/>
    <path class="cp-ink" data-boil=".7" d="{poly(THIGH_TOP)}"/>
    <path class="cp-lime" data-boil=".6" d="{poly(THIGH_PANEL)}"/>
    <!-- neck -->
    <path class="cp-void" d="M199 150 L217 150 L215 170 L201 170 Z"/>
    <!-- strands falling over the shoulders -->
    <g class="cp-hair-front">
      <path data-boil="1.4" fill="url(#cpHairFill)" d="{FRONT[0]}"/>
      <path data-boil="1.4" fill="url(#cpHairFill)" d="{FRONT[1]}"/>
    </g>

    <!-- head: traced from the anime frames (tilt baked in) -->
    <g class="cp-head">
      <path class="cp-void cp-crown" data-boil=".9" d="{HEAD}"/>
      <path class="cp-visor" data-boil=".35" d="{VISOR}"/>
      <g class="cp-eyes">
        {eye_svg()}
      </g>
      <path class="cp-sleepy" d="{SLEEPY}"/>
      <path class="cp-happy" d="{HAPPY}"/>
      <path class="cp-mouth-shut" data-boil=".25" d="{MOUTH}"/>
      <g class="cp-mouth-open"><path d="{GRIN}" fill="#fff"/></g>
    </g>
  </g>

  <!-- top leg: shin dangles from the crossed-over knee, foot bobbing -->
  <g class="cp-leg cp-leg-top">
    <path class="cp-ink" data-boil=".8" d="{poly(SHIN_TOP)}"/>
    <path d="M166 306 L183 309" stroke="#7b3dff" stroke-width="1.4"/>
  </g>

  <g class="cp-shards">
    {shards}
  </g>

  <g class="cp-think"><path d="M286 64 L294 54 L293 70 Z"/><path d="M300 48 L312 34 L308 56 Z"/><path d="M318 28 L332 12 L328 38 Z"/></g>
  <g class="cp-zzz"><text x="284" y="68">z</text><text x="300" y="50">z</text><text x="318" y="30">Z</text></g>
</svg>'''


if __name__ == '__main__':
    root = pathlib.Path(__file__).resolve().parent.parent
    svg = build()
    js = ('/* GENERATED by tools/build_capri.py — edit that script, not this file. */\n'
          'window.CAPRI_SVG = `\n' + svg + '`;\n')
    (root / 'renderer' / 'capri-art.js').write_text(js)
    print('wrote renderer/capri-art.js', len(js), 'bytes')
