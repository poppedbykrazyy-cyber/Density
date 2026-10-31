/* Custom skins: play your own Capri clip (GIF / APNG / WebP / PNG sprite sheet /
 * WebM / MP4) in place of the drawn avatar, for an exact look.
 *
 *  - two clips: "idle" (always) and an optional "talk" clip used while she speaks
 *  - sprite sheets: a single image split into cols x rows frames played at fps
 *  - background removal: auto (keys out the colour in the clip's corners) or a
 *    picked colour, with tolerance — so a clip on a solid backdrop becomes a cut-out
 *
 * Stored in IndexedDB (no size squeeze); other windows are told to reload via a
 * localStorage "capri.skin.rev" bump (the perch window listens for it).
 */
(function () {
  const DB = 'capri', STORE = 'kv', KEY = 'skin';
  const MAX_FRAMES = 360;

  // ---------------- storage ----------------
  function open() {
    return new Promise((res, rej) => {
      const r = indexedDB.open(DB, 1);
      r.onupgradeneeded = () => r.result.createObjectStore(STORE);
      r.onsuccess = () => res(r.result);
      r.onerror = () => rej(r.error);
    });
  }
  async function get() {
    try {
      const db = await open();
      return await new Promise((res) => {
        const q = db.transaction(STORE).objectStore(STORE).get(KEY);
        q.onsuccess = () => res(q.result || null);
        q.onerror = () => res(null);
      });
    } catch { return null; }
  }
  async function put(value) {
    const db = await open();
    await new Promise((res, rej) => {
      const tx = db.transaction(STORE, 'readwrite');
      if (value) tx.objectStore(STORE).put(value, KEY); else tx.objectStore(STORE).delete(KEY);
      tx.oncomplete = res;
      tx.onerror = () => rej(tx.error);
    });
    localStorage.setItem('capri.skin.rev', String(Date.now()));
  }
  const defaults = () => ({ idle: null, talk: null, sheet: { cols: 1, rows: 1, fps: 12 }, key: { mode: 'off', color: '#00ff00', tol: 70, soft: 40 }, scale: 1 });

  // ---------------- frame sources ----------------
  // Each source returns frame(t) -> { img, sx, sy, sw, sh } (or null while loading).
  async function makeSource(clip, sheet) {
    const { blob, type } = clip;
    if (type.startsWith('video/')) {
      const v = document.createElement('video');
      Object.assign(v, { muted: true, loop: true, playsInline: true, autoplay: true, src: URL.createObjectURL(blob) });
      v.play().catch(() => {});
      return {
        frame: () => (v.readyState >= 2 ? { img: v, sx: 0, sy: 0, sw: v.videoWidth, sh: v.videoHeight } : null),
        stop: () => { v.pause(); URL.revokeObjectURL(v.src); },
      };
    }
    let frames = null; // [{ bmp, dur }]
    if ('ImageDecoder' in window && /gif|webp|png|apng/.test(type)) {
      try {
        const dec = new ImageDecoder({ data: await blob.arrayBuffer(), type });
        await dec.tracks.ready;
        const n = Math.min(MAX_FRAMES, dec.tracks.selectedTrack.frameCount || 1);
        frames = [];
        for (let i = 0; i < n; i++) {
          const { image } = await dec.decode({ frameIndex: i });
          frames.push({ bmp: await createImageBitmap(image), dur: (image.duration || 100000) / 1000 });
          image.close();
        }
        dec.close();
      } catch { frames = null; }
    }
    if (!frames) frames = [{ bmp: await createImageBitmap(blob), dur: 100 }];

    // animated image: frames by their own durations
    if (frames.length > 1) {
      const total = frames.reduce((a, f) => a + f.dur, 0);
      return {
        frame: (t) => {
          let x = t % total;
          for (const f of frames) { if ((x -= f.dur) < 0) return { img: f.bmp, sx: 0, sy: 0, sw: f.bmp.width, sh: f.bmp.height }; }
          const f = frames[0];
          return { img: f.bmp, sx: 0, sy: 0, sw: f.bmp.width, sh: f.bmp.height };
        },
        stop: () => frames.forEach((f) => f.bmp.close()),
      };
    }
    // single image, optionally a sprite sheet
    const bmp = frames[0].bmp;
    const cols = Math.max(1, sheet.cols | 0), rows = Math.max(1, sheet.rows | 0);
    const fw = bmp.width / cols, fh = bmp.height / rows, count = cols * rows;
    return {
      frame: (t) => {
        const i = Math.floor((t / 1000) * (sheet.fps || 12)) % count;
        return { img: bmp, sx: (i % cols) * fw, sy: Math.floor(i / cols) * fh, sw: fw, sh: fh };
      },
      stop: () => bmp.close(),
    };
  }

  // ---------------- renderer ----------------
  const hex = (h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));

  // Draws the current clip into `canvas` (square art box; the window edge is at 3/4 height),
  // removing the background colour if asked.
  async function createRenderer(canvas, skin) {
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    const sources = { idle: await makeSource(skin.idle, skin.sheet) };
    if (skin.talk) sources.talk = await makeSource(skin.talk, skin.sheet);
    let clip = 'idle', raf = 0, last = 0, autoKey = null;
    const t0 = performance.now();
    const key = skin.key || { mode: 'off' };
    const S = canvas.width, BASE = Math.round(S * 0.815); // her clip's bottom sits a little over the edge (0.75)

    function draw(now) {
      raf = requestAnimationFrame(draw);
      if (now - last < 1000 / 30) return; // ~30 fps is plenty
      last = now;
      const src = sources[clip] || sources.idle;
      const f = src.frame(now - t0);
      if (!f || !f.sw) return;
      // fit inside the box above (and slightly over) the edge, bottom-centred
      const boxW = S, boxH = BASE * (skin.scale || 1);
      const k = Math.min(boxW / f.sw, boxH / f.sh);
      const w = f.sw * k, h = f.sh * k;
      ctx.clearRect(0, 0, S, S);
      ctx.drawImage(f.img, f.sx, f.sy, f.sw, f.sh, (S - w) / 2, BASE - h, w, h);
      if (key.mode === 'off') return;

      const x0 = Math.max(0, Math.floor((S - w) / 2)), y0 = Math.max(0, Math.floor(BASE - h));
      const iw = Math.min(S - x0, Math.ceil(w)), ih = Math.min(S - y0, Math.ceil(h));
      if (iw <= 0 || ih <= 0) return;
      const img = ctx.getImageData(x0, y0, iw, ih);
      const d = img.data;
      let kc;
      if (key.mode === 'auto') {
        if (!autoKey) { // average of the four corners of the clip
          const px = (x, y) => { const o = (y * iw + x) * 4; return [d[o], d[o + 1], d[o + 2]]; };
          const cs = [px(1, 1), px(iw - 2, 1), px(1, ih - 2), px(iw - 2, ih - 2)];
          autoKey = [0, 1, 2].map((c) => cs.reduce((a, p) => a + p[c], 0) / 4);
        }
        kc = autoKey;
      } else kc = hex(key.color || '#00ff00');
      const tol = key.tol ?? 70, soft = Math.max(1, key.soft ?? 40);
      for (let i = 0; i < d.length; i += 4) {
        const dist = Math.hypot(d[i] - kc[0], d[i + 1] - kc[1], d[i + 2] - kc[2]);
        if (dist < tol) d[i + 3] = 0;
        else if (dist < tol + soft) d[i + 3] = Math.round(d[i + 3] * (dist - tol) / soft);
      }
      ctx.putImageData(img, x0, y0);
    }
    raf = requestAnimationFrame(draw);
    return {
      setClip(name) { clip = sources[name] ? name : 'idle'; },
      stop() { cancelAnimationFrame(raf); Object.values(sources).forEach((s) => s.stop()); },
    };
  }

  window.CapriSkin = { get, put, defaults, createRenderer };
})();
