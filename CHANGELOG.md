# Changelog

All notable MCPvP changes are documented here.

## [Unreleased]

### Added
- Persistent role ranks: Owner, Co-owner, Admin, SR mod, Mod, Creator, Booster and Player.
- `/rank set`, `/rank add` and `/rank remove` administration commands.
- Configurable rank prefixes in the tab list and scoreboard.
- Arena theme assignment for Classic, Desert, Nether, Snow, Sky, Nature, Stone, Water, Medieval, Industrial, Fantasy, Town, End and Modern.
- Shrinking per-player match world border.
- Tag-triggered GitHub Release workflow that attaches the built plugin JAR.

### Changed
- Removed win-threshold ranks; player ranks are now assigned and persisted by UUID.
- Advanced development version to 0.7.0-SNAPSHOT.

### Notes
- Arena themes describe the intended block palette/style. They do not auto-generate or repaint physical maps.
- Existing arena records and spawn locations are retained. Existing win-based rank tiers are no longer used.
