# Changelog

All notable MCPvP changes are documented here.

This project follows Semantic Versioning:
- PATCH (1.0.x): backwards-compatible bug fixes.
- MINOR (1.x.0): backwards-compatible features.
- MAJOR (x.0.0): major changes that may break compatibility.

## [Unreleased]

### Added
- Player-to-player duel requests with accept/deny and automatic expiry.
- Spectator mode for active matches with return location and game mode restoration.
- Top 10 wins leaderboard through `/stats top`.
- Lobby deaths retain vanilla drop behavior; match deaths use match restoration.

### Added
- Inventory-based kit creation, listing, deletion and kit giving.
- Arena creation and configurable player spawn points.
- Same-kit 1v1 matchmaking queues.
- Match start and end handling for player deaths and disconnects.
- Player inventory, armor, offhand, location and basic status restoration after matches.
- Persistent player win/loss statistics.
- Lobby setup and return commands.
- Command permissions and updated project documentation.

### Development
- Version advanced to 0.2.0-SNAPSHOT.
- Gameplay systems remain under active development; this is not yet the 1.0.0 stable release.
