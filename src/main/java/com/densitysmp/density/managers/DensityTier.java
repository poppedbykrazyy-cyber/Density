package com.densitysmp.density.managers;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum DensityTier {

    ELIMINATED("Eliminated", NamedTextColor.DARK_RED),
    FRAGILE("Fragile", NamedTextColor.WHITE),
    LEAN("Lean", NamedTextColor.AQUA),
    BALANCED("Balanced", NamedTextColor.GREEN),
    FORTIFIED("Fortified", NamedTextColor.GOLD),
    TITAN("Titan", NamedTextColor.RED),
    COLOSSUS("Colossus", NamedTextColor.DARK_PURPLE);

    private final String displayName;
    private final TextColor color;

    DensityTier(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
}
