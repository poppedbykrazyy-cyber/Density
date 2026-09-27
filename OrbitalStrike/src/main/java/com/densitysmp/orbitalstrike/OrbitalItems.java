package com.densitysmp.orbitalstrike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Builds and reads orbital fishing rods. All settings live in the item's PersistentDataContainer. */
public final class OrbitalItems {
    public static final String TYPE = "shot_type";
    public static final String UID = "uid";
    public static final String INFINITE = "infinite";
    public static final String SLAM = "slam";
    public static final String PENETRATE = "penetrate";
    public static final String RINGS = "ring_count";
    public static final String MODE = "mode";
    public static final String PACK_SIZE = "pack_size";
    public static final String PROJECTILES = "projectiles";
    public static final String SPREAD = "spread";
    public static final String WARP_X = "warp_x";
    public static final String WARP_Y = "warp_y";
    public static final String WARP_Z = "warp_z";
    public static final String SCALE = "scale";
    public static final String RADIUS_MIN = "radius_min";
    public static final String RADIUS_MAX = "radius_max";
    public static final String LENGTH = "length";
    public static final String THICKNESS = "thickness";
    public static final String GAP = "gap";
    public static final String MESSAGE = "message";
    public static final String GOLEM_AMOUNT = "golem_amount";
    public static final String CLUSTER_COUNT = "cluster_count";
    public static final String CLUSTER_RADIUS = "cluster_radius";
    public static final String SKULL_COUNT = "skull_count";

    private static final String[] MODE_NAMES = {"", " (Inward)", " (Outward)"};

    private OrbitalItems() {
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("orbitalstrike", name);
    }

    private static Component gray(String s) {
        return Component.text(s, NamedTextColor.GRAY);
    }

    private static Component darkGray(String s) {
        return Component.text(s, NamedTextColor.DARK_GRAY);
    }

    static String modeSuffix(int mode) {
        return mode >= 0 && mode < MODE_NAMES.length ? MODE_NAMES[mode] : "";
    }

