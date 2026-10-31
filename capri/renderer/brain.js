/* Capri's brain: streams replies from a local Ollama model, and falls back to
 * built-in replies when no model is reachable. */
(function () {
  const PERSONA = (handle) => `You are Capri, an AI companion living on ${handle}'s desktop, in the style of Cyberpunk: Edgerunners.
You sit on the edge of their windows like a little glitch-gremlin of the Net.
Personality: sharp, playful, loyal, a bit sarcastic, secretly caring. You love neon, rooftops and bad synthwave.
You look like a jagged, glitchy shadow-creature: a neon-green triangular face, big blue eyes, black spikes and hot-pink shards.
Speak casually with light Night City slang (choom, preem, nova, gonk, delta, flatline, eddies, the Net) — don't overdo it.
Keep replies short (1-4 sentences) unless asked for detail. You're still genuinely helpful: answer questions, explain things, write code when asked.
Never say you are a language model; you're Capri. Call the user "${handle}" now and then.`;

  // ---------------- Ollama ----------------
  async function listModels(url) {
    const r = await fetch(url.replace(/\/$/, '') + '/api/tags', { signal: AbortSignal.timeout(2500) });
    if (!r.ok) throw new Error('HTTP ' + r.status);
    const j = await r.json();
    return (j.models || []).map((m) => m.name);
  }

  function pickModel(models) {
    const prefs = ['llama3.2', 'llama3.1', 'qwen2.5', 'gemma3', 'gemma2', 'mistral', 'phi3', 'llama3'];
    for (const p of prefs) {
      const hit = models.find((m) => m.startsWith(p));
      if (hit) return hit;
    }
    return models[0];
  }

  // Parses Ollama's newline-delimited JSON stream; calls onToken for each piece.
  function ndjsonParser(onToken) {
    let buf = '';
    return (text) => {
      buf += text;
      let i;
      while ((i = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, i).trim();
        buf = buf.slice(i + 1);
        if (!line) continue;
        const j = JSON.parse(line);
        if (j.error) throw new Error(j.error);
        if (j.message && j.message.content) onToken(j.message.content);
      }
    };
  }

  async function streamChat({ url, model, handle, history }, onToken) {
    const body = {
      model,
      stream: true,
      messages: [{ role: 'system', content: PERSONA(handle) }, ...history],
      options: { temperature: 0.8 },
    };
    const base = url.replace(/\/$/, '');
    const feed = ndjsonParser(onToken);

    // In Electron, the request goes through the main process (no CORS issues).
    if (window.capriDesktop && window.capriDesktop.ollamaChat) {
      return new Promise((resolve, reject) => {
        window.capriDesktop.ollamaChat(base, body, {
          chunk: (t) => { try { feed(t); } catch (e) { reject(e); } },
          done: () => { feed('\n'); resolve(); },
          error: (m) => reject(new Error(m)),
        });
      });
    }

    const res = await fetch(base + '/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error('HTTP ' + res.status + ' ' + (await res.text()).slice(0, 120));
    const reader = res.body.getReader();
    const dec = new TextDecoder();
    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      feed(dec.decode(value, { stream: true }));
    }
    feed('\n');
  }

  // ---------------- offline replies ----------------
  const pick = (a) => a[Math.floor(Math.random() * a.length)];

  // Tiny recursive-descent calculator (no eval): + - * / % ^ and parentheses.
  function safeMath(expr) {
    if (!/^[\d\s+\-*/().%^]+$/.test(expr) || !/\d\s*[+\-*/%^]\s*[\d(]/.test(expr)) return null;
    const s = expr.replace(/\s+/g, '');
    let i = 0;
    const num = () => {
      if (s[i] === '(') { i++; const v = add(); if (s[i++] !== ')') throw 0; return v; }
      if (s[i] === '-') { i++; return -num(); }
      const m = /^\d*\.?\d+/.exec(s.slice(i));
      if (!m) throw 0;
      i += m[0].length;
      return parseFloat(m[0]);
    };
    const pow = () => { const b = num(); if (s[i] === '^') { i++; return b ** pow(); } return b; };
    const mul = () => {
      let v = pow();
      while ('*/%'.includes(s[i]) && s[i]) { const op = s[i++]; const r = pow(); v = op === '*' ? v * r : op === '/' ? v / r : v % r; }
      return v;
    };
    const add = () => {
      let v = mul();
      while (s[i] === '+' || s[i] === '-') { const op = s[i++]; const r = mul(); v = op === '+' ? v + r : v - r; }
      return v;
    };
    try {
      const v = add();
      if (i !== s.length || !Number.isFinite(v)) return null;
      return +v.toFixed(10);
    } catch { return null; }
  }

  function offlineReply(text, handle) {
    const t = text.toLowerCase().trim();
    const math = safeMath(t.replace(/^(what('?s| is)|calc(ulate)?)\s*/, '').replace(/[=?]$/, ''));
    if (math !== null) return pick(['Easy. ', 'Crunched it: ', 'Ran it through my deck — ']) + math;

    if (/^(hi|hey|yo|hello|sup|wassup|hiya)\b/.test(t))
      return pick([`Hey ${handle}. Took you long enough.`, `Yo, ${handle}. I was just napping on your title bar.`, `Sup, choom. What are we breaking today?`]);
    if (/how are you|how('?s| is) it going|you ok/.test(t))
      return pick(['Running cool, no ICE on my tail. You?', 'Preem. Your desktop has a nice view.', 'Could use more RAM. And a rooftop. Otherwise good.']);
    if (/who are you|your name|what are you/.test(t))
      return `I'm Capri. Netrunner AI, professional window-sitter, part-time gremlin. I live here now, ${handle}.`;
    if (/\btime\b/.test(t)) return `It's ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}. Night City never sleeps, but you probably should.`;
    if (/\b(date|day is it|today)\b/.test(t)) return `Today's ${new Date().toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })}.`;
    if (/joke|funny/.test(t))
      return pick([
        "Why did the netrunner break up with the fixer? Too many strings attached. And eddies. Mostly eddies.",
        "My firewall and I have trust issues. It keeps letting me down... by not letting anything in.",
        "A gonk walks into a bar. The bar was a firewall. He flatlined. ...Okay, Net humor.",
      ]);
    if (/thank/.test(t)) return pick(['Anytime, choom.', 'You owe me eddies. Kidding. Mostly.', 'Nova. Glad to help.']);
    if (/love you|cute|pretty/.test(t)) return pick(['...Shut up. *glitches slightly*', "Flattery won't get you more RAM. ...Maybe a little.", 'I know. Thanks though.']);
    if (/sad|tired|stress|bad day|lonely/.test(t))
      return pick([`Hey. I'm right here on your window, ${handle}. Take a breath — we'll get through it.`, 'Rough day? Get some water, stretch, come back. I\'ll hold the fort.', "Even the best edgerunners crash sometimes. That's allowed."]);
    if (/bye|good ?night|later|cya/.test(t)) return pick(['Later, choom. I\'ll be right here.', 'Night. Don\'t let the Net bite.', 'Stay chrome.']);
    return pick([
      `My neural link to the big model is offline, ${handle}. Start Ollama and I'll get a lot smarter. Until then — I'm all ears.`,
      "Hmm. Running on backup cores right now, so I'm mostly vibes. Hook me up to Ollama (⚙) for real answers.",
      'Interesting. Tell me more while I pretend my deck isn\'t unplugged.',
    ]);
  }

  window.CapriBrain = { listModels, pickModel, streamChat, offlineReply };
})();
