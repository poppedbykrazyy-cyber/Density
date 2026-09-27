package com.densitysmp.orbitalstrike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /ob &lt;shot&gt; &lt;x y z&gt; [numbers...] [inward|outward] [slam] [text]   - fire a strike at a position
 * /ob give &lt;shot&gt; [amount] [numbers...] [inward|outward] [true|false] [slam] [text]   - get an orbital rod
 */
public final class OrbitalCommand implements TabExecutor {
    private static final int MAX_TEXT = 16;

    /** A rod kind: its numeric parameters (name + default, -1 = required) and how to build it. */
    private record Kind(String name, String[] params, int[] defaults, boolean modes, boolean text, Builder builder) {
        boolean slam() {
            return shotType().supportsSlam() && !stab();
        }

        boolean stab() {
            return name.endsWith("_stab");
        }

        ShotType shotType() {
            // Build a throwaway item to learn its type.
            return OrbitalItems.type(builder.build(defaultsFilled(), 0, "TNT", false));
        }

        int[] defaultsFilled() {
            int[] v = defaults.clone();
            for (int i = 0; i < v.length; i++) {
                if (v[i] < 0) v[i] = 1;
            }
            return v;
        }
    }

    @FunctionalInterface
    private interface Builder {
        ItemStack build(int[] n, int mode, String text, boolean inf);
    }

    private static final Map<String, Kind> KINDS = new LinkedHashMap<>();

    private static void kind(String name, String[] params, int[] defaults, boolean modes, boolean text, Builder b) {
        KINDS.put(name, new Kind(name, params, defaults, modes, text, b));
    }

    private static String[] p(String... names) {
        return names;
    }

    private static int[] d(int... defaults) {
        return defaults;
    }

    static {
        kind("stab", p(), d(), false, false, (n, m, t, inf) -> OrbitalItems.stab(inf));
        kind("nuke", p("rings"), d(10), true, false, (n, m, t, inf) -> OrbitalItems.nuke(n[0], m, inf));
        kind("compressed_nuke", p("rings"), d(3), true, false, (n, m, t, inf) -> OrbitalItems.compressedNuke(n[0], m, inf));
        kind("cross", p("size"), d(10), false, false, (n, m, t, inf) -> OrbitalItems.cross(n[0], 1, inf));
        kind("x_shot", p("size"), d(15), false, false, (n, m, t, inf) -> OrbitalItems.xShot(n[0], inf));
        kind("ring", p("inner", "outer"), d(5, 15), false, false, (n, m, t, inf) -> OrbitalItems.ring(n[0], n[1], inf));
        kind("line", p("length"), d(20), false, false, (n, m, t, inf) -> OrbitalItems.line(n[0], inf));
        kind("hexagon", p("size"), d(15), false, false, (n, m, t, inf) -> OrbitalItems.hexagon(n[0], inf));
        kind("pentagon", p("size"), d(15), false, false, (n, m, t, inf) -> OrbitalItems.pentagon(n[0], inf));
        kind("star", p("size"), d(15), false, false, (n, m, t, inf) -> OrbitalItems.star(n[0], inf));
        kind("star_of_david", p("size"), d(15), false, false, (n, m, t, inf) -> OrbitalItems.starOfDavid(n[0], inf));
        kind("heart", p("size"), d(10), false, false, (n, m, t, inf) -> OrbitalItems.heart(n[0], inf));
        kind("scatter", p("radius", "clusters"), d(20, 8), false, false, (n, m, t, inf) -> OrbitalItems.scatter(n[0], n[1], inf));
        kind("grid", p("size", "spacing"), d(20, 4), false, false, (n, m, t, inf) -> OrbitalItems.grid(n[0], n[1], inf));
        kind("text", p("size"), d(-1), false, true, (n, m, t, inf) -> OrbitalItems.text(t, n[0], inf));
        kind("cluster", p("count", "radius", "rings"), d(5, 30, 2), false, false, (n, m, t, inf) -> OrbitalItems.cluster(n[0], n[1], n[2], inf));
        kind("railgun", p("arrows", "spread"), d(64, 3), false, false, (n, m, t, inf) -> OrbitalItems.railgun(n[0], n[1], inf));
        kind("wither_skull", p("skulls"), d(200), false, false, (n, m, t, inf) -> OrbitalItems.witherSkull(n[0], inf));
        kind("wolf", p("wolves"), d(10), false, false, (n, m, t, inf) -> OrbitalItems.wolf(n[0], inf));
        kind("golem", p("golems"), d(5), false, false, (n, m, t, inf) -> OrbitalItems.golem(n[0], inf));
        kind("wither", p(), d(), false, false, (n, m, t, inf) -> OrbitalItems.wither(inf));
        kind("stasis_aim", p(), d(), false, false, (n, m, t, inf) -> OrbitalItems.stasisAim(inf));

        stab("circle_stab", "Circle Stab", ShotType.NUKE, 3, OrbitalItems.RINGS);
        stab("cross_stab", "Cross Stab", ShotType.CROSS, 10, null);
        stab("x_stab", "X Stab", ShotType.X_SHOT, 15, null);
        stab("ring_stab", "Ring Stab", ShotType.RING, 15, null);
        stab("trench_stab", "Trench Stab", ShotType.LINE, 20, OrbitalItems.LENGTH);
        stab("hexagon_stab", "Hexagon Stab", ShotType.HEXAGON, 15, null);
        stab("pentagon_stab", "Pentagon Stab", ShotType.PENTAGON, 15, null);
        stab("star_stab", "Star Stab", ShotType.STAR, 15, null);
        stab("star_of_david_stab", "Star of David Stab", ShotType.STAR_OF_DAVID, 15, null);
        stab("heart_stab", "Heart Stab", ShotType.HEART, 10, null);
        stab("grid_stab", "Grid Stab", ShotType.GRID, 20, null);
        stab("scatter_stab", "Scatter Stab", ShotType.SCATTER, 20, null);
        kind("text_stab", p("size"), d(-1), false, true, (n, m, t, inf) -> OrbitalItems.stabShape(ShotType.TEXT, "Text Stab", n[0],
                Map.of(OrbitalItems.MESSAGE, t, OrbitalItems.SCALE, n[0]), inf,
                Component.text("Sky-to-bedrock text stab", NamedTextColor.GRAY),
                Component.text("\"" + t + "\" Size: " + n[0], NamedTextColor.GRAY)));
    }

