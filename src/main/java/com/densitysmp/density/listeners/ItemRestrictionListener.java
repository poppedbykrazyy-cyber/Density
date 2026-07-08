package com.densitysmp.density.listeners;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ItemRestrictionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 3000;

    private final DensityPlugin plugin;
    private final Set<Material> bannedItems = EnumSet.noneOf(Material.class);
    private final int pearlLimit;
    private final boolean enabled;
    private final Map<UUID, Long> lastNotified = new HashMap<>();

    public ItemRestrictionListener(DensityPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("item-restrictions.enabled", true);
        this.pearlLimit = plugin.getConfig().getInt("item-restrictions.ender-pearl-limit", 16);

        for (String name : plugin.getConfig().getStringList("item-restrictions.banned-items")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                bannedItems.add(material);
            } else {
                plugin.getLogger().warning("Unknown banned item in config: " + name);
            }
        }

        if (enabled) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
                    plugin.getServer().getOnlinePlayers().forEach(this::enforce), 20L, 20L);
        }
    }

    // Banned items never show up as a craft result
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!enabled) return;
        ItemStack result = event.getInventory().getResult();
        if (result != null && bannedItems.contains(result.getType())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!enabled) return;
        if (bannedItems.contains(event.getRecipe().getResult().getType())) {
            event.setCancelled(true);
        }
    }

    // Totems never fire even if one slips into a hand for a tick.
    // The Colossus auto-totem perk cancels damage directly and never
    // reaches this event, so it keeps working.
    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!enabled) return;
        if (event.getEntity() instanceof Player && bannedItems.contains(Material.TOTEM_OF_UNDYING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem().getItemStack();

        if (bannedItems.contains(stack.getType())) {
            event.setCancelled(true);
            event.getItem().remove();
            notifyBanned(player, stack.getType());
            return;
        }

        if (stack.getType() == Material.ENDER_PEARL && pearlLimit > 0) {
            int current = countPearls(player);
            if (current >= pearlLimit) {
                event.setCancelled(true);
                notifyPearlLimit(player);
            } else if (current + stack.getAmount() > pearlLimit) {
                // Pick up only what fits under the limit, leave the rest on the ground
                int allowed = pearlLimit - current;
                event.setCancelled(true);
                ItemStack partial = stack.clone();
                partial.setAmount(allowed);
                int notAdded = player.getInventory().addItem(partial).values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                stack.setAmount(stack.getAmount() - (allowed - notAdded));
                event.getItem().setItemStack(stack);
                notifyPearlLimit(player);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        enforce(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastNotified.remove(event.getPlayer().getUniqueId());
    }

    private void enforce(Player player) {
        PlayerInventory inv = player.getInventory();
        boolean removedBanned = false;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && bannedItems.contains(item.getType())) {
                inv.setItem(slot, null);
                removedBanned = true;
            }
        }

        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && bannedItems.contains(cursor.getType())) {
            player.getOpenInventory().setCursor(null);
            removedBanned = true;
        }

        if (removedBanned) {
            notifyBanned(player, null);
        }

        if (pearlLimit > 0) {
            int excess = countPearls(player) - pearlLimit;
            if (excess > 0) {
                trimPearls(player, excess);
                notifyPearlLimit(player);
            }
        }
    }

    private int countPearls(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    // Removes excess pearls from the inventory and drops them at the
    // player's feet; the pickup cap stops them from being re-collected.
    private void trimPearls(Player player, int excess) {
        PlayerInventory inv = player.getInventory();
        int toDrop = excess;
        for (int slot = inv.getSize() - 1; slot >= 0 && excess > 0; slot--) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() != Material.ENDER_PEARL) continue;
            int take = Math.min(excess, item.getAmount());
            if (take >= item.getAmount()) {
                inv.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
            excess -= take;
        }
        while (toDrop > 0) {
            int amount = Math.min(toDrop, 16);
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.ENDER_PEARL, amount));
            toDrop -= amount;
        }
    }

    private void notifyBanned(Player player, Material material) {
        if (!canNotify(player)) return;
        String what = material != null ? prettyName(material) : "Shields and totems";
        player.sendMessage(Component.text(what + " ", NamedTextColor.RED)
                .append(Component.text(material != null ? "is banned on this server!" : "are banned on this server!", NamedTextColor.GRAY)));
    }

    private void notifyPearlLimit(Player player) {
        if (!canNotify(player)) return;
        player.sendMessage(Component.text("Ender pearl limit: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("you can only carry " + pearlLimit + "!", NamedTextColor.GRAY)));
    }

    private boolean canNotify(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastNotified.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MS) {
            return false;
        }
        lastNotified.put(player.getUniqueId(), now);
        return true;
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
