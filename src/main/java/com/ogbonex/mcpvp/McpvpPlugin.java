package com.ogbonex.mcpvp;

import org.bukkit.plugin.java.JavaPlugin;

public final class McpvpPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("MCPvP " + getPluginMeta().getVersion() + " has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MCPvP has been disabled.");
    }
}
