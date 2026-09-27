# OrbitalStrike (Paper)

A Paper 1.21.11 port of the **OrbitalStrike+** Fabric mod (v1.0.18, by MC_Clip). It's server-side only, so players join with a vanilla client.

Get an orbital fishing rod with `/ob give`, cast it, then reel it in while looking at a block. TNT rains down 50 blocks above that block. The instant rods (wolves, golems, wither, stasis, stasis aim) fire as soon as you cast. Rods are single-use unless given with `true` (infinite). In creative mode they aren't used up, same as the original mod.

## Build

```
mvn -f OrbitalStrike/pom.xml package
```

The jar is written to `OrbitalStrike/target/OrbitalStrike-1.0.18.jar`. The **Build OrbitalStrike** GitHub Action also builds it on every push; download it from the run's *Artifacts* section.

## Commands

All commands need `orbitalstrike.admin` (op by default). Firing rods needs `orbitalstrike.use` (everyone by default). `<pos>` is `x y z`, and `~` / `~5` relative coordinates work.

| Command | Does |
|---|---|
| `/ob nuke <pos> [rings] [inward\|outward] [slam]` | Circular TNT bombardment (default 10 rings) |
| `/ob compressed_nuke <pos> [rings] [inward\|outward] [slam]` | Evenly spaced rings (default 3) |
| `/ob stab <x> <z>` | TNT column from build limit to bedrock |
| `/ob cross\|x_shot\|line\|hexagon\|pentagon\|star\|star_of_david\|heart <pos> [size] [slam]` | Shape strikes |
| `/ob ring <pos> [inner] [outer] [slam]` | Donut |
| `/ob scatter <pos> [radius] [clusters] [slam]` | Carpet bombing |
| `/ob grid <pos> [size] [spacing] [slam]` | Grid |
| `/ob cluster <pos> [count] [radius] [rings] [slam]` | Several small nukes |
| `/ob text <pos> <size 1-10> [slam] <text>` | Writes text in TNT (max 16 chars) |
| `/ob circle_stab\|cross_stab\|x_stab\|ring_stab\|trench_stab\|hexagon_stab\|pentagon_stab\|star_stab\|star_of_david_stab\|heart_stab\|grid_stab\|scatter_stab <pos> [size]` | A stab column at every point of the shape |
| `/ob text_stab <pos> <size> <text>` | Text made of stab columns |
| `/ob wither_skull <pos> [skulls]` | Blue wither skull cannon (default 200) |
| `/ob wolves\|golems <count>` | Armored wolf pack / iron golem army |
| `/ob wither` | Summon a Wither |
| `/ob stasis <pos>` / `/ob stasis_aim` | Teleport |
| `/ob give <type> [amount] [params...] [inward\|outward] [true\|false] [slam] [text]` | Get a rod. `true` = infinite |
| `/ob give stasis <amount> <pos> [true\|false]` | Rod that warps you to a fixed spot |
| `/ob give railgun [amount] [arrows] [spread]` | 20-damage arrow barrage from the sky |

`/ob` commands fire exactly at `<pos>`, so use something like `~ ~50 ~` for a falling strike. Rods add the 50 blocks for you.

**Slam**: TNT waits on the ground (up to 15 s) and the whole volley detonates at once.

## Paper notes

- Paper and Spigot cap how many TNT entities tick per tick (`max-tnt-per-tick` in `spigot.yml`, default 100). Big nukes and stabs spawn thousands of TNT, so they go off slower than on Fabric. Raise it if you want the original feel.
- Text strikes draw the letters with Java's built-in fonts. On a very minimal server image without any fonts installed, text shots will fail with an error. Installing `fontconfig` fixes it.
