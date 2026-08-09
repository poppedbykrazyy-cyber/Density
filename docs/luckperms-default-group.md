# LuckPerms — `default` Group Setup

Permission nodes for the `default` group (every player who joins) on Density SMP.
Run these from the **server console** or in-game as an operator.

Syntax reminder: `lp group <group> permission set <node> <true|false>`
(`/lp` and `/luckperms` both work.)

---

## EssentialsX — Teleport (TPA)

```
lp group default permission set essentials.tpa true
lp group default permission set essentials.tpahere true
lp group default permission set essentials.tpaccept true
lp group default permission set essentials.tpdeny true
lp group default permission set essentials.tpacancel true
lp group default permission set essentials.tptoggle true
```

- `essentials.tpa` — send a teleport request (`/tpa <player>`)
- `essentials.tpahere` — ask someone to teleport to you (`/tpahere <player>`)
- `essentials.tpaccept` / `essentials.tpdeny` — needed by the **receiving** player, or requests can't be answered
- `essentials.tpacancel` — cancel your own outgoing request
- `essentials.tptoggle` — let players block incoming requests

**Do NOT give these to default** unless you want instant/free teleporting:

```
essentials.tpa.others
essentials.teleport.timer.bypass      # skips the teleport warmup countdown
essentials.teleport.cooldown.bypass   # skips the teleport cooldown
essentials.tp                         # direct teleport, no consent — staff only
essentials.tphere
essentials.tppos
```

---

## EssentialsX — Homes

```
lp group default permission set essentials.sethome true
lp group default permission set essentials.home true
lp group default permission set essentials.delhome true
lp group default permission set essentials.sethome.multiple true
lp group default permission set essentials.sethome.multiple.default true
```

Multiple homes are driven by config, not just the node. In `plugins/Essentials/config.yml`:

```yaml
sethome-multiple:
  default: 2
  vip: 5
  staff: 10
```

`essentials.sethome.multiple.default` then grants 2 homes. Give a rank
`essentials.sethome.multiple.vip` for 5, and so on. Only the highest matching
node counts. `essentials.sethome.multiple.unlimited` exists — keep it off default.

---

## EssentialsX — Messaging & Chat

```
lp group default permission set essentials.msg true
lp group default permission set essentials.reply true
lp group default permission set essentials.msgtoggle true
lp group default permission set essentials.ignore true
lp group default permission set essentials.mail true
lp group default permission set essentials.mail.send true
lp group default permission set essentials.me true
lp group default permission set essentials.helpop true
```

- `essentials.msg` — `/msg`, `/tell`, `/w`
- `essentials.reply` — `/r`, `/reply` (separate node from `msg`)
- `essentials.msgtoggle` — let players close their DMs
- `essentials.ignore` — `/ignore <player>`; you want this one, it cuts down on reports

Staff-only, leave off default: `essentials.msg.color`, `essentials.socialspy`,
`essentials.ignore.exempt`, `essentials.mute`.

---

## EssentialsX — General Quality of Life

```
lp group default permission set essentials.spawn true
lp group default permission set essentials.afk true
lp group default permission set essentials.list true
lp group default permission set essentials.help true
lp group default permission set essentials.motd true
lp group default permission set essentials.rules true
lp group default permission set essentials.seen true
lp group default permission set essentials.suicide true
lp group default permission set essentials.warp true
lp group default permission set essentials.warp.list true
lp group default permission set essentials.balance true
lp group default permission set essentials.pay true
lp group default permission set essentials.ignore true
lp group default permission set essentials.build true
```

`essentials.build` matters — if `essentials.build` handling is enabled in the
Essentials config, players without it can't place or break blocks.

### PvP server notes

On a PvP/SMP server, these are the ones that usually get abused. Consider
leaving them **off** the default group:

- `essentials.back` — teleport to your death spot, undoes losing a fight
- `essentials.back.ondeath` — same, automatic
- `essentials.god`, `essentials.heal`, `essentials.feed`
- `essentials.fly`, `essentials.speed`
- `essentials.workbench`, `essentials.enderchest` — portable storage mid-combat
- `essentials.repair`, `essentials.kit.*`
- `essentials.invsee`, `essentials.gamemode`, `essentials.item`, `essentials.give`

To explicitly deny one instead of just not granting it:

```
lp group default permission set essentials.back false
```

A `false` value **overrides** an inherited `true` — use it when a parent group
already grants something.

---

## Simple Voice Chat

```
lp group default permission set voicechat.connect true
lp group default permission set voicechat.speak true
lp group default permission set voicechat.listen true
lp group default permission set voicechat.groups true
```

- `voicechat.connect` — connect to the voice server at all
- `voicechat.speak` — transmit; revoke this to voice-mute someone
- `voicechat.listen` — receive audio
- `voicechat.groups` — create and join voice groups

Admin node — staff only:

```
lp group admin permission set voicechat.admin true
```

To voice-mute a single player without touching the group:

```
lp user <player> permission set voicechat.speak false
```

Add a duration to make it temporary:

```
lp user <player> permission settemp voicechat.speak false 1h
```

---

## Density SMP

`/density`, `/densitytop`, `/compress`, and `/expand` have no permission node in
`plugin.yml`, so they're available to everyone by default — nothing to grant.

Admin nodes default to `op`. To give them to a staff group instead of opping:

```
lp group admin permission set density.admin true
lp group admin permission set density.bypass true
```

`density.bypass` skips density elimination — do not put it on default.

---

## Verifying

```
lp group default info
lp group default permission info
lp user <player> permission check essentials.tpa
lp user <player> info
```

`permission check` is the one to use when a player says a command isn't working —
it shows the granted value and which group it was inherited from.

---

## Full paste block

Everything above, minus the staff-only nodes. Paste into the server console:

```
lp group default permission set essentials.tpa true
lp group default permission set essentials.tpahere true
lp group default permission set essentials.tpaccept true
lp group default permission set essentials.tpdeny true
lp group default permission set essentials.tpacancel true
lp group default permission set essentials.tptoggle true
lp group default permission set essentials.sethome true
lp group default permission set essentials.home true
lp group default permission set essentials.delhome true
lp group default permission set essentials.sethome.multiple true
lp group default permission set essentials.sethome.multiple.default true
lp group default permission set essentials.msg true
lp group default permission set essentials.reply true
lp group default permission set essentials.msgtoggle true
lp group default permission set essentials.ignore true
lp group default permission set essentials.mail true
lp group default permission set essentials.mail.send true
lp group default permission set essentials.me true
lp group default permission set essentials.helpop true
lp group default permission set essentials.spawn true
lp group default permission set essentials.afk true
lp group default permission set essentials.list true
lp group default permission set essentials.help true
lp group default permission set essentials.motd true
lp group default permission set essentials.rules true
lp group default permission set essentials.seen true
lp group default permission set essentials.suicide true
lp group default permission set essentials.warp true
lp group default permission set essentials.warp.list true
lp group default permission set essentials.balance true
lp group default permission set essentials.pay true
lp group default permission set essentials.build true
lp group default permission set voicechat.connect true
lp group default permission set voicechat.speak true
lp group default permission set voicechat.listen true
lp group default permission set voicechat.groups true
```

No reload needed — LuckPerms applies changes immediately. If you edited
`Essentials/config.yml` for multiple homes, run `/ess reload` for that part.
