package com.densitysmp.density.commands;

import com.densitysmp.density.DensityPlugin;
import com.densitysmp.density.managers.DensityManager;
import com.densitysmp.density.managers.DensityTier;
import com.densitysmp.density.managers.FlowLevel;
import com.densitysmp.density.managers.TierManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DensityCommand implements CommandExecutor {

    private final DensityPlugin plugin;

    public DensityCommand(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("Specify a player!", NamedTextColor.RED));
            return true;
        }

        DensityManager dm = plugin.getDensityManager();
        TierManager tm = plugin.getTierManager();
        double density = dm.getDensity(target);
        DensityTier tier = tm.getTier(target);
        double flow = plugin.getFlowManager().getFlow(target);
        FlowLevel flowLevel = plugin.getFlowManager().getFlowLevel(target);

        // Density bar
        int barLen = 30;
        int filled = (int) (density / dm.getMaxDensity() * barLen);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLen; i++) bar.append(i < filled ? "|" : ":");

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  === ", NamedTextColor.DARK_GRAY)
                .append(Component.text(target.getName(), NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.text(" ===", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text("  Density: ", NamedTextColor.WHITE)
                .append(Component.text(String.format("%.1f", density), NamedTextColor.YELLOW))
                .append(Component.text(" / " + String.format("%.0f", dm.getMaxDensity()), NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  ", NamedTextColor.WHITE)
                .append(Component.text(bar.toString(), tier.getColor())));
        sender.sendMessage(Component.text("  Tier: ", NamedTextColor.WHITE)
                .append(Component.text(tier.getDisplayName(), tier.getColor()).decorate(TextDecoration.BOLD)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  Flow: ", NamedTextColor.WHITE)
                .append(Component.text((int) flow + "/100", NamedTextColor.YELLOW))
                .append(flowLevel != FlowLevel.NONE
                        ? Component.text(" [" + flowLevel.getDisplayName() + "]", flowLevel.getColor())
                        : Component.empty()));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  Combat Stats:", NamedTextColor.WHITE).decorate(TextDecoration.UNDERLINED));

        double dmgBonus = tm.getDamageBonus(target);
        if (dmgBonus > 0) {
            sender.sendMessage(Component.text("   Bonus Damage: +", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.1f", dmgBonus), NamedTextColor.GREEN)));
        }

        double lifesteal = tm.getLifesteal(target);
        if (lifesteal > 0) {
            sender.sendMessage(Component.text("   Lifesteal: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.0f%%", lifesteal * 100), NamedTextColor.GREEN)));
        }

        double thorns = tm.getThorns(target);
        if (thorns > 0) {
            sender.sendMessage(Component.text("   Thorns: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.0f%%", thorns * 100), NamedTextColor.GOLD)));
        }

        if (tm.hasAutoTotem(target)) {
            sender.sendMessage(Component.text("   Auto-Totem: ", NamedTextColor.GRAY)
                    .append(Component.text("Active", NamedTextColor.GOLD)));
        }

        if (flowLevel.hasNoKnockback()) {
            sender.sendMessage(Component.text("   KB Immune: ", NamedTextColor.GRAY)
                    .append(Component.text("Active (Flow)", NamedTextColor.RED)));
        }

        sender.sendMessage(Component.empty());
        return true;
    }
}
