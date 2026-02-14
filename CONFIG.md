# Trouble in Trork Town (TTT) - Installation Guide

This guide covers everything you need to install, configure, and run the TTT mod for Hytale.

---

## Requirements

- **Hytale** installed via the official launcher
- **Java 25** (bundled with Hytale, or install separately)
- **Gradle** (optional - wrapper included)

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
version=0.2.0
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

The JAR will be in `build/out/ttt-0.2.0.jar`

### 4. Install the JAR

Copy the built JAR to your Mods folder:

```bash
# Windows
copy build\out\ttt-*.jar "%APPDATA%\Hytale\UserData\Mods\"

# Linux
cp build/out/ttt-*.jar ~/.local/share/Hytale/UserData/Mods/
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

TTT generates a config file on first run. Edit it at:

```
<Server>/config/ncode/ttt/config.json
```

### Full Configuration Reference

```json
{
  "InnocentColor": "#33CC76",
  "TraitorColor": "#B01515",
  "RequiredPlayersToStartRound": 3,
  "MinAmountOfTraitors": 1,
  "TraitorsRatio": 4,
  "MinAmountOfDetectives": 0,
  "DetectivesRatio": 11,
  "MaxDetectives": 10,
  "TimeBeforeRoundInSeconds": 10,
  "RoundDurationInSeconds": 600,
  "TimeAfterRoundInSeconds": 5,
  "TimeToVoteMapInSeconds": 30,
  "TimeBeforeChangingMapInSeconds": 5,
  "KarmaStartingValue": 1000,
  "KarmaForDisconnectingMiddleRound": -100,
  "KaramPointsForTraitorKillingInnocent": 10,
  "KaramPointsForTraitorKillingDetective": 20,
  "KaramPointsForTraitorKillingTraitor": -100,
  "KarmaPointsForInnocentKillingTraitor": 10,
  "KarmaPointsForInnocentKillingDetective": -100,
  "KarmaPointsForInnocentKillingInnocent": -50,
  "KarmaPointsForDetectiveKillingTraitor": 10,
  "KarmaPointsForDetectiveKillingInnocent": -50,
  "KarmaPointsForDetectiveKillingDetective": -100,
  "StartingItemsInHotbar": [
    "Weapon_Shortbow_Combat:1"
  ],
  "StartingItemsInInventory": [
    "Weapon_Arrow_Crude:200"
  ],
  "TraitorStoreItems": [
    "Weapon_Daggers_Doomed:1"
  ],
  "DetectiveStoreItems": [
    "Weapon_Staff_Frost:1",
    "Weapon_Deployable_Healing_Totem:1"
  ],
  "PlayerGraveId": "Player_Grave",
  "LootBoxBlockId": "Furniture_Human_Ruins_Chest_Small",
  "RoundsPerMap": 8,
  "MapsInARowForVoting": 3,
  "WorldTemplatesFolder": "universe/templates"
}
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
"StartingItemsInHotbar": [
"Weapon_Shortbow_Combat:1",
"Consumable_Potion_Health:3"
]
```

---

## Custom Maps

TTT supports custom map templates for variety. Maps are loaded from the `WorldTemplatesFolder`.

### Map Structure

```
universe/templates/
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
    },
    {
      "x": 110,
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

## Commands

### Player Commands

| Command         | Description                            |
|-----------------|----------------------------------------|
| `/ttt shop`     | Open the role-specific equipment store |
| `/ttt map vote` | Open the map vote GUI                  |

### Admin Commands

| Command                | Description            |
|------------------------|------------------------|
| `/ttt`                 | Main TTT admin command |
| `/ttt spawn add`       | Add player spawn point |
| `/ttt spawn show`      | Visualize spawn points |
| `/ttt loot spawn add`  | Add loot spawn point   |
| `/ttt loot spawn show` | Visualize loot spawns  |
| `/ttt loot force`      | Force spawn loot       |
| `/ttt map finish`      | End current map        |
| `/ttt debug`           | Debug information      |
| `/ttt debug memory`    | Memory usage stats     |

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
2. Check Java version: `java -version` (needs 25)
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
