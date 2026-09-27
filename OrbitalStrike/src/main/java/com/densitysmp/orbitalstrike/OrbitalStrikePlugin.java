package com.densitysmp.orbitalstrike;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrbitalStrikePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Text shots rasterise with java.awt; make sure it never tries to open a display.
        System.setProperty("java.awt.headless", "true");

        OrbitalCommand command = new OrbitalCommand();
        PluginCommand ob = getCommand("ob");
        if (ob != null) {
            ob.setExecutor(command);
            ob.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new RodListener(this), this);
        getServer().getScheduler().runTaskTimer(this, StrikeLogic::tickSlam, 1L, 1L);
        getLogger().info("OrbitalStrike ready");
    }

    @Override
    public void onDisable() {
        StrikeLogic.clearSlam();
    }
}
