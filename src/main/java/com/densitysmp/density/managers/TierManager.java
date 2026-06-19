package com.densitysmp.density.managers;

import com.densitysmp.density.DensityPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TierManager {

    private final DensityPlugin plugin;
    private final Map<UUID, Long> autoTotemCooldowns = new HashMap<>();

    public TierManager(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    public DensityTier getTier(double density) {
        if (density <= 0) return DensityTier.ELIMINATED;
        if (density <= 3) return DensityTier.FRAGILE;
        if (density <= 7) return DensityTier.LEAN;
        if (density <= 15) return DensityTier.BALANCED;
        if (density <= 25) return DensityTier.FORTIFIED;
        if (density <= 40) return DensityTier.TITAN;
        return DensityTier.COLOSSUS;
    }

    public DensityTier getTier(Player player) {
        return getTier(plugin.getDensityManager().getDensity(player));
    }

    public void applyTierEffects(Player player) {
        DensityTier tier = getTier(player);
        if (tier == DensityTier.ELIMINATED) return;

        String tierKey = tier.name().toLowerCase();

        int extraHearts = plugin.getConfig().getInt("tiers." + tierKey + ".extra-hearts", 0);
        applyMaxHealth(player, extraHearts);

        double armorToughness = plugin.getConfig().getDouble("tiers." + tierKey + ".armor-toughness", 0);
        applyArmorToughness(player, armorToughness);

        FlowManager fm = plugin.getFlowManager();
        double flowSpeed = fm.getFlowSpeedBoost(player);
        applySpeedBoost(player, flowSpeed);

        if (fm.hasFlowRegen(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, false));
        }

        if (fm.hasFlowStrength(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false, false));
        }

        if (plugin.getConfig().getBoolean("particles.enabled") && plugin.getConfig().getBoolean("particles.aura")) {
            spawnAuraParticles(player, tier);
        }
    }

    private void applyMaxHealth(Player player, int extraHearts) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr == null) return;

        healthAttr.getModifiers().stream()
                .filter(m -> m.getKey().toString().equals("densitysmp:health"))
                .forEach(healthAttr::removeModifier);

        if (extraHearts != 0) {
            AttributeModifier modifier = new AttributeModifier(
                    org.bukkit.NamespacedKey.fromString("densitysmp:health"),
                    extraHearts,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            healthAttr.addModifier(modifier);
        }
    }

    private void applyArmorToughness(Player player, double bonus) {
        AttributeInstance toughnessAttr = player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
        if (toughnessAttr == null) return;

        toughnessAttr.getModifiers().stream()
                .filter(m -> m.getKey().toString().equals("densitysmp:toughness"))
                .forEach(toughnessAttr::removeModifier);

        if (bonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                    org.bukkit.NamespacedKey.fromString("densitysmp:toughness"),
                    bonus,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            toughnessAttr.addModifier(modifier);
        }
    }

    private void applySpeedBoost(Player player, double boost) {
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr == null) return;

        speedAttr.getModifiers().stream()
                .filter(m -> m.getKey().toString().equals("densitysmp:flow_speed"))
                .forEach(speedAttr::removeModifier);

        if (boost > 0) {
            AttributeModifier modifier = new AttributeModifier(
                    org.bukkit.NamespacedKey.fromString("densitysmp:flow_speed"),
                    boost,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
            speedAttr.addModifier(modifier);
        }
    }

    private void spawnAuraParticles(Player player, DensityTier tier) {
        Location loc = player.getLocation().add(0, 1, 0);
        FlowLevel flowLevel = plugin.getFlowManager().getFlowLevel(player);

        switch (tier) {
            case FRAGILE -> player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0.3, 0.2, 0.005);
            case FORTIFIED -> player.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.3, 0.3, 0.3, 0.02);
            case TITAN -> player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.3, 0.3, 0.3, 0.01);
            case COLOSSUS -> {
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 5, 0.4, 0.4, 0.4, 0.03);
                player.getWorld().spawnParticle(Particle.WITCH, loc, 2, 0.3, 0.4, 0.3, 0.01);
            }
            default -> {}
        }

        if (flowLevel.ordinal() >= FlowLevel.FLOWING.ordinal()) {
            player.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.3, 0.2, 0.02);
        }
        if (flowLevel == FlowLevel.TRANSCENDENT) {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 3, 0.3, 0.5, 0.3, 0.02);
        }
    }

    public double getDamageBonus(Player player) {
        DensityTier tier = getTier(player);
        String tierKey = tier.name().toLowerCase();
        double tierBonus = plugin.getConfig().getDouble("tiers." + tierKey + ".damage-bonus", 0);
        double flowBonus = plugin.getFlowManager().getFlowDamageBonus(player);
        return tierBonus + flowBonus;
    }

    public double getLifesteal(Player player) {
        DensityTier tier = getTier(player);
        String tierKey = tier.name().toLowerCase();
        return plugin.getConfig().getDouble("tiers." + tierKey + ".lifesteal", 0);
    }

    public double getThorns(Player player) {
        DensityTier tier = getTier(player);
        String tierKey = tier.name().toLowerCase();
        return plugin.getConfig().getDouble("tiers." + tierKey + ".thorns", 0);
    }

    public boolean hasAutoTotem(Player player) {
        DensityTier tier = getTier(player);
        String tierKey = tier.name().toLowerCase();
        return plugin.getConfig().getBoolean("tiers." + tierKey + ".auto-totem", false);
    }

    public boolean tryAutoTotem(Player player) {
        if (!hasAutoTotem(player)) return false;

        long now = System.currentTimeMillis();
        Long lastUse = autoTotemCooldowns.get(player.getUniqueId());
        int cooldownSeconds = plugin.getConfig().getInt("tiers.colossus.auto-totem-cooldown", 300);

        if (lastUse != null && (now - lastUse) < cooldownSeconds * 1000L) return false;

        autoTotemCooldowns.put(player.getUniqueId(), now);
        return true;
    }
}
