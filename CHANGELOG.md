# Changelog

All notable MCPvP changes are documented here.

## [0.7.1] - Unreleased

### Added
- Arena region selection using `/arena pos1 <name>` and `/arena pos2 <name>`.
- Block placement and breaking inside match arena regions.
- Automatic arena block-data regeneration after matches.
- Arena reservation prevents concurrent matches from editing the same arena.
- Configurable arena snapshot size limit.
- Match world border starts at 200 blocks and shrinks to 20 blocks over 5 minutes by default.

### Fixed
- Arena selection now requires both spawn points and both region corners.
- Match completion restores the arena after death, forfeit, and disconnect.

### Versioning
- Patch release sequence: 0.7.0 → 0.7.1 → 0.7.2. Minor version 0.8.0 is not used for this patch update.


## 0.7.3-SNAPSHOT
- Added a main navigation GUI at `/menu` and `/mcpvp menu`.
- Added kit-specific GUI icons, automatic icon selection, configurable icon materials and queue-count lore.
- Refreshed plugin description and README branding to MCPvP — The Arena.
