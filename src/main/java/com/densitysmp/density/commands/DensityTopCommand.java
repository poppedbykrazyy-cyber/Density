package com.densitysmp.density.commands;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DensityTopCommand implements CommandExecutor {

    private final DensityPlugin plugin;

    public DensityTopCommand(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LinkedHashMap<UUID, Double> top = plugin.getDensityManager().getTopDensities(10);

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  === ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Density Leaderboard", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.text(" ===", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.empty());

        if (top.isEmpty()) {
            sender.sendMessage(Component.text("  No players yet!", NamedTextColor.GRAY));
            sender.sendMessage(Component.empty());
            return true;
        }

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "Unknown";
            DensityTier tier = plugin.getTierManager().getTier(entry.getValue());

            NamedTextColor rankColor = switch (rank) {
                case 1 -> NamedTextColor.GOLD;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.RED;
                default -> NamedTextColor.DARK_GRAY;
            };

            String medal = switch (rank) {
                case 1 -> "#1";
                case 2 -> "#2";
                case 3 -> "#3";
                default -> "#" + rank;
            };

            sender.sendMessage(Component.text("  " + medal + " ", rankColor).decorate(TextDecoration.BOLD)
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.1f", entry.getValue()), NamedTextColor.YELLOW))
                    .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                    .append(Component.text(tier.getDisplayName(), tier.getColor()))
                    .append(Component.text("]", NamedTextColor.DARK_GRAY)));
            rank++;
        }

        sender.sendMessage(Component.empty());
        return true;
    }
}
