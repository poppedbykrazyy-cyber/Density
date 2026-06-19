package com.densitysmp.density.managers;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum FlowLevel {

    NONE("", NamedTextColor.DARK_GRAY, 0, 0, false, false, false, false, 0),
    WARMED_UP("Warmed Up", NamedTextColor.YELLOW, 0.5, 0.05, false, false, false, false, 0),
    IN_THE_ZONE("In The Zone", NamedTextColor.GOLD, 1.0, 0.10, true, false, false, false, 0),
    FLOWING("Flowing", NamedTextColor.RED, 1.5, 0.15, true, false, false, false, 40),
    UNSTOPPABLE("Unstoppable", TextColor.color(170, 0, 0), 2.0, 0.20, true, true, false, false, 60),
    TRANSCENDENT("Transcendent", NamedTextColor.DARK_PURPLE, 3.0, 0.25, true, true, true, true, 80);

    private final String displayName;
    private final TextColor color;
    private final double damageBonus;
    private final double speedBoost;
    private final boolean regen;
    private final boolean noKnockback;
    private final boolean strength;
    private final boolean weakenEnemy;
    private final int shieldBreakBonus;

    FlowLevel(String displayName, TextColor color, double damageBonus, double speedBoost,
              boolean regen, boolean noKnockback, boolean strength, boolean weakenEnemy, int shieldBreakBonus) {
        this.displayName = displayName;
        this.color = color;
        this.damageBonus = damageBonus;
        this.speedBoost = speedBoost;
        this.regen = regen;
        this.noKnockback = noKnockback;
        this.strength = strength;
        this.weakenEnemy = weakenEnemy;
        this.shieldBreakBonus = shieldBreakBonus;
    }

    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
    public double getDamageBonus() { return damageBonus; }
    public double getSpeedBoost() { return speedBoost; }
    public boolean hasRegen() { return regen; }
    public boolean hasNoKnockback() { return noKnockback; }
    public boolean hasStrength() { return strength; }
    public boolean hasWeakenEnemy() { return weakenEnemy; }
    public int getShieldBreakBonus() { return shieldBreakBonus; }
}
