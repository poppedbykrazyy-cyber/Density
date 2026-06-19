package com.densitysmp.density.weapons;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WeaponListener implements Listener {

    private final DensityPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    private static final NamespacedKey SIPHON_ARROW = new NamespacedKey("densitysmp", "siphon_arrow");

    public WeaponListener(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isOnCooldown(Player player, String ability, int cooldownSeconds) {
        String key = player.getUniqueId() + ":" + ability;
        UUID fakeUuid = UUID.nameUUIDFromBytes(key.getBytes());
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(fakeUuid)) {
            long remaining = (cooldowns.get(fakeUuid) + (cooldownSeconds * 1000L) - now) / 1000;
            if (remaining > 0) {
                player.sendMessage(Component.text("On cooldown! " + remaining + "s", NamedTextColor.RED));
                return true;
            }
        }
        cooldowns.put(fakeUuid, now);
        return false;
    }

    private boolean checkDensity(Player player, CustomWeapon weapon) {
        if (plugin.getDensityManager().getDensity(player) < weapon.getMinDensity()) {
            player.sendMessage(Component.text("Need " + weapon.getMinDensity() + " density to use this!", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    @EventHandler
    public void onWeaponHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack held = attacker.getInventory().getItemInMainHand();
        CustomWeapon weapon = CustomWeapon.fromItem(held);
        if (weapon == null) return;
        if (!checkDensity(attacker, weapon)) { event.setCancelled(true); return; }

        switch (weapon) {
            case DENSITY_BLADE -> {
                double density = plugin.getDensityManager().getDensity(attacker);
                double bonus = density * plugin.getConfig().getDouble("weapons.density-blade.density-scaling", 0.1);
                event.setDamage(event.getDamage() + bonus);

                if (event.getEntity() instanceof Player victim) {
                    double weakChance = plugin.getConfig().getDouble("weapons.density-blade.weakness-chance", 0.15);
                    if (random.nextDouble() < weakChance) {
                        int dur = plugin.getConfig().getInt("weapons.density-blade.weakness-duration", 3);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur * 20, 0, true, false, true));
                        attacker.sendMessage(Component.text("Weakness applied!", NamedTextColor.DARK_PURPLE));
                    }
                }
            }
            case VOID_AXE -> {
                if (event.getEntity() instanceof Player victim) {
                    double steal = plugin.getConfig().getDouble("weapons.void-axe.density-steal", 0.3);
                    DensityManager dm = plugin.getDensityManager();
                    double actualSteal = Math.min(steal, dm.getDensity(victim));
                    dm.removeDensity(victim, actualSteal);
                    dm.addDensity(attacker, actualSteal);

                    int shieldTicks = plugin.getConfig().getInt("weapons.void-axe.shield-disable-ticks", 160);
                    victim.setCooldown(Material.SHIELD, shieldTicks);

                    victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);
                    attacker.sendMessage(Component.text("Void Axe: -" + String.format("%.1f", actualSteal) + " density", NamedTextColor.DARK_GRAY));
                }
            }
            case FLOW_SCYTHE -> {
                double flowMult = plugin.getConfig().getDouble("weapons.flow-scythe.flow-multiplier", 2.0);
                plugin.getFlowManager().addFlow(attacker, plugin.getFlowManager().getFlow(attacker) > 0 ? 5 * flowMult : 5);

                double sweepRadius = plugin.getConfig().getDouble("weapons.flow-scythe.sweep-radius", 3.0);
                double sweepDmg = event.getDamage() * plugin.getConfig().getDouble("weapons.flow-scythe.sweep-damage", 0.4);

                for (Entity entity : attacker.getNearbyEntities(sweepRadius, 1, sweepRadius)) {
                    if (entity instanceof Player target && !target.equals(attacker) && !target.equals(event.getEntity())) {
                        target.damage(sweepDmg, attacker);
                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                    }
                }

                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            }
            default -> {}
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        CustomWeapon weapon = CustomWeapon.fromItem(held);
        if (weapon == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!checkDensity(player, weapon)) return;

        if (weapon == CustomWeapon.MASS_SHIFTER) {
            handleMassShifterBoost(player);
        }
    }

    @EventHandler
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        CustomWeapon weapon = CustomWeapon.fromItem(held);
        if (weapon != CustomWeapon.MASS_SHIFTER) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        handleMassShifterSwap(player, target);
    }

    private void handleMassShifterBoost(Player player) {
        int cooldown = plugin.getConfig().getInt("weapons.mass-shifter.boost-cooldown", 45);
        if (isOnCooldown(player, "mass_boost", cooldown)) return;

        DensityManager dm = plugin.getDensityManager();
        double cost = plugin.getConfig().getDouble("weapons.mass-shifter.boost-density-cost", 1);
        if (dm.getDensity(player) <= cost) {
            player.sendMessage(Component.text("Not enough density for boost!", NamedTextColor.RED));
            return;
        }

        dm.removeDensity(player, cost);
        int duration = plugin.getConfig().getInt("weapons.mass-shifter.boost-duration", 8) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true, true));

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        player.sendMessage(Component.text("COMBAT BOOST! ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                .append(Component.text("Strength I + Speed I (" + duration / 20 + "s) | -" + String.format("%.1f", cost) + " density", NamedTextColor.GRAY)));
    }

    private void handleMassShifterSwap(Player player, Player target) {
        int cooldown = plugin.getConfig().getInt("weapons.mass-shifter.swap-cooldown", 60);
        if (isOnCooldown(player, "mass_swap", cooldown)) return;

        DensityManager dm = plugin.getDensityManager();
        double playerD = dm.getDensity(player);
        double targetD = dm.getDensity(target);

        dm.setDensity(player, targetD);
        dm.setDensity(target, playerD);

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);

        player.sendMessage(Component.text("DENSITY SWAPPED! ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                .append(Component.text(String.format("%.1f", playerD) + " -> " + String.format("%.1f", targetD), NamedTextColor.YELLOW)));
        target.sendMessage(Component.text(player.getName() + " swapped densities! ", NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.1f", targetD) + " -> " + String.format("%.1f", playerD), NamedTextColor.RED)));
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;
        if (CustomWeapon.fromItem(bow) != CustomWeapon.SIPHON_BOW) return;
        if (!checkDensity(player, CustomWeapon.SIPHON_BOW)) { event.setCancelled(true); return; }

        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(SIPHON_ARROW, PersistentDataType.BOOLEAN, true);
            arrow.setGlowing(true);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(SIPHON_ARROW, PersistentDataType.BOOLEAN)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!(event.getHitEntity() instanceof Player victim)) return;

        DensityManager dm = plugin.getDensityManager();
        double steal = plugin.getConfig().getDouble("weapons.siphon-bow.density-steal-per-arrow", 0.2);
        double actual = Math.min(steal, dm.getDensity(victim));
        dm.removeDensity(victim, actual);
        dm.addDensity(shooter, actual);

        int slowDur = plugin.getConfig().getInt("weapons.siphon-bow.slowness-duration", 2) * 20;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, 1, true, false, true));

        shooter.sendMessage(Component.text("Siphon: -" + String.format("%.1f", actual) + " density", NamedTextColor.DARK_PURPLE));
        victim.getWorld().spawnParticle(Particle.WITCH, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);
    }
}
