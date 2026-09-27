package com.densitysmp.orbitalstrike;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Orbital rods fire when reeled in (strikes need a bobber out first, like the original mod),
 * except the "instant" types (wolves, golems, wither, stasis, stasis aim) which fire on cast.
 */
public final class RodListener implements Listener {
    private final Plugin plugin;

    public RodListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD
                ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        ItemStack item = player.getInventory().getItem(hand);
        if (!OrbitalItems.isOrbital(item) || !player.hasPermission("orbitalstrike.use")) {
            return;
        }
        ShotType type = OrbitalItems.type(item);
        if (type == null) {
            return;
        }

        boolean casting = event.getState() == PlayerFishEvent.State.FISHING;
        boolean reeling = switch (event.getState()) {
            case CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND, FAILED_ATTEMPT, REEL_IN -> true;
            default -> false;
        };
        if (type.isInstant() ? !casting : !reeling) {
            return;
        }

        int x = 0, y = 0, z = 0;
        if (!type.needsNoTarget()) {
            Block target = Strikes.raycast(player);
            if (target == null) {
                return;
            }
            x = target.getX();
            y = target.getY();
            z = target.getZ();
        }

        if (type.isInstant()) {
            // Don't throw a bobber for instant rods.
            event.setCancelled(true);
        }

        Strikes.execute(type, player.getWorld(), player, item, x, y, z, 50);

        if (!OrbitalItems.flag(item, OrbitalItems.INFINITE) && player.getGameMode() != GameMode.CREATIVE) {
            String uid = OrbitalItems.uid(item);
            if (type.isInstant()) {
                breakRod(player, hand, uid);
            } else {
                // Let the vanilla reel-in finish first.
                plugin.getServer().getScheduler().runTask(plugin, () -> breakRod(player, hand, uid));
            }
        }
    }

    private static void breakRod(Player player, EquipmentSlot hand, String uid) {
        if (!player.isOnline()) {
            return;
        }
        ItemStack current = player.getInventory().getItem(hand);
        if (OrbitalItems.isOrbital(current) && uid != null && uid.equals(OrbitalItems.uid(current))) {
            player.getInventory().setItem(hand, null);
            player.broadcastSlotBreak(hand);
        }
    }
}
