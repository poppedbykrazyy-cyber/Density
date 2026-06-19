package com.densitysmp.density.managers;

import com.densitysmp.density.DensityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardManager {

    private final DensityPlugin plugin;

    public ScoreboardManager(DensityPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("density", Criteria.DUMMY,
                Component.text("DENSITY SMP").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        DensityManager dm = plugin.getDensityManager();
        FlowManager fm = plugin.getFlowManager();
        double density = dm.getDensity(player);
        double flow = fm.getFlow(player);
        DensityTier tier = plugin.getTierManager().getTier(player);
        FlowLevel flowLevel = fm.getFlowLevel(player);

        int line = 15;

        obj.getScore("§8§m                    ").setScore(line--);
        obj.getScore(" ").setScore(line--);
        obj.getScore("§fDensity: §e" + String.format("%.1f", density) + "§7/§e" + String.format("%.0f", dm.getMaxDensity())).setScore(line--);
        obj.getScore("§fTier: §" + getColorCode(tier) + tier.getDisplayName()).setScore(line--);
        obj.getScore("  ").setScore(line--);

        // Flow display
        int flowBarLen = 10;
        int flowFilled = (int) (flow / fm.getMaxFlow() * flowBarLen);
        StringBuilder flowBar = new StringBuilder("§fFlow: ");
        String flowColorCode = getFlowColorCode(flowLevel);
        for (int i = 0; i < flowBarLen; i++) {
            flowBar.append(i < flowFilled ? flowColorCode + "|" : "§8|");
        }
        flowBar.append(" §f").append((int) flow);
        obj.getScore(flowBar.toString()).setScore(line--);

        if (flowLevel != FlowLevel.NONE) {
            obj.getScore("§" + flowColorCode + flowLevel.getDisplayName()).setScore(line--);
        }

        obj.getScore("   ").setScore(line--);

        double dmgBonus = plugin.getTierManager().getDamageBonus(player);
        if (dmgBonus > 0) {
            obj.getScore("§a+" + String.format("%.1f", dmgBonus) + " §7Damage").setScore(line--);
        }

        double lifesteal = plugin.getTierManager().getLifesteal(player);
        if (lifesteal > 0) {
            obj.getScore("§a" + String.format("%.0f%%", lifesteal * 100) + " §7Lifesteal").setScore(line--);
        }

        obj.getScore("    ").setScore(line--);
        obj.getScore("§7Kill to steal: §e" + String.format("%.1f", dm.getDensityPerKill())).setScore(line--);
        obj.getScore("§8§m                     ").setScore(line--);

        player.setScoreboard(board);
    }

    private String getColorCode(DensityTier tier) {
        return switch (tier) {
            case FRAGILE -> "f";
            case LEAN -> "b";
            case BALANCED -> "a";
            case FORTIFIED -> "6";
            case TITAN -> "c";
            case COLOSSUS -> "5";
            default -> "4";
        };
    }

    private String getFlowColorCode(FlowLevel level) {
        return switch (level) {
            case WARMED_UP -> "e";
            case IN_THE_ZONE -> "6";
            case FLOWING -> "c";
            case UNSTOPPABLE -> "4";
            case TRANSCENDENT -> "5";
            default -> "8";
        };
    }
}
