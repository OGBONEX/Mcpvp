package com.ogbonex.mcpvp;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class McpvpPlugin extends JavaPlugin implements Listener {
    private KitManager kits;
    private ArenaManager arenas;
    private StatsManager stats;
    private MatchManager matches;
    private QueueManager queues;
    private DuelManager duels;
    private SpectatorManager spectators;
    private LobbyDisplayManager displays;
    private RankManager ranks;
    private PvpProgressionManager progression;
    private PlayerSettingsManager playerSettings;
    private Location lobby;

    @Override public void onEnable() {
        saveDefaultConfig();
        kits = new KitManager();
        arenas = new ArenaManager();
        stats = new StatsManager();
        matches = new MatchManager();
        queues = new QueueManager();
        duels = new DuelManager();
        spectators = new SpectatorManager();
        ranks = new RankManager(this);
        progression = new PvpProgressionManager(this);
        playerSettings = new PlayerSettingsManager();
        displays = new LobbyDisplayManager(this, id -> stats.get(id, "wins"), ranks::getRank, id -> playerSettings.enabled(id, "scoreboard", true));
        Bukkit.getScheduler().runTaskTimer(this, displays::refreshAll, 20L, 20L);
        loadLobby();
        getServer().getPluginManager().registerEvents(this, this);
        bind("kit", new KitCommand());
        bind("arena", new ArenaCommand());
        bind("queue", new QueueCommand());
        bind("leave", new LeaveCommand());
        bind("settings", new SettingsCommand());
        bind("mcpvp", new MainCommand());
        bind("stats", new StatsCommand());
        bind("duel", new DuelCommand());
        bind("spec", new SpectatorCommand());
        bind("menu", new MainMenuCommand());
        bind("match", new MatchCommand());
        bind("rank", new RankCommand());
        bind("elo", new ProgressionCommand());
        bind("streak", new ProgressionCommand());
        bind("history", new ProgressionCommand());
        bind("rematch", new RematchCommand());
        getLogger().info("MCPvP " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override public void onDisable() {
        if (queues != null) queues.clear();
        if (matches != null) matches.shutdown();
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) command.setExecutor(executor);
    }

    private void loadLobby() {
        String world = getConfig().getString("lobby.world", "");
        World w = Bukkit.getWorld(world);
        if (w != null) lobby = new Location(w, getConfig().getDouble("lobby.x"),
                getConfig().getDouble("lobby.y"), getConfig().getDouble("lobby.z"),
                (float)getConfig().getDouble("lobby.yaw"), (float)getConfig().getDouble("lobby.pitch"));
    }

    private void saveLobby(Location l) {
        getConfig().set("lobby.world", l.getWorld().getName());
        getConfig().set("lobby.x", l.getX()); getConfig().set("lobby.y", l.getY());
        getConfig().set("lobby.z", l.getZ()); getConfig().set("lobby.yaw", l.getYaw());
        getConfig().set("lobby.pitch", l.getPitch()); saveConfig(); lobby = l.clone();
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(ChatColor.GOLD + "[MCPvP] " + ChatColor.GRAY + text);
    }

    private String clean(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    @EventHandler public void onBlockPlace(BlockPlaceEvent event) {
        if (matches.inMatch(event.getPlayer().getUniqueId()) && !matches.canEdit(event.getPlayer(), event.getBlockPlaced().getLocation())) event.setCancelled(true);
    }
    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        if (matches.inMatch(event.getPlayer().getUniqueId()) && !matches.canEdit(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        if (matches.inMatch(event.getEntity().getUniqueId())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            matches.handleDeath(event.getEntity());
        }
    }
    @EventHandler public void onOpenWorldPvpDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (matches.inMatch(victim.getUniqueId())) return;
        progression.recordDeath(victim);
        stats.add(victim.getUniqueId(), "deaths", 1);
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            stats.add(killer.getUniqueId(), "kills", 1);
            progression.recordKill(killer);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        queues.leave(event.getPlayer().getUniqueId());
        duels.clearFor(event.getPlayer().getUniqueId());
        spectators.restore(event.getPlayer());
        matches.handleQuit(event.getPlayer());
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        matches.handleRespawn(event);
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.equals("MCPvP Settings")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String action = clicked.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(this, "settings-action"), org.bukkit.persistence.PersistentDataType.STRING);
            if (action == null) return;
            if (action.equals("scoreboard")) playerSettings.toggle(player, "scoreboard");
            if (action.equals("players")) playerSettings.toggle(player, "players");
            playerSettings.apply(player);
            new SettingsCommand().onCommand(player, null, "settings", new String[0]);
            return;
        }
        if (title.equals("MCPvP Menu")) {
            event.setCancelled(true);
            ItemStack selected = event.getCurrentItem();
            if (selected == null || !selected.hasItemMeta()) return;
            String action = selected.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(this, "menu-action"), org.bukkit.persistence.PersistentDataType.STRING);
            if ("kits".equals(action)) new KitsMenuCommand().onCommand(player, null, "kits", new String[0]);
            else if ("stats".equals(action)) { player.closeInventory(); new StatsCommand().onCommand(player, null, "stats", new String[0]); }
            else if ("leaderboard".equals(action)) { player.closeInventory(); new StatsCommand().onCommand(player, null, "stats", new String[]{"top"}); }
            else if ("arenas".equals(action)) { player.closeInventory(); msg(player, "Arena presets and maps: " + String.join(", ", arenas.names())); }
            return;
        }
        if (!title.equals("MCPvP Kits")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String kit = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (!kits.exists(kit)) return;
        player.closeInventory();
        String queuedKit = queues.queuedKit(player.getUniqueId());
        if (queuedKit != null && queuedKit.equalsIgnoreCase(kit)) {
            queues.leave(player.getUniqueId());
            msg(player, "You left the " + kit + " queue.");
        } else {
            queues.join(player, kit);
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            matches.restorePending(event.getPlayer());
            for (Player online : Bukkit.getOnlinePlayers()) playerSettings.apply(online);
        }, 1L);
    }

    private final class KitManager {
        private final File folder = new File(getDataFolder(), "kits");
        KitManager() { folder.mkdirs(); }
        File file(String name) { return new File(folder, clean(name) + ".yml"); }
        boolean exists(String name) { return file(name).isFile(); }
        List<String> names() {
            File[] files = folder.listFiles((d,n) -> n.endsWith(".yml"));
            if (files == null) return List.of();
            return Arrays.stream(files).map(f -> f.getName().substring(0, f.getName().length()-4)).sorted().toList();
        }
        boolean create(Player player, String name) {
            String id = clean(name);
            if (id.isBlank() || exists(id)) return false;
            YamlConfiguration y = new YamlConfiguration();
            PlayerInventory inv = player.getInventory();
            y.set("display-name", name);
            Material icon = Arrays.stream(inv.getContents()).filter(Objects::nonNull).filter(item -> item.getType() != Material.AIR).map(ItemStack::getType).findFirst().orElse(Material.IRON_SWORD);
            y.set("icon", icon.name());
            y.set("contents", Arrays.asList(inv.getContents()));
            y.set("armor", Arrays.asList(inv.getArmorContents()));
            y.set("offhand", inv.getItemInOffHand());
            y.set("created-by", player.getUniqueId().toString());
            try { y.save(file(id)); return true; } catch (IOException ex) {
                getLogger().warning("Could not save kit " + id + ": " + ex.getMessage()); return false;
            }
        }
        boolean delete(String name) { return exists(name) && file(name).delete(); }
        Material icon(String name) {
            if (!exists(name)) return Material.IRON_SWORD;
            String value = YamlConfiguration.loadConfiguration(file(name)).getString("icon", "IRON_SWORD");
            Material material = Material.matchMaterial(value == null ? "IRON_SWORD" : value);
            return material == null || !material.isItem() ? Material.IRON_SWORD : material;
        }
        boolean setIcon(String name, String materialName) {
            if (!exists(name)) return false;
            Material material = Material.matchMaterial(materialName);
            if (material == null || !material.isItem() || material == Material.AIR) return false;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file(name));
            y.set("icon", material.name());
            try { y.save(file(name)); return true; } catch (IOException ex) { getLogger().warning("Could not update kit icon: " + ex.getMessage()); return false; }
        }
        boolean apply(Player player, String name) {
            if (!exists(name)) return false;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file(name));
            PlayerInventory inv = player.getInventory();
            inv.clear();
            List<?> contents = y.getList("contents", List.of());
            ItemStack[] items = contents.toArray(new ItemStack[0]);
            inv.setContents(items);
            List<?> armor = y.getList("armor", List.of());
            ItemStack[] armorItems = armor.toArray(new ItemStack[0]);
            inv.setArmorContents(Arrays.copyOf(armorItems, 4));
            Object offhand = y.get("offhand");
            inv.setItemInOffHand(offhand instanceof ItemStack item ? item : null);
            player.setHealth(player.getMaxHealth()); player.setFoodLevel(20); player.setFireTicks(0);
            player.setFallDistance(0); player.setSaturation(20);
            return true;
        }
    }

    private final class ArenaManager {
        private final File file = new File(getDataFolder(), "arenas.yml");
        private YamlConfiguration data;
        private final Set<String> inUse = new HashSet<>();
        ArenaManager() {
            data = YamlConfiguration.loadConfiguration(file);
            File presetFile = new File(getDataFolder(), "arena-presets.yml");
            if (!presetFile.exists()) saveResource("arena-presets.yml", false);
            YamlConfiguration presets = YamlConfiguration.loadConfiguration(presetFile);
            for (Map<?, ?> preset : presets.getMapList("arenas")) {
                Object rawName = preset.get("name");
                if (rawName == null) continue;
                String id = clean(String.valueOf(rawName));
                if (id.isBlank()) continue;
                if (!data.contains(id)) {
                    data.createSection(id);
                    data.set(id + ".display-name", String.valueOf(rawName));
                    data.set(id + ".theme", String.valueOf(preset.containsKey("theme") ? preset.get("theme") : "Classic"));
                    data.set(id + ".preset", true);
                }
            }
            save();
        }
        void save() { try { data.save(file); } catch (IOException ex) { getLogger().warning(ex.getMessage()); } }
        boolean create(String name) {
            String id = clean(name);
            if (id.isBlank() || data.contains(id)) return false;
            data.createSection(id); data.set(id + ".display-name", name); data.set(id + ".theme", "Classic"); save(); return true;
        }
        boolean setTheme(String name, String theme) {
            String id = clean(name);
            if (!data.contains(id) || !ArenaThemes.isValid(theme)) return false;
            data.set(id + ".theme", theme); save(); return true;
        }
        String theme(String name) { return data.getString(clean(name) + ".theme", "Classic"); }
        boolean set(String name, String key, Location loc) {
            if (!data.contains(clean(name)) || loc.getWorld() == null) return false;
            String base = clean(name) + "." + key;
            data.set(base + ".world", loc.getWorld().getName());
            data.set(base + ".x", loc.getX()); data.set(base + ".y", loc.getY()); data.set(base + ".z", loc.getZ());
            data.set(base + ".yaw", loc.getYaw()); data.set(base + ".pitch", loc.getPitch()); save(); return true;
        }
        Location get(String name, String key) {
            String base = clean(name) + "." + key;
            World w = Bukkit.getWorld(data.getString(base + ".world", ""));
            if (w == null || !data.contains(base + ".x")) return null;
            return new Location(w, data.getDouble(base + ".x"), data.getDouble(base + ".y"),
                    data.getDouble(base + ".z"), (float)data.getDouble(base + ".yaw"), (float)data.getDouble(base + ".pitch"));
        }
        boolean hasRegion(String name) {
            Location one=get(name,"region.pos1"), two=get(name,"region.pos2");
            return one!=null && two!=null && one.getWorld()!=null && one.getWorld().equals(two.getWorld());
        }
        boolean contains(String name, Location location) {
            Location one=get(name,"region.pos1"), two=get(name,"region.pos2");
            if(one==null || two==null || location.getWorld()==null || !location.getWorld().equals(one.getWorld()) || !one.getWorld().equals(two.getWorld())) return false;
            return location.getBlockX()>=Math.min(one.getBlockX(),two.getBlockX()) && location.getBlockX()<=Math.max(one.getBlockX(),two.getBlockX())
                    && location.getBlockY()>=Math.min(one.getBlockY(),two.getBlockY()) && location.getBlockY()<=Math.max(one.getBlockY(),two.getBlockY())
                    && location.getBlockZ()>=Math.min(one.getBlockZ(),two.getBlockZ()) && location.getBlockZ()<=Math.max(one.getBlockZ(),two.getBlockZ());
        }
        World world(String name) {
            Location corner=get(name,"region.pos1");
            return corner==null ? null : corner.getWorld();
        }
        boolean delete(String name) {
            String id = clean(name);
            if (!data.contains(id)) return false;
            data.set(id, null); save(); return true;
        }
        List<String> names() {
            return data.getKeys(false).stream().sorted().toList();
        }
        String firstReady() {
            for (String name : names()) {
                if (!inUse.contains(name) && get(name, "spawn-a") != null && get(name, "spawn-b") != null && hasRegion(name)) return name;
            }
            return null;
        }
        boolean reserve(String name) { return inUse.add(clean(name)); }
        void release(String name) { inUse.remove(clean(name)); }
    }

    private final class StatsManager {
        private final File file = new File(getDataFolder(), "stats.yml");
        private final YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        int get(UUID id, String key) { return data.getInt(id + "." + key); }
        void add(UUID id, String key, int amount) {
            data.set(id + "." + key, get(id,key) + amount);
            try { data.save(file); } catch (IOException ex) { getLogger().warning(ex.getMessage()); }
        }
    }

    private final class MatchManager {
        private final Map<UUID, Match> active = new HashMap<>();
        private final Map<UUID, Snapshot> snapshots = new HashMap<>();
        private final class Snapshot {
            final ItemStack[] contents, armor; final ItemStack offhand;
            final Location location; final int food; final double health; final float exp; final int level; final GameMode gameMode; final boolean allowFlight;
            Snapshot(Player p) {
                contents = p.getInventory().getContents().clone(); armor = p.getInventory().getArmorContents().clone();
                offhand = p.getInventory().getItemInOffHand(); location = p.getLocation().clone();
                food = p.getFoodLevel(); health = p.getHealth(); exp = p.getExp(); level = p.getLevel(); gameMode = p.getGameMode(); allowFlight = p.getAllowFlight();
            }
            void restore(Player p) {
                p.getInventory().clear(); p.getInventory().setContents(contents); p.getInventory().setArmorContents(armor);
                p.getInventory().setItemInOffHand(offhand); p.setFoodLevel(food);
                p.setHealth(Math.max(1, Math.min(health, p.getMaxHealth()))); p.setExp(exp); p.setLevel(level);
                p.setFireTicks(0); p.setFallDistance(0); p.setAllowFlight(allowFlight); p.setGameMode(gameMode); p.teleport(location);
            }
        }
        private final class Match {
            final UUID one, two; final String kit, arena; WorldBorder border; Map<String,BlockState> arenaSnapshot;
            Match(Player a, Player b, String kit, String arena) {
                one=a.getUniqueId(); two=b.getUniqueId(); this.kit=kit; this.arena=arena;
            }
            UUID opponent(UUID id) { return one.equals(id) ? two : one; }
        }
        boolean start(Player a, Player b, String kit) {
            String arena = arenas.firstReady();
            if (arena == null) { msg(a, "No ready arena. An admin must set both spawns and both arena region corners."); msg(b, "No ready arena is configured."); return false; }
            Location sa=arenas.get(arena,"spawn-a"), sb=arenas.get(arena,"spawn-b");
            if (sa == null || sb == null) return false;
            snapshots.put(a.getUniqueId(), new Snapshot(a)); snapshots.put(b.getUniqueId(), new Snapshot(b));
            Match match = new Match(a,b,kit,arena); active.put(match.one,match); active.put(match.two,match);
            if (!arenas.reserve(arena)) {
                active.remove(match.one); active.remove(match.two);
                snapshots.remove(match.one); snapshots.remove(match.two);
                return false;
            }
            try {
                match.arenaSnapshot = captureArena(arena);
            } catch (IllegalStateException ex) {
                arenas.release(arena);
                active.remove(match.one); active.remove(match.two);
                snapshots.remove(match.one); snapshots.remove(match.two);
                msg(a, ex.getMessage()); msg(b, ex.getMessage());
                return false;
            }
            a.teleport(sa); b.teleport(sb); kits.apply(a,kit); kits.apply(b,kit);
            a.sendMessage(ChatColor.GREEN + "Match started: " + kit); b.sendMessage(ChatColor.GREEN + "Match started: " + kit);
            a.setGameMode(GameMode.SURVIVAL); b.setGameMode(GameMode.SURVIVAL);
            applyBorder(match, sa, sb);
            String theme = arenas.theme(arena);
            a.sendMessage(ChatColor.GRAY + "Arena theme: " + theme); b.sendMessage(ChatColor.GRAY + "Arena theme: " + theme);
            return true;
        }
        private void applyBorder(Match match, Location a, Location b) {
            if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return;
            WorldBorder border = Bukkit.createWorldBorder();
            border.setCenter((a.getX() + b.getX()) / 2.0, (a.getZ() + b.getZ()) / 2.0);
            double start = getConfig().getDouble("matches.border-start-size", 200.0);
            double end = getConfig().getDouble("matches.border-end-size", 18.0);
            long seconds = Math.max(1L, getConfig().getLong("matches.border-shrink-seconds", 90L));
            border.setSize(start);
            border.setDamageAmount(0.0);
            border.setWarningDistance(3);
            border.setSize(end, seconds);
            match.border = border;
            Player one = Bukkit.getPlayer(match.one), two = Bukkit.getPlayer(match.two);
            if (one != null) one.setWorldBorder(border);
            if (two != null) two.setWorldBorder(border);
        }
        void handleDeath(Player loser) {
            Match m = active.get(loser.getUniqueId()); if (m == null) return;
            Player winner = Bukkit.getPlayer(m.opponent(loser.getUniqueId()));
            if (winner != null) {
                stats.add(winner.getUniqueId(),"wins",1);
                stats.add(winner.getUniqueId(),"kills",1);
                progression.recordResult(winner, loser, m.kit);
                winner.sendMessage(ChatColor.GREEN + "You won the " + m.kit + " match!");
            }
            stats.add(loser.getUniqueId(),"losses",1);
            stats.add(loser.getUniqueId(),"deaths",1);
            progression.recordLoss(loser, winner, m.kit);
            active.remove(m.one); active.remove(m.two);
            restoreArena(m);
            arenas.release(m.arena);
            if (winner != null) restore(winner);
            loser.setWorldBorder(null);
        }
        void handleRespawn(PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            Snapshot snapshot = snapshots.get(player.getUniqueId());
            if (snapshot == null) return;
            event.setRespawnLocation(snapshot.location);
            Bukkit.getScheduler().runTaskLater(McpvpPlugin.this, () -> restore(player), 1L);
        }
        void restorePending(Player player) {
            if (snapshots.containsKey(player.getUniqueId()) && !active.containsKey(player.getUniqueId())) restore(player);
        }
        void handleQuit(Player player) {
            Match m = active.get(player.getUniqueId()); if (m == null) return;
            Player winner = Bukkit.getPlayer(m.opponent(player.getUniqueId()));
            if (winner != null) { stats.add(winner.getUniqueId(),"wins",1); stats.add(winner.getUniqueId(),"kills",1); progression.recordResult(winner, player, m.kit); winner.sendMessage(ChatColor.GREEN + "Opponent left. You win!"); }
            stats.add(player.getUniqueId(),"losses",1); stats.add(player.getUniqueId(),"deaths",1); progression.recordLoss(player, winner, m.kit); finish(m);
        }
        void finish(Match m) {
            active.remove(m.one); active.remove(m.two);
            restoreArena(m);
            arenas.release(m.arena);
            Player a=Bukkit.getPlayer(m.one), b=Bukkit.getPlayer(m.two);
            restore(a); restore(b);
        }
        void restore(Player p) {
            if (p == null) return;
            Snapshot s=snapshots.remove(p.getUniqueId());
            try { p.setWorldBorder(null); } catch (Throwable ignored) { }
            if (s != null) s.restore(p);
        }
        boolean inMatch(UUID id) { return active.containsKey(id); }
        boolean canEdit(Player player, Location location) {
            Match match = active.get(player.getUniqueId());
            return match != null && arenas.contains(match.arena, location);
        }
        private Map<String, BlockState> captureArena(String arena) {
            Location one=arenas.get(arena, "region.pos1"), two=arenas.get(arena, "region.pos2");
            Map<String, BlockState> snapshot=new LinkedHashMap<>();
            if(one==null || two==null || one.getWorld()==null || !one.getWorld().equals(two.getWorld())) return snapshot;
            int minX=Math.min(one.getBlockX(),two.getBlockX()), maxX=Math.max(one.getBlockX(),two.getBlockX());
            int minY=Math.max(one.getWorld().getMinHeight(),Math.min(one.getBlockY(),two.getBlockY()));
            int maxY=Math.min(one.getWorld().getMaxHeight()-1,Math.max(one.getBlockY(),two.getBlockY()));
            int minZ=Math.min(one.getBlockZ(),two.getBlockZ()), maxZ=Math.max(one.getBlockZ(),two.getBlockZ());
            long volume=(long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);
            long limit=getConfig().getLong("matches.max-arena-snapshot-blocks",100000L);
            if(volume>limit) throw new IllegalStateException("Arena region exceeds matches.max-arena-snapshot-blocks ("+limit+")");
            World world=one.getWorld();
            for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++) {
                Block block=world.getBlockAt(x,y,z);
                snapshot.put(x+","+y+","+z,block.getState());
            }
            return snapshot;
        }
        private void restoreArena(Match match) {
            if(match.arenaSnapshot==null || match.arenaSnapshot.isEmpty()) return;
            World world=arenas.world(match.arena);
            if(world==null) return;
            for(Map.Entry<String,BlockState> entry:match.arenaSnapshot.entrySet()) {
                try {
                    entry.getValue().update(true, false);
                } catch(RuntimeException ex) {
                    getLogger().warning("Could not restore arena block "+entry.getKey()+": "+ex.getMessage());
                }
            }
            match.arenaSnapshot.clear();
        }
        void forfeit(Player player) {
            Match m = active.get(player.getUniqueId());
            if (m == null) { msg(player, "You are not in a match."); return; }
            Player winner = Bukkit.getPlayer(m.opponent(player.getUniqueId()));
            stats.add(player.getUniqueId(), "losses", 1);
            stats.add(player.getUniqueId(), "deaths", 1);
            progression.recordLoss(player, winner, m.kit);
            if (winner != null) {
                stats.add(winner.getUniqueId(), "wins", 1);
                stats.add(winner.getUniqueId(), "kills", 1);
                progression.recordResult(winner, player, m.kit);
                msg(winner, "Your opponent forfeited. You win!");
            }
            msg(player, "You forfeited the match.");
            finish(m);
        }
        boolean rematch(Player player) {
            PvpProgressionManager.RematchRequest request = progression.rematch(player.getUniqueId());
            if (request == null) { msg(player, "No recent opponent is available for a rematch."); return false; }
            Player opponent = Bukkit.getPlayer(request.opponent());
            if (opponent == null || !opponent.isOnline()) { msg(player, "Your previous opponent is not online."); return false; }
            if (inMatch(player.getUniqueId()) || inMatch(opponent.getUniqueId())) { msg(player, "Both players must be outside a match."); return false; }
            msg(opponent, player.getName() + " requested a rematch. They can accept with /rematch accept.");
            progression.pendingRematch(opponent.getUniqueId(), player.getUniqueId(), request.kit());
            msg(player, "Rematch request sent to " + opponent.getName() + ".");
            return true;
        }
        boolean acceptRematch(Player player) {
            PvpProgressionManager.RematchRequest request = progression.takePendingRematch(player.getUniqueId());
            if (request == null) { msg(player, "You have no pending rematch."); return false; }
            Player challenger = Bukkit.getPlayer(request.opponent());
            if (challenger == null || !challenger.isOnline() || inMatch(challenger.getUniqueId()) || inMatch(player.getUniqueId())) { msg(player, "The rematch is no longer available."); return false; }
            return start(challenger, player, request.kit());
        }
        Player opponent(Player player) {
            Match m = active.get(player.getUniqueId());
            return m == null ? null : Bukkit.getPlayer(m.opponent(player.getUniqueId()));
        }
        void shutdown() {
            for (Match match : new HashSet<>(active.values())) { restoreArena(match); arenas.release(match.arena); }
            for (UUID id : new ArrayList<>(snapshots.keySet())) restore(Bukkit.getPlayer(id));
            active.clear();
        }
    }

    private final class DuelManager {
        private final Map<UUID, UUID> requests = new HashMap<>();
        private final Map<UUID, BukkitTask> expiry = new HashMap<>();
        void challenge(Player challenger, Player target) {
            if (challenger.equals(target)) { msg(challenger, "You cannot challenge yourself."); return; }
            if (matches.inMatch(challenger.getUniqueId()) || matches.inMatch(target.getUniqueId())) {
                msg(challenger, "Both players must be outside a match."); return;
            }
            requests.put(target.getUniqueId(), challenger.getUniqueId());
            BukkitTask old = expiry.remove(target.getUniqueId());
            if (old != null) old.cancel();
            expiry.put(target.getUniqueId(), Bukkit.getScheduler().runTaskLater(McpvpPlugin.this, () -> {
                requests.remove(target.getUniqueId()); expiry.remove(target.getUniqueId());
            }, 20L * 30));
            msg(challenger, "Duel request sent to " + target.getName() + ".");
            msg(target, challenger.getName() + " challenged you. Use /duel accept or /duel deny (expires in 30 seconds).");
        }
        void accept(Player target) {
            UUID challengerId = requests.remove(target.getUniqueId());
            BukkitTask task = expiry.remove(target.getUniqueId());
            if (task != null) task.cancel();
            Player challenger = challengerId == null ? null : Bukkit.getPlayer(challengerId);
            if (challenger == null || !challenger.isOnline()) { msg(target, "That duel request has expired."); return; }
            if (matches.inMatch(challengerId) || matches.inMatch(target.getUniqueId())) {
                msg(target, "A player is already in a match."); return;
            }
            String kit = getConfig().getString("duels.default-kit", "");
            if (kit.isBlank()) kit = kits.names().stream().findFirst().orElse("");
            if (kit.isBlank() || !kits.exists(kit)) { msg(target, "No valid default duel kit is configured."); return; }
            matches.start(challenger, target, kit);
        }
        void deny(Player target) {
            UUID id = requests.remove(target.getUniqueId());
            BukkitTask task = expiry.remove(target.getUniqueId());
            if (task != null) task.cancel();
            Player challenger = id == null ? null : Bukkit.getPlayer(id);
            msg(target, "Duel request denied.");
            if (challenger != null) msg(challenger, target.getName() + " denied your duel request.");
        }
        void clearFor(UUID id) {
            requests.remove(id);
            BukkitTask task = expiry.remove(id);
            if (task != null) task.cancel();
            requests.entrySet().removeIf(e -> e.getValue().equals(id));
        }
    }

    private final class SpectatorManager {
        private final Map<UUID, GameMode> previousModes = new HashMap<>();
        private final Map<UUID, Location> previousLocations = new HashMap<>();
        void spectate(Player spectator, Player target) {
            if (spectator.equals(target) || !matches.inMatch(target.getUniqueId())) {
                msg(spectator, "That player is not currently in a match."); return;
            }
            if (matches.inMatch(spectator.getUniqueId())) { msg(spectator, "You cannot spectate while in a match."); return; }
            previousModes.putIfAbsent(spectator.getUniqueId(), spectator.getGameMode());
            previousLocations.putIfAbsent(spectator.getUniqueId(), spectator.getLocation().clone());
            spectator.setGameMode(GameMode.SPECTATOR);
            spectator.teleport(target.getLocation());
            msg(spectator, "Spectating " + target.getName() + ". Use /spec leave to return.");
        }
        void restore(Player player) {
            GameMode mode = previousModes.remove(player.getUniqueId());
            Location location = previousLocations.remove(player.getUniqueId());
            if (mode != null) player.setGameMode(mode);
            if (location != null && player.isOnline()) player.teleport(location);
        }
    }

    private final class QueueManager {
        private final Map<String, LinkedList<UUID>> waiting = new HashMap<>();
        void join(Player player, String kit) {
            if (!kits.exists(kit)) { msg(player,"Unknown kit. Use /kit list."); return; }
            if (matches.inMatch(player.getUniqueId())) { msg(player,"You are already in a match."); return; }
            String current = queuedKit(player.getUniqueId());
            if (current != null && current.equalsIgnoreCase(kit)) {
                msg(player, "You are already queued for " + kit + " (position " + position(player.getUniqueId()) + "). Use /leave to leave.");
                return;
            }
            leave(player.getUniqueId());
            LinkedList<UUID> line=waiting.computeIfAbsent(clean(kit), k -> new LinkedList<>());
            while (!line.isEmpty()) {
                UUID otherId=line.removeFirst();
                Player other=Bukkit.getPlayer(otherId);
                if (other != null && other.isOnline() && !matches.inMatch(otherId)) {
                    if (matches.start(other,player,kit)) return;
                }
            }
            line.add(player.getUniqueId());
            msg(player,"Queued for " + kit + ". Position: " + line.size() + ". Use /leave or click this kit again to leave.");
        }
        int count(String kit) { return waiting.getOrDefault(clean(kit), new LinkedList<>()).size(); }
        String queuedKit(UUID id) {
            for (Map.Entry<String, LinkedList<UUID>> entry : waiting.entrySet()) {
                if (entry.getValue().contains(id)) return entry.getKey();
            }
            return null;
        }
        int position(UUID id) {
            String kit = queuedKit(id);
            if (kit == null) return 0;
            return waiting.get(kit).indexOf(id) + 1;
        }
        boolean leave(UUID id) {
            boolean removed = false;
            for (LinkedList<UUID> line : waiting.values()) removed |= line.remove(id);
            waiting.values().removeIf(LinkedList::isEmpty);
            return removed;
        }
        void clear() { waiting.clear(); }
    }

    private final class KitCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length==0) { msg(s,"/kit create <name>, /kit list, /kit icon <name> <material>, /kit delete <name>, /kit give <player> <name>"); return true; }
            switch(a[0].toLowerCase(Locale.ROOT)) {
                case "create" -> {
                    if (!(s instanceof Player p) || !s.hasPermission("mcpvp.admin")) { msg(s,"You need to be an admin in-game to create kits."); return true; }
                    if (a.length<2) { msg(s,"Usage: /kit create <name>"); return true; }
                    msg(s,kits.create(p,a[1]) ? "Created kit " + a[1] + " from your inventory." : "Could not create kit. It may already exist.");
                }
                case "list" -> msg(s,"Kits: " + (kits.names().isEmpty() ? "none" : String.join(", ",kits.names())));
                case "icon" -> {
                    if (!s.hasPermission("mcpvp.admin") || a.length < 3) { msg(s, "Usage: /kit icon <name> <material> (admin)"); break; }
                    msg(s, kits.setIcon(a[1], a[2]) ? "Updated kit icon." : "Kit or item material not found.");
                }
                case "delete" -> {
                    if (!s.hasPermission("mcpvp.admin") || a.length<2) { msg(s,"Usage: /kit delete <name> (admin)"); return true; }
                    msg(s,kits.delete(a[1]) ? "Deleted kit." : "Kit not found.");
                }
                case "give" -> {
                    if (!s.hasPermission("mcpvp.admin") || a.length<3) { msg(s,"Usage: /kit give <player> <name> (admin)"); return true; }
                    Player target=Bukkit.getPlayerExact(a[1]);
                    msg(s,target!=null && kits.apply(target,a[2]) ? "Kit given." : "Player or kit not found.");
                }
                default -> msg(s,"Unknown subcommand. Use /kit.");
            }
            return true;
        }
    }

    private final class ArenaCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length > 0 && a[0].equalsIgnoreCase("presets")) {
                msg(s, "Arena presets: " + arenas.names().size() + " available. Set both spawn points to activate a map.");
                return true;
            }
            if (!s.hasPermission("mcpvp.admin") || !(s instanceof Player p)) { msg(s,"This command requires an admin in-game."); return true; }
            if (a.length==0) { msg(s,"/arena create <name>, /arena theme <name> <theme>, /arena setspawn <name> <a|b>, /arena pos1 <name>, /arena pos2 <name>, /arena delete <name>, /arena list, /arena presets"); return true; }
            switch(a[0].toLowerCase(Locale.ROOT)) {
                case "create" -> msg(s,a.length>1 && arenas.create(a[1]) ? "Arena created." : "Could not create arena.");
                case "theme" -> {
                    if (a.length < 3) { msg(s, "Usage: /arena theme <name> <theme>. Themes: " + String.join(", ", ArenaThemes.THEMES)); break; }
                    msg(s, arenas.setTheme(a[1], String.join(" ", Arrays.copyOfRange(a, 2, a.length))) ? "Arena theme updated." : "Arena or theme not found.");
                }
                case "pos1", "pos2" -> {
                    if (a.length < 2) { msg(s, "Usage: /arena " + a[0] + " <name>"); break; }
                    msg(s, arenas.set(a[1], a[0].equalsIgnoreCase("pos1") ? "region.pos1" : "region.pos2", p.getLocation()) ? "Arena region corner saved." : "Arena not found.");
                }
                case "setspawn" -> {
                    if (a.length<3 || !(a[2].equalsIgnoreCase("a") || a[2].equalsIgnoreCase("b"))) { msg(s,"Usage: /arena setspawn <name> <a|b>"); break; }
                    msg(s,arenas.set(a[1],a[2].equalsIgnoreCase("a")?"spawn-a":"spawn-b",p.getLocation()) ? "Arena spawn saved." : "Arena not found.");
                }
                case "delete" -> msg(s,a.length>1 && arenas.delete(a[1]) ? "Arena deleted." : "Arena not found.");
                case "list" -> msg(s,"Arenas: " + String.join(", ",arenas.names()));
                default -> msg(s,"Unknown subcommand.");
            }
            return true;
        }
    }

    private final class QueueCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player p)) { msg(s,"Players only."); return true; }
            if (a.length == 0) {
                new KitsMenuCommand().onCommand(p, c, "queue", new String[0]);
                return true;
            }
            if (a[0].equalsIgnoreCase("status")) {
                if (a.length > 1) {
                    msg(p,"Players waiting for " + a[1] + ": " + queues.count(a[1]));
                } else {
                    String kit = queues.queuedKit(p.getUniqueId());
                    if (kit == null) msg(p, "You are not currently queued.");
                    else msg(p, "Queue: " + kit + " | Position: " + queues.position(p.getUniqueId()) + " | Waiting: " + queues.count(kit) + ". Use /leave to leave.");
                }
            } else if (a[0].equalsIgnoreCase("join") && a.length>1) queues.join(p,a[1]);
            else if (a[0].equalsIgnoreCase("leave")) {
                msg(p, queues.leave(p.getUniqueId()) ? "You left the queue." : "You are not currently queued.");
            } else msg(p,"Usage: /queue join <kit>, /queue leave or /queue status [kit]");
            return true;
        }
    }

    private final class LeaveCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            msg(player, queues.leave(player.getUniqueId()) ? "You left the queue." : "You are not currently queued.");
            return true;
        }
    }

    private final class MainCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length==0) { msg(s,"MCPvP commands: /menu, /queue, /kit, /arena, /stats, /mcpvp lobby"); return true; }
            if (a[0].equalsIgnoreCase("menu")) { if (s instanceof Player p) new MainMenuCommand().onCommand(p, c, l, new String[0]); else msg(s, "Players only."); }
            else if (a[0].equalsIgnoreCase("setlobby") && s instanceof Player p && s.hasPermission("mcpvp.admin")) {
                saveLobby(p.getLocation()); msg(s,"Lobby location saved.");
            } else if (a[0].equalsIgnoreCase("lobby") && s instanceof Player p) {
                if (lobby == null) msg(s,"Lobby is not configured.");
                else p.teleport(lobby);
            } else if (a[0].equalsIgnoreCase("reload") && s.hasPermission("mcpvp.admin")) {
                reloadConfig(); loadLobby(); msg(s,"Configuration reloaded.");
            } else msg(s,"Usage: /mcpvp lobby, /mcpvp setlobby, /mcpvp reload");
            return true;
        }
    }

    private final class KitsMenuCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            List<String> names = kits.names();
            if (names.isEmpty()) { msg(player, "No kits have been created yet."); return true; }
            Inventory menu = Bukkit.createInventory(null, Math.min(54, ((names.size() + 8) / 9) * 9), ChatColor.DARK_AQUA + "MCPvP Kits");
            for (String name : names.stream().limit(54).toList()) {
                ItemStack icon = new ItemStack(kits.icon(name));
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + name);
                boolean queuedHere = name.equalsIgnoreCase(Objects.toString(queues.queuedKit(player.getUniqueId()), ""));
                meta.setLore(List.of(
                        ChatColor.GRAY + (queuedHere ? "Click again to leave this queue" : "Click to queue for this kit"),
                        ChatColor.DARK_GRAY + "Waiting: " + queues.count(name),
                        ChatColor.DARK_GRAY + "Your position: " + (queuedHere ? queues.position(player.getUniqueId()) : "Not queued"),
                        ChatColor.DARK_GRAY + "Icon: " + kits.icon(name).name()));
                icon.setItemMeta(meta);
                menu.addItem(icon);
            }
            player.openInventory(menu);
            return true;
        }
    }

    private final class MainMenuCommand implements CommandExecutor {
        private void button(Inventory inventory, int slot, Material material, String name, String action, String... lore) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + name);
            List<String> lines = new ArrayList<>();
            for (String line : lore) lines.add(ChatColor.GRAY + line);
            meta.setLore(lines);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(McpvpPlugin.this, "menu-action"), org.bukkit.persistence.PersistentDataType.STRING, action);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "MCPvP Menu");
            button(menu, 10, Material.IRON_SWORD, "Queue", "kits", "Browse kits and join matchmaking with /queue.");
            button(menu, 12, Material.PLAYER_HEAD, "My Statistics", "stats", "View your wins, losses and match record.");
            button(menu, 14, Material.GOLD_INGOT, "Top Players", "leaderboard", "View the wins leaderboard.");
            button(menu, 16, Material.MAP, "Arena List", "arenas", "View available arena records.");
            player.openInventory(menu);
            return true;
        }
    }

    private final class PlayerSettingsManager {
        private final File file = new File(getDataFolder(), "player-settings.yml");
        private final YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        boolean enabled(UUID id, String key, boolean fallback) { return data.getBoolean(id + "." + key, fallback); }
        void toggle(Player player, String key) {
            boolean next = !enabled(player.getUniqueId(), key, true);
            data.set(player.getUniqueId() + "." + key, next);
            try { data.save(file); } catch (IOException ex) { getLogger().warning("Could not save player settings: " + ex.getMessage()); }
            msg(player, key + " setting: " + (next ? "ON" : "OFF"));
        }
        void apply(Player player) {
            boolean visible = enabled(player.getUniqueId(), "players", true);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                if (visible) player.showPlayer(McpvpPlugin.this, other);
                else player.hidePlayer(McpvpPlugin.this, other);
            }
            if (enabled(player.getUniqueId(), "scoreboard", true)) displays.refresh(player);
            else player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private final class SettingsCommand implements CommandExecutor {
        private void button(Inventory inv, int slot, Material material, String label, String action, boolean enabled) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + label + (enabled ? " ON" : " OFF"));
            meta.setLore(List.of(ChatColor.GRAY + "Click to toggle"));
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(McpvpPlugin.this, "settings-action"), org.bukkit.persistence.PersistentDataType.STRING, action);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "MCPvP Settings");
            button(inv, 11, Material.OAK_SIGN, "Show players", "players", playerSettings.enabled(player.getUniqueId(), "players", true));
            button(inv, 15, Material.COMPARATOR, "Scoreboard", "scoreboard", playerSettings.enabled(player.getUniqueId(), "scoreboard", true));
            player.openInventory(inv);
            return true;
        }
    }

    private final class RematchCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            if (args.length > 0 && args[0].equalsIgnoreCase("accept")) matches.acceptRematch(player);
            else matches.rematch(player);
            return true;
        }
    }

    private final class ProgressionCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            String sub = command.getName().toLowerCase(Locale.ROOT);
            if (sub.equals("elo")) msg(player, "Your ELO: " + progression.elo(player.getUniqueId()) + " | Tier: " + progression.tier(player.getUniqueId()));
            else if (sub.equals("streak")) msg(player, "Killstreak: " + progression.streak(player.getUniqueId()) + " (best " + progression.bestStreak(player.getUniqueId()) + ") | Win streak: " + progression.winStreak(player.getUniqueId()) + " (best " + progression.bestWinStreak(player.getUniqueId()) + ")");
            else msg(player, "Recent matches: " + progression.history(player.getUniqueId()));
            return true;
        }
    }

    private final class MatchCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { msg(sender, "Players only."); return true; }
            if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
                Player opponent = matches.opponent(player);
                if (opponent == null) msg(player, "You are not currently in a match.");
                else msg(player, "Your opponent is " + opponent.getName() + ".");
                return true;
            }
            if (args[0].equalsIgnoreCase("forfeit")) matches.forfeit(player);
            else msg(player, "Usage: /match info or /match forfeit");
            return true;
        }
    }

    private final class DuelCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player player)) { msg(s, "Players only."); return true; }
            if (a.length == 0) { msg(player, "/duel <player>, /duel accept, /duel deny"); return true; }
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "accept" -> duels.accept(player);
                case "deny" -> duels.deny(player);
                default -> {
                    Player target = Bukkit.getPlayerExact(a[0]);
                    if (target == null) msg(player, "That player is not online.");
                    else duels.challenge(player, target);
                }
            }
            return true;
        }
    }

    private final class SpectatorCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player player)) { msg(s, "Players only."); return true; }
            if (a.length == 0) { msg(player, "/spec <player> or /spec leave"); return true; }
            if (a[0].equalsIgnoreCase("leave")) spectators.restore(player);
            else {
                Player target = Bukkit.getPlayerExact(a[0]);
                if (target == null) msg(player, "That player is not online.");
                else spectators.spectate(player, target);
            }
            return true;
        }
    }

    private final class RankCommand implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                if (sender instanceof Player player) msg(sender, player.getName() + " rank: " + ranks.getRank(player.getUniqueId()));
                else msg(sender, "Usage: /rank <player> or /rank <set|add|remove> <player> [rank]");
                return true;
            }
            String action = args[0].toLowerCase(Locale.ROOT);
            if (action.equals("set") || action.equals("add")) {
                if (!sender.hasPermission("mcpvp.rank.manage") && !sender.hasPermission("mcpvp.admin")) { msg(sender, "You do not have permission to manage ranks."); return true; }
                if (args.length < 3) { msg(sender, "Usage: /rank " + action + " <player> <rank>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String requested = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String rank = ranks.resolve(requested);
                if (rank == null) { msg(sender, "Unknown rank. Valid ranks: " + String.join(", ", RankManager.RANKS)); return true; }
                ranks.setRank(target.getUniqueId(), rank);
                Player online = target.getPlayer();
                if (online != null) displays.refresh(online);
                msg(sender, "Set " + target.getName() + " rank to " + rank + ".");
                return true;
            }
            if (action.equals("remove")) {
                if (!sender.hasPermission("mcpvp.rank.manage") && !sender.hasPermission("mcpvp.admin")) { msg(sender, "You do not have permission to manage ranks."); return true; }
                if (args.length != 2) { msg(sender, "Usage: /rank remove <player>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                ranks.removeRank(target.getUniqueId());
                Player online = target.getPlayer();
                if (online != null) displays.refresh(online);
                msg(sender, "Removed custom rank from " + target.getName() + ". They are now Player.");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && target.getName() == null) { msg(sender, "Player not found."); return true; }
            msg(sender, target.getName() + " rank: " + ranks.getRank(target.getUniqueId()));
            return true;
        }
    }

    private final class StatsCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length > 0 && a[0].equalsIgnoreCase("top")) {
                String metric = a.length > 1 && a[1].equalsIgnoreCase("kills") ? "kills" : "wins";
                List<String> lines = new ArrayList<>();
                for (String key : stats.data.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(key);
                        lines.add(stats.get(id, metric) + "|" + Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(id.toString()));
                    } catch (IllegalArgumentException ignored) { }
                }
                lines.sort((x,y) -> Integer.compare(Integer.parseInt(y.substring(0,y.indexOf('|'))), Integer.parseInt(x.substring(0,x.indexOf('|')))));
                msg(s, "Top " + metric + ": " + (lines.isEmpty() ? "No stats yet." : String.join(", ", lines.stream().limit(10).toList())));
                return true;
            }
            Player target = a.length>0 ? Bukkit.getPlayerExact(a[0]) : (s instanceof Player p ? p : null);
            if (target == null) { msg(s,"Usage: /stats [online-player|top]"); return true; }
            int wins=stats.get(target.getUniqueId(),"wins"), losses=stats.get(target.getUniqueId(),"losses");
            int kills=stats.get(target.getUniqueId(),"kills"), deaths=stats.get(target.getUniqueId(),"deaths");
            msg(s,target.getName() + " - Wins: " + wins + " | Losses: " + losses + " | Kills: " + kills + " | Deaths: " + deaths + " | Matches: " + (wins+losses));
            return true;
        }
    }
}
