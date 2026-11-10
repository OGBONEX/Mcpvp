# MCPvP

A classic-inspired PvP minigame plugin for **Paper 1.21.11**, built with **Java 21** and **Gradle**.

## Current development status

Development version: `0.6.0-SNAPSHOT`

Implemented in this milestone:
- Create, list, delete and give inventory-based kits.
- Create arenas and save both player spawn points.
- Same-kit 1v1 matchmaking queues.
- Match start, death/quit win handling and inventory/location restoration.
- Persistent wins and losses and a top-wins leaderboard.
- Duel requests with accept/deny and a 30-second expiry.
- Spectator mode for active 1v1 matches.
- Interactive kit selector GUI that queues players when they click a kit.
- Match status and forfeit commands.
- Arena deletion command.
- Queue status lookup by kit.
- 80 named arena preset slots across multiple themes; administrators must build/import maps and set both spawn points.
- Configurable tab header/footer and rank prefixes.
- Configurable sidebar scoreboard.
- Win-based ranks: Rookie, Fighter, Warrior, Elite, Champion and Legend.
- `/rank [online-player]` displays a player's rank and win count.
- Player kill/death statistics and top-kills leaderboard.
- Lobby location and reload commands.
- YAML configuration and data files.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/kit create <name>` | Save your current inventory as a kit | `mcpvp.admin` |
| `/kit list` | List available kits | Everyone |
| `/kit delete <name>` | Delete a kit | `mcpvp.admin` |
| `/kit give <player> <name>` | Give a kit to an online player | `mcpvp.admin` |
| `/arena create <name>` | Create an arena | `mcpvp.admin` |
| `/arena setspawn <name> <a|b>` | Set arena spawn points | `mcpvp.admin` |
| `/arena delete <name>` | Delete an arena | `mcpvp.admin` |
| `/arena list` | List arenas | `mcpvp.admin` |
| `/arena presets` | Show available arena preset slots | Everyone |
| `/rank [player]` | View a player's win-based rank | Everyone |
| `/queue join <kit>` | Queue for a same-kit match | Everyone |
| `/queue leave` | Leave matchmaking | Everyone |
| `/queue status <kit>` | See how many players are waiting for a kit | Everyone |
| `/stats [player]` | View wins and losses | Everyone |
| `/stats top` | Show top 10 by wins | Everyone |
| `/stats top kills` | Show top 10 by kills | Everyone |
| `/duel <player>` | Send a duel request | Everyone |
| `/duel accept` | Accept a duel request | Everyone |
| `/duel deny` | Deny a duel request | Everyone |
| `/spec <player>` | Spectate an active match | Everyone |
| `/spec leave` | Leave spectator mode | Everyone |
| `/kits` | Open kit selector and queue GUI | Everyone |
| `/match info` | View current opponent | Everyone |
| `/match forfeit` | Forfeit current match | Everyone |
| `/mcpvp setlobby` | Set lobby at your location | `mcpvp.admin` |
| `/mcpvp lobby` | Return to configured lobby | Everyone |
| `/mcpvp reload` | Reload plugin configuration | `mcpvp.admin` |

## Duel setup

Duel requests use the first available kit by default. Set `duels.default-kit` in `config.yml` to choose a specific kit. The selected kit must exist, and at least one arena must have both spawn points configured.

## Arena setup

1. Run `/arena create arena1`.
2. Stand at the first player's spawn and run `/arena setspawn arena1 a`.
3. Stand at the second player's spawn and run `/arena setspawn arena1 b`.
4. Create kits with `/kit create <name>`.
5. Players can queue with `/queue join <name>`.

## Build

Requirements: Java 21 and Gradle 8.10+.

```bash
gradle clean build
```

The JAR is written to `build/libs/`.

## Versioning

MCPvP follows Semantic Versioning. Development versions use `0.x` until the first tested stable release. See [CHANGELOG.md](CHANGELOG.md).

## Arena presets

The plugin seeds 80 named arena slots from `arena-presets.yml`. These are preset records, not prebuilt world downloads. For each map, build or import the physical arena, then set both spawns using `/arena setspawn <name> a` and `/arena setspawn <name> b`. Only arenas with both spawn points configured are eligible for matchmaking.

## Tab, scoreboard and ranks

The tab header/footer, sidebar scoreboard and rank tiers are configurable in `config.yml`. Default ranks are Rookie (0 wins), Fighter (10), Warrior (25), Elite (50), Champion (100) and Legend (250). Rank prefixes appear next to player names in the tab list.
