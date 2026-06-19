package com.densitysmp.density.weapons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public enum CustomWeapon {

    DENSITY_BLADE(
            "density_blade",
            Material.NETHERITE_SWORD,
            "Density Blade",
            TextColor.color(170, 0, 170),
            5, 4,
            "Damage scales with your density",
            "Bonus: +density/10 damage",
            "15% chance to apply Weakness"
    ),

    VOID_AXE(
            "void_axe",
            Material.NETHERITE_AXE,
            "Void Axe",
            TextColor.color(50, 50, 50),
            5, 3,
            "Steals 0.3 density per hit",
            "Disables shields for 8 seconds"
    ),

    FLOW_SCYTHE(
            "flow_scythe",
            Material.NETHERITE_HOE,
            "Flow Scythe",
            TextColor.color(255, 85, 85),
            10, 5,
            "Each hit grants 2x Flow",
            "Sweeping: hits all within 3 blocks"
    ),

    SIPHON_BOW(
            "siphon_bow",
            Material.BOW,
            "Siphon Bow",
            TextColor.color(255, 85, 255),
            8, 5,
            "Arrows steal 0.2 density",
            "Arrows apply Slowness II (2s)"
    ),

    MASS_SHIFTER(
            "mass_shifter",
            Material.BLAZE_ROD,
            "Mass Shifter",
            TextColor.color(85, 255, 255),
            0, 4,
            "Right-click player: Swap densities",
            "Right-click air: Combat Boost (8s)",
            "Boost costs 1 density"
    );

    private final String id;
    private final Material baseMaterial;
    private final String displayName;
    private final TextColor color;
    private final int minDensity;
    private final int densityCost;
    private final String[] loreLines;

    public static final NamespacedKey WEAPON_KEY = new NamespacedKey("densitysmp", "custom_weapon");

    CustomWeapon(String id, Material baseMaterial, String displayName, TextColor color,
                 int minDensity, int densityCost, String... loreLines) {
        this.id = id;
        this.baseMaterial = baseMaterial;
        this.displayName = displayName;
        this.color = color;
        this.minDensity = minDensity;
        this.densityCost = densityCost;
        this.loreLines = loreLines;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(displayName, color)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("DENSITY WEAPON", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        for (String line : loreLines) {
            lore.add(Component.text(" " + line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        if (minDensity > 0) {
            lore.add(Component.text("Requires: " + minDensity + " density", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Altar Cost: " + densityCost + " density", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(WEAPON_KEY, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public static CustomWeapon fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String weaponId = item.getItemMeta().getPersistentDataContainer()
                .get(WEAPON_KEY, PersistentDataType.STRING);
        if (weaponId == null) return null;
        for (CustomWeapon weapon : values()) {
            if (weapon.id.equals(weaponId)) return weapon;
        }
        return null;
    }

    public String getId() { return id; }
    public Material getBaseMaterial() { return baseMaterial; }
    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
    public int getMinDensity() { return minDensity; }
    public int getDensityCost() { return densityCost; }
}
