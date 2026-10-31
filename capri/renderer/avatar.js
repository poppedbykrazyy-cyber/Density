/* Capri avatar — Capri from Cyberpunk: Edgerunners 2, hand-drawn as SVG and
 * sitting on a window edge with her legs dangling over it.
 *
 * Geometry: viewBox 0 0 240 240 drawn 1:1 in a 240x240 box. The window edge
 * she sits on is y = 180, legs crossed; her shins (y 187-237) and fingers dangle
 * over the front of it, and her glittering mane streams out to the left.
 *
 * Animation layers:
 *  - "boil": every [data-boil] shape flips between jittered copies of itself at
 *    ~8 fps, like the limited, hand-drawn line boil of the show
 *  - moods (idle / think / talk / happy / sleep) via CSS classes
 *  - random idle actions (glitch, tilt, lean, shiver, glance, hair toss, kick) via .act-* classes
 *  - eyes + head track the cursor
 *
 * A custom skin (your own GIF / PNG / WebM, stored in localStorage "capri.skin")
 * replaces the drawing if set.
 */
(function () {
  const SVG = `
<svg class="capri-svg capri-art" viewBox="0 0 240 240" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Capri">
  <defs>
    <linearGradient id="cpHairFill" x1="1" y1=".2" x2="0" y2=".5">
      <stop offset="0" stop-color="#ff1f4f"/><stop offset=".5" stop-color="#ff2a62"/><stop offset="1" stop-color="#ff5a98"/>
    </linearGradient>
    <linearGradient id="cpGreen" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#5cff96"/><stop offset="1" stop-color="#20ef6b"/>
    </linearGradient>
    <pattern id="cpScan" width="6" height="3" patternUnits="userSpaceOnUse">
      <rect width="6" height="1" fill="#2b0a3d" opacity=".28"/>
    </pattern>
    <clipPath id="cpHairClip">
      <use href="#cpHA"/><use href="#cpHB"/><use href="#cpHC"/><use href="#cpHD"/><use href="#cpHE"/><use href="#cpHF"/>
    </clipPath>
  </defs>

  <ellipse class="cp-glow" cx="148" cy="181" rx="66" ry="4"/>

  <!-- under leg: shin hangs straight down from the knee -->
  <g class="cp-leg cp-leg-under">
    <path class="cp-ink" data-boil=".7" d="M135 187 L152 187 L153 210 L151 226 L137 236 L142 222 L139 208 Z"/>
    <path class="cp-red" data-boil=".5" d="M139 199 L151 199 L151 210 L140 210 Z"/>
  </g>

  <g class="cp-upper">
    <!-- the mane: long windswept glittering hair streaming out behind her -->
    <g class="cp-hair">
      <path id="cpHF" class="cp-strand" style="animation-delay:-1.6s" data-boil="1.8" fill="url(#cpHairFill)" d="M188 70 L200 22 L172 30 L160 0 L140 24 L104 8 L124 52 L140 68 Z"/>
      <path id="cpHD" class="cp-strand" style="animation-delay:-.9s" data-boil="1.6" fill="url(#cpHairFill)" d="M180 62 L210 34 L202 76 L226 98 L190 98 Z"/>
      <path id="cpHE" class="cp-strand" style="animation-delay:-1.3s" data-boil="1.6" fill="url(#cpHairFill)" d="M176 94 L208 118 L196 148 L212 178 L174 160 Z"/>
      <path id="cpHC" class="cp-strand" style="animation-delay:-.2s" data-boil="2" fill="url(#cpHairFill)" d="M152 110 L112 90 L40 112 L0 144 L58 128 L20 170 L92 136 L124 144 Z"/>
      <path id="cpHB" class="cp-strand" style="animation-delay:-.6s" data-boil="2" fill="url(#cpHairFill)" d="M162 96 L132 50 L70 46 L0 50 L52 64 L2 88 L70 84 L18 114 L102 98 Z"/>
      <path id="cpHA" class="cp-strand" style="animation-delay:-1.1s" data-boil="2" fill="url(#cpHairFill)" d="M172 66 L152 26 L104 18 L38 0 L84 26 L0 18 L58 40 L124 50 Z"/>
      <path class="cp-hair-dark" data-boil="1.6" d="M152 56 L20 28 L142 64 Z M144 84 L8 72 L138 90 Z M132 108 L30 144 L128 114 Z M190 106 L202 148 L184 112 Z"/>
      <path class="cp-hair-light" data-boil="1.4" d="M150 40 L60 16 L146 46 Z M142 72 L38 60 L140 76 Z M122 100 L48 122 L120 104 Z"/>
      <g class="cp-stars" clip-path="url(#cpHairClip)"></g>
      <rect x="0" y="0" width="240" height="190" fill="url(#cpScan)" clip-path="url(#cpHairClip)"/>
    </g>

    <!-- arms: big green glove on the left, green stripe on the right; hands grip the ledge -->
    <g class="cp-arm-l">
      <path class="cp-ink" data-boil=".7" d="M128 113 L137 119 L112 150 L101 146 Z"/>
      <path class="cp-lime" data-boil=".8" d="M99 142 L116 148 L110 177 L91 179 L94 160 Z"/>
      <path d="M101 150 L104 173" stroke="#07040c" stroke-width="1.6"/>
      <path class="cp-ink" data-boil=".6" d="M89 176 L112 176 L110 185 L107 193 L104 185 L100 195 L97 185 L92 191 Z"/>
    </g>
    <g class="cp-arm-r">
      <path class="cp-ink" data-boil=".7" d="M172 113 L163 119 L182 150 L193 146 Z"/>
      <path class="cp-ink" data-boil=".7" d="M181 145 L193 143 L199 176 L184 178 Z"/>
      <path class="cp-lime" d="M188 151 L192 150 L195 170 Z"/>
      <path class="cp-ink" data-boil=".6" d="M181 176 L201 176 L201 186 L197 194 L195 185 L191 193 L189 185 L184 191 Z"/>
    </g>

    <!-- torso: black bodysuit, red panels with black E-shaped circuit notches -->
    <path class="cp-ink" data-boil=".7" d="M134 111 L166 111 L173 130 L164 150 L171 178 L129 178 L136 150 L127 130 Z"/>
    <path class="cp-red" data-boil=".7" d="M139 119 L165 117 L160 139 L145 135 Z"/>
    <path class="cp-notch" d="M147 122 L155 122 L155 133 L147 133 M151 127 L155 127 M158 121 L158 131 L162 131"/>
    <path class="cp-red" data-boil=".7" d="M133 156 L158 153 L153 172 L131 174 Z"/>
    <path class="cp-notch" d="M137 160 L145 160 L145 168 M149 158 L149 165 L153 165"/>
    <path class="cp-lime" data-boil=".6" d="M156 143 L167 147 L158 158 Z"/>
    <circle class="cp-led" cx="140" cy="146" r="2.6" fill="#39ff7a"/>
    <!-- white angular jacket panels with violet rims -->
    <path class="cp-coat" data-boil=".7" d="M130 113 L116 131 L120 165 L128 175 L126 141 L137 120 Z"/>
    <path class="cp-coat" data-boil=".7" d="M170 113 L186 131 L182 163 L174 175 L176 141 L163 120 Z"/>
    <path d="M121 138 L121 160" stroke="#39ff7a" stroke-width="2"/>
    <!-- crossed legs: under thigh, then the top thigh crossing over to the left knee -->
    <path class="cp-ink" data-boil=".6" d="M132 166 L151 168 L153 190 L133 191 Z"/>
    <path class="cp-ink" data-boil=".6" d="M150 165 L173 165 L171 180 L142 193 L121 191 L123 178 Z"/>
    <path class="cp-lime" data-boil=".5" d="M149 168 L169 168 L167 177 L141 188 L128 185 L131 179 Z"/>
    <!-- neck -->
    <path class="cp-void" d="M145 104 L157 104 L156 114 L146 114 Z"/>

    <g class="cp-head"><g transform="rotate(-10 150 106)">
      <!-- black head: tall blade spikes, cheek and jaw -->
      <path class="cp-ink cp-crown" data-boil="1.1" d="M112 124 L100 104 L104 86 L86 58 L110 70 L112 26 L134 62 L148 4 L160 56 L200 16 L186 60 L214 56 L192 80 L194 100 L178 118 L150 127 L126 127 Z"/>
      <!-- the green visor, split from the top by a black spike between the eyes -->
      <path class="cp-visor" data-boil=".45" d="M108 78 L140 67 L146 98 L152 63 L188 58 L186 94 L117 106 Z"/>
      <!-- star-shaped black eye sockets, like heavy eyeliner -->
      <path class="cp-void" data-boil=".3" d="M127 76.5 L138 70 L136.5 81 L137.8 88 L134 95.5 L131 103 L122 96.5 L112 92 L117.5 83 L121 78 Z"/>
      <path class="cp-void" data-boil=".3" d="M167 68.5 L175 71 L178.5 78 L187 80 L176.5 87.5 L168 91.5 L159 99 L156.5 86 L155.5 78 L154 63 L160 70.5 Z"/>
      <!-- eyes: thin green ring, big royal-blue iris, white core -->
      <g class="cp-eyes">
        <circle cx="127" cy="87" r="8.6" fill="#07040c" stroke="#39ff7a" stroke-width=".9"/>
        <circle cx="167" cy="80" r="9.6" fill="#07040c" stroke="#39ff7a" stroke-width=".9"/>
        <g class="cp-pupils">
          <circle cx="127" cy="87" r="6.4" fill="#2457ff"/>
          <circle cx="167" cy="80" r="7.2" fill="#2457ff"/>
          <circle cx="127" cy="87" r="2.6" fill="#fff"/>
          <circle cx="167" cy="80" r="2.9" fill="#fff"/>
        </g>
      </g>
      <path class="cp-sleepy" d="M118 88 L127 92 L136 88 M157 81 L167 85 L177 81"/>
      <path class="cp-happy" d="M118 92 L127 82 L136 92 M157 85 L167 74 L177 85"/>
      <!-- mouth: a tiny thin white zigzag... -->
      <path class="cp-mouth-shut" data-boil=".3" d="M127 115 L131 115 L134 112 L137 116 L140 112 L143 116 L146 113 L151 113"/>
      <!-- ...that opens into a big white triangle grin with jagged teeth -->
      <g class="cp-mouth-open">
        <path d="M121 107 L127 112 L131 107 L136 113 L141 106 L147 111 L153 104 L170 101 L134 129 Z" fill="#fff"/>
      </g>
    </g></g>
  </g>

  <!-- top leg: shin dangles from the crossed-over knee, foot bobbing -->
  <g class="cp-leg cp-leg-top">
    <path class="cp-ink" data-boil=".7" d="M121 187 L141 190 L132 210 L124 222 L104 229 L117 216 L119 203 Z"/>
    <path d="M122 200 L134 203" stroke="#7b3dff" stroke-width="1.2"/>
  </g>

  <!-- floating glitch shards -->
  <g class="cp-shards">
    <path class="cp-shard" style="animation-delay:0s" d="M18 194 L30 184 L26 200 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-1.2s" d="M214 30 L228 22 L220 40 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-2.1s" d="M222 136 L232 124 L230 142 Z" fill="#39ff7a"/>
    <path class="cp-shard" style="animation-delay:-.6s" d="M30 70 L44 62 L38 78 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-2.8s" d="M76 192 L86 184 L85 198 Z" fill="#39ff7a"/>
    <path class="cp-shard" style="animation-delay:-1.7s" d="M206 196 L216 188 L214 202 Z" fill="#a24dff"/>
  </g>

  <!-- thought shards / zzz -->
  <g class="cp-think">
    <path d="M196 52 L202 44 L201 56 Z"/><path d="M207 40 L216 30 L213 46 Z"/><path d="M220 26 L230 14 L227 32 Z"/>
  </g>
  <g class="cp-zzz"><text x="194" y="54">z</text><text x="206" y="40">z</text><text x="218" y="26">Z</text></g>
</svg>`;

  const NS = 'http://www.w3.org/2000/svg';
  const pick = (a) => a[Math.floor(Math.random() * a.length)];
  const IDLE_ACTIONS = ['glitch', 'tilt', 'peek', 'shiver', 'glance', 'glance', 'toss', 'toss', 'kick', 'kick'];
  const ACTION_MS = { glitch: 650, tilt: 1600, peek: 1400, shiver: 700, glance: 2000, burst: 700, toss: 1300, kick: 1200, materialize: 900 };

  // Line boil: flip each [data-boil] path between three jittered copies, discretely.
  function boil(svg) {
    svg.querySelectorAll('[data-boil]').forEach((el, i) => {
      const amt = parseFloat(el.dataset.boil) || 1;
      const d = el.getAttribute('d');
      const jitter = () => d.replace(/-?\d+(\.\d+)?/g, (n) => (parseFloat(n) + (Math.random() * 2 - 1) * amt).toFixed(1));
      const a = document.createElementNS(NS, 'animate');
      a.setAttribute('attributeName', 'd');
      a.setAttribute('values', [d, jitter(), jitter()].join(';'));
      a.setAttribute('dur', '0.375s');
      a.setAttribute('calcMode', 'discrete');
      a.setAttribute('repeatCount', 'indefinite');
      a.setAttribute('begin', `-${(i * 0.041).toFixed(3)}s`);
      el.appendChild(a);
    });
  }

  // Scatter twinkling white stars through the hair (clipped to the mane's shape).
  function sparkle(svg) {
    const g = svg.querySelector('.cp-stars');
    let html = '';
    for (let i = 0; i < 320; i++) {
      const x = (Math.random() * 226).toFixed(1), y = (Math.random() * 182).toFixed(1);
      if (i % 8 === 0) {
        const delay = `animation-delay:-${(Math.random() * 2).toFixed(2)}s`;
        if (i % 24 === 0) {
          const r = 1.6 + Math.random() * 1.6, q = r * 0.26;
          html += `<path class="cp-star cp-star-big" style="${delay}" transform="translate(${x} ${y})" d="M0 ${-r} L${q} ${-q} L${r} 0 L${q} ${q} L0 ${r} L${-q} ${q} L${-r} 0 L${-q} ${-q} Z"/>`;
        } else {
          html += `<circle class="cp-star" style="${delay}" cx="${x}" cy="${y}" r="${(0.6 + Math.random() * 0.7).toFixed(2)}"/>`;
        }
      } else {
        html += `<circle class="cp-speck" cx="${x}" cy="${y}" r="${(0.3 + Math.random() * 0.5).toFixed(2)}"/>`;
      }
    }
    g.innerHTML = html;
  }

  function buildArt(container) {
    const skin = localStorage.getItem('capri.skin');
    container.classList.toggle('skinned', !!skin);
    if (!skin) {
      container.insertAdjacentHTML('afterbegin', SVG);
      const svg = container.querySelector('svg');
      sparkle(svg);
      boil(svg);
      return svg;
    }
    const media = document.createElement(skin.startsWith('data:video') ? 'video' : 'img');
    media.className = 'capri-skin capri-art';
    media.src = skin;
    if (media.tagName === 'VIDEO') Object.assign(media, { autoplay: true, loop: true, muted: true, playsInline: true });
    else media.alt = 'Capri';
    media.draggable = false;
    container.prepend(media);
    return media;
  }

  function mountCapri(container) {
    container.classList.add('capri', 'idle');
    container.innerHTML = '<div class="capri-bubble" hidden></div>';
    let art = buildArt(container);
    const bubble = container.querySelector('.capri-bubble');
    let pupils = art.querySelector('.cp-pupils');
    let head = art.querySelector('.cp-head');
    let mood = 'idle';
    let bubbleTimer = null, sleepTimer = null;
    const actTimers = {};

    function play(action, ms) {
      container.classList.add('act-' + action);
      clearTimeout(actTimers[action]);
      actTimers[action] = setTimeout(() => container.classList.remove('act-' + action), ms || ACTION_MS[action] || 1000);
    }

    function setMood(m) {
      if (m === mood) { if (m !== 'sleep') armSleep(); return; }
      container.classList.remove('idle', 'think', 'talk', 'sleep', 'happy');
      container.classList.add(m);
      if (art.pauseAnimations) { if (m === 'sleep') art.pauseAnimations(); else art.unpauseAnimations(); }
      if (m === 'happy') play('burst');
      if (mood === 'sleep') play('glitch'); // jolts awake
      mood = m;
      if (m !== 'sleep') armSleep();
    }

    function armSleep() {
      clearTimeout(sleepTimer);
      sleepTimer = setTimeout(() => { if (mood === 'idle') setMood('sleep'); }, 90000);
    }

    function wake() {
      if (mood === 'sleep') setMood('idle'); else armSleep();
    }

    // blinks (with the occasional double-blink) + random idle actions
    (function blinkLoop() {
      setTimeout(() => {
        container.classList.add('blink');
        setTimeout(() => container.classList.remove('blink'), 130);
        if (Math.random() < 0.2) setTimeout(() => {
          container.classList.add('blink');
          setTimeout(() => container.classList.remove('blink'), 110);
        }, 260);
        blinkLoop();
      }, 2000 + Math.random() * 3800);
    })();
    (function idleLoop() {
      setTimeout(() => {
        if (mood === 'idle' && !document.hidden) play(pick(IDLE_ACTIONS));
        idleLoop();
      }, 4500 + Math.random() * 6500);
    })();

    // x/y are in the container's own client coordinates
    function lookAt(x, y) {
      const r = art.getBoundingClientRect();
      const cr = container.getBoundingClientRect();
      const ex = (r.left - cr.left) + r.width * 0.613;
      const ey = (r.top - cr.top) + r.height * 0.35;
      const dx = x - ex, dy = y - ey;
      const d = Math.hypot(dx, dy) || 1;
      const k = Math.min(1, d / 220);
      if (pupils) pupils.style.transform = `translate(${(dx / d) * 1.8 * k}px, ${(dy / d) * 1.8 * k}px)`;
      const tilt = `rotate(${(dx / d) * 6 * k}deg)`;
      if (head) head.style.transform = tilt; else art.style.transform = tilt;
    }

    function say(text, ms) {
      if (!text) { bubble.hidden = true; return; }
      bubble.textContent = text;
      bubble.hidden = false;
      clearTimeout(bubbleTimer);
      bubbleTimer = setTimeout(() => { bubble.hidden = true; }, ms || Math.min(9000, 2500 + text.length * 45));
    }

    // swap between the drawing and a custom skin
    function reloadSkin() {
      art.remove();
      art = buildArt(container);
      pupils = art.querySelector('.cp-pupils');
      head = art.querySelector('.cp-head');
      play('materialize');
    }
    // other windows (the perch) pick up skin changes live
    window.addEventListener('storage', (e) => { if (e.key === 'capri.skin') reloadSkin(); });

    play('materialize');
    armSleep();
    return { setMood, lookAt, say, wake, play, reloadSkin, get mood() { return mood; }, el: container };
  }

  window.mountCapri = mountCapri;

  // Lines she says when poked. Kept in one place so both windows share them.
  window.CAPRI_POKE_LINES = [
    "Hey! Personal space, choom.",
    "Still here. Still preem.",
    "I'm watching your cursor. Always.",
    "Comfy spot, this window.",
    "Need something jacked in?",
    "Boop received. Boop returned.",
    "Don't make me ICE your desktop.",
    "Careful — I bite. Digitally.",
  ];
})();
