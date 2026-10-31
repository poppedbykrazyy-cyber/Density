(function () {
  const bridge = window.capriDesktop;
  const el = document.getElementById('capri');
  const capri = mountCapri(el);

  // ---- drag her anywhere; a click without movement opens the chat ----
  let down = null;
  el.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    down = { x: e.screenX, y: e.screenY, moved: false };
    el.setPointerCapture(e.pointerId);
    bridge.dragStart();
  });
  el.addEventListener('pointermove', (e) => {
    if (!down) return;
    if (!down.moved && Math.hypot(e.screenX - down.x, e.screenY - down.y) > 4) {
      down.moved = true;
      el.classList.add('dragging');
      capri.setMood('happy');
      capri.say('Wheee—');
    }
    if (down.moved) bridge.dragMove();
  });
  el.addEventListener('pointerup', () => {
    if (!down) return;
    const wasDrag = down.moved;
    down = null;
    el.classList.remove('dragging');
    bridge.dragEnd();
    if (wasDrag) {
      capri.setMood('idle');
      capri.play('peek');
      capri.say(['Comfy.', 'Nice view from here.', 'This edge is mine now.', 'Perfect spot.'][Math.floor(Math.random() * 4)], 2200);
    } else if (capri.mood === 'sleep') {
      capri.setMood('idle');
      capri.say('Huh? I\'m up, I\'m up.');
    } else {
      bridge.unperch();
    }
  });
  el.addEventListener('contextmenu', (e) => { e.preventDefault(); bridge.perchMenu(); });

  // ---- clicks pass through everything except Capri herself ----
  let ignoring = false;
  document.addEventListener('mousemove', (e) => {
    const over = !!e.target.closest('#capri .capri-art') || !!down;
    if (over === ignoring) { ignoring = !over; bridge.setIgnore(ignoring); }
  });

  // ---- live updates from the main process / chat window ----
  bridge.onCursor(({ x, y }) => {
    const r = el.getBoundingClientRect();
    capri.lookAt(x - r.left, y - r.top);
  });
  bridge.onMood((m) => capri.setMood(m));
  bridge.onSay((t) => { capri.wake(); capri.say(t); });
  bridge.onPerched(() => {
    capri.play('materialize');
    capri.setMood('happy');
    capri.say('Drag me onto any window. I\'ll sit there and keep watch.', 4200);
    setTimeout(() => capri.setMood('idle'), 900);
  });
})();
