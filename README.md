# MCPvP

A classic-inspired PvP minigame plugin for **Paper 1.21.11**, built with **Java 21** and **Gradle**.

## Current development status

Development version: `0.7.0-SNAPSHOT`

Implemented:
- Inventory-based kit creation, listing, deletion and giving.
- Same-kit 1v1 matchmaking, duel requests and spectator mode.
- Match restoration, wins/losses and player statistics.
- 80 named arena preset slots across 14 themes.
- Configurable tab list and sidebar scoreboard.
- Persistent role ranks: Owner, Co-owner, Admin, SR mod, Mod, Creator, Booster and Player.
- Rank management with `/rank set`, `/rank add` and `/rank remove`.
- Shrinking per-player match world border to bring opponents toward the center of the arena.
- Arena theme assignment with `/arena theme <name> <theme>`.
- GitHub Actions build and tag-triggered GitHub Releases with the plugin JAR attached.

## Rank commands

- `/rank` — show your rank.
- `/rank <player>` — view a player's rank.
- `/rank set <player> <rank>` — assign a rank.
- `/rank add <player> <rank>` — assign a rank (alias-style command).
- `/rank remove <player>` — reset a player's rank to Player.

Valid ranks: `Owner`, `Co-owner`, `Admin`, `SR mod`, `Mod`, `Creator`, `Booster`, `Player`.

Rank assignments are stored by UUID in `ranks.yml`. The rank management commands require `mcpvp.rank.manage` or `mcpvp.admin`.

## Arena themes

Available themes: Classic, Desert, Nether, Snow, Sky, Nature, Stone, Water, Medieval, Industrial, Fantasy, Town, End and Modern.

Set a theme with `/arena theme <name> <theme>`. For example:
```
/arena theme NetherPit Nether
/arena theme DesertTemple Desert
```

Themes label the intended visual style of the arena; they do not automatically build or repaint the map. Administrators must build/import the physical arena using the selected theme, then set both spawn points. The 80 preset records are not prebuilt world downloads.

## Arena setup

1. Build or import the arena map.
2. Set the theme with `/arena theme <name> <theme>`.
3. Stand at player A's spawn and run `/arena setspawn <name> a`.
4. Stand at player B's spawn and run `/arena setspawn <name> b`.
5. Create kits with `/kit create <name>`.
6. Players can queue with `/queue join <name>`.

Only arenas with both spawn points configured are eligible for matchmaking.

## Match border

The match border is applied individually to both match participants. Defaults in `config.yml`:
- Starting diameter: 80 blocks
- Final diameter: 18 blocks
- Shrink duration: 90 seconds

Adjust `matches.border-start-size`, `matches.border-end-size` and `matches.border-shrink-seconds` to tune it.

## Main commands

| Command | Description |
|---|---|
| `/kit create <name>` | Save your inventory as a kit |
| `/kit list` | List kits |
| `/arena create <name>` | Create an arena record |
| `/arena theme <name> <theme>` | Assign an arena theme |
| `/arena setspawn <name> <a|b>` | Set arena spawns |
| `/arena presets` | Show available arena preset slots |
| `/rank <player>` | View a player's rank |
| `/rank set <player> <rank>` | Set a player's rank |
| `/rank add <player> <rank>` | Assign a rank |
| `/rank remove <player>` | Reset to Player |
| `/queue join <kit>` | Queue for a match |
| `/queue leave` | Leave matchmaking |
| `/duel <player>` | Challenge a player |
| `/kits` | Open the kit selector |
| `/match info` | View current opponent |
| `/match forfeit` | Forfeit current match |
| `/stats [player]` | View statistics |
| `/mcpvp setlobby` | Set the lobby |
| `/mcpvp lobby` | Return to lobby |
| `/mcpvp reload` | Reload configuration |

## Build

Requirements: Java 21 and Gradle 8.14.2.

```bash
gradle clean build
```

The plugin JAR is written to `build/libs/`.

## GitHub Releases

Pushing a version tag such as `v0.7.0` triggers the release workflow. It builds the plugin and publishes a GitHub Release with generated release notes and the compiled JAR attached. Releases are published from tags; normal pushes to `main` only run the build workflow.
