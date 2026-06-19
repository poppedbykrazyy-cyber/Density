package com.densitysmp.density.listeners;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final DensityPlugin plugin;

    public PlayerListener(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDensityManager().initializePlayer(player);

        double density = plugin.getDensityManager().getDensity(player);
        DensityTier tier = plugin.getTierManager().getTier(player);

        if (plugin.getDensityManager().isEliminated(player) && !player.hasPermission("density.bypass")) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("You have been eliminated! Density reached zero.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  DENSITY SMP", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("  Kill. Steal. Flow. Dominate.", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  Density: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", density), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("  Tier: ", NamedTextColor.GRAY)
                .append(Component.text(tier.getDisplayName(), tier.getColor()).decorate(TextDecoration.BOLD)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  /density", NamedTextColor.YELLOW)
                .append(Component.text(" - View your stats", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /densitytop", NamedTextColor.YELLOW)
                .append(Component.text(" - Leaderboard", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDensityManager().saveDensity(event.getPlayer().getUniqueId());
    }
}
