const { contextBridge, ipcRenderer } = require('electron');

const pending = new Map();
let nextId = 1;
ipcRenderer.on('ollama-chunk', (_e, { id, text }) => pending.get(id)?.chunk(text));
ipcRenderer.on('ollama-done', (_e, { id }) => { pending.get(id)?.done(); pending.delete(id); });
ipcRenderer.on('ollama-error', (_e, { id, message }) => { pending.get(id)?.error(message); pending.delete(id); });

const on = (ch) => (cb) => ipcRenderer.on(ch, (_e, data) => cb(data));

contextBridge.exposeInMainWorld('capriDesktop', {
  perch: () => ipcRenderer.send('perch'),
  unperch: () => ipcRenderer.send('unperch'),
  minimize: () => ipcRenderer.send('minimize'),
  quit: () => ipcRenderer.send('quit'),
  setIgnore: (v) => ipcRenderer.send('set-ignore', !!v),
  mood: (m) => ipcRenderer.send('mood', String(m)),
  say: (t) => ipcRenderer.send('say', String(t)),
  dragStart: () => ipcRenderer.send('drag-start'),
  dragMove: () => ipcRenderer.send('drag-move'),
  dragEnd: () => ipcRenderer.send('drag-end'),
  perchMenu: () => ipcRenderer.send('perch-menu'),
  onCursor: on('cursor'),
  onMood: on('mood'),
  onSay: on('say'),
  onPerched: on('perched'),
  ollamaChat(url, body, handlers) {
    const id = nextId++;
    pending.set(id, handlers);
    ipcRenderer.send('ollama-chat', { id, url, body });
  },
});
