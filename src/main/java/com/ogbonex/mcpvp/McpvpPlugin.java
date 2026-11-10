package com.ogbonex.mcpvp;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
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
        loadLobby();
        getServer().getPluginManager().registerEvents(this, this);
        bind("kit", new KitCommand());
        bind("arena", new ArenaCommand());
        bind("queue", new QueueCommand());
        bind("mcpvp", new MainCommand());
        bind("stats", new StatsCommand());
        bind("duel", new DuelCommand());
        bind("spec", new SpectatorCommand());
        bind("kits", new KitsMenuCommand());
        bind("match", new MatchCommand());
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

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        if (matches.inMatch(event.getEntity().getUniqueId())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            matches.handleDeath(event.getEntity());
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
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("MCPvP Kits")) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String kit = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (!kits.exists(kit)) return;
        player.closeInventory();
        queues.join(player, kit);
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> matches.restorePending(event.getPlayer()), 1L);
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
            y.set("contents", Arrays.asList(inv.getContents()));
            y.set("armor", Arrays.asList(inv.getArmorContents()));
            y.set("offhand", inv.getItemInOffHand());
            y.set("created-by", player.getUniqueId().toString());
            try { y.save(file(id)); return true; } catch (IOException ex) {
                getLogger().warning("Could not save kit " + id + ": " + ex.getMessage()); return false;
            }
        }
        boolean delete(String name) { return exists(name) && file(name).delete(); }
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
        ArenaManager() { data = YamlConfiguration.loadConfiguration(file); }
        void save() { try { data.save(file); } catch (IOException ex) { getLogger().warning(ex.getMessage()); } }
        boolean create(String name) {
            String id = clean(name);
            if (id.isBlank() || data.contains(id)) return false;
            data.createSection(id); save(); return true;
        }
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
        boolean delete(String name) {
            String id = clean(name);
            if (!data.contains(id)) return false;
            data.set(id, null); save(); return true;
        }
        List<String> names() {
            return data.getKeys(false).stream().sorted().toList();
        }
        String firstReady() {
            for (String name : names()) if (get(name, "spawn-a") != null && get(name, "spawn-b") != null) return name;
            return null;
        }
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
            final UUID one, two; final String kit, arena;
            Match(Player a, Player b, String kit, String arena) {
                one=a.getUniqueId(); two=b.getUniqueId(); this.kit=kit; this.arena=arena;
            }
            UUID opponent(UUID id) { return one.equals(id) ? two : one; }
        }
        boolean start(Player a, Player b, String kit) {
            String arena = arenas.firstReady();
            if (arena == null) { msg(a, "No ready arena. An admin must create an arena and set both spawns."); msg(b, "No ready arena is configured."); return false; }
            Location sa=arenas.get(arena,"spawn-a"), sb=arenas.get(arena,"spawn-b");
            if (sa == null || sb == null) return false;
            snapshots.put(a.getUniqueId(), new Snapshot(a)); snapshots.put(b.getUniqueId(), new Snapshot(b));
            Match match = new Match(a,b,kit,arena); active.put(match.one,match); active.put(match.two,match);
            a.teleport(sa); b.teleport(sb); kits.apply(a,kit); kits.apply(b,kit);
            a.sendMessage(ChatColor.GREEN + "Match started: " + kit); b.sendMessage(ChatColor.GREEN + "Match started: " + kit);
            a.setGameMode(GameMode.SURVIVAL); b.setGameMode(GameMode.SURVIVAL);
            return true;
        }
        void handleDeath(Player loser) {
            Match m = active.get(loser.getUniqueId()); if (m == null) return;
            Player winner = Bukkit.getPlayer(m.opponent(loser.getUniqueId()));
            if (winner != null) {
                stats.add(winner.getUniqueId(),"wins",1);
                winner.sendMessage(ChatColor.GREEN + "You won the " + m.kit + " match!");
            }
            stats.add(loser.getUniqueId(),"losses",1);
            active.remove(m.one); active.remove(m.two);
            restore(winner);
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
            if (winner != null) { stats.add(winner.getUniqueId(),"wins",1); winner.sendMessage(ChatColor.GREEN + "Opponent left. You win!"); }
            stats.add(player.getUniqueId(),"losses",1); finish(m);
        }
        void finish(Match m) {
            active.remove(m.one); active.remove(m.two);
            Player a=Bukkit.getPlayer(m.one), b=Bukkit.getPlayer(m.two);
            restore(a); restore(b);
        }
        void restore(Player p) {
            if (p == null) return;
            Snapshot s=snapshots.remove(p.getUniqueId());
            if (s != null) s.restore(p);
        }
        boolean inMatch(UUID id) { return active.containsKey(id); }
        void forfeit(Player player) {
            Match m = active.get(player.getUniqueId());
            if (m == null) { msg(player, "You are not in a match."); return; }
            Player winner = Bukkit.getPlayer(m.opponent(player.getUniqueId()));
            stats.add(player.getUniqueId(), "losses", 1);
            if (winner != null) {
                stats.add(winner.getUniqueId(), "wins", 1);
                msg(winner, "Your opponent forfeited. You win!");
            }
            msg(player, "You forfeited the match.");
            finish(m);
        }
        Player opponent(Player player) {
            Match m = active.get(player.getUniqueId());
            return m == null ? null : Bukkit.getPlayer(m.opponent(player.getUniqueId()));
        }
        void shutdown() { for (UUID id : new ArrayList<>(snapshots.keySet())) restore(Bukkit.getPlayer(id)); active.clear(); }
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
            msg(player,"Queued for " + kit + ". Waiting for an opponent...");
        }
        int count(String kit) { return waiting.getOrDefault(clean(kit), new LinkedList<>()).size(); }
        void leave(UUID id) {
            for (LinkedList<UUID> line : waiting.values()) line.remove(id);
            waiting.values().removeIf(LinkedList::isEmpty);
        }
        void clear() { waiting.clear(); }
    }

    private final class KitCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length==0) { msg(s,"/kit create <name>, /kit list, /kit delete <name>, /kit give <player> <name>"); return true; }
            switch(a[0].toLowerCase(Locale.ROOT)) {
                case "create" -> {
                    if (!(s instanceof Player p) || !s.hasPermission("mcpvp.admin")) { msg(s,"You need to be an admin in-game to create kits."); return true; }
                    if (a.length<2) { msg(s,"Usage: /kit create <name>"); return true; }
                    msg(s,kits.create(p,a[1]) ? "Created kit " + a[1] + " from your inventory." : "Could not create kit. It may already exist.");
                }
                case "list" -> msg(s,"Kits: " + (kits.names().isEmpty() ? "none" : String.join(", ",kits.names())));
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
            if (!s.hasPermission("mcpvp.admin") || !(s instanceof Player p)) { msg(s,"This command requires an admin in-game."); return true; }
            if (a.length==0) { msg(s,"/arena create <name>, /arena setspawn <name> <a|b>, /arena delete <name>, /arena list"); return true; }
            switch(a[0].toLowerCase(Locale.ROOT)) {
                case "create" -> msg(s,a.length>1 && arenas.create(a[1]) ? "Arena created." : "Could not create arena.");
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
            if (a.length==0) { msg(s,"/queue join <kit>, /queue leave, /queue status <kit>"); return true; }
            if (a[0].equalsIgnoreCase("join") && a.length>1) queues.join(p,a[1]);
            else if (a[0].equalsIgnoreCase("leave")) { queues.leave(p.getUniqueId()); msg(p,"You left the queue."); }
            else if (a[0].equalsIgnoreCase("status") && a.length>1) msg(p,"Players waiting for " + a[1] + ": " + queues.count(a[1]));
            else msg(p,"Usage: /queue join <kit>, /queue leave or /queue status <kit>");
            return true;
        }
    }

    private final class MainCommand implements CommandExecutor {
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (a.length==0) { msg(s,"MCPvP commands: /kit, /arena, /queue, /stats, /mcpvp lobby"); return true; }
            if (a[0].equalsIgnoreCase("setlobby") && s instanceof Player p && s.hasPermission("mcpvp.admin")) {
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
                ItemStack icon = new ItemStack(Material.IRON_SWORD);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + name);
                meta.setLore(List.of(ChatColor.GRAY + "Click to queue for this kit"));
                icon.setItemMeta(meta);
                menu.addItem(icon);
            }
            player.openInventory(menu);
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
