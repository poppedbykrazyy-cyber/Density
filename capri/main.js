// Capri — desktop AI companion. Electron main process.
const { app, BrowserWindow, ipcMain, screen, globalShortcut, Menu } = require('electron');
const path = require('path');

// Geometry shared with the renderer CSS:
//  chat window 460x750, Capri (240x240) drawn at x=seatX (0-220, draggable), y=0;
//  the panel's top edge is at y=180
//  perch window 380x240, Capri drawn at x=140, y=0; the edge she sits on is y=180
const CHAT = { w: 460, h: 750 };
const PERCH = { w: 380, h: 240, capriX: 140, edgeY: 180 };
let seatX = 196; // where she sits along the chat window's edge (renderer reports changes)

let chat = null;
let perch = null;
let drag = null;

if (!app.requestSingleInstanceLock()) app.quit();
// let her speak replies even when the chat window is hidden behind perch mode
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');

function makeWindow(opts, file) {
  const win = new BrowserWindow({
    frame: false,
    transparent: true,
    resizable: false,
    hasShadow: false,
    show: false,
    backgroundColor: '#00000000',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      backgroundThrottling: false,
    },
    ...opts,
  });
  win.loadFile(path.join(__dirname, 'renderer', file));
  return win;
}

function createWindows() {
  const wa = screen.getPrimaryDisplay().workArea;
  chat = makeWindow(
    { width: CHAT.w, height: CHAT.h, x: wa.x + wa.width - CHAT.w - 40, y: wa.y + Math.max(0, (wa.height - CHAT.h) / 2), title: 'Capri' },
    'index.html'
  );
  chat.once('ready-to-show', () => chat.show());
  chat.on('closed', () => app.quit());

  perch = makeWindow(
    { width: PERCH.w, height: PERCH.h, skipTaskbar: true, alwaysOnTop: true, // not focusable, so clicking her doesn't steal focus from the window she sits on
      focusable: process.platform === 'linux', title: 'Capri (perched)' },
    'perch.html'
  );
  perch.setAlwaysOnTop(true, 'floating');
  perch.setVisibleOnAllWorkspaces(true);
}

function clampToDisplay(x, y, w, h) {
  const wa = screen.getDisplayNearestPoint({ x: Math.round(x + w / 2), y: Math.round(y + h / 2) }).workArea;
  return [
    Math.round(Math.min(Math.max(x, wa.x), wa.x + wa.width - w)),
    Math.round(Math.min(Math.max(y, wa.y), wa.y + wa.height - h)),
  ];
}

function doPerch() {
  if (!chat || !perch || perch.isVisible()) return;
  const [cx, cy] = chat.getPosition();
  perch.setPosition(cx + seatX - PERCH.capriX, cy);
  perch.showInactive();
  chat.hide();
  perch.webContents.send('perched');
}

function doUnperch() {
  if (!chat || !perch) return;
  if (perch.isVisible()) {
    const [px, py] = perch.getPosition();
    const [x, y] = clampToDisplay(px + PERCH.capriX - seatX, py, CHAT.w, CHAT.h);
    chat.setPosition(x, y);
    perch.hide();
  }
  chat.show();
  chat.focus();
}

// Seat her on top of the taskbar / dock (bottom of the work area).
function sitOnTaskbar() {
  const [px, py] = perch.getPosition();
  const wa = screen.getDisplayNearestPoint({ x: px + PERCH.w / 2, y: py }).workArea;
  perch.setPosition(px, wa.y + wa.height - PERCH.edgeY);
}

app.whenReady().then(() => {
  createWindows();

  // Eyes follow your cursor everywhere on screen.
  let last = '';
  setInterval(() => {
    const p = screen.getCursorScreenPoint();
    const key = p.x + ',' + p.y;
    if (key === last) return;
    last = key;
    for (const w of [chat, perch]) {
      if (w && !w.isDestroyed() && w.isVisible()) {
        const b = w.getBounds();
        w.webContents.send('cursor', { x: p.x - b.x, y: p.y - b.y });
      }
    }
  }, 33);

  globalShortcut.register('CommandOrControl+Shift+Space', () => {
    if (chat.isVisible() && chat.isFocused()) doPerch(); else doUnperch();
  });
});

app.on('will-quit', () => globalShortcut.unregisterAll());
app.on('second-instance', () => doUnperch());
app.on('window-all-closed', () => app.quit());

// ---------------- IPC ----------------
ipcMain.on('perch', doPerch);
ipcMain.on('seat', (_e, x) => { if (Number.isFinite(x)) seatX = Math.min(220, Math.max(0, x)); });
ipcMain.on('unperch', doUnperch);
ipcMain.on('minimize', () => chat && chat.minimize());
ipcMain.on('quit', () => app.quit());

ipcMain.on('set-ignore', (e, ignore) => {
  // click-through needs `forward` to keep receiving mousemove; Linux doesn't support it.
  if (process.platform === 'linux') return;
  const w = BrowserWindow.fromWebContents(e.sender);
  if (w) w.setIgnoreMouseEvents(!!ignore, { forward: true });
});

ipcMain.on('mood', (_e, m) => perch && perch.webContents.send('mood', m));
ipcMain.on('say', (_e, t) => perch && perch.isVisible() && perch.webContents.send('say', t));

ipcMain.on('drag-start', () => {
  drag = { cursor: screen.getCursorScreenPoint(), win: perch.getPosition() };
});
ipcMain.on('drag-move', () => {
  if (!drag) return;
  const c = screen.getCursorScreenPoint();
  perch.setPosition(drag.win[0] + c.x - drag.cursor.x, drag.win[1] + c.y - drag.cursor.y);
});
ipcMain.on('drag-end', () => {
  drag = null;
  // Dropped close to the taskbar? Snap her onto it.
  const [px, py] = perch.getPosition();
  const wa = screen.getDisplayNearestPoint({ x: px + PERCH.w / 2, y: py }).workArea;
  const edge = py + PERCH.edgeY;
  const bottom = wa.y + wa.height;
  if (Math.abs(edge - bottom) < 60 || edge > bottom) sitOnTaskbar();
  else if (py < wa.y - PERCH.edgeY + 20) perch.setPosition(px, wa.y - PERCH.edgeY + 20);
});

ipcMain.on('perch-menu', () => {
  Menu.buildFromTemplate([
    { label: 'Open chat', click: doUnperch },
    { label: 'Sit on taskbar', click: sitOnTaskbar },
    { type: 'separator' },
    { label: 'Quit Capri', click: () => app.quit() },
  ]).popup({ window: perch });
});

// Ollama requests run here so the renderer never hits CORS.
ipcMain.on('ollama-chat', async (e, { id, url, body }) => {
  const reply = (ch, data) => { if (!e.sender.isDestroyed()) e.sender.send(ch, { id, ...data }); };
  try {
    const res = await fetch(url + '/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error('HTTP ' + res.status + ' ' + (await res.text()).slice(0, 120));
    const dec = new TextDecoder();
    for await (const chunk of res.body) reply('ollama-chunk', { text: dec.decode(chunk, { stream: true }) });
    reply('ollama-done', {});
  } catch (err) {
    reply('ollama-error', { message: err.message });
  }
});
