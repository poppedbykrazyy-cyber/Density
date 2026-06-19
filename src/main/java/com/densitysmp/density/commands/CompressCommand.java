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

public class CompressCommand implements CommandExecutor {

    private final DensityPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CompressCommand(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only!", NamedTextColor.RED));
            return true;
        }

        int cd = plugin.getConfig().getInt("compress-cooldown", 300);
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId())) {
            long remaining = (cooldowns.get(player.getUniqueId()) + (cd * 1000L) - now) / 1000;
            if (remaining > 0) {
                player.sendMessage(Component.text("Cooldown! " + remaining + "s", NamedTextColor.RED));
                return true;
            }
        }

        DensityManager dm = plugin.getDensityManager();
        double amount = plugin.getConfig().getDouble("compress-amount", 2);

        if (dm.getDensity(player) >= dm.getMaxDensity()) {
            player.sendMessage(Component.text("Already at max density!", NamedTextColor.RED));
            return true;
        }
        if (player.getHealth() <= 4.0) {
            player.sendMessage(Component.text("Need at least 2 hearts!", NamedTextColor.RED));
            return true;
        }

        DensityTier oldTier = plugin.getTierManager().getTier(player);
        dm.addDensity(player, amount);
        player.setHealth(Math.max(1.0, player.getHealth() - 4.0));
        DensityTier newTier = plugin.getTierManager().getTier(player);
        cooldowns.put(player.getUniqueId(), now);

        player.getWorld().spawnParticle(Particle.COMPOSTER, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);

        player.sendMessage(Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("+" + String.format("%.1f", amount) + " density", NamedTextColor.GREEN))
                .append(Component.text(" (Cost: 2 hearts)", NamedTextColor.RED)));

        if (newTier != oldTier) {
            player.sendMessage(Component.text("TIER UP! ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                    .append(Component.text(newTier.getDisplayName(), newTier.getColor()).decorate(TextDecoration.BOLD)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }
        return true;
    }
}
