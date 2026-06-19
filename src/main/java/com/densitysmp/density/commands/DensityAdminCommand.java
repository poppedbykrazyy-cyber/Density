package com.densitysmp.density.commands;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DensityAdminCommand implements CommandExecutor {

    private final DensityPlugin plugin;

    public DensityAdminCommand(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("density.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            case "restore" -> handleRestore(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /densityadmin set <player> <amount>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            plugin.getDensityManager().setDensity(target, amount);
            sender.sendMessage(Component.text("Set " + target.getName() + "'s density to " + String.format("%.1f", amount), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your density has been set to " + String.format("%.1f", amount) + " by an admin.", NamedTextColor.YELLOW));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /densityadmin reset <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        plugin.getDensityManager().resetDensity(target);
        sender.sendMessage(Component.text("Reset " + target.getName() + "'s density to default.", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Your density has been reset by an admin.", NamedTextColor.YELLOW));
    }

    private void handleRestore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /densityadmin restore <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        plugin.getDensityManager().resetDensity(target);
        target.setGameMode(GameMode.SURVIVAL);
        sender.sendMessage(Component.text("Restored " + target.getName() + " with default density.", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You have been restored! Your density has been reset.", NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("DensitySMP configuration reloaded!", NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("DensitySMP Admin Commands:", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("  /da set <player> <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set density", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /da reset <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Reset density to default", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /da restore <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Restore eliminated player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /da reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload config", NamedTextColor.GRAY)));
    }
}
