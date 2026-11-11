# Changelog

## v0.7.9
- Added free-for-all sessions with `/ffa join <arena> <kit>` and `/ffa leave`.
- FFA restores the player's saved inventory, armor, location and game settings when they leave.
- FFA players keep their kit on respawn and return to the FFA spawn.
- FFA sessions end safely on disconnect and restore the player's snapshot when they next join.

## v0.7.8
- Added separate Ranked and Unranked matchmaking queues for each kit.
- Added a queue-mode choice GUI after selecting a kit.
- Added `/queue ranked <kit>` and `/queue unranked <kit>`.
- ELO is now changed only by Ranked matches; Unranked matches leave rating unchanged.
- Rematches preserve the previous match's queue mode.
- Fixed queue matching so players are not silently removed if no arena is available.

## v0.7.5
- Replaced the player-facing `/kits` command with `/queue`, which opens the kit-selection GUI.
- Removed the `/kits` command registration; kit administration through `/kit create`, `/kit delete` and `/kit icon` remains available.
- Updated the main menu to label the kit selector as Queue.
- Set the displayed and build version to MCPvP v0.7.5.

## v0.7.4
- Added `/leave` (alias `/qleave`) to leave matchmaking.
- Clicking the currently queued kit in the queue GUI now toggles queue exit.
- Selecting another kit switches queues.
- Added personal queue position and current queue details to `/queue status`.
- Improved kit selector lore with live queue state and position.

## v0.7.3
- Added a main navigation GUI at `/menu` and `/mcpvp menu`.
- Added kit-specific GUI icons, automatic icon selection, configurable icon materials and queue-count lore.

## v0.7.1
- Added arena region selection using `/arena pos1 <name>` and `/arena pos2 <name>`.
- Added block placement and breaking inside match arena regions.
- Added automatic arena block-data regeneration after matches.
- Added arena reservation to prevent concurrent matches from editing the same arena.
- Added a configurable arena snapshot size limit.
- Added a match world border that starts at 200 blocks and shrinks to 20 blocks over 5 minutes by default.
- Fixed arena selection to require both spawn points and both region corners.
- Fixed match completion restoration after death, forfeit and disconnect.

Patch version sequence: 0.7.0 → 0.7.1 → 0.7.2 → 0.7.3 → 0.7.4 → 0.7.5.
