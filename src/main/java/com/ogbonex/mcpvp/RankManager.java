package com.ogbonex.mcpvp;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

final class RankManager {
    static final List<String> RANKS = List.of("Owner", "Co-owner", "Admin", "SR mod", "Mod", "Creator", "Booster", "Player");
    private final File file;
    private final YamlConfiguration data;

    RankManager(org.bukkit.plugin.java.JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "ranks.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    String resolve(String input) {
        for (String rank : RANKS) if (rank.equalsIgnoreCase(input.trim())) return rank;
        return null;
    }

    String getRank(UUID uuid) {
        return resolve(data.getString(uuid.toString(), "Player")) == null ? "Player" : resolve(data.getString(uuid.toString(), "Player"));
    }

    void setRank(UUID uuid, String rank) {
        data.set(uuid.toString(), rank);
        save();
    }

    void removeRank(UUID uuid) {
        data.set(uuid.toString(), "Player");
        save();
    }

    private void save() {
        try { data.save(file); } catch (IOException ex) { throw new IllegalStateException("Could not save ranks.yml", ex); }
    }
}
