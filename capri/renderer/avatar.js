/* Capri avatar — Capri from Cyberpunk: Edgerunners 2, hand-drawn as SVG and
 * sitting on a window edge with her legs dangling over it.
 *
 * Geometry: viewBox 0 0 240 240 drawn 1:1 in a 240x240 box. The window edge
 * she sits on is y = 180; her shins (y 187-237) and fingers dangle over the front
 * of it, and her glittering pink mane streams out to the left.
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
      <stop offset="0" stop-color="#ff2563"/><stop offset=".45" stop-color="#ff2f7e"/><stop offset="1" stop-color="#ff69ad"/>
    </linearGradient>
    <linearGradient id="cpGreen" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#6dffa2"/><stop offset="1" stop-color="#1ff06a"/>
    </linearGradient>
    <pattern id="cpScan" width="6" height="3" patternUnits="userSpaceOnUse">
      <rect width="6" height="1" fill="#2b0a3d" opacity=".32"/>
    </pattern>
    <clipPath id="cpHairClip">
      <use href="#cpHA"/><use href="#cpHB"/><use href="#cpHC"/><use href="#cpHD"/><use href="#cpHE"/><use href="#cpHF"/>
    </clipPath>
  </defs>

  <ellipse class="cp-glow" cx="150" cy="181" rx="64" ry="4"/>

  <!-- shins dangling over the window edge, ankles crossed (they swing from the knee) -->
  <g class="cp-leg cp-leg-l">
    <path class="cp-ink" data-boil=".7" d="M132 187 L149 187 L151 213 L150 226 L132 237 L140 222 L137 206 Z"/>
    <path d="M136 205 L149 204" stroke="#7b3dff" stroke-width="1.2"/>
  </g>
  <g class="cp-leg cp-leg-r">
    <path class="cp-ink" data-boil=".7" d="M152 187 L170 187 L166 206 L164 222 L177 237 L156 229 L153 213 Z"/>
    <path class="cp-red" data-boil=".6" d="M155 196 L168 197 L166 209 L155 208 Z"/>
  </g>

  <g class="cp-upper">
    <!-- the mane: long windswept hot-pink hair, glittering with stars, streaming left -->
    <g class="cp-hair">
      <path id="cpHF" class="cp-strand" style="animation-delay:-1.6s" data-boil="1.8" fill="url(#cpHairFill)" d="M182 74 L192 26 L164 34 L156 0 L136 30 L100 12 L124 56 L138 70 Z"/>
      <path id="cpHD" class="cp-strand" style="animation-delay:-.9s" data-boil="1.6" fill="url(#cpHairFill)" d="M174 64 L198 38 L192 78 L210 98 L182 96 Z"/>
      <path id="cpHE" class="cp-strand" style="animation-delay:-1.3s" data-boil="1.6" fill="url(#cpHairFill)" d="M170 92 L198 118 L188 148 L202 178 L168 160 Z"/>
      <path id="cpHC" class="cp-strand" style="animation-delay:-.2s" data-boil="2" fill="url(#cpHairFill)" d="M152 108 L112 88 L40 112 L0 142 L58 126 L20 168 L92 134 L124 142 Z"/>
      <path id="cpHB" class="cp-strand" style="animation-delay:-.6s" data-boil="2" fill="url(#cpHairFill)" d="M162 94 L132 50 L70 46 L0 50 L52 64 L2 86 L70 82 L18 112 L102 96 Z"/>
      <path id="cpHA" class="cp-strand" style="animation-delay:-1.1s" data-boil="2" fill="url(#cpHairFill)" d="M172 66 L152 28 L104 20 L38 2 L84 28 L0 20 L58 42 L124 50 Z"/>
      <path class="cp-hair-dark" data-boil="1.6" d="M152 56 L20 28 L142 64 Z M144 82 L8 70 L138 88 Z M132 106 L30 142 L128 112 Z M184 104 L196 148 L178 112 Z"/>
      <path class="cp-hair-light" data-boil="1.4" d="M150 42 L60 18 L146 48 Z M142 70 L38 58 L140 74 Z M122 98 L48 120 L120 102 Z"/>
      <g class="cp-stars" clip-path="url(#cpHairClip)"></g>
      <rect x="0" y="0" width="240" height="190" fill="url(#cpScan)" clip-path="url(#cpHairClip)"/>
    </g>

    <!-- arms: left with the big green glove, right with a green stripe; hands grip the ledge -->
    <g class="cp-arm-l">
      <path class="cp-ink" data-boil=".7" d="M128 113 L137 119 L119 150 L108 146 Z"/>
      <path class="cp-lime" data-boil=".8" d="M106 142 L122 148 L118 177 L99 179 L101 160 Z"/>
      <path d="M108 150 L112 173" stroke="#07040c" stroke-width="1.6"/>
      <path class="cp-ink" data-boil=".6" d="M98 176 L121 176 L119 185 L116 193 L113 185 L109 195 L106 185 L101 191 Z"/>
    </g>
    <g class="cp-arm-r">
      <path class="cp-ink" data-boil=".7" d="M172 113 L163 119 L182 150 L193 146 Z"/>
      <path class="cp-ink" data-boil=".7" d="M181 145 L193 143 L199 176 L184 178 Z"/>
      <path class="cp-lime" d="M188 151 L192 150 L195 170 Z"/>
      <path class="cp-ink" data-boil=".6" d="M181 176 L201 176 L201 186 L197 194 L195 185 L191 193 L189 185 L184 191 Z"/>
    </g>

    <!-- torso: black bodysuit, red panels with black circuit notches -->
    <path class="cp-ink" data-boil=".7" d="M134 111 L166 111 L173 130 L164 150 L171 180 L129 180 L136 150 L127 130 Z"/>
    <path class="cp-red" data-boil=".7" d="M139 119 L165 117 L160 138 L145 134 Z"/>
    <path d="M146 123 L152 123 L152 128 L157 128 M158 121 L158 131 L162 131" stroke="#07040c" stroke-width="1.4" fill="none"/>
    <path class="cp-red" data-boil=".7" d="M133 158 L157 155 L151 177 L131 178 Z"/>
    <path d="M138 163 L146 163 L146 170 M149 160 L149 167" stroke="#07040c" stroke-width="1.4" fill="none"/>
    <path class="cp-lime" data-boil=".6" d="M156 145 L167 149 L158 160 Z"/>
    <circle class="cp-led" cx="140" cy="146" r="2.6" fill="#39ff7a"/>
    <!-- white angular jacket panels with violet rims -->
    <path class="cp-coat" data-boil=".7" d="M130 113 L116 131 L120 167 L128 177 L126 141 L137 120 Z"/>
    <path class="cp-coat" data-boil=".7" d="M170 113 L186 131 L182 165 L174 177 L176 141 L163 120 Z"/>
    <path d="M121 140 L121 160" stroke="#39ff7a" stroke-width="2"/>
    <!-- thighs resting on the ledge, knees at the edge -->
    <path class="cp-ink" data-boil=".6" d="M133 164 L150 167 L150 190 L130 190 Z"/>
    <path class="cp-lime" data-boil=".5" d="M133 166 L150 169 L150 180 L132 180 Z"/>
    <path class="cp-ink" data-boil=".6" d="M151 167 L168 164 L172 190 L152 190 Z"/>
    <path d="M131 185 L150 185 M152 185 L171 185" stroke="#7b3dff" stroke-width="1"/>
    <!-- neck -->
    <path class="cp-void" d="M146 102 L156 102 L155 114 L147 114 Z"/>

    <g class="cp-head">
      <!-- tall black spikes -->
      <path class="cp-ink cp-crown" data-boil="1.2" d="M126 84 L98 66 L126 74 L120 16 L140 66 L150 4 L158 64 L196 14 L174 70 L208 66 L180 84 Z"/>
      <!-- black head + lower face -->
      <path class="cp-void" data-boil=".5" d="M119 70 L185 64 L182 97 L168 113 L150 118 L133 112 L118 95 Z"/>
      <!-- green mask plates around each eye -->
      <path class="cp-plate" data-boil=".5" d="M122 72 L148 80 L148 104 L131 106 L121 92 Z"/>
      <path class="cp-plate" data-boil=".5" d="M152 80 L182 68 L180 94 L168 106 L152 104 Z"/>
      <!-- eyes: black ring, royal-blue iris, white core; little black ticks under them -->
      <g class="cp-eyes">
        <circle cx="136" cy="91" r="9.4" fill="#07040c"/>
        <circle cx="166" cy="88" r="10.6" fill="#07040c"/>
        <circle cx="136" cy="91" r="7.9" fill="none" stroke="#39ff7a" stroke-width="1"/>
        <circle cx="166" cy="88" r="9" fill="none" stroke="#39ff7a" stroke-width="1"/>
        <g class="cp-pupils">
          <circle cx="136" cy="91" r="6.6" fill="#1e5bff"/>
          <circle cx="166" cy="88" r="7.6" fill="#1e5bff"/>
          <circle cx="136" cy="91" r="2.8" fill="#fff"/>
          <circle cx="166" cy="88" r="3.3" fill="#fff"/>
        </g>
        <path d="M127 101 L125 106 M145 100 L147 104 M157 99 L155 104 M175 97 L178 101" stroke="#07040c" stroke-width="1.5"/>
      </g>
      <path class="cp-sleepy" d="M128 92 L136 96 L144 92 M157 89 L166 93 L175 89"/>
      <path class="cp-happy" d="M128 96 L136 86 L144 96 M157 93 L166 82 L175 93"/>
      <!-- mouth: a thin white zigzag... -->
      <path class="cp-mouth-shut" data-boil=".4" d="M139 110 L142 107 L145 111 L148 107 L151 111 L154 106 L157 110 L160 106 L163 109"/>
      <!-- ...that opens into a big white triangular grin -->
      <g class="cp-mouth-open">
        <path d="M136 106 L168 102 L151 120 Z" fill="#fff"/>
        <path d="M139 107 L145 111 L150 106 L155 110 L161 105 L166 104" stroke="#07040c" stroke-width="1.1" fill="none"/>
      </g>
    </g>
  </g>

  <!-- floating glitch shards -->
  <g class="cp-shards">
    <path class="cp-shard" style="animation-delay:0s" d="M18 194 L30 184 L26 200 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-1.2s" d="M212 30 L226 22 L218 40 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-2.1s" d="M222 136 L232 124 L230 142 Z" fill="#39ff7a"/>
    <path class="cp-shard" style="animation-delay:-.6s" d="M30 70 L44 62 L38 78 Z" fill="#a24dff"/>
    <path class="cp-shard" style="animation-delay:-2.8s" d="M88 188 L98 180 L97 194 Z" fill="#39ff7a"/>
    <path class="cp-shard" style="animation-delay:-1.7s" d="M206 196 L216 188 L214 202 Z" fill="#a24dff"/>
  </g>

  <!-- thought shards / zzz -->
  <g class="cp-think">
    <path d="M190 52 L196 44 L195 56 Z"/><path d="M202 40 L211 30 L208 46 Z"/><path d="M216 26 L226 14 L223 32 Z"/>
  </g>
  <g class="cp-zzz"><text x="188" y="54">z</text><text x="200" y="40">z</text><text x="214" y="26">Z</text></g>
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
    for (let i = 0; i < 170; i++) {
      const x = (Math.random() * 212).toFixed(1), y = (Math.random() * 180).toFixed(1);
      const delay = `animation-delay:-${(Math.random() * 2).toFixed(2)}s`;
      if (i % 8 === 0) {
        const r = 1.8 + Math.random() * 1.6, q = r * 0.26;
        html += `<path class="cp-star cp-star-big" style="${delay}" transform="translate(${x} ${y})" d="M0 ${-r} L${q} ${-q} L${r} 0 L${q} ${q} L0 ${r} L${-q} ${q} L${-r} 0 L${-q} ${-q} Z"/>`;
      } else {
        html += `<circle class="cp-star" style="${delay}" cx="${x}" cy="${y}" r="${(0.5 + Math.random() * 0.9).toFixed(2)}"/>`;
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
      const ex = (r.left - cr.left) + r.width * 0.63;
      const ey = (r.top - cr.top) + r.height * 0.37;
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
