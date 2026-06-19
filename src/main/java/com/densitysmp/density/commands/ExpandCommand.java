package com.densitysmp.density.commands;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityManager;
import com.densitysmp.density.managers.DensityTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExpandCommand implements CommandExecutor {

    private final DensityPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ExpandCommand(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only!", NamedTextColor.RED));
            return true;
        }

        int cd = plugin.getConfig().getInt("expand-cooldown", 300);
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId())) {
            long remaining = (cooldowns.get(player.getUniqueId()) + (cd * 1000L) - now) / 1000;
            if (remaining > 0) {
                player.sendMessage(Component.text("Cooldown! " + remaining + "s", NamedTextColor.RED));
                return true;
            }
        }

        DensityManager dm = plugin.getDensityManager();
        double amount = plugin.getConfig().getDouble("expand-amount", 2);

        if (dm.getDensity(player) <= amount) {
            player.sendMessage(Component.text("Not enough density! You'd be eliminated!", NamedTextColor.RED));
            return true;
        }

        DensityTier oldTier = plugin.getTierManager().getTier(player);
        dm.removeDensity(player, amount);
        DensityTier newTier = plugin.getTierManager().getTier(player);
        cooldowns.put(player.getUniqueId(), now);

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.08);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        player.sendMessage(Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("-" + String.format("%.1f", amount) + " density", NamedTextColor.AQUA))
                .append(Component.text(" (Shed mass)", NamedTextColor.GRAY)));

        if (newTier != oldTier) {
            player.sendMessage(Component.text("TIER CHANGE! ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                    .append(Component.text(newTier.getDisplayName(), newTier.getColor()).decorate(TextDecoration.BOLD)));
        }
        return true;
    }
}
