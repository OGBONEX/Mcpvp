package com.ogbonex.mcpvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import java.util.*;
import java.util.function.ToIntFunction;

final class LobbyDisplayManager {
    private final JavaPlugin plugin;
    private final java.util.function.Function<UUID, String> ranks;
    private final java.util.function.ToIntFunction<UUID> wins;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    LobbyDisplayManager(JavaPlugin plugin, java.util.function.ToIntFunction<UUID> wins, java.util.function.Function<UUID, String> ranks) {
        this.plugin = plugin;
        this.wins = wins;
        this.ranks = ranks;
    }

    String rankName(UUID playerId) { return ranks.apply(playerId); }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    void refresh(Player player) {
        int count = wins.applyAsInt(player.getUniqueId());
        String rank = ranks.apply(player.getUniqueId());
        String prefix = plugin.getConfig().getString("rank-prefixes." + rank.toLowerCase(java.util.Locale.ROOT).replace(" ", "-").replace("_", "-"), "&7[" + rank + "]");
        player.setPlayerListName(color(prefix) + ChatColor.RESET + " " + player.getName());
        player.setPlayerListHeaderFooter(
                color(plugin.getConfig().getString("tab.header", "&6&lMCPvP")) + "\n" +
                        color(plugin.getConfig().getString("tab.subheader", "&7Classic PvP Network")),
                color(plugin.getConfig().getString("tab.footer", "&eOnline: &f%online%"))
                        .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));

        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            boards.remove(player.getUniqueId());
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id -> manager.getNewScoreboard());
        Objective objective = board.getObjective("mcpvp");
        if (objective == null) objective = board.registerNewObjective("mcpvp", "dummy",
                color(plugin.getConfig().getString("scoreboard.title", "&6&lMCPvP")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (String entry : new HashSet<>(board.getEntries())) board.resetScores(entry);
        List<String> lines = List.of(
                color("&7" + Bukkit.getOnlinePlayers().size() + " online"),
                color("&f "),
                color("&eRank: &f" + rank),
                color("&eWins: &f" + count),
                color("&eQueue: &f" + plugin.getConfig().getString("scoreboard.queue-label", "Use /queue")),
                color("&a "),
                color("&7MCPvP Network")
        );
        int score = lines.size();
        for (String line : lines) objective.getScore(line).setScore(score--);
        player.setScoreboard(board);
    }
}
