package com.densitysmp.density.managers;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlowManager {

    private final DensityPlugin plugin;
    private final Map<UUID, Double> flowMap = new HashMap<>();
    private final Map<UUID, Long> lastCombatTime = new HashMap<>();
    private final Map<UUID, FlowLevel> currentLevel = new HashMap<>();

    private final double maxFlow;
    private final double meleeGain;
    private final double arrowGain;
    private final double killGain;
    private final double damageLoss;
    private final double idleDecay;
    private final int combatTimeout;

    public FlowManager(DensityPlugin plugin) {
        this.plugin = plugin;

        maxFlow = plugin.getConfig().getDouble("flow.max-flow", 100);
        meleeGain = plugin.getConfig().getDouble("flow.gain.melee-hit", 5);
        arrowGain = plugin.getConfig().getDouble("flow.gain.arrow-hit", 4);
        killGain = plugin.getConfig().getDouble("flow.gain.kill", 25);
        damageLoss = plugin.getConfig().getDouble("flow.loss.take-damage", 15);
        idleDecay = plugin.getConfig().getDouble("flow.loss.idle-decay-per-second", 2);
        combatTimeout = plugin.getConfig().getInt("flow.combat-timeout", 8);

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDecay, 20L, 20L);
    }

    public double getFlow(Player player) {
        return flowMap.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void addFlow(Player player, double amount) {
        double current = getFlow(player);
        double newFlow = Math.min(maxFlow, current + amount);
        setFlow(player, newFlow);
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void removeFlow(Player player, double amount) {
        double current = getFlow(player);
        double newFlow = Math.max(0, current - amount);
        setFlow(player, newFlow);
    }

    private void setFlow(Player player, double flow) {
        double oldFlow = getFlow(player);
        flowMap.put(player.getUniqueId(), flow);

        FlowLevel oldLevel = getFlowLevel(oldFlow);
        FlowLevel newLevel = getFlowLevel(flow);

        if (newLevel.ordinal() > oldLevel.ordinal()) {
            player.sendMessage(Component.text()
                    .append(Component.text("FLOW ", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                    .append(Component.text(newLevel.getDisplayName(), newLevel.getColor()).decorate(TextDecoration.BOLD))
                    .build());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f + (newLevel.ordinal() * 0.2f));
        }

        currentLevel.put(player.getUniqueId(), newLevel);
    }

    public void onMeleeHit(Player attacker) {
        addFlow(attacker, meleeGain);
    }

    public void onArrowHit(Player shooter) {
        addFlow(shooter, arrowGain);
    }

    public void onKill(Player killer) {
        addFlow(killer, killGain);
    }

    public void onTakeDamage(Player victim) {
        removeFlow(victim, damageLoss);
        lastCombatTime.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public void onDeath(Player player) {
        flowMap.put(player.getUniqueId(), 0.0);
        currentLevel.put(player.getUniqueId(), FlowLevel.NONE);
    }

    private void tickDecay() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            double flow = getFlow(player);
            if (flow <= 0) continue;

            Long lastCombat = lastCombatTime.get(uuid);
            if (lastCombat != null && (now - lastCombat) > (combatTimeout * 1000L)) {
                removeFlow(player, idleDecay);
            }
        }
    }

    public FlowLevel getFlowLevel(double flow) {
        if (flow >= 100) return FlowLevel.TRANSCENDENT;
        if (flow >= 80) return FlowLevel.UNSTOPPABLE;
        if (flow >= 60) return FlowLevel.FLOWING;
        if (flow >= 40) return FlowLevel.IN_THE_ZONE;
        if (flow >= 20) return FlowLevel.WARMED_UP;
        return FlowLevel.NONE;
    }

    public FlowLevel getFlowLevel(Player player) {
        return getFlowLevel(getFlow(player));
    }

    public double getFlowDamageBonus(Player player) {
        return getFlowLevel(player).getDamageBonus();
    }

    public double getFlowSpeedBoost(Player player) {
        return getFlowLevel(player).getSpeedBoost();
    }

    public boolean hasFlowKnockbackImmunity(Player player) {
        return getFlowLevel(player).hasNoKnockback();
    }

    public boolean hasFlowStrength(Player player) {
        return getFlowLevel(player).hasStrength();
    }

    public boolean hasFlowRegen(Player player) {
        return getFlowLevel(player).hasRegen();
    }

    public boolean hasFlowWeakenEnemy(Player player) {
        return getFlowLevel(player).hasWeakenEnemy();
    }

    public int getFlowShieldBreakBonus(Player player) {
        return getFlowLevel(player).getShieldBreakBonus();
    }

    public Component buildFlowBar(Player player) {
        double flow = getFlow(player);
        FlowLevel level = getFlowLevel(flow);
        int barLength = 20;
        int filled = (int) (flow / maxFlow * barLength);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "|" : ":");
        }

        TextColor barColor = flow > 0 ? level.getColor() : NamedTextColor.DARK_GRAY;

        return Component.text()
                .append(Component.text("Flow ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(bar.toString(), barColor))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + (int) flow, NamedTextColor.WHITE))
                .append(level != FlowLevel.NONE
                        ? Component.text(" " + level.getDisplayName(), level.getColor())
                        : Component.empty())
                .build();
    }

    public double getMaxFlow() { return maxFlow; }
}
