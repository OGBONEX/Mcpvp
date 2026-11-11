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
    private final Map<UUID, Long> pendingAt = new HashMap<>();

    public record RematchRequest(UUID opponent, String kit, boolean ranked) {}

    public PvpProgressionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progression.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int elo(UUID id) { return data.getInt(id + ".elo", 1000); }
    public int streak(UUID id) { return data.getInt(id + ".killstreak", 0); }
    public int bestStreak(UUID id) { return data.getInt(id + ".best-killstreak", 0); }
    public int winStreak(UUID id) { return data.getInt(id + ".winstreak", 0); }
    public int bestWinStreak(UUID id) { return data.getInt(id + ".best-winstreak", 0); }

    public String tier(UUID id) {
        int rating = elo(id);
        if (rating >= 1700) return "HT";
        if (rating >= 1400) return "MT";
        return "LT";
    }

    public void recordResult(Player winner, Player loser, String kit, boolean ranked) {
        UUID win = winner.getUniqueId(), lose = loser.getUniqueId();
        int oldWinner = elo(win), oldLoser = elo(lose);
        int winnerDelta = 0, loserDelta = 0;
        if (ranked) {
            double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (oldLoser - oldWinner) / 400.0));
            double expectedLoser = 1.0 / (1.0 + Math.pow(10.0, (oldWinner - oldLoser) / 400.0));
            winnerDelta = Math.max(1, (int)Math.round(32 * (1.0 - expectedWinner)));
            loserDelta = (int)Math.round(32 * (0.0 - expectedLoser));
            data.set(win + ".elo", oldWinner + winnerDelta);
            data.set(lose + ".elo", Math.max(0, oldLoser + loserDelta));
        }
        int current = winStreak(win) + 1;
        data.set(win + ".winstreak", current);
        data.set(win + ".best-winstreak", Math.max(bestWinStreak(win), current));
        recent.put(win, new RematchRequest(lose, kit, ranked));
        recent.put(lose, new RematchRequest(win, kit, ranked));
        addHistory(win, (ranked ? "Ranked W vs " : "Unranked W vs ") + loser.getName() + " (" + kit + ")" + (ranked ? " +" + winnerDelta : ""));
        save();
        if (ranked) {
            winner.sendMessage(ChatColor.AQUA + "ELO +" + winnerDelta + " (" + (oldWinner + winnerDelta) + ") | " + tier(win));
            loser.sendMessage(ChatColor.RED + "ELO " + loserDelta + " (" + Math.max(0, oldLoser + loserDelta) + ")");
        }
    }

    public void recordLoss(Player loser, Player winner, String kit, boolean ranked) {
        UUID id = loser.getUniqueId();
        data.set(id + ".winstreak", 0);
        data.set(id + ".killstreak", 0);
        if (winner != null) {
            recent.put(id, new RematchRequest(winner.getUniqueId(), kit, ranked));
            recent.put(winner.getUniqueId(), new RematchRequest(id, kit, ranked));
            addHistory(id, (ranked ? "Ranked L vs " : "Unranked L vs ") + winner.getName() + " (" + kit + ")");
        } else addHistory(id, (ranked ? "Ranked L (" : "Unranked L (") + kit + ")");
        save();
    }

    public void recordKill(Player player) {
        UUID id = player.getUniqueId();
        int value = streak(id) + 1;
        data.set(id + ".killstreak", value);
        data.set(id + ".best-killstreak", Math.max(bestStreak(id), value));
        save();
        if (value > 0 && value % 5 == 0) player.sendMessage(ChatColor.GOLD + "Killstreak: " + value + "!");
    }

    public void recordDeath(Player player) {
        data.set(player.getUniqueId() + ".killstreak", 0);
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
    public void pendingRematch(UUID target, UUID challenger, String kit, boolean ranked) {
        pending.put(target, new RematchRequest(challenger, kit, ranked));
        pendingAt.put(target, System.currentTimeMillis());
    }
    public RematchRequest takePendingRematch(UUID id) {
        Long created = pendingAt.remove(id);
        RematchRequest request = pending.remove(id);
        if (created == null || System.currentTimeMillis() - created > 30_000L) return null;
        return request;
    }

    private void save() {
        try { data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("Could not save progression data: " + ex.getMessage()); }
    }
}
