# SoulPvP v0.7.13

A competitive Minecraft PvP system for **Paper 1.21.11**, built with **Java 21** and **Gradle**. Create custom kits, send players into fast matchmaking, challenge rivals, and run block-editable arenas that reset between matches.

## Current version

**SoulPvP v0.7.13**

Implemented:
- Main navigation GUI via `/menu` or `/mcpvp menu`.
- Queue GUI via `/queue`, with kit-specific item icons, queue counts and personal queue position.
- Kit icons automatically use the first non-empty item in the creator's inventory.
- Admins can customize icons with `/kit icon <name> <material>`.
- Inventory-based kit creation, listing, deletion and giving.
- Separate ranked and unranked same-kit 1v1 matchmaking, FFA arenas, duel requests and spectator mode.
- Match restoration, wins/losses and player statistics.
- 80 named arena preset records across 14 themes.
- Configurable tab list and sidebar scoreboard, including persistent role ranks.
- Rank management: Owner, Co-owner, Admin, SR mod, Mod, Creator, Booster and Player.
- Per-match shrinking world border starting at 200 blocks.
- Arena block placement and breaking inside configured regions, with arena restoration after matches.
- Arena theme assignment with `/arena theme <name> <theme>`.
- GitHub Actions builds and tag-triggered GitHub Releases with the JAR attached.
- Persistent ELO ratings with Bronze, Silver, Gold, Diamond and Master tiers.
- ELO updates only after ranked 1v1 matches; unranked matches do not change rating.
- Separate persistent killstreaks and match win streaks, with separate best records.
- `/settings` GUI with saved player visibility and scoreboard preferences.
- `/leave` is registered and removes the player from matchmaking.
- Recent match history via `/history`.
- Rematch requests with `/rematch` and `/rematch accept`.
- `/elo` and `/streak` player commands.
- Party management with invitations, leader transfer, member removal, party chat and configurable size limit.
- Persistent achievements for wins, kills, ELO milestones and killstreaks, with achievement points.

## GUI navigation

- `/menu` — opens the main menu.
- `/queue` — opens the kit selector. Each kit displays its configured icon, queue count and your position. Clicking a kit joins its queue; clicking the same kit again leaves.
- Main menu buttons provide shortcuts to the kit selector, personal statistics, wins leaderboard and arena list.

## Kit management

- `/kit create <name>` — saves your current inventory as a kit (admin).
- `/kit list` — lists saved kits.
- `/kit icon <name> <material>` — changes a kit's GUI icon (admin; use a Bukkit material name such as DIAMOND_SWORD).
- `/kit delete <name>` — deletes a kit (admin).
- `/kit give <player> <name>` — gives a saved kit to a player (admin).
- `/queue` — opens the queue GUI.
- `/queue join <kit>` — joins that kit's unranked matchmaking queue.
- `/queue ranked <kit>` or `/queue unranked <kit>` — joins the selected queue mode.
- Selecting a kit in `/queue` opens a Ranked / Unranked choice menu.
- `/leave` or `/queue leave` — leaves matchmaking.
- Clicking the same kit again in the queue GUI leaves that queue.
- `/queue status` — shows your current kit, mode, position and waiting count.
- `/queue status <kit>` — checks the number of players waiting for a kit.
- `/settings` — opens personal settings for player visibility and the scoreboard.
- `/ffa join <arena> <kit>` — join a configured free-for-all arena.
- `/ffa leave` — restore your inventory and return to your previous location.

## Achievements

- `/achievements` — view progress, unlocked achievements and total achievement points.
- Achievements unlock automatically for match wins, PvP kills, ELO milestones and killstreak milestones.
- Achievements are saved in `achievements.yml` and persist across restarts.

## Party commands

