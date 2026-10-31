(function () {
  const $ = (id) => document.getElementById(id);
  const bridge = window.capriDesktop; // only present inside Electron (see preload.js)
  if (bridge) document.documentElement.classList.add('electron');

  const capri = mountCapri($('capri'));
  const log = $('log');
  const input = $('input');
  const sendBtn = $('send');

  // ---------------- settings ----------------
  const cfg = Object.assign(
    { url: 'http://127.0.0.1:11434', model: '', name: 'choom', offline: false },
    JSON.parse(localStorage.getItem('capri.cfg') || '{}')
  );
  const history = JSON.parse(localStorage.getItem('capri.history') || '[]');
  let activeModel = null;
  let busy = false;

  function saveCfg() { localStorage.setItem('capri.cfg', JSON.stringify(cfg)); }
  function saveHistory() { localStorage.setItem('capri.history', JSON.stringify(history.slice(-40))); }

  function fillSettings() {
    $('cfg-url').value = cfg.url;
    $('cfg-model').value = cfg.model;
    $('cfg-name').value = cfg.name === 'choom' ? '' : cfg.name;
    $('cfg-offline').checked = cfg.offline;
  }
  function readSettings() {
    cfg.url = $('cfg-url').value.trim() || 'http://127.0.0.1:11434';
    cfg.model = $('cfg-model').value.trim();
    cfg.name = $('cfg-name').value.trim() || 'choom';
    cfg.offline = $('cfg-offline').checked;
  }

  // ---------------- mood sync (chat window <-> perch window) ----------------
  function mood(m) { capri.setMood(m); if (bridge) bridge.mood(m); }

  // ---------------- chat log ----------------
  function addMsg(role, text, extra) {
    const el = document.createElement('div');
    el.className = 'msg msg-' + role + (extra ? ' ' + extra : '');
    if (role !== 'sys') {
      const who = document.createElement('div');
      who.className = 'who';
      who.textContent = role === 'capri' ? 'CAPRI //' : cfg.name.toUpperCase() + ' //';
      el.appendChild(who);
    }
    const body = document.createElement('div');
    body.className = 'body';
    body.textContent = text;
    el.appendChild(body);
    log.appendChild(el);
    log.scrollTop = log.scrollHeight;
    return body;
  }

  // ---------------- link status ----------------
  async function checkLink(verbose) {
    const dot = $('link-dot');
    dot.className = 'dot';
    $('link-status').textContent = 'LINK: SCANNING';
    if (cfg.offline) return setLink(false, 'OFFLINE MODE');
    try {
      const models = await CapriBrain.listModels(cfg.url);
      if (!models.length) {
        setLink(false, 'NO MODELS');
        if (verbose) addMsg('sys', 'Ollama is running but has no models. Run: ollama pull llama3.2');
        return;
      }
      activeModel = cfg.model && models.includes(cfg.model) ? cfg.model : cfg.model || CapriBrain.pickModel(models);
      setLink(true, 'ONLINE');
      if (verbose) addMsg('sys', 'Neural link established → ' + activeModel);
    } catch {
      setLink(false, 'LOCAL CORE');
      if (verbose) addMsg('sys', 'No Ollama at ' + cfg.url + ' — using built-in replies.');
    }
  }
  function setLink(on, label) {
    if (!on) activeModel = null;
    $('link-dot').className = 'dot ' + (on ? 'on' : 'off');
    $('link-status').textContent = 'LINK: ' + label;
    $('hud-model').textContent = 'MODEL: ' + (activeModel ? activeModel.split(':')[0].toUpperCase() : 'BUILT-IN');
    $('sync-bar').style.width = on ? '100%' : '35%';
  }

  // ---------------- commands ----------------
  const COMMANDS = {
    help: () => addMsg('sys',
      '/help  this list\n/clear  wipe chat memory\n/perch  let Capri sit on your screen\n/sleep  send her to sleep\n/model <name>  switch Ollama model\n/link  re-scan for Ollama'),
    clear: () => { history.length = 0; saveHistory(); log.innerHTML = ''; addMsg('sys', 'Memory wiped. Fresh start.'); },
    perch: () => bridge ? bridge.perch() : addMsg('sys', 'Perch mode needs the desktop app (npm start).'),
    sleep: () => mood('sleep'),
    model: (arg) => { cfg.model = arg; saveCfg(); checkLink(true); },
    link: () => checkLink(true),
  };

  // ---------------- send ----------------
  async function send(text) {
    text = text.trim();
    if (!text || busy) return;
    capri.wake();

    if (text.startsWith('/')) {
      const [cmd, ...rest] = text.slice(1).split(' ');
      (COMMANDS[cmd] || (() => addMsg('sys', 'Unknown command. Try /help')))(rest.join(' ').trim());
      return;
    }

    CapriVoice.stop();
    addMsg('user', text);
    history.push({ role: 'user', content: text });
    busy = true;
    sendBtn.disabled = true;
    mood('think');

    let reply = '';
    const body = addMsg('capri', '');
    body.classList.add('caret');

    try {
      if (!activeModel) throw Object.assign(new Error('offline'), { offline: true });
      await CapriBrain.streamChat(
        { url: cfg.url, model: activeModel, handle: cfg.name, history: history.slice(-20) },
        (tok) => {
          if (!reply) mood('talk');
          reply += tok;
          body.textContent = reply;
          log.scrollTop = log.scrollHeight;
        }
      );
      if (!reply) reply = '...';
    } catch (e) {
      if (!e.offline) {
        addMsg('sys', 'Link dropped: ' + e.message + ' — falling back to local core.', 'err');
        setLink(false, 'LOCAL CORE');
      }
      await new Promise((r) => setTimeout(r, 450 + Math.random() * 500));
      mood('talk');
      reply = CapriBrain.offlineReply(text, cfg.name);
      await typeOut(body, reply);
    }

    body.classList.remove('caret');
    body.textContent = reply;
    history.push({ role: 'assistant', content: reply });
    saveHistory();
    if (bridge) bridge.say(reply.length > 140 ? reply.slice(0, 137) + '...' : reply);

    if (CapriVoice.cfg.enabled && CapriVoice.available) {
      CapriVoice.speak(reply, { onstart: () => mood('talk'), onend: () => mood('idle') });
    } else {
      setTimeout(() => mood('idle'), 600);
    }
    busy = false;
    sendBtn.disabled = false;
    input.focus();
  }

  async function typeOut(el, text) {
    for (let i = 1; i <= text.length; i += 2) {
      el.textContent = text.slice(0, i);
      log.scrollTop = log.scrollHeight;
      await new Promise((r) => setTimeout(r, 14));
    }
  }

  // ---------------- wiring ----------------
  $('composer').addEventListener('submit', (e) => { e.preventDefault(); const t = input.value; input.value = ''; autosize(); send(t); });
  input.addEventListener('keydown', (e) => {
    capri.wake();
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); $('composer').requestSubmit(); }
  });
  function autosize() { input.style.height = 'auto'; input.style.height = Math.min(110, input.scrollHeight) + 'px'; }
  input.addEventListener('input', autosize);

  $('btn-settings').onclick = () => { fillSettings(); fillVoice(); fillSkin(); $('settings').hidden = !$('settings').hidden; };
  $('cfg-save').onclick = () => { readSettings(); saveCfg(); $('settings').hidden = true; checkLink(true); };
  $('cfg-test').onclick = () => { readSettings(); checkLink(true); };
  // ---------------- custom skin (exact look; see skin.js) ----------------
  let skin = null;
  let skinQueue = Promise.resolve();
  function skinStatus() {
    $('sk-status').textContent = skin.idle
      ? `Idle: ${skin.idle.name}${skin.talk ? ' · Talk: ' + skin.talk.name : ' · no talk clip (idle plays while she speaks)'}`
      : 'Using the drawn Capri. Load a clip of her for an exact look.';
  }
  async function fillSkin() {
    await skinQueue;
    skin = Object.assign(CapriSkin.defaults(), await CapriSkin.get());
    skinStatus();
    $('sk-cols').value = skin.sheet.cols; $('sk-rows').value = skin.sheet.rows; $('sk-fps').value = skin.sheet.fps;
    $('sk-key').value = skin.key.mode; $('sk-color').value = skin.key.color;
    $('sk-tol').value = skin.key.tol; $('sk-tol-val').textContent = skin.key.tol;
    $('sk-scale').value = skin.scale; $('sk-scale-val').textContent = Math.round(skin.scale * 100) + '%';
  }
  // saves run one at a time, each with a snapshot of the settings at that moment
  function saveSkin() {
    const snapshot = skin.idle ? { ...skin, sheet: { ...skin.sheet }, key: { ...skin.key } } : null;
    skinQueue = skinQueue.then(async () => {
      await CapriSkin.put(snapshot);
      await capri.reloadSkin();
    }).catch((e) => addMsg('sys', 'Skin error: ' + e.message, 'err'));
    skinStatus();
    return skinQueue;
  }
  for (const which of ['idle', 'talk']) {
    $('sk-' + which).onchange = async (e) => {
      const file = e.target.files[0];
      e.target.value = '';
      if (!file) return;
      if (file.size > 60 * 1024 * 1024) return addMsg('sys', 'That clip is over 60 MB — trim it first.', 'err');
      if (!skin) await fillSkin();
      skin[which] = { blob: file, type: file.type || 'image/png', name: file.name };
      if (which === 'talk' && !skin.idle) skin.idle = skin.talk;
      await saveSkin();
      addMsg('sys', `${which === 'idle' ? 'Idle' : 'Talk'} clip loaded: ${file.name}`);
    };
  }
  $('sk-reset').onclick = async () => { skin = CapriSkin.defaults(); await saveSkin(); await fillSkin(); addMsg('sys', 'Back to the drawn Capri.'); };
  for (const id of ['sk-cols', 'sk-rows', 'sk-fps', 'sk-key', 'sk-color', 'sk-tol', 'sk-scale']) {
    $(id).addEventListener('change', () => {
      if (!skin) return;
      skin.sheet = { cols: +$('sk-cols').value || 1, rows: +$('sk-rows').value || 1, fps: +$('sk-fps').value || 12 };
      skin.key = { ...skin.key, mode: $('sk-key').value, color: $('sk-color').value, tol: +$('sk-tol').value };
      skin.scale = +$('sk-scale').value;
      saveSkin();
    });
  }
  $('sk-tol').addEventListener('input', () => { $('sk-tol-val').textContent = $('sk-tol').value; });
  $('sk-scale').addEventListener('input', () => { $('sk-scale-val').textContent = Math.round($('sk-scale').value * 100) + '%'; });

  // ---------------- voice settings ----------------
  function fillVoice() {
    const v = CapriVoice.cfg;
    const sel = $('vc-voice');
    const list = CapriVoice.voices();
    sel.innerHTML = '<option value="">auto (bright English voice)</option>' +
      list.map((x) => `<option value="${x.name.replace(/"/g, '&quot;')}">${x.name.replace(/</g, '&lt;')} — ${x.lang}</option>`).join('');
    sel.value = list.some((x) => x.name === v.voice) ? v.voice : '';
    $('vc-on').checked = v.enabled;
    $('vc-chirps').checked = v.chirps;
    $('vc-pitch').value = v.pitch;
    $('vc-rate').value = v.rate;
    $('vc-pitch-val').textContent = (+v.pitch).toFixed(2);
    $('vc-rate-val').textContent = (+v.rate).toFixed(2);
  }
  const readVoice = () => CapriVoice.set({
    enabled: $('vc-on').checked,
    chirps: $('vc-chirps').checked,
    voice: $('vc-voice').value,
    pitch: +$('vc-pitch').value,
    rate: +$('vc-rate').value,
  });
  for (const id of ['vc-on', 'vc-chirps', 'vc-voice', 'vc-pitch', 'vc-rate']) {
    $(id).addEventListener('input', () => { readVoice(); fillVoice(); });
  }
  $('vc-test').onclick = () => {
    readVoice();
    CapriVoice.speak(`Hey ${cfg.name}. Capri online. Your desktop's mine now — try not to mind the glitches.`,
      { onstart: () => mood('talk'), onend: () => mood('idle') });
  };
  $('vc-preset').onclick = () => { CapriVoice.set({ ...CapriVoice.PRESET, voice: '' }); fillVoice(); $('vc-test').click(); };
  CapriVoice.onVoicesChanged(() => { if (!$('settings').hidden) fillVoice(); });
  if (!CapriVoice.available) $('voice-box').classList.add('unavailable');

  $('btn-perch').onclick = () => bridge && bridge.perch();
  $('btn-min').onclick = () => bridge && bridge.minimize();
  $('btn-close').onclick = () => bridge && bridge.quit();

  // poke her
  function poke() {
    if (capri.mood === 'sleep') { capri.setMood('idle'); capri.say('Mm... five more minutes...'); return; }
    capri.setMood('happy');
    if (Math.random() < 0.35) capri.play('glitch');
    const line = CAPRI_POKE_LINES[Math.floor(Math.random() * CAPRI_POKE_LINES.length)];
    capri.say(line);
    CapriVoice.speak(line);
    setTimeout(() => { if (capri.mood === 'happy') capri.setMood('idle'); }, 900);
  }

  // She sits near the window's top-right corner; drag her to scoot along the edge.
  const seatEl = $('capri');
  const SEAT_MIN = 0, SEAT_MAX = 160; // her box's left edge, in px from the window's left
  let seatX = 140;
  function setSeat(x) {
    seatX = Math.round(Math.min(SEAT_MAX, Math.max(SEAT_MIN, x)));
    seatEl.style.left = seatX + 'px';
    seatEl.classList.toggle('bubble-flip', seatX < 50); // keep her speech bubble on-screen
  }
  setSeat(+(localStorage.getItem('capri.seatX') || 140));
  if (bridge) bridge.seat(seatX);
  let scoot = null;
  seatEl.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    scoot = { x: e.clientX, from: seatX, moved: false };
    seatEl.setPointerCapture(e.pointerId);
  });
  seatEl.addEventListener('pointermove', (e) => {
    if (!scoot) return;
    const dx = e.clientX - scoot.x;
    if (!scoot.moved && Math.abs(dx) > 4) {
      scoot.moved = true;
      seatEl.classList.add('dragging');
      capri.wake();
      capri.say(['Scooting over~', 'Hey, careful!', 'Wheee—', 'Fine, fine, I\'ll move.'][Math.floor(Math.random() * 4)], 1500);
    }
    if (scoot.moved) setSeat(scoot.from + dx);
  });
  seatEl.addEventListener('pointerup', () => {
    if (!scoot) return;
    const moved = scoot.moved;
    scoot = null;
    seatEl.classList.remove('dragging');
    if (!moved) return poke();
    localStorage.setItem('capri.seatX', seatX);
    if (bridge) bridge.seat(seatX);
    capri.play('kick');
  });

  // eyes follow the cursor — globally in Electron, inside the page in a browser
  function lookFromPage(x, y) {
    const r = $('capri').getBoundingClientRect();
    capri.lookAt(x - r.left, y - r.top);
  }
  if (bridge) {
    bridge.onCursor(({ x, y }) => lookFromPage(x, y));
    // Let clicks fall through the transparent parts of the window to whatever is behind it.
    let ignoring = false;
    document.addEventListener('mousemove', (e) => {
      const over = e.target.closest('.panel, #capri');
      if (!over !== ignoring) { ignoring = !over; bridge.setIgnore(ignoring); }
    });
    bridge.onMood((m) => capri.setMood(m));
  } else {
    document.addEventListener('mousemove', (e) => { lookFromPage(e.clientX, e.clientY); capri.wake(); });
  }

  // Night City clock
  function tick() {
    $('hud-clock').textContent = 'NC ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
    $('ram').textContent = (busy ? 9 + Math.floor(Math.random() * 3) : 12) + '/12';
  }
  setInterval(tick, 1000);
  tick();

  // ---------------- boot ----------------
  for (const m of history.slice(-12)) addMsg(m.role === 'user' ? 'user' : 'capri', m.content);
  if (!history.length) {
    addMsg('capri', `Hey, ${cfg.name}. I'm Capri — I'll be living on your windows from now on. Click me if you get lonely. Type /help for tricks.`);
  }
  setTimeout(() => capri.say('Jacked in. Hi, ' + cfg.name + '!'), 500);
  checkLink(false);
  input.focus();
})();
