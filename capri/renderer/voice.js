/* Capri's voice: speaks her replies with the system's text-to-speech, tuned
 * bright and quick like a digital entity, with optional glitchy "data chirps"
 * layered on top via Web Audio. Settings live in localStorage "capri.voice". */
(function () {
  const synth = window.speechSynthesis;
  const PRESET = { pitch: 1.45, rate: 1.12, chirps: true, volume: 0.9 };
  const cfg = Object.assign(
    { enabled: true, voice: '', ...PRESET },
    JSON.parse(localStorage.getItem('capri.voice') || '{}')
  );
  const save = () => localStorage.setItem('capri.voice', JSON.stringify(cfg));
  const voices = () => (synth ? synth.getVoices() : []);

  // Default to a bright, young-sounding English voice if the user hasn't picked one.
  function pickDefault(list) {
    const prefs = [/aria/i, /jenny/i, /ana\b/i, /natural.*female/i, /samantha/i, /zira/i, /google us english/i, /female/i];
    for (const re of prefs) {
      const v = list.find((x) => re.test(x.name) && /^en/i.test(x.lang));
      if (v) return v;
    }
    return list.find((x) => /^en/i.test(x.lang)) || list[0];
  }

  // ---- glitchy data chirps ----
  let ctx = null;
  function chirp(strength = 1) {
    if (!cfg.chirps) return;
    try {
      ctx = ctx || new (window.AudioContext || window.webkitAudioContext)();
      const t = ctx.currentTime;
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = Math.random() < 0.5 ? 'square' : 'sawtooth';
      const f = 900 + Math.random() * 1800;
      o.frequency.setValueAtTime(f, t);
      o.frequency.exponentialRampToValueAtTime(f * (Math.random() < 0.5 ? 0.45 : 1.9), t + 0.05);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.03 * cfg.volume * strength, t + 0.005);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.06);
      o.connect(g).connect(ctx.destination);
      o.start(t);
      o.stop(t + 0.07);
    } catch { /* audio unavailable */ }
  }

  // Keep speech short and readable: no code blocks, links or markdown symbols.
  function clean(text) {
    return text
      .replace(/```[\s\S]*?```/g, ' (code) ')
      .replace(/`([^`]*)`/g, '$1')
      .replace(/https?:\/\/\S+/g, 'a link')
      .replace(/[*_#>~|]/g, '')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 420);
  }

  let current = 0;
  function speak(text, { onstart, onend } = {}) {
    const id = ++current;
    const done = () => { if (id === current && onend) onend(); };
    if (!cfg.enabled || !synth || !text) { done(); return; }
    synth.cancel();
    const u = new SpeechSynthesisUtterance(clean(text));
    const list = voices();
    const v = list.find((x) => x.name === cfg.voice) || pickDefault(list);
    if (v) u.voice = v;
    u.pitch = cfg.pitch;
    u.rate = cfg.rate;
    u.volume = cfg.volume;
    u.onstart = () => { chirp(1.3); if (id === current && onstart) onstart(); };
    u.onboundary = (e) => { if (e.name === 'word' && Math.random() < 0.3) chirp(); };
    u.onend = done;
    u.onerror = done;
    synth.speak(u);
    // safety net in case the engine never reports the end
    setTimeout(done, 4000 + clean(text).length * 110 / cfg.rate);
  }

  function stop() { current++; if (synth) synth.cancel(); }

  window.CapriVoice = {
    available: !!synth,
    PRESET,
    cfg,
    voices,
    speak,
    stop,
    chirp,
    set(patch) { Object.assign(cfg, patch); save(); },
    onVoicesChanged(cb) { if (synth) synth.addEventListener('voiceschanged', cb); },
  };
})();
