package com.densitysmp.density.listeners;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MovementListener implements Listener {

    private final DensityPlugin plugin;

    public MovementListener(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("action-bar.enabled", true)) {
            double flow = plugin.getFlowManager().getFlow(player);
            if (flow > 0) {
                player.sendActionBar(plugin.getFlowManager().buildFlowBar(player));
            }
        }
    }
}