    private static void stab(String name, String display, ShotType base, int def, String extraKey) {
        kind(name, p("size"), d(def), false, false, (n, m, t, inf) -> {
            int size = n[0];
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put(OrbitalItems.SCALE, size);
            if (extraKey != null) extra.put(extraKey, size);
            if (base == ShotType.RING) {
                extra.put(OrbitalItems.RADIUS_MIN, size / 3);
                extra.put(OrbitalItems.RADIUS_MAX, size);
            }
            return OrbitalItems.stabShape(base, display, size, extra, inf,
                    Component.text("Sky-to-bedrock " + display.toLowerCase(Locale.ROOT), NamedTextColor.GRAY),
                    Component.text("Size: " + size, NamedTextColor.GRAY));
        });
    }

    /** Names accepted by /ob &lt;name&gt; (the summon form); some differ from their /ob give name. */
    private static final Map<String, String> SUMMON_ALIASES = Map.of(
            "wolves", "wolf",
            "golems", "golem",
            "trench", "trench_stab");

    // ---- Command ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        try {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("give")) {
                give(sender, args);
            } else {
                summon(sender, sub, args);
            }
        } catch (UsageException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private static final class UsageException extends Exception {
        UsageException(String message) {
            super(message);
        }
    }

    /** Parsed flags and trailing values shared by both forms. */
    private static final class Parsed {
        int[] numbers;
        int mode;
        boolean infinity;
        boolean slam;
        String text = "TNT";
    }

    private static Parsed parseRest(Kind kind, String[] args, int from, boolean allowInfinity) throws UsageException {
        Parsed out = new Parsed();
        out.numbers = kind.defaults().clone();
        int i = from;
        for (int n = 0; n < out.numbers.length && i < args.length && isInt(args[i]); n++, i++) {
            out.numbers[n] = Integer.parseInt(args[i]);
            if (out.numbers[n] < 1) throw new UsageException(kind.params()[n] + " must be at least 1");
        }
        for (int n = 0; n < out.numbers.length; n++) {
            if (out.numbers[n] < 0) throw new UsageException("Missing <" + kind.params()[n] + ">");
        }
        if (kind.text() && out.numbers[0] > 10) {
            throw new UsageException("Text size must be 1-10");
        }

        for (; i < args.length; i++) {
            String flag = args[i].toLowerCase(Locale.ROOT);
            if (kind.modes() && flag.equals("inward")) {
                out.mode = 1;
            } else if (kind.modes() && flag.equals("outward")) {
                out.mode = 2;
            } else if (allowInfinity && (flag.equals("true") || flag.equals("false"))) {
                out.infinity = flag.equals("true");
            } else if (kind.slam() && flag.equals("slam")) {
                out.slam = true;
            } else {
                break;
            }
        }

        if (kind.text()) {
            if (i >= args.length) throw new UsageException("Missing <text>");
            String text = String.join(" ", java.util.Arrays.copyOfRange(args, i, args.length));
            out.text = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        } else if (i < args.length) {
            throw new UsageException("Unexpected argument: " + args[i]);
        }
        return out;
    }

    private static ItemStack build(Kind kind, Parsed parsed) {
        ItemStack item = kind.builder().build(parsed.numbers, parsed.mode, parsed.text, parsed.infinity);
        return parsed.slam ? OrbitalItems.applySlam(item) : item;
    }

    private void give(CommandSender sender, String[] args) throws UsageException {
        if (!(sender instanceof Player player)) throw new UsageException("Only players can receive orbital rods");
        if (args.length < 2) throw new UsageException("Usage: /ob give <type> [amount] [params] [true|false]");
        String name = args[1].toLowerCase(Locale.ROOT);

        int i = 2;
        int amount = 1;
        if (i < args.length && isInt(args[i])) {
            amount = Integer.parseInt(args[i++]);
            if (amount < 1 || amount > 64) throw new UsageException("Amount must be 1-64");
        }

        List<ItemStack> items = new ArrayList<>();
        String label;
        if (name.equals("stasis")) {
            // /ob give stasis <amount> <x y z> [true|false]
            int[] pos = parsePos(sender, args, i, 3);
            i += 3;
            boolean inf = false;
            if (i < args.length) {
                String flag = args[i++].toLowerCase(Locale.ROOT);
                if (!flag.equals("true") && !flag.equals("false")) throw new UsageException("Unexpected argument: " + flag);
                inf = flag.equals("true");
            }
            if (i < args.length) throw new UsageException("Unexpected argument: " + args[i]);
            for (int n = 0; n < amount; n++) items.add(OrbitalItems.stasis(pos[0], pos[1], pos[2], inf));
            label = "Stasis (" + pos[0] + ", " + pos[1] + ", " + pos[2] + ")";
        } else {
            Kind kind = KINDS.get(name);
            if (kind == null) throw new UsageException("Unknown rod type: " + args[1]);
            Parsed parsed = parseRest(kind, args, i, true);
            for (int n = 0; n < amount; n++) items.add(build(kind, parsed));
            label = plain(items.get(0));
        }

        for (ItemStack item : items) {
            player.getInventory().addItem(item).values()
                    .forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
        }
        sender.sendMessage(Component.text("Gave " + amount + "x " + label, NamedTextColor.GREEN));
    }

    private void summon(CommandSender sender, String sub, String[] args) throws UsageException {
        String name = SUMMON_ALIASES.getOrDefault(sub, sub);
        Player player = sender instanceof Player p ? p : null;
        World world = senderWorld(sender);

        switch (name) {
            case "wolf", "golem" -> {
                Player pl = requirePlayer(player);
                if (args.length < 2 || !isInt(args[1])) throw new UsageException("Usage: /ob " + sub + " <count>");
                int count = Integer.parseInt(args[1]);
                if (count < 1) throw new UsageException("count must be at least 1");
                if (name.equals("wolf")) StrikeLogic.summonWolves(pl, count);
                else StrikeLogic.summonGolems(pl, count);
                return;
            }
            case "wither" -> {
                StrikeLogic.summonWither(requirePlayer(player));
                return;
            }
            case "stasis" -> {
                Player pl = requirePlayer(player);
                int[] pos = parsePos(sender, args, 1, 3);
                StrikeLogic.teleportStasis(pl, pos[0], pos[1], pos[2]);
                return;
            }
            case "stasis_aim" -> {
                Player pl = requirePlayer(player);
                Block target = Strikes.raycast(pl);
                if (target == null) throw new UsageException("You're not looking at a block in range");
                StrikeLogic.teleportStasis(pl, target.getX(), target.getY(), target.getZ());
                return;
            }
            case "stab" -> {
                // /ob stab <x> <z>
                int[] xz = parsePos(sender, args, 1, 2);
                if (args.length > 3) throw new UsageException("Unexpected argument: " + args[3]);
                StrikeLogic.summonStab(world, xz[0], xz[1]);
                return;
            }
            default -> {
            }
        }

        Kind kind = KINDS.get(name);
        if (kind == null || name.equals("railgun")) {
            throw new UsageException("Unknown subcommand: " + args[0] + " (try /ob help)");
        }
        int[] pos = parsePos(sender, args, 1, 3);
        Parsed parsed = parseRest(kind, args, 4, false);
        ItemStack spec = build(kind, parsed);
        try {
            Strikes.execute(kind.shotType(), world, player, spec, pos[0], pos[1], pos[2], 0);
        } catch (RuntimeException | Error e) {
            // Text rendering uses java.awt fonts, which some minimal server images lack.
            throw new UsageException("Strike failed: " + e);
        }
    }

    // ---- Helpers ----

    private static Player requirePlayer(Player player) throws UsageException {
        if (player == null) throw new UsageException("Only players can use this");
        return player;
    }

    private static World senderWorld(CommandSender sender) {
        if (sender instanceof Entity e) return e.getWorld();
        if (sender instanceof BlockCommandSender b) return b.getBlock().getWorld();
        return Bukkit.getWorlds().get(0);
    }

    private static Location senderLocation(CommandSender sender) {
        if (sender instanceof Entity e) return e.getLocation();
        if (sender instanceof BlockCommandSender b) return b.getBlock().getLocation();
        return new Location(senderWorld(sender), 0, 0, 0);
    }

    /** Reads `count` block coordinates (x y z, or x z) starting at args[from]. Supports ~ and ~n. */
    private static int[] parsePos(CommandSender sender, String[] args, int from, int count) throws UsageException {
        if (args.length < from + count) {
            throw new UsageException("Missing position: expected " + (count == 3 ? "<x> <y> <z>" : "<x> <z>"));
        }
        Location base = senderLocation(sender);
        double[] bases = count == 3
                ? new double[]{base.getX(), base.getY(), base.getZ()}
                : new double[]{base.getX(), base.getZ()};
        int[] out = new int[count];
        for (int n = 0; n < count; n++) {
            String tok = args[from + n];
            try {
                double v = tok.startsWith("~")
                        ? bases[n] + (tok.length() > 1 ? Double.parseDouble(tok.substring(1)) : 0)
                        : Double.parseDouble(tok);
                out[n] = (int) Math.floor(v);
            } catch (NumberFormatException e) {
                throw new UsageException("Invalid coordinate: " + tok);
            }
        }
        return out;
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String plain(ItemStack item) {
        Component name = item.getItemMeta().displayName();
        return name == null ? item.getType().name()
                : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name);
    }

    private static void line(CommandSender s, String cmd, String desc) {
        s.sendMessage(Component.text(cmd, NamedTextColor.YELLOW).append(Component.text(" - " + desc, NamedTextColor.GRAY)));
    }

    private static void header(CommandSender s, String text) {
        s.sendMessage(Component.text(text, NamedTextColor.GOLD));
    }

    private static void sendHelp(CommandSender s) {
        header(s, "=== OrbitalStrike Commands ===");
        line(s, "/ob nuke <pos> [rings] [inward|outward] [slam]", "Circular TNT bombardment");
        line(s, "/ob compressed_nuke <pos> [rings] [inward|outward] [slam]", "Organized circular bombardment");
        line(s, "/ob stab <x> <z>", "Vertical TNT attack");
        line(s, "/ob cross|x_shot|star|line <pos> [size] [slam]", "Shape attacks");
        line(s, "/ob ring <pos> [inner] [outer] [slam]", "Donut bombardment");
        line(s, "/ob hexagon|pentagon <pos> [size] [slam]", "Polygon attacks");
        line(s, "/ob heart|star_of_david <pos> [size] [slam]", "Special shapes");
        line(s, "/ob scatter <pos> [radius] [clusters] [slam]", "Carpet bombing");
        line(s, "/ob grid <pos> [size] [spacing] [slam]", "Grid bombardment");
        line(s, "/ob cluster <pos> [count] [radius] [rings] [slam]", "Multi-nuke carpet bombing");
        line(s, "/ob text <pos> <size> [slam] <text>", "Text bombardment");
        line(s, "/ob wither_skull <pos> [skulls]", "Blue wither skull cannon");
        line(s, "/ob wolves|golems <count>", "Summon armies");
        line(s, "/ob stasis <pos> | stasis_aim", "Teleport");
        s.sendMessage(Component.text("/ob wither", NamedTextColor.YELLOW).append(Component.text(" - Summon the Wither", NamedTextColor.RED)));
        header(s, "--- Stab (sky-to-bedrock) ---");
        line(s, "/ob circle_stab|cross_stab|x_stab|... <pos> [size]", "Shape stab attacks");
        line(s, "/ob trench_stab <pos> [length]", "Line stab in facing direction");
        line(s, "/ob text_stab <pos> <size> <text>", "Text stab");
        header(s, "--- Give ---");
        line(s, "/ob give <type> [amount] [params] [true|false] [slam]", "Give orbital rod (true = infinite)");
        line(s, "/ob give stasis <amount> <pos> [true|false]", "Rod that warps to a fixed spot");
        line(s, "/ob give railgun [amount] [arrows] [spread]", "Arrow barrage rod");
    }

    // ---- Tab completion ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("help");
            options.add("give");
            options.add("stab");
            options.addAll(List.of("wolves", "golems", "wither", "stasis", "stasis_aim"));
            for (String k : KINDS.keySet()) {
                if (!k.equals("stab") && !k.equals("railgun") && !k.equals("wolf") && !k.equals("golem")
                        && !k.equals("wither") && !k.equals("stasis_aim")) {
                    options.add(k);
                }
            }
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                options.addAll(KINDS.keySet());
                options.add("stasis");
            } else if (args.length == 3) {
                options.add("1");
            } else {
                Kind kind = KINDS.get(args[1].toLowerCase(Locale.ROOT));
                if (args[1].equalsIgnoreCase("stasis") && args.length <= 6) {
                    options.add("~");
                } else if (kind != null) {
                    int paramIndex = args.length - 4;
                    if (paramIndex < kind.params().length) options.add("<" + kind.params()[paramIndex] + ">");
                    addFlags(options, kind, true);
                } else {
                    options.addAll(List.of("true", "false"));
                }
            }
        } else {
            String name = SUMMON_ALIASES.getOrDefault(args[0].toLowerCase(Locale.ROOT), args[0].toLowerCase(Locale.ROOT));
            int coords = name.equals("stab") ? 2 : 3;
            boolean hasPos = !name.equals("wolf") && !name.equals("golem") && !name.equals("wither") && !name.equals("stasis_aim");
            if (hasPos && args.length <= 1 + coords) {
                options.add("~");
            } else {
                Kind kind = KINDS.get(name);
                if (kind != null && hasPos && !name.equals("stab")) {
                    int paramIndex = args.length - 2 - coords;
                    if (paramIndex < kind.params().length) options.add("<" + kind.params()[paramIndex] + ">");
                    addFlags(options, kind, false);
                }
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.toLowerCase(Locale.ROOT).startsWith(last) && !o.startsWith("<"));
        return options;
    }

    private static void addFlags(List<String> options, Kind kind, boolean give) {
        if (kind.modes()) options.addAll(List.of("inward", "outward"));
        if (give) options.addAll(List.of("true", "false"));
        if (kind.slam()) options.add("slam");
    }
}
