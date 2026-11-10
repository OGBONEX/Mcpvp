# MCPvP

A classic-inspired PvP minigame plugin for **Paper 1.21.11**, built with **Java 21** and **Gradle**.

## Current development status

Development version: `0.2.0-SNAPSHOT`

Implemented in this milestone:
- Create, list, delete and give inventory-based kits.
- Create arenas and save both player spawn points.
- Same-kit 1v1 matchmaking queues.
- Match start, death/quit win handling and inventory/location restoration.
- Persistent wins and losses.
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
| `/arena list` | List arenas | `mcpvp.admin` |
| `/queue join <kit>` | Queue for a same-kit match | Everyone |
| `/queue leave` | Leave matchmaking | Everyone |
| `/stats [player]` | View wins and losses | Everyone |
| `/mcpvp setlobby` | Set lobby at your location | `mcpvp.admin` |
| `/mcpvp lobby` | Return to configured lobby | Everyone |
| `/mcpvp reload` | Reload plugin configuration | `mcpvp.admin` |

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