    private static Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    public static ItemStack create(ShotType type, String displayName, boolean infinity, Map<String, Object> extra, Component... lore) {
        ItemStack stack = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (infinity) {
            meta.setUnbreakable(true);
        } else if (meta instanceof Damageable damageable) {
            // One durability left: the bar shows it's single-use. The rod is removed after firing either way.
            damageable.setDamage(Material.FISHING_ROD.getMaxDurability() - 1);
        }
        meta.displayName(Component.text(displayName + (infinity ? " (∞)" : "")));

        List<Component> lines = new ArrayList<>(List.of(lore));
        if (infinity) {
            lines.add(Component.text("Unbreakable (∞)", NamedTextColor.GOLD));
        }
        meta.lore(lines);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key(TYPE), PersistentDataType.STRING, type.id());
        pdc.set(key(UID), PersistentDataType.STRING, UUID.randomUUID().toString());
        if (infinity) {
            pdc.set(key(INFINITE), PersistentDataType.BOOLEAN, true);
        }
        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Integer i) {
                    pdc.set(key(e.getKey()), PersistentDataType.INTEGER, i);
                } else if (v instanceof Double d) {
                    pdc.set(key(e.getKey()), PersistentDataType.DOUBLE, d);
                } else if (v instanceof Boolean b) {
                    pdc.set(key(e.getKey()), PersistentDataType.BOOLEAN, b);
                } else if (v != null) {
                    pdc.set(key(e.getKey()), PersistentDataType.STRING, v.toString());
                }
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    // ---- Factories, matching the original mod's items ----

    public static ItemStack stab(boolean inf) {
        return create(ShotType.STAB, "Stab Shot", inf, null,
                gray("Vertical TNT bombardment"), darkGray("Cast to strike at facing block"));
    }

    public static ItemStack nuke(int rings, int mode, boolean inf) {
        String suffix = modeSuffix(mode);
        return create(ShotType.NUKE, "Nuke Shot" + suffix, inf, params(RINGS, rings, MODE, mode),
                gray("Circular TNT bombardment"),
                gray("Rings: " + rings + (mode > 0 ? " Mode: " + suffix.trim() : "")),
                darkGray("Cast to strike at facing block"));
    }

    public static ItemStack compressedNuke(int rings, int mode, boolean inf) {
        String suffix = modeSuffix(mode);
        return create(ShotType.COMPRESSED_NUKE, "Compressed Nuke" + suffix, inf, params(RINGS, rings, MODE, mode),
                gray("Organized circular TNT bombardment"),
                gray("Rings: " + rings + (mode > 0 ? " Mode: " + suffix.trim() : "")));
    }

    public static ItemStack wolf(int count, boolean inf) {
        return create(ShotType.WOLF, "Orbital Wolf", inf, params(PACK_SIZE, count),
                gray("Summon a wolf army"), gray("Wolves: " + count), darkGray("Cast to summon"));
    }

    public static ItemStack stasis(int x, int y, int z, boolean inf) {
        return create(ShotType.STASIS, "Stasis", inf, params(WARP_X, x, WARP_Y, y, WARP_Z, z),
                gray("Teleport to coordinate"), darkGray("Location: " + x + ", " + y + ", " + z));
    }

    public static ItemStack stasisAim(boolean inf) {
        return create(ShotType.STASIS_AIM, "Stasis Aim", inf, null,
                gray("Teleport to facing block"), darkGray("Cast to teleport"));
    }

    public static ItemStack railgun(int arrows, double spread, boolean inf) {
        return create(ShotType.RAILGUN, "Railgun", inf, params(PROJECTILES, arrows, SPREAD, spread),
                gray("High-velocity arrow barrage"), gray("Arrows: " + arrows), darkGray("Cast to fire from above"));
    }

    public static ItemStack cross(int arm, int width, boolean inf) {
        return create(ShotType.CROSS, "Cross Shot", inf, params(SCALE, arm, THICKNESS, width),
                gray("Cross-shaped TNT bombardment"), gray("Arm: " + arm + " Width: " + width));
    }

    public static ItemStack xShot(int arm, boolean inf) {
        return create(ShotType.X_SHOT, "X Shot", inf, params(SCALE, arm),
                gray("Diagonal cross TNT bombardment"), gray("Arm: " + arm));
    }

    public static ItemStack ring(int inner, int outer, boolean inf) {
        return create(ShotType.RING, "Ring Shot", inf, params(RADIUS_MIN, inner, RADIUS_MAX, outer),
                gray("Donut-shaped TNT bombardment"), gray("Inner: " + inner + " Outer: " + outer));
    }

    public static ItemStack line(int length, boolean inf) {
        return create(ShotType.LINE, "Line Shot", inf, params(LENGTH, length),
                gray("Linear TNT trench"), gray("Length: " + length));
    }

    public static ItemStack hexagon(int size, boolean inf) {
        return create(ShotType.HEXAGON, "Hexagon Shot", inf, params(SCALE, size),
                gray("Hexagonal TNT bombardment"), gray("Size: " + size));
    }

    public static ItemStack pentagon(int size, boolean inf) {
        return create(ShotType.PENTAGON, "Pentagon Shot", inf, params(SCALE, size),
                gray("Pentagonal TNT bombardment"), gray("Size: " + size));
    }

    public static ItemStack star(int arm, boolean inf) {
        return create(ShotType.STAR, "Star Shot", inf, params(SCALE, arm),
                gray("5-pointed star TNT bombardment"), gray("Arm: " + arm));
    }

    public static ItemStack starOfDavid(int size, boolean inf) {
        return create(ShotType.STAR_OF_DAVID, "Star of David Shot", inf, params(SCALE, size),
                gray("Hexagram TNT bombardment"), gray("Size: " + size));
    }

    public static ItemStack heart(int size, boolean inf) {
        return create(ShotType.HEART, "Heart Shot", inf, params(SCALE, size),
                gray("Heart-shaped TNT bombardment"), gray("Size: " + size));
    }

    public static ItemStack scatter(int radius, int clusters, boolean inf) {
        return create(ShotType.SCATTER, "Scatter Shot", inf, params(SCALE, radius, RINGS, clusters),
                gray("Carpet bombing"), gray("Radius: " + radius + " Clusters: " + clusters));
    }

    public static ItemStack grid(int size, int spacing, boolean inf) {
        return create(ShotType.GRID, "Grid Shot", inf, params(SCALE, size, GAP, spacing),
                gray("Grid-pattern TNT bombardment"), gray("Size: " + size + " Spacing: " + spacing));
    }

    public static ItemStack text(String content, int size, boolean inf) {
        return create(ShotType.TEXT, "Text Shot", inf, params(MESSAGE, content, SCALE, size),
                gray("Text-shaped TNT bombardment"), gray("\"" + content + "\" Size: " + size));
    }

    public static ItemStack golem(int count, boolean inf) {
        return create(ShotType.GOLEM, "Orbital Golem", inf, params(GOLEM_AMOUNT, count),
                gray("Summon an iron golem army"), gray("Golems: " + count));
    }

    public static ItemStack cluster(int count, int radius, int rings, boolean inf) {
        return create(ShotType.CLUSTER, "Cluster Strike", inf,
                params(CLUSTER_COUNT, count, CLUSTER_RADIUS, radius, RINGS, rings),
                gray("Multi-point TNT carpet bombing"),
                gray("Clusters: " + count + " Radius: " + radius + " Rings: " + rings));
    }

    public static ItemStack wither(boolean inf) {
        return create(ShotType.WITHER, "Orbital Wither", inf, null,
                Component.text("Summon the Wither", NamedTextColor.RED),
                Component.text("EXTREME DANGER", NamedTextColor.DARK_RED));
    }

    public static ItemStack witherSkull(int skulls, boolean inf) {
        return create(ShotType.WITHER_SKULL, "Wither Skull Cannon", inf, params(SKULL_COUNT, skulls),
                Component.text("Launches blue wither skulls in a circle", NamedTextColor.BLUE),
                gray("Skulls: " + skulls + " Height: 20"));
    }

    /** Stab variant of a shape: a sky-to-bedrock column at every point of the shape. */
    public static ItemStack stabShape(ShotType base, String displayName, int size, Map<String, Object> extra, boolean inf, Component... lore) {
        Map<String, Object> all = extra == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extra);
        all.put(PENETRATE, true);
        return create(base, displayName, inf, all, lore);
    }

    public static ItemStack applySlam(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(key(SLAM), PersistentDataType.BOOLEAN, true);
        Component name = meta.displayName();
        meta.displayName(name == null ? Component.text("(Slam)") : name.append(Component.text(" (Slam)")));
        stack.setItemMeta(meta);
        return stack;
    }

    // ---- Readers ----

    private static PersistentDataContainer pdc(ItemStack stack) {
        if (stack == null || stack.getType() != Material.FISHING_ROD || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer();
    }

    public static boolean isOrbital(ItemStack stack) {
        PersistentDataContainer pdc = pdc(stack);
        return pdc != null && pdc.has(key(TYPE), PersistentDataType.STRING) && pdc.has(key(UID), PersistentDataType.STRING);
    }

    public static ShotType type(ItemStack stack) {
        String id = string(stack, TYPE, null);
        return id == null ? null : ShotType.fromId(id);
    }

    public static String uid(ItemStack stack) {
        return string(stack, UID, null);
    }

    public static int integer(ItemStack stack, String name, int def) {
        PersistentDataContainer pdc = pdc(stack);
        if (pdc == null) return def;
        Integer v = pdc.get(key(name), PersistentDataType.INTEGER);
        return v == null ? def : v;
    }

    public static double decimal(ItemStack stack, String name, double def) {
        PersistentDataContainer pdc = pdc(stack);
        if (pdc == null) return def;
        Double v = pdc.get(key(name), PersistentDataType.DOUBLE);
        return v == null ? def : v;
    }

    public static String string(ItemStack stack, String name, String def) {
        PersistentDataContainer pdc = pdc(stack);
        if (pdc == null) return def;
        String v = pdc.get(key(name), PersistentDataType.STRING);
        return v == null || v.isEmpty() ? def : v;
    }

    public static boolean flag(ItemStack stack, String name) {
        PersistentDataContainer pdc = pdc(stack);
        if (pdc == null) return false;
        Boolean v = pdc.get(key(name), PersistentDataType.BOOLEAN);
        return v != null && v;
    }
}
