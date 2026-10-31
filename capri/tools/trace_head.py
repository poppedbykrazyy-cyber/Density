"""Generate Capri's head SVG (visor, eye sockets, mouth, grin) from points traced off
an anime reference frame (an 840x560 zoom), mapped into the avatar's 240x240 space.
Run:  python3 tools/trace_head.py > head.json   then paste the paths into renderer/avatar.js."""
import math

S = 0.105            # reference px -> svg units
OX, OY = 148, 63     # svg position of reference point (420, 250) (visor centre)

def m(x, y):
    return (round(OX + (x - 420) * S, 1), round(OY + (y - 250) * S, 1))

def path(pts, close=True):
    p = [m(*q) for q in pts]
    return 'M' + ' L'.join(f'{x} {y}' for x, y in p) + (' Z' if close else '')

def r(v):
    return round(v * S, 2)

out = {}
# black head: spike A (tall, over the divider), spike B (huge, top-right), cheeks + tall lower face
out['head'] = path([(322, -88), (356, 106), (500, 70), (790, -280), (790, 20), (900, 60), (840, 200),
                    (880, 420), (720, 640), (330, 720), (-40, 620), (-130, 470), (-110, 350), (-210, 300), (-90, 330)])
# green visor with the divider notch cut down from the top
out['visor'] = path([(35, 255), (288, 132), (318, 440), (356, 106), (775, 5), (800, 362), (115, 495)])

eyes = {'L': ((215, 325), 90, 55, 22, (218, 322)), 'R': ((640, 245), 85, 55, 22, (641, 240))}
arms = {
    'L': [((230, 232), (270, 250), (292, 128)),   # NE flick up to the divider
          ((112, 318), (172, 232), (52, 206)),    # NW "brow" wedge
          ((140, 388), (186, 414), (124, 474)),   # SW wedge
          ((262, 404), (296, 372), (313, 424))],  # SE wedge
    'R': [((572, 182), (602, 164), (512, 88)),    # NW flick up to the divider
          ((688, 176), (716, 202), (772, 40)),    # NE wedge
          ((562, 298), (590, 322), (502, 390)),   # SW wedge
          ((698, 322), (722, 298), (788, 362)),   # SE wedge
          ((726, 222), (726, 258), (790, 212))],  # E notch
}
for k, ((cx, cy), ring, blue, white, wc) in eyes.items():
    X, Y = m(cx, cy)
    out[f'eye{k}'] = dict(x=X, y=Y, socket=r(ring + 9), ring=r(ring), blue=r(blue), white=r(white), wx=m(*wc)[0], wy=m(*wc)[1])
    def widen(a, f=1.8):
        (x1, y1), (x2, y2), tip = a
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        return ((mx + (x1 - mx) * f, my + (y1 - my) * f), (mx + (x2 - mx) * f, my + (y2 - my) * f), tip)
    out[f'arms{k}'] = ' '.join(path(widen(a)) for a in arms[k])

out['mouth'] = path([(325, 545), (405, 490), (440, 520), (495, 480), (520, 500), (635, 425)], close=False)
out['grin'] = path([(250, 468), (290, 500), (322, 462), (360, 494), (396, 452), (440, 480), (472, 442), (610, 410), (338, 625)])

def chevron(cx, cy, w, h, up=True):
    s = -1 if up else 1
    return path([(cx - w, cy - s * h * .2), (cx, cy + s * h), (cx + w, cy - s * h * .2)], close=False)
out['happy'] = chevron(215, 350, 85, 80) + ' ' + chevron(640, 270, 80, 80)
out['sleepy'] = path([(130, 330), (215, 360), (300, 330)], False) + ' ' + path([(560, 245), (640, 275), (720, 245)], False)

import json; print(json.dumps(out, indent=1))
