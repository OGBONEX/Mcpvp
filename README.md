# MCPvP

A classic-inspired Minecraft PvP plugin for **Paper 1.21.11**, built with **Java 21** and **Gradle**.

## Project status

The project is currently in early development. The initial repository scaffold is in place; gameplay systems are not yet implemented.

## Planned systems

- Custom kits and kit abilities
- Arenas and match management
- Player statistics
- Queue and matchmaking systems
- Additional classic MCPvP-style features

## Build

Requirements:
- Java 21
- Gradle 8.10 or newer

Run:

```bash
gradle clean build
```

The plugin JAR will be created in `build/libs/`.

## Versioning

MCPvP follows Semantic Versioning. Development builds use a `0.x` version until the first stable release:

- `1.0.0`: first stable release
- `1.0.1`: first patch release
- `1.1.0`: first backwards-compatible feature release
- `2.0.0`: a major release with possible compatibility changes

See [CHANGELOG.md](CHANGELOG.md) for release history.
