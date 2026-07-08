# Density SMP

A PvP-focused Minecraft SMP plugin for Paper 1.21.1 with two interlocking combat systems: **Density** (permanent stat stolen from kills) and **Flow** (temporary combat momentum from chaining hits).

## Core Concept

**Kill players to steal their Density.** Higher density = tankier with lifesteal and thorns. Lower density = glass cannon with bonus damage and armor pierce.

**Chain hits to build Flow.** Landing hits and getting kills fills your Flow meter, granting stacking combat buffs. Getting hit or dying resets your Flow. The best PvP players maintain both high density AND high flow.

## Density Tiers (Permanent)

| Tier | Density | Hearts | Damage | Special |
|------|---------|--------|--------|---------|
| **Fragile** | 1-3 | -4 | +2.0 | Armor Pierce, faster pearls |
| **Lean** | 4-7 | -2 | +1.0 | 5% Lifesteal |
| **Balanced** | 8-15 | 0 | 0 | No bonuses |
| **Fortified** | 16-25 | +2 | 0 | 10% Lifesteal, +2 Armor Toughness |
| **Titan** | 26-40 | +6 | 0 | 15% Lifesteal, 15% Thorns, +4 Armor Toughness |
| **Colossus** | 41-50 | +10 | +1.0 | 20% Lifesteal, 25% Thorns, Auto-Totem |

## Flow System (Temporary)

| Flow Level | Threshold | Speed | Damage | Special |
|------------|-----------|-------|--------|---------|
| **Warmed Up** | 20 | +5% | +0.5 | - |
| **In The Zone** | 40 | +10% | +1.0 | Regeneration |
| **Flowing** | 60 | +15% | +1.5 | Regen + Shield Break bonus |
| **Unstoppable** | 80 | +20% | +2.0 | + KB Immunity |
| **Transcendent** | 100 | +25% | +3.0 | + Strength I, Weaken enemies |

Flow builds from: melee hits (+5), arrow hits (+4), kills (+25)
Flow drops from: taking damage (-15), death (reset to 0), idle decay (-2/s after 8s)

## Custom Weapons (Density Altar)

| Weapon | Base | Density Cost | Effect |
|--------|------|-------------|--------|
| **Density Blade** | Netherite Sword | 4 | Damage scales with your density, 15% Weakness proc |
| **Void Axe** | Netherite Axe | 3 | Steals 0.3 density per hit, 8s shield disable |
| **Flow Scythe** | Netherite Hoe | 5 | 2x Flow gain, sweeping cleave hits nearby enemies |
| **Siphon Bow** | Bow | 5 | Arrows steal density and apply Slowness II |
| **Mass Shifter** | Blaze Rod | 4 | Swap densities with target / Combat boost (Str+Speed) |

### Density Altar

Build a **3x3 obsidian platform** with a **lodestone on top**. Sneak + right-click to open the crafting menu. Weapons cost density to forge and require the base item in your inventory.

## Item Restrictions

Configured under `item-restrictions` in `config.yml`:

- **Shields and Totems of Undying are banned** — they can't be crafted, totems never activate, and any that end up in a player's inventory (trades, chests, drops) are auto-deleted.
- **Ender pearl limit: 16** — players can't pick up pearls past the limit, and excess pearls are dropped on the ground.

The Colossus Auto-Totem perk still works — it doesn't use a real totem item.

## Commands

| Command | Description |
|---------|-------------|
| `/density [player]` | View density, tier, flow, and combat stats |
| `/densitytop` | Density leaderboard |
| `/compress` | -2 hearts, +2 density (5min CD) |
| `/expand` | -2 density (5min CD) |
| `/densityadmin set <player> <amount>` | Set density (OP) |
| `/densityadmin reset <player>` | Reset to default (OP) |
| `/densityadmin restore <player>` | Restore eliminated player (OP) |
| `/densityadmin reload` | Reload config (OP) |

## Building

Requires **Java 21** and **Maven**.

```bash
mvn clean package
```

Output: `target/DensitySMP-1.0.0.jar`

## Installation

1. Build or download the JAR
2. Drop in `plugins/` on Paper 1.21.1
3. Restart server
4. Configure `plugins/DensitySMP/config.yml`

## Resource Pack

Custom textures in `resourcepack/`. Generate with:

```bash
pip install Pillow
python3 generate_textures.py
```

Creates `DensitySMP-ResourcePack.zip` with tier badges, density orbs, particle textures, and the pack icon.
