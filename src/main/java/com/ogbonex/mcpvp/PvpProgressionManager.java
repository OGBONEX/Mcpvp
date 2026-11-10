package com.ogbonex.mcpvp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PvpProgressionManager {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, RematchRequest> recent = new HashMap<>();
    private final Map<UUID, RematchRequest> pending = new HashMap<>();

    public record RematchRequest(UUID opponent, String kit) {}

    public PvpProgressionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progression.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int elo(UUID id) { return data.getInt(id + ".elo", 1000); }
    public int streak(UUID id) { return data.getInt(id + ".streak", 0); }
    public int bestStreak(UUID id) { return data.getInt(id + ".best-streak", 0); }

    public String tier(UUID id) {
        int rating = elo(id);
        if (rating >= 2000) return "Master";
        if (rating >= 1700) return "Diamond";
        if (rating >= 1400) return "Gold";
        if (rating >= 1150) return "Silver";
        return "Bronze";
    }

    public void recordResult(Player winner, Player loser, String kit) {
        UUID win = winner.getUniqueId(), lose = loser.getUniqueId();
        int oldWinner = elo(win), oldLoser = elo(lose);
        double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (oldLoser - oldWinner) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10.0, (oldWinner - oldLoser) / 400.0));
        int winnerDelta = Math.max(1, (int)Math.round(32 * (1.0 - expectedWinner)));
        int loserDelta = (int)Math.round(32 * (0.0 - expectedLoser));
        data.set(win + ".elo", oldWinner + winnerDelta);
        data.set(lose + ".elo", Math.max(0, oldLoser + loserDelta));
        int current = streak(win) + 1;
        data.set(win + ".streak", current);
        data.set(win + ".best-streak", Math.max(bestStreak(win), current));
        recent.put(win, new RematchRequest(lose, kit));
        recent.put(lose, new RematchRequest(win, kit));
        addHistory(win, "W vs " + loser.getName() + " (" + kit + ") +" + winnerDelta);
        addHistory(lose, "L vs " + winner.getName() + " (" + kit + ") " + loserDelta);
        save();
        winner.sendMessage(ChatColor.AQUA + "ELO +" + winnerDelta + " (" + (oldWinner + winnerDelta) + ") | " + tier(win));
        loser.sendMessage(ChatColor.RED + "ELO " + loserDelta + " (" + Math.max(0, oldLoser + loserDelta) + ")");
    }

    public void recordLoss(Player loser, Player winner, String kit) {
        UUID id = loser.getUniqueId();
        data.set(id + ".streak", 0);
        if (winner != null) {
            recent.put(id, new RematchRequest(winner.getUniqueId(), kit));
            recent.put(winner.getUniqueId(), new RematchRequest(id, kit));
            addHistory(id, "L vs " + winner.getName() + " (" + kit + ")");
        } else addHistory(id, "L (" + kit + ")");
        save();
    }

    public void recordKill(Player player) {
        UUID id = player.getUniqueId();
        int value = streak(id) + 1;
        data.set(id + ".streak", value);
        data.set(id + ".best-streak", Math.max(bestStreak(id), value));
        save();
        if (value > 0 && value % 5 == 0) player.sendMessage(ChatColor.GOLD + "Killstreak: " + value + "!");
    }

    public void recordDeath(Player player) {
        data.set(player.getUniqueId() + ".streak", 0);
        save();
    }

    private void addHistory(UUID id, String entry) {
        List<String> history = new ArrayList<>(data.getStringList(id + ".history"));
        history.add(0, entry);
        if (history.size() > 10) history = new ArrayList<>(history.subList(0, 10));
        data.set(id + ".history", history);
    }

    public String history(UUID id) {
        List<String> entries = data.getStringList(id + ".history");
        return entries.isEmpty() ? "No matches recorded." : String.join(" | ", entries.subList(0, Math.min(5, entries.size())));
    }

    public RematchRequest rematch(UUID id) { return recent.get(id); }
    public void pendingRematch(UUID target, UUID challenger, String kit) {
        pending.put(target, new RematchRequest(challenger, kit));
    }
    public RematchRequest takePendingRematch(UUID id) { return pending.remove(id); }

    private void save() {
        try { data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("Could not save progression data: " + ex.getMessage()); }
    }
}
