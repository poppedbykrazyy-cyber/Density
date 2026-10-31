# CAPRI // desktop AI companion

A fan-made desktop AI assistant themed on **Capri** from *Cyberpunk: Edgerunners 2*. She has a huge
windswept hot-pink mane full of stars, tall black spikes, green mask plates around big blue eyes,
a white zigzag grin, a white jacket with violet rims, and red circuit patches. She sits on top of
her chat window with her legs dangling over the edge. Detach her and she sits on top of any window
on your screen, follows your cursor with her eyes, and talks back.

![Capri animated](preview.gif)

![Capri chat window](screenshot.png)

> Unofficial fan project, not affiliated with CD PROJEKT RED or Studio Trigger.

## Run it

```bash
cd capri
npm install
npm start
```

### Give her a brain (optional, free, offline)

Capri talks through a local model with [Ollama](https://ollama.com). Nothing leaves your machine and you don't need an API key.

```bash
ollama pull llama3.2
```

Leave Ollama running and Capri finds it at `http://127.0.0.1:11434` and picks a model by
herself. If Ollama isn't running, she falls back to built-in replies: chit-chat, time and date,
jokes, and a calculator (`what is (2+3)*4^2`). You can change the endpoint, model and your
handle in **⚙ settings**.

## Things to try

| | |
|---|---|
| **⇱** or `Ctrl+Shift+Space` | **Perch mode**: hides the chat and leaves Capri on your desktop, always on top |
| Drag her | Drop her on the edge of any window. Drop her near the bottom of the screen and she snaps onto the taskbar |
| Click her | Opens the chat in perch mode. In the chat window it pokes her |
| Right-click her | Menu: open chat / sit on taskbar / quit |
| `/help` | Commands: `/clear`, `/perch`, `/sleep`, `/model <name>`, `/link` |
| ⚙ → **Skin** | Load your own Capri GIF/PNG/WebM to replace the drawn version |

## How she's animated

- **Line boil:** every shard flips between hand-jittered copies of itself at about 8 fps, like the show's limited animation.
- **Always on:** each hair strand flutters in the wind, the stars in her hair twinkle, and purple and green glitch shards drift past. She also swings her legs, breathes, blinks (sometimes twice), flickers, and has RGB-split edges. Her eyes and head track your cursor anywhere on screen.
- **Moods:** *thinking* (shards float, eyes roll up, head sways), *talking* (the grin snaps open and shut, body bobs, legs kick faster, hair whips), *happy* (^^ eyes plus the big grin), *asleep* after 90 s idle (head droops, zzz, everything stills).
- **Random idle actions:** hair tosses, leg kicks, glitch bursts, head tilts, leaning forward, spike shivers, and eye glances.
- **Glitch-in** entrance when she appears, and flailing legs while you drag her.

### Custom skin (exact 1:1 look)

The built-in Capri is a hand-drawn SVG. For a frame-exact look, open ⚙ → **Skin** and load your
own clip of her, up to about 4.5 MB. A cut-out GIF or WebM with transparency looks best. The skin
is shared with the perch window and keeps the mood animations. **DEFAULT** switches back to the
drawing.

## Files

```
capri/
├── main.js          Electron main: windows, perch/drag/snap, global cursor, Ollama proxy
├── preload.js       safe bridge exposed to the pages as window.capriDesktop
└── renderer/
    ├── avatar.js    Capri herself (SVG + line boil + moods + idle actions + skins)
    ├── avatar.css   her colours and every animation
    ├── index.html   chat window   (+ style.css, app.js)
    ├── perch.html   perched-only window (+ perch.js)
    └── brain.js     persona prompt, Ollama streaming, offline replies
```

You can also preview the UI in a browser without Electron. Perch mode is disabled there:

```bash
cd capri/renderer && python3 -m http.server 5173
```

Then open <http://localhost:5173>.

## Notes

- Capri can't detect where other apps' windows are. You drag her onto an edge and she stays there, always on top.
- On Linux, clicks don't pass through the transparent parts of her windows (Electron doesn't support click-through forwarding there).
