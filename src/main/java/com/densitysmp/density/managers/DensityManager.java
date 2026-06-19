package com.densitysmp.density.managers;

import com.densitysmp.density.DensityPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class DensityManager {

    private final DensityPlugin plugin;
    private final Map<UUID, Double> densityMap = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final double startingDensity;
    private final double maxDensity;
    private final double minDensity;
    private final double densityPerKill;

    public DensityManager(DensityPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");

        startingDensity = plugin.getConfig().getDouble("starting-density", 10);
        maxDensity = plugin.getConfig().getDouble("max-density", 50);
        minDensity = plugin.getConfig().getDouble("min-density", 0);
        densityPerKill = plugin.getConfig().getDouble("density-per-kill", 1);

        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                double density = dataConfig.getDouble("players." + key + ".density");
                densityMap.put(uuid, density);
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Double> entry : densityMap.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString() + ".density", entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
        }
    }

    public void saveDensity(UUID uuid) {
        dataConfig.set("players." + uuid.toString() + ".density", getDensity(uuid));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save density for " + uuid);
        }
    }

    public double getDensity(UUID uuid) {
        return densityMap.getOrDefault(uuid, startingDensity);
    }

    public double getDensity(Player player) {
        return getDensity(player.getUniqueId());
    }

    public void setDensity(UUID uuid, double density) {
        density = Math.max(minDensity, Math.min(maxDensity, density));
        densityMap.put(uuid, density);
        saveDensity(uuid);
    }

    public void setDensity(Player player, double density) {
        setDensity(player.getUniqueId(), density);
    }

    public void addDensity(Player player, double amount) {
        double oldDensity = getDensity(player);
        setDensity(player, oldDensity + amount);
    }

    public void removeDensity(Player player, double amount) {
        double oldDensity = getDensity(player);
        setDensity(player, oldDensity - amount);
    }

    public void initializePlayer(Player player) {
        if (!densityMap.containsKey(player.getUniqueId())) {
            densityMap.put(player.getUniqueId(), startingDensity);
            saveDensity(player.getUniqueId());
        }
    }

    public void handleKill(Player killer, Player victim) {
        double stealAmount = Math.min(densityPerKill, getDensity(victim));
        addDensity(killer, stealAmount);
        removeDensity(victim, stealAmount);
    }

    public boolean isEliminated(Player player) {
        return getDensity(player) <= minDensity;
    }

    public void resetDensity(Player player) {
        setDensity(player, startingDensity);
    }

    public double getMaxDensity() {
        return maxDensity;
    }

    public double getMinDensity() {
        return minDensity;
    }

    public double getStartingDensity() {
        return startingDensity;
    }

    public double getDensityPerKill() {
        return densityPerKill;
    }

    public LinkedHashMap<UUID, Double> getTopDensities(int limit) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(densityMap.entrySet());
        sorted.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());

        LinkedHashMap<UUID, Double> top = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<UUID, Double> entry : sorted) {
            if (count >= limit) break;
            top.put(entry.getKey(), entry.getValue());
            count++;
        }
        return top;
    }

    public Map<UUID, Double> getAllDensities() {
        return new HashMap<>(densityMap);
    }
}