- `/party create` — create a party.
- `/party invite <player>` — invite an online player (leader only; invitations expire after 60 seconds).
- `/party accept` — accept an invitation.
- `/party info` — show members and the party leader.
- `/party leave` — leave your current party; leadership transfers to the next member if the leader leaves.
- `/party kick <player>` — remove a member (leader only).
- `/party disband` — disband the party (leader only).
- `/party split` — randomly divide the party into two balanced, separate parties, keeping the original leader in Team A; the original leader stays in Team A.
- `/party ffa <arena> <kit>` — send every online party member into the same FFA arena and kit (leader only). All members must be free of matches, queues and other FFA sessions.
- `/party chat <message>` — send a message to party members.
- Configure `party.max-size` in `config.yml` (default: 8).

Party splitting creates two separate party groups for organising teams; it does not automatically start a team match. Party FFA joins the existing shared FFA arena, so other players in that arena can also participate.

## Match commands

- `/duel <player>` — challenge a player.
- `/duel accept` / `/duel deny` — respond to a duel request.
- `/spec <player>` / `/spec leave` — spectate or leave spectator mode.
- `/match info` — view your opponent.
- `/match forfeit` — forfeit your current match.
- `/stats [player]` — view match statistics.
- `/elo` — view your rating and tier.
- `/streak` — view current and best killstreak.
- `/history` — view your five most recent match results.
- `/rematch` — request a rematch with your last opponent; `/rematch accept` accepts a request.
- `/stats top` or `/stats top kills` — view leaderboards.

## Arena setup

1. Build or import an arena map.
2. Optionally assign a theme with `/arena theme <name> <theme>`.
3. Set opposite corners using `/arena pos1 <name>` and `/arena pos2 <name>`.
4. Set player spawns with `/arena setspawn <name> a` and `/arena setspawn <name> b`.
5. Create kits and let players queue.

Only arenas with both spawn points and both region corners configured are eligible for 1v1 matchmaking. FFA requires `spawn-a` and both region corners. An arena is reserved while a match is active. The 80 preset records are not prebuilt world downloads; admins must build/import the actual maps.

### Arena restoration

Players can place and break blocks only inside the configured region during a match. The system captures the arena before the match and restores saved block states when the match ends, including after a death, forfeit or disconnect. Snapshots are held in memory, so a server crash during a match can interrupt restoration. Region volume is limited by `matches.max-arena-snapshot-blocks`.

### Match border

Default border settings:
- Starting diameter: 200 blocks
- Final diameter: 20 blocks
- Shrink duration: 300 seconds (5 minutes)

Tune `matches.border-start-size`, `matches.border-end-size` and `matches.border-shrink-seconds` in `config.yml`.

## Rank commands

- `/rank` — show your rank.
- `/rank <player>` — view a player's rank.
- `/rank set <player> <rank>` and `/rank add <player> <rank>` — assign a rank.
- `/rank remove <player>` — reset a player's rank to Player.

Rank assignments are stored by UUID in `ranks.yml`. Rank management requires `mcpvp.rank.manage` or `mcpvp.admin`.

## Build

Requirements: Java 21 and Gradle 8.14.2.

```bash
gradle clean build
```

The JAR is written to `build/libs/`.

## GitHub Releases

Pushing a version tag such as `v0.7.9` triggers the release workflow. You can also publish manually from GitHub Actions using **Publish SoulPvP Release → Run workflow**, entering the version without the `v` prefix. The workflow builds the plugin and publishes a GitHub Release titled `SoulPvP vX.Y.Z`, with generated release notes, `SoulPvP-vX.Y.Z.jar` and a SHA-256 checksum attached. Normal pushes to `main` build the plugin and upload a temporary JAR artifact.

## Queue controls

- `/queue` opens the kit selection GUI.
- `/leave` or `/queue leave` leaves your current queue.
- Clicking the same kit again in the queue GUI leaves that kit's queue.
- Clicking a different kit switches your queue to the newly selected kit.
- `/queue status` shows your current kit, queue position, and number of waiting players.
- `/kits` is not registered; use `/queue` instead.
