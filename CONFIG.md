# Trouble in Trork Town (TTT) - Installation Guide

This guide covers everything you need to install, configure, and run the TTT mod for Hytale.

---

## Requirements

- **Hytale** installed via the official launcher
- **Java 25** (bundled with Hytale, or install separately)
- **Gradle** (optional—wrapper included)

---

## Quick Install (Pre-built JAR)

1. Download the latest release from [GitHub Releases](https://github.com/kuttz-dev/hytale-ttt-mod/releases)
2. Copy `ttt-x.x.x.jar` to your Hytale Mods folder:
   ```
   Windows: %APPDATA%/Hytale/UserData/Mods/
   Linux:   ~/.local/share/Hytale/UserData/Mods/
   macOS:   ~/Library/Application Support/Hytale/UserData/Mods/
   ```
3. Start Hytale and create/join a server

---

## Build from Source

### 1. Clone the Repository

```bash
git clone https://github.com/kuttz-dev/hytale-ttt-mod.git
cd hytale-ttt-mod
```

### 2. Configure gradle.properties

Edit `gradle.properties` if needed:

```properties
# Plugin version
version=0.5.0
# Java version (Hytale uses 25)
java_version=25
# Release channel: "release" or "pre-release"
patchline=release
# Load mods from user's Mods folder during development
load_user_mods=true
```

### 3. Build the JAR

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

The JAR will be in `build/libs/ttt-X.X.X.jar`

### 4. Install the JAR

Copy the built JAR to your Mods folder:

```bash
# Windows
copy build\libs\ttt-*.jar "%APPDATA%\Hytale\UserData\Mods\"

# Linux
cp build/libs/ttt-*.jar ~/.local/share/Hytale/UserData/Mods/
```

---

## Running a Development Server

The mod includes a run configuration for testing:

```bash
# Start a local development server
./gradlew runServer
```

This creates a `run/` directory with server files.

---

## Configuration

TTT generates two config files on the first run. Edit it at:

```
<Server>/config/ncode/ttt/config.json
<Server>/config/ncode/ttt/weapons_config.json
```

### Configuration Options Explained

| Option                        | Default | Description                      |
|-------------------------------|---------|----------------------------------|
| `RequiredPlayersToStartRound` | 3       | Minimum players to start a round |
| `TraitorsRatio`               | 4       | 1 traitor per N players          |
| `DetectivesRatio`             | 11      | 1 detective per N players        |
| `RoundDurationInSeconds`      | 600     | Round length (10 minutes)        |
| `KarmaStartingValue`          | 1000    | Starting karma for new players   |
| `RoundsPerMap`                | 8       | Rounds before map vote           |

### Item Format

Items use the format `ItemId:Amount` or `ItemId:Amount|ItemId:Amount` for bundles:

```json
{
  "StartingItemsInHotbar": [
    "Weapon_Shortbow_Combat:1",
    "Consumable_Potion_Health:3"
  ]
}
```

---

## Custom Maps

TTT supports custom map templates for variety. Maps are loaded from the folder `mods/ncode_ttt/maps`.

### Map Structure

```
mods/ncode_ttt/maps
├── map_mansion/
│   ├── chunks/
│       ├── 0.0.region.bin
│       ├── ...
│   ├── preview.png          (for voting UI)
│   └── config.json          (spawn points, loot spawns)
├── map_office/
│   └── ...
└── map_warehouse/
    └── ...
```

### Map Config (config.json)

Each map can have its own spawn point and loot configuration:

```json
{
  "spawnPoints": [
    {
      "x": 100,
      "y": 64,
      "z": 100
    },
    {
      "x": 105,
      "y": 64,
      "z": 100
    }
  ],
  "lootSpawnPoints": [
    {
      "x": 150,
      "y": 64,
      "z": 200,
      "table": "weapons"
    }
  ]
}
```

### Adding Spawn Points In-Game

Use admin commands to define spawn points:

```
/ttt spawn add           - Add spawn at current position
/ttt spawn show          - Show all spawn points
/ttt loot spawn add      - Add weapon loot spawn at current position
/ttt loot spawn show     - Show all loot spawn points
```

---

## Commands and Permissions (auto-generated from source)

Below is an updated list of commands and the permission nodes added in the codebase. If you change commands in code,
re-run this extraction to keep docs in sync.

### Player commands

| Command / Alias                         | Usage / Notes                                                    | Permission node          |
|-----------------------------------------|------------------------------------------------------------------|--------------------------|
| `/ttt shop` (aliases: `/store`, `/buy`) | Open role-specific equipment store (traitor/detective only)      | `ttt.shop.open`          |
| `/ttt map vote` (alias: `votemap`)      | Open the map vote GUI (only available after last round finished) | `ttt.map.vote`           |
| `/t`                                    | Traitors-only chat shortcut (sends message to alive traitors)    | none (role-checked)      |
| `/spectator [target]`                   | Toggle spectator mode for a component (or for target)            | none (usable by players) |
| `/change-world <template>`              | Debug: load a new world instance and teleport players            | none (debug command)     |

Notes: many `ttt` subcommands live under `/ttt` and inherit the `TTT_USER_GROUP` group by default; specific subcommands
may require explicit permission nodes (see admin commands below).

### Admin commands

These commands require admin permission nodes (either via `requirePermission(...)` or by being in the
`TTT_ADMIN_GROUP`):

| Command                              | Description                                   | Permission node   |
|--------------------------------------|-----------------------------------------------|-------------------|
| `/ttt role set <role> [target]`      | Force-set a player's role                     | `ttt.role.set`    |
| `/ttt credits set <amount> [target]` | Set a player's credits                        | `ttt.credits.set` |
| `/ttt map finish` (alias: `end`)     | Force finish current map                      | `ttt.map.finish`  |
| `/ttt map create <name>`             | Create a new map template (copies templates)  | `ttt.map.crud`    |
| `/ttt map read/list`                 | List or show map details                      | `ttt.map.crud`    |
| `/ttt map update <old> <new>`        | Rename a map and update config                | `ttt.map.crud`    |
| `/ttt map delete <name> confirm`     | Delete a map (requires explicit confirmation) | `ttt.map.crud`    |

Other admin/management commands (no explicit node but grouped under admin group):

- `/ttt debug ...` — Debug commands (get-position, memory, info) — available to users (debug)
- `/ttt spawn add` — Add player spawn point (writes to map config) — inherits `ttt.groups.user`
- `/ttt spawn show` — Show spawn points — inherits `ttt.groups.user`
- `/ttt loot spawn add` — Add loot spawn point — inherits `ttt.groups.user`
- `/ttt loot spawn show` — Show loot spawn points — inherits `ttt.groups.user`
- `/ttt loot force` — Force spawn loot in current world — inherits `ttt.groups.user`

### Permission nodes (defined in source)

- `ttt.map.vote`       — Open map vote GUI
- `ttt.map.finish`     — Force finish current map
- `ttt.map.crud`       — Create/read/update/delete maps
- `ttt.shop.open`      — Open the traitor/detective shop
- `ttt.role.set`       — Set a player's role (admin)
- `ttt.credits.set`    — Set a player's credits (admin)

Permission groups (used when registering permissions at plugin start):

- `ttt.groups.user`   — TTT user group (default for `/ttt` collection)
- `ttt.groups.admin`  — TTT admin group (admin-level commands)

Reference: constants are declared at `src/main/java/ar/ncode/plugin/model/CustomPermissions.java` and assigned to groups
at plugin startup in `TroubleInTrorkTownPlugin.java`.

---

## Localization

TTT supports multiple languages. Language files are in:

```
src/main/resources/Server/Languages/
├── en-US/ncodeTTT.lang
└── es-AR/ncodeTTT.lang
```

### Adding a New Language

1. Copy `en-US/ncodeTTT.lang` to your language folder (e.g., `nl-NL/`)
2. Translate the values
3. Rebuild and install

---

## Troubleshooting

### Mod doesn't load

1. Check Hytale version matches `patchline` in gradle.properties
2. Verify JAR is in correct Mods folder
3. Check server logs for errors

### Config not generating

1. Ensure server has write permissions to config folder
2. Check for JSON syntax errors in existing config

### Nothing happens in game

1. Verify `RequiredPlayersToStartRound` setting
2. Check if server is running in correct mode

### Build fails

1. Ensure Hytale is installed (needs HytaleServer.jar)
2. Check the Java version: `java -version` (needs 25)
3. Try `./gradlew clean build`

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

**Repositories:**

- Original: https://github.com/kuttz-dev/hytale-ttt-mod

---

## License

This mod is open-source. See the repository for license details.

---

## Credits

- **josephkm** - Original author
- **ncode.ar** - Development team
- Inspired by Trouble in Terrorist Town (Garry's Mod) by Bad King Urgrain
- Thanks for all the help HageneeZ - First contributor
