# LuckPerms — Group Ladder & Bold Prefixes

Owner → Admin → Moderator → Staff → Member, with bold coloured prefixes.

---

## 1. Create the groups

```
lp creategroup owner
lp creategroup admin
lp creategroup moderator
lp creategroup staff
lp creategroup member
```

Groups that already exist will just report "already exists" — harmless, no
existing data is touched.

---

## 2. Set weights

Weight decides sort order in the group list and which prefix wins when a player
is in more than one group.

```
lp group owner setweight 100
lp group admin setweight 90
lp group moderator setweight 70
lp group staff setweight 50
lp group member setweight 10
```

---

## 3. Prefixes (bold)

```
lp group owner meta setprefix 100 "&e&lOwner &r"
lp group admin meta setprefix 90 "&c&lAdmin &r"
lp group moderator meta setprefix 70 "&6&lModerator &r"
lp group staff meta setprefix 50 "&9&lStaff &r"
lp group member meta setprefix 10 "&7&lMember &r"
```

| Group | Code | Colour | Matches |
|-------|------|--------|---------|
| Owner | `&e&l` | Yellow, bold | yellow shield |
| Admin | `&c&l` | Red, bold | red shield |
| Moderator | `&6&l` | Gold, bold | orange shield (no true orange in Minecraft) |
| Staff | `&9&l` | Blue, bold | blue shield |
| Member | `&7&l` | Grey, bold | — |

### The bold gotcha — order matters

`&l` must come **after** the colour code:

- `&c&l` → **red bold** ✅
- `&l&c` → red, **not** bold ❌

A colour code resets all active formatting, so a colour placed after `&l` wipes
the bold. This is the single most common reason a prefix "won't go bold".

The trailing `&r` resets formatting so the player's name isn't bold too. Drop it
if you want the whole line bold. The space before `&r` keeps the prefix from
jamming into the username.

### If your chat plugin uses MiniMessage

Some newer chat plugins take MiniMessage instead of `&` codes:

```
lp group owner meta setprefix 100 "<yellow><bold>Owner </bold></yellow>"
lp group admin meta setprefix 90 "<red><bold>Admin </bold></red>"
lp group moderator meta setprefix 70 "<gold><bold>Moderator </bold></gold>"
lp group staff meta setprefix 50 "<blue><bold>Staff </bold></blue>"
lp group member meta setprefix 10 "<gray><bold>Member </bold></gray>"
```

Use one style or the other, not both. EssentialsX Chat uses `&` codes.

---

## 4. Inheritance

Each rank inherits everything below it, so you only grant a permission once at
the lowest rank that should have it.

```
lp group staff parent add member
lp group moderator parent add staff
lp group admin parent add moderator
lp group owner parent add admin
```

---

## 5. Make Member the baseline

`default` is the group LuckPerms puts everyone in and it can't be deleted. Have
it inherit `member` so every player picks up the Member prefix and perms:

```
lp group default parent add member
```

Then keep `default` empty and manage everything on `member`.

If you already ran the EssentialsX/voicechat nodes against `default`, either
leave them there or move them onto `member`:

```
lp group member permission set essentials.tpa true
lp group member permission set essentials.sethome true
lp group member permission set essentials.msg true
lp group member permission set voicechat.connect true
...
```

---

## 6. Assigning players

```
lp user <player> parent add moderator
lp user <player> parent set owner        # replaces all groups instead of adding
lp user <player> parent remove moderator
```

Temporary rank:

```
lp user <player> parent addtemp staff 30d
```

---

## 7. Making prefixes actually show in chat

LuckPerms stores the prefix — it does not render it. You need a chat plugin
reading it through Vault.

Required: **Vault** + **EssentialsX Chat** (separate jar from EssentialsX).

In `plugins/Essentials/config.yml`:

```yaml
chat:
  format: '{PREFIX}&r{DISPLAYNAME}&r: {MESSAGE}'
```

Then `/ess reload`.

If prefixes still don't appear, check in this order:

1. Is `EssentialsXChat.jar` in `plugins/`? (EssentialsX alone won't do it)
2. Is Vault installed? `/plugins` should list it green
3. `lp group owner info` — confirm the prefix is actually stored
4. `/ess reload` after any config.yml edit

### Tab list

Prefixes don't reach the tab list on their own. TAB or a similar plugin can pull
them via the `%luckperms_prefix%` PlaceholderAPI placeholder (needs
PlaceholderAPI + the LuckPerms expansion: `/papi ecloud download LuckPerms`).

---

## 8. Verifying

```
lp group owner info
lp group member info
lp user <player> meta info
```

`lp user <player> meta info` shows the resolved prefix and which group it came
from — use it when a player has the wrong tag.

---

## Full paste block

```
lp creategroup owner
lp creategroup admin
lp creategroup moderator
lp creategroup staff
lp creategroup member

lp group owner setweight 100
lp group admin setweight 90
lp group moderator setweight 70
lp group staff setweight 50
lp group member setweight 10

lp group owner meta setprefix 100 "&e&lOwner &r"
lp group admin meta setprefix 90 "&c&lAdmin &r"
lp group moderator meta setprefix 70 "&6&lModerator &r"
lp group staff meta setprefix 50 "&9&lStaff &r"
lp group member meta setprefix 10 "&7&lMember &r"

lp group staff parent add member
lp group moderator parent add staff
lp group admin parent add moderator
lp group owner parent add admin

lp group default parent add member
```
