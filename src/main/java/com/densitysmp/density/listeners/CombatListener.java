package com.densitysmp.density.listeners;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityManager;
import com.densitysmp.density.managers.DensityTier;
import com.densitysmp.density.managers.FlowManager;
import com.densitysmp.density.managers.TierManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final DensityPlugin plugin;
    private final Map<UUID, Integer> killStreaks = new HashMap<>();

    public CombatListener(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = null;
        boolean isArrow = false;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            attacker = p;
            isArrow = true;
        }

        if (attacker == null) return;

        TierManager tm = plugin.getTierManager();
        FlowManager fm = plugin.getFlowManager();

        // Apply damage bonus from tier + flow
        double bonus = tm.getDamageBonus(attacker);
        if (bonus > 0) {
            event.setDamage(event.getDamage() + bonus);
        }

        // Flow: weaken enemy on hit (Transcendent)
        if (event.getEntity() instanceof Player victim) {
            if (fm.hasFlowWeakenEnemy(attacker)) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 0, true, false, false));
            }
        }

        // Build flow on hit
        if (isArrow) {
            fm.onArrowHit(attacker);
        } else {
            fm.onMeleeHit(attacker);
        }

        // Lifesteal
        if (event.getEntity() instanceof Player) {
            double lifesteal = tm.getLifesteal(attacker);
            if (lifesteal > 0) {
                double healAmount = event.getFinalDamage() * lifesteal;
                double newHealth = Math.min(attacker.getHealth() + healAmount,
                        attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                attacker.setHealth(newHealth);
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 1, 0.2, 0.1, 0.2, 0);
            }
        }

        // Thorns for victim
        if (event.getEntity() instanceof Player victim) {
            double thorns = tm.getThorns(victim);
            if (thorns > 0 && !isArrow) {
                double thornsDmg = event.getFinalDamage() * thorns;
                attacker.damage(thornsDmg);
                attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
            }

            // Flow loss for victim
            fm.onTakeDamage(victim);

            // Flow knockback immunity
            if (fm.hasFlowKnockbackImmunity(victim)) {
                event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(0.1));
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Auto-totem: prevent lethal damage
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            TierManager tm = plugin.getTierManager();
            if (tm.tryAutoTotem(player)) {
                event.setCancelled(true);
                player.setHealth(4.0);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.3);
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false, false));

                player.sendMessage(Component.text("AUTO-TOTEM! ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                        .append(Component.text("Your density saved you!", NamedTextColor.YELLOW)));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Reset flow on death
        plugin.getFlowManager().onDeath(victim);
        killStreaks.put(victim.getUniqueId(), 0);

        if (killer == null || killer.equals(victim)) return;

        DensityManager dm = plugin.getDensityManager();
        TierManager tm = plugin.getTierManager();
        FlowManager fm = plugin.getFlowManager();

        DensityTier oldKillerTier = tm.getTier(killer);
        DensityTier oldVictimTier = tm.getTier(victim);

        // Kill streak tracking
        int streak = killStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
        killStreaks.put(killer.getUniqueId(), streak);

        double baseSteal = dm.getDensityPerKill();
        double streakBonus = getStreakBonus(streak);
        double totalSteal = baseSteal + streakBonus;
        double stealAmount = Math.min(totalSteal, dm.getDensity(victim));

        dm.handleKill(killer, victim);
        if (streakBonus > 0) {
            dm.addDensity(killer, streakBonus);
        }

        // Flow on kill
        fm.onKill(killer);

        double killerDensity = dm.getDensity(killer);
        double victimDensity = dm.getDensity(victim);
        DensityTier newKillerTier = tm.getTier(killer);
        DensityTier newVictimTier = tm.getTier(victim);

        // Messages
        killer.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("+" + String.format("%.1f", stealAmount), NamedTextColor.GREEN))
                .append(Component.text(" density from ", NamedTextColor.GRAY))
                .append(Component.text(victim.getName(), NamedTextColor.RED))
                .append(streakBonus > 0
                        ? Component.text(" (+" + String.format("%.1f", streakBonus) + " streak bonus!)", NamedTextColor.GOLD)
                        : Component.empty())
                .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format("%.1f", killerDensity), NamedTextColor.YELLOW))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .build());

        victim.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("-" + String.format("%.1f", stealAmount), NamedTextColor.RED))
                .append(Component.text(" density to ", NamedTextColor.GRAY))
                .append(Component.text(killer.getName(), NamedTextColor.GREEN))
                .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format("%.1f", victimDensity), NamedTextColor.YELLOW))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .build());

        // Kill streak broadcast
        if (plugin.getConfig().getBoolean("killstreak.broadcast", true)) {
            if (streak == 3 || streak == 5 || streak == 10 || streak == 15 || streak == 20) {
                plugin.getServer().broadcast(Component.text()
                        .append(Component.text("[", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(killer.getName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                        .append(Component.text(" is on a ", NamedTextColor.YELLOW))
                        .append(Component.text(streak + " kill streak!", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                        .build());
            }
        }

        // Tier change notifications
        if (newKillerTier != oldKillerTier) {
            killer.sendMessage(Component.text()
                    .append(Component.text("TIER UP! ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text(newKillerTier.getDisplayName(), newKillerTier.getColor()).decorate(TextDecoration.BOLD))
                    .build());
            killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        }

        if (newVictimTier != oldVictimTier && newVictimTier != DensityTier.ELIMINATED) {
            victim.sendMessage(Component.text()
                    .append(Component.text("TIER DOWN! ", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                    .append(Component.text(newVictimTier.getDisplayName(), newVictimTier.getColor()).decorate(TextDecoration.BOLD))
                    .build());
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }

        if (plugin.getConfig().getBoolean("particles.kill-effect", true)) {
            killer.getWorld().spawnParticle(Particle.SOUL, killer.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        }

        if (dm.isEliminated(victim) && !victim.hasPermission("density.bypass")) {
            handleElimination(victim);
        }
    }

    private double getStreakBonus(int streak) {
        if (streak >= 10) return plugin.getConfig().getDouble("killstreak.10-kills-bonus", 2.0);
        if (streak >= 5) return plugin.getConfig().getDouble("killstreak.5-kills-bonus", 1.0);
        if (streak >= 3) return plugin.getConfig().getDouble("killstreak.3-kills-bonus", 0.5);
        return 0;
    }

    private void handleElimination(Player player) {
        if (plugin.getConfig().getBoolean("elimination.broadcast", true)) {
            plugin.getServer().broadcast(Component.text()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Density", NamedTextColor.DARK_PURPLE))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.getName(), NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD))
                    .append(Component.text(" has been eliminated!", NamedTextColor.RED))
                    .build());
        }

        plugin.getServer().getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f));

        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);

        if (plugin.getConfig().getBoolean("elimination.ban-on-eliminate", false)) {
            int hours = plugin.getConfig().getInt("elimination.ban-duration", 24);
            player.kick(Component.text("Eliminated! Density reached zero.", NamedTextColor.RED));
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("Eliminated! Ask an admin to /da restore you.", NamedTextColor.RED));
        }
    }
}
