package com.densitysmp.density;

import com.densitysmp.density.commands.CompressCommand;
import com.densitysmp.density.commands.DensityAdminCommand;
import com.densitysmp.density.commands.DensityCommand;
import com.densitysmp.density.commands.DensityTopCommand;
import com.densitysmp.density.commands.ExpandCommand;
import com.densitysmp.density.listeners.CombatListener;
import com.densitysmp.density.listeners.ItemRestrictionListener;
import com.densitysmp.density.listeners.MovementListener;
import com.densitysmp.density.listeners.PlayerListener;
import com.densitysmp.density.managers.DensityManager;
import com.densitysmp.density.managers.FlowManager;
import com.densitysmp.density.managers.ScoreboardManager;
import com.densitysmp.density.managers.TierManager;
import com.densitysmp.density.weapons.AltarManager;
import com.densitysmp.density.weapons.WeaponListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DensityPlugin extends JavaPlugin {

    private static DensityPlugin instance;
    private DensityManager densityManager;
    private TierManager tierManager;
    private FlowManager flowManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        tierManager = new TierManager(this);
        densityManager = new DensityManager(this);
        flowManager = new FlowManager(this);
        scoreboardManager = new ScoreboardManager(this);

        getCommand("density").setExecutor(new DensityCommand(this));
        getCommand("densitytop").setExecutor(new DensityTopCommand(this));
        getCommand("densityadmin").setExecutor(new DensityAdminCommand(this));
        getCommand("compress").setExecutor(new CompressCommand(this));
        getCommand("expand").setExecutor(new ExpandCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarManager(this), this);
        getServer().getPluginManager().registerEvents(new ItemRestrictionListener(this), this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            getServer().getOnlinePlayers().forEach(player -> {
                tierManager.applyTierEffects(player);
                scoreboardManager.updateScoreboard(player);
            });
        }, 20L, 40L);

        getLogger().info("DensitySMP enabled! Kill. Steal. Flow. Dominate.");
    }

    @Override
    public void onDisable() {
        if (densityManager != null) {
            densityManager.saveAll();
        }
        getLogger().info("DensitySMP disabled.");
    }

    public static DensityPlugin getInstance() { return instance; }
    public DensityManager getDensityManager() { return densityManager; }
    public TierManager getTierManager() { return tierManager; }
    public FlowManager getFlowManager() { return flowManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
