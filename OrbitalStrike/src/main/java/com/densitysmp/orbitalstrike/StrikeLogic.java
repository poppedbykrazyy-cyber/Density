package com.densitysmp.orbitalstrike;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class StrikeLogic {
    private static final int BASE_FUSE = 75;
    private static final double FALL_VELOCITY = -0.035;
    private static final double SPREAD_MULT = 1.35;
    private static final int FUSE_STEP = 2;
    private static final int SLAM_FUSE = 1200;
    private static final int SLAM_TIMEOUT_TICKS = 300;

    private static final List<SlamVolley> slamVolleys = new ArrayList<>();
    private static List<TNTPrimed> slamCollector = null;

    private StrikeLogic() {
    }

    // ---- TNT helpers ----

    private static void spawnTnt(World world, double x, double y, double z, int fuse, double vx, double vy, double vz) {
        TNTPrimed tnt = world.spawn(new Location(world, x, y, z), TNTPrimed.class, t -> {
            t.setFuseTicks(slamCollector != null ? SLAM_FUSE : fuse);
            t.setVelocity(new Vector(vx, vy, vz));
        });
        if (slamCollector != null) {
            slamCollector.add(tnt);
        }
    }

    /** Launches one TNT per offset from the centre so it drifts outward and lands on that offset. */
    public static void spawnPattern(World world, int cx, int cy, int cz, List<double[]> offsets, int fuse) {
        for (double[] off : offsets) {
            double vx = off[0] / BASE_FUSE * SPREAD_MULT;
            double vz = off[1] / BASE_FUSE * SPREAD_MULT;
            spawnTnt(world, cx + 0.5, cy, cz + 0.5, fuse, vx, FALL_VELOCITY, vz);
        }
    }

    public static void spawnPattern(World world, int cx, int cy, int cz, List<double[]> offsets) {
        spawnPattern(world, cx, cy, cz, offsets, BASE_FUSE);
    }

    // ---- Nukes ----

    /** mode 0 = all at once, 1 = inward (outer rings first), 2 = outward (centre first). */
    public static void summonNuke(World world, int x, int y, int z, int rings, int mode) {
        int coreFuse = mode == 1 ? BASE_FUSE + rings * FUSE_STEP : BASE_FUSE;
        spawnTnt(world, x + 0.5, y, z + 0.5, coreFuse, 0.0, FALL_VELOCITY, 0.0);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int ring = 0; ring < rings; ring++) {
            int r = TntShapes.ringRadius(ring);
            int count = TntShapes.ringCount(ring, r);
            int fuse = switch (mode) {
                case 1 -> BASE_FUSE + (rings - 1 - ring) * FUSE_STEP;
                case 2 -> BASE_FUSE + (ring + 1) * FUSE_STEP;
                default -> BASE_FUSE;
            };
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double vx = Math.cos(angle) * ((double) r / BASE_FUSE) * SPREAD_MULT;
                double vz = Math.sin(angle) * ((double) r / BASE_FUSE) * SPREAD_MULT;
                spawnTnt(world, x + 0.5, y, z + 0.5, fuse, vx, FALL_VELOCITY, vz);
            }
        }
    }

    public static void summonCompressedNuke(World world, int x, int y, int z, int rings, int mode) {
        List<List<double[]>> ringData = TntShapes.compressedNukeByRing(rings);
        for (int ring = 0; ring < ringData.size(); ring++) {
            int fuse = switch (mode) {
                case 1 -> BASE_FUSE + (ringData.size() - 1 - ring) * FUSE_STEP;
                case 2 -> BASE_FUSE + ring * FUSE_STEP;
                default -> BASE_FUSE;
            };
            spawnPattern(world, x, y, z, ringData.get(ring), fuse);
        }
    }

    public static void summonCluster(World world, int x, int y, int z, int clusterCount, int radius, int rings) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int c = 0; c < clusterCount; c++) {
            double angle = rng.nextDouble() * 2.0 * Math.PI;
            double dist = rng.nextDouble() * radius;
            int cx = x + (int) Math.round(Math.cos(angle) * dist);
            int cz = z + (int) Math.round(Math.sin(angle) * dist);
            summonNuke(world, cx, y, cz, rings, 0);
        }
    }

    // ---- Stabs ----

    /** Two instant-fuse TNT on every Y level from build limit down to the bottom of the world. */
    public static void summonStab(World world, int x, int z) {
        int minY = world.getMinHeight();
        int topY = world.getMaxHeight() - 1;
        for (int y = topY; y >= minY; y--) {
            for (int i = 0; i < 2; i++) {
                world.spawn(new Location(world, x + 0.5, y, z + 0.5), TNTPrimed.class, t -> {
                    t.setFuseTicks(0);
                    t.setVelocity(new Vector());
                });
            }
        }
    }

    public static void summonPatternStab(World world, int cx, int cz, List<double[]> offsets) {
        for (double[] off : offsets) {
            summonStab(world, cx + (int) Math.round(off[0]), cz + (int) Math.round(off[1]));
        }
    }

    // ---- Summons ----

    private static Vector randomLaunch(double speed, double lift, double base) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double yaw = rng.nextDouble() * 2.0 * Math.PI;
        double pitch = (rng.nextDouble() - 0.5) * Math.PI * 0.5;
        return new Vector(
                Math.cos(yaw) * Math.cos(pitch) * speed,
                Math.sin(pitch) * lift + base,
                Math.sin(yaw) * Math.cos(pitch) * speed);
    }

    public static void summonWolves(Player player, int packSize) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        for (int i = 0; i < packSize; i++) {
            Wolf wolf = world.spawn(origin, Wolf.class, w -> {
                w.setTamed(true);
                w.setOwner(player);
                w.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.WOLF_ARMOR));
                w.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1, false, true));
                w.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 3600, 1, false, true));
                w.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 9600, 0, false, true));
            });
            wolf.setVelocity(randomLaunch(0.8, 0.6, 0.3));
        }
    }

    public static void summonGolems(Player player, int amount) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        for (int i = 0; i < amount; i++) {
            IronGolem golem = world.spawn(origin, IronGolem.class, g -> g.setPlayerCreated(true));
            golem.setVelocity(randomLaunch(0.6, 0.4, 0.3));
        }
    }

    public static void summonWither(Player player) {
        player.getWorld().spawn(player.getLocation().add(0.0, 3.0, 0.0), Wither.class);
    }

    public static void teleportStasis(Player player, int x, int y, int z) {
        World world = player.getWorld();
        Location from = player.getLocation();
        Location to = new Location(world, x + 0.5, y, z + 0.5, from.getYaw(), from.getPitch());
        effectsAt(from);
        player.teleport(to);
        effectsAt(to);
    }

    private static void effectsAt(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.spawnParticle(Particle.PORTAL, loc.clone().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.5);
    }

    public static void summonRailgun(World world, int x, int y, int z, int arrowCount, double spreadRadius) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int spawnY = world.getMaxHeight() - 10;
        for (int i = 0; i < arrowCount; i++) {
            double sx = x + 0.5 + (rng.nextDouble() - 0.5) * spreadRadius * 2.0;
            double sz = z + 0.5 + (rng.nextDouble() - 0.5) * spreadRadius * 2.0;
            world.spawn(new Location(world, sx, spawnY, sz), Arrow.class, a -> {
                a.setVelocity(new Vector(0.0, -100.0, 0.0));
                a.setCritical(true);
                a.setDamage(20.0);
                a.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
            });
        }
    }

    /** Charged (blue) wither skulls sprayed downward from `altitude` blocks above the target, plus one TNT to kick it off. */
    public static void summonWitherSkullCannon(World world, int x, int y, int z, int skullCount, int altitude) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double cx = x + 0.5;
        double cz = z + 0.5;
        double topY = y + altitude;
        double speed = 3.0;
        for (int i = 0; i < skullCount; i++) {
            double theta = rng.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(rng.nextDouble());
            double horiz = Math.sin(phi);
            double vert = -Math.cos(phi);
            double rr = rng.nextDouble() * 2.0;
            Location at = new Location(world, cx + Math.cos(theta) * rr, topY, cz + Math.sin(theta) * rr);
            Vector velocity = new Vector(horiz * Math.cos(theta) * speed, vert * speed, horiz * Math.sin(theta) * speed);
            world.spawn(at, WitherSkull.class, s -> {
                s.setCharged(true);
                s.setVelocity(velocity);
            });
        }
        world.spawn(new Location(world, cx, topY + 2.0, cz), TNTPrimed.class, t -> {
            t.setFuseTicks(0);
            t.setVelocity(new Vector());
        });
    }

    // ---- Slam: TNT waits on the ground, then the whole volley detonates together ----

    public static void beginSlamVolley() {
        slamCollector = new ArrayList<>();
    }

    public static void endSlamVolley() {
        if (slamCollector != null && !slamCollector.isEmpty()) {
            slamVolleys.add(new SlamVolley(slamCollector));
        }
        slamCollector = null;
    }

    public static void tickSlam() {
        Iterator<SlamVolley> volleys = slamVolleys.iterator();
        while (volleys.hasNext()) {
            SlamVolley volley = volleys.next();
            volley.age++;
            boolean allLanded = true;
            Iterator<TNTPrimed> it = volley.tnt.iterator();
            while (it.hasNext()) {
                TNTPrimed t = it.next();
                if (!t.isValid()) {
                    it.remove();
                } else if (!t.isOnGround()) {
                    allLanded = false;
                }
            }
            if (volley.tnt.isEmpty()) {
                volleys.remove();
            } else if (allLanded || volley.age >= SLAM_TIMEOUT_TICKS) {
                for (TNTPrimed t : volley.tnt) {
                    t.setFuseTicks(0);
                }
                volleys.remove();
            }
        }
    }

    public static void clearSlam() {
        slamVolleys.clear();
        slamCollector = null;
    }

    private static final class SlamVolley {
        final List<TNTPrimed> tnt;
        int age;

        SlamVolley(List<TNTPrimed> tnt) {
            this.tnt = tnt;
        }
    }
}
