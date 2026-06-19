package com.densitysmp.density.weapons;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class AltarManager implements Listener {

    private final DensityPlugin plugin;
    private static final NamespacedKey ALTAR_GUI = new NamespacedKey("densitysmp", "altar_gui");

    public AltarManager(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAltar(Block block) {
        if (block.getType() != Material.LODESTONE) return false;

        Block below = block.getRelative(0, -1, 0);
        if (below.getType() != Material.OBSIDIAN) return false;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (below.getRelative(x, 0, z).getType() != Material.OBSIDIAN) return false;
            }
        }

        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getPlayer().isSneaking()) return;

        if (isAltar(event.getClickedBlock())) {
            event.setCancelled(true);
            openAltarGUI(event.getPlayer());
        }
    }

    public void openAltarGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text("Density Altar", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));

        ItemStack border = createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }

        double playerDensity = plugin.getDensityManager().getDensity(player);

        gui.setItem(4, createInfoItem(player, playerDensity));

        int[] slots = {19, 21, 23, 25, 31};
        CustomWeapon[] weapons = CustomWeapon.values();

        for (int i = 0; i < weapons.length && i < slots.length; i++) {
            CustomWeapon weapon = weapons[i];
            ItemStack display = weapon.createItem();
            ItemMeta meta = display.getItemMeta();

            List<Component> lore = meta.lore();
            if (lore == null) lore = new java.util.ArrayList<>();

            lore.add(Component.empty());
            if (playerDensity >= weapon.getDensityCost()) {
                lore.add(Component.text("CLICK TO CRAFT", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Cost: " + weapon.getDensityCost() + " density", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("NOT ENOUGH DENSITY", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Need: " + weapon.getDensityCost() + " (You: " + String.format("%.1f", playerDensity) + ")", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            meta.getPersistentDataContainer().set(ALTAR_GUI, PersistentDataType.STRING, weapon.getId());
            display.setItemMeta(meta);
            gui.setItem(slots[i], display);
        }

        ItemStack baseReq = createGuiItem(Material.PAPER,
                "&eBase Item Required",
                "&7Place the base item in your",
                "&7inventory before crafting!",
                "",
                "&7Gravity Hammer: &fNetherite Axe",
                "&7Void Blade: &fNetherite Sword",
                "&7Neutron Star: &fMace",
                "&7Singularity Bow: &fBow",
                "&7Mass Shifter: &fBlaze Rod");
        gui.setItem(49, baseReq);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        if (title == null) return;

        if (!title.equals(Component.text("Density Altar", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String weaponId = clicked.getItemMeta().getPersistentDataContainer()
                .get(ALTAR_GUI, PersistentDataType.STRING);
        if (weaponId == null) return;

        CustomWeapon weapon = null;
        for (CustomWeapon w : CustomWeapon.values()) {
            if (w.getId().equals(weaponId)) {
                weapon = w;
                break;
            }
        }
        if (weapon == null) return;

        double playerDensity = plugin.getDensityManager().getDensity(player);
        if (playerDensity < weapon.getDensityCost()) {
            player.sendMessage(Component.text("Not enough density! Need " + weapon.getDensityCost() + " density.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        boolean hasBaseItem = false;
        int baseSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == weapon.getBaseMaterial() && CustomWeapon.fromItem(item) == null) {
                hasBaseItem = true;
                baseSlot = i;
                break;
            }
        }

        if (!hasBaseItem) {
            player.sendMessage(Component.text("You need a " + weapon.getBaseMaterial().name().toLowerCase().replace('_', ' ') + " in your inventory!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        player.getInventory().setItem(baseSlot, null);
        plugin.getDensityManager().removeDensity(player, weapon.getDensityCost());

        ItemStack weaponItem = weapon.createItem();
        player.getInventory().addItem(weaponItem);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        player.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Altar", NamedTextColor.DARK_PURPLE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Crafted ", NamedTextColor.GREEN))
                .append(Component.text(weapon.getDisplayName(), weapon.getColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GREEN))
                .build());

        player.closeInventory();
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name.replace("&e", "").replace("&7", "").replace("&f", ""), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (lore.length > 0) {
            List<Component> loreList = Arrays.stream(lore)
                    .map(l -> Component.text(l.replaceAll("&[0-9a-f]", ""), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .toList();
            meta.lore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(Player player, double density) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Density Altar", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = List.of(
                Component.empty(),
                Component.text("Sacrifice your density to", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("forge powerful weapons!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Your Density: ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(String.format("%.1f", density), NamedTextColor.YELLOW)),
                Component.empty(),
                Component.text("Structure: 3x3 Obsidian base", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("with Lodestone on top", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak + Right-click to open", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
