package com.densitysmp.orbitalstrike;

import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

/** Fires the strike an orbital rod describes. Shared by rods (fired 50 blocks up) and /ob (fired at the given spot). */
public final class Strikes {
    private Strikes() {
    }

    /** The block the player is looking at, out to the server view distance. */
    public static Block raycast(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        double range = world.getViewDistance() * 16.0;
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true);
        return hit == null ? null : hit.getHitBlock();
    }

    /**
     * @param player may be null for strikes that don't summon or teleport; line shapes then face south
     * @param heightOffset added to y for falling TNT (rods use 50)
     */
    public static void execute(ShotType type, World world, Player player, ItemStack spec, int x, int y, int z, int heightOffset) {
        if (OrbitalItems.flag(spec, OrbitalItems.PENETRATE)) {
            List<double[]> offsets = offsets(type, spec, player);
            if (offsets != null) {
                StrikeLogic.summonPatternStab(world, x, z, offsets);
                return;
            }
        }
        boolean slam = OrbitalItems.flag(spec, OrbitalItems.SLAM) && type.supportsSlam();
        if (slam) {
            StrikeLogic.beginSlamVolley();
        }
        try {
            fire(type, world, player, spec, x, y, z, y + heightOffset);
        } finally {
            if (slam) {
                StrikeLogic.endSlamVolley();
            }
        }
    }

    static List<double[]> offsets(ShotType type, ItemStack s, Player player) {
        return switch (type) {
            case NUKE, COMPRESSED_NUKE -> TntShapes.compressedNuke(OrbitalItems.integer(s, OrbitalItems.RINGS, 3));
            case CROSS -> TntShapes.cross(OrbitalItems.integer(s, OrbitalItems.SCALE, 10), OrbitalItems.integer(s, OrbitalItems.THICKNESS, 1));
            case X_SHOT -> TntShapes.xShape(OrbitalItems.integer(s, OrbitalItems.SCALE, 15));
            case RING -> TntShapes.ring(OrbitalItems.integer(s, OrbitalItems.RADIUS_MIN, 5), OrbitalItems.integer(s, OrbitalItems.RADIUS_MAX, 15));
            case LINE -> TntShapes.line(OrbitalItems.integer(s, OrbitalItems.LENGTH, 20),
                    player == null ? 0.0 : Math.toRadians(player.getLocation().getYaw()));
            case HEXAGON -> TntShapes.hexagon(OrbitalItems.integer(s, OrbitalItems.SCALE, 15));
            case PENTAGON -> TntShapes.pentagon(OrbitalItems.integer(s, OrbitalItems.SCALE, 15));
            case STAR -> TntShapes.star(OrbitalItems.integer(s, OrbitalItems.SCALE, 15));
            case STAR_OF_DAVID -> TntShapes.starOfDavid(OrbitalItems.integer(s, OrbitalItems.SCALE, 15));
            case HEART -> TntShapes.heart(OrbitalItems.integer(s, OrbitalItems.SCALE, 10));
            case SCATTER -> TntShapes.scatter(OrbitalItems.integer(s, OrbitalItems.SCALE, 20), OrbitalItems.integer(s, OrbitalItems.RINGS, 8));
            case GRID -> TntShapes.grid(OrbitalItems.integer(s, OrbitalItems.SCALE, 20), OrbitalItems.integer(s, OrbitalItems.GAP, 4));
            case TEXT -> TntShapes.text(OrbitalItems.string(s, OrbitalItems.MESSAGE, "TNT"), OrbitalItems.integer(s, OrbitalItems.SCALE, 3));
            default -> null;
        };
    }

    private static void fire(ShotType type, World w, Player player, ItemStack s, int x, int y, int z, int yUp) {
        switch (type) {
            case NUKE -> StrikeLogic.summonNuke(w, x, yUp, z, OrbitalItems.integer(s, OrbitalItems.RINGS, 3), OrbitalItems.integer(s, OrbitalItems.MODE, 0));
            case COMPRESSED_NUKE -> StrikeLogic.summonCompressedNuke(w, x, yUp, z, OrbitalItems.integer(s, OrbitalItems.RINGS, 3), OrbitalItems.integer(s, OrbitalItems.MODE, 0));
            case CLUSTER -> StrikeLogic.summonCluster(w, x, yUp, z,
                    OrbitalItems.integer(s, OrbitalItems.CLUSTER_COUNT, 5),
                    OrbitalItems.integer(s, OrbitalItems.CLUSTER_RADIUS, 30),
                    OrbitalItems.integer(s, OrbitalItems.RINGS, 2));
            case CROSS, X_SHOT, RING, LINE, HEXAGON, PENTAGON, STAR, STAR_OF_DAVID, HEART, SCATTER, GRID, TEXT ->
                    StrikeLogic.spawnPattern(w, x, yUp, z, offsets(type, s, player));
            case STAB -> StrikeLogic.summonStab(w, x, z);
            case RAILGUN -> StrikeLogic.summonRailgun(w, x, y, z,
                    OrbitalItems.integer(s, OrbitalItems.PROJECTILES, 64),
                    OrbitalItems.decimal(s, OrbitalItems.SPREAD, 3.0));
            case WITHER_SKULL -> StrikeLogic.summonWitherSkullCannon(w, x, y, z, OrbitalItems.integer(s, OrbitalItems.SKULL_COUNT, 200), 20);
            case WOLF -> StrikeLogic.summonWolves(player, OrbitalItems.integer(s, OrbitalItems.PACK_SIZE, 10));
            case STASIS -> StrikeLogic.teleportStasis(player,
                    OrbitalItems.integer(s, OrbitalItems.WARP_X, 0),
                    OrbitalItems.integer(s, OrbitalItems.WARP_Y, 0),
                    OrbitalItems.integer(s, OrbitalItems.WARP_Z, 0));
            case STASIS_AIM -> StrikeLogic.teleportStasis(player, x, y + 1, z);
            case GOLEM -> StrikeLogic.summonGolems(player, OrbitalItems.integer(s, OrbitalItems.GOLEM_AMOUNT, 5));
            case WITHER -> StrikeLogic.summonWither(player);
        }
    }
}
