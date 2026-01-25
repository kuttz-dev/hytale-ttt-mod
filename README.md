# Trouble in Terrorist Town (TTT) Features

This mod is inspired by the original Trouble in Terrorist Town (TTT) multiplayer gamemode included with Garry’s Mod. It is a
social deduction game where players are assigned roles and must work together (or
against each other) to achieve their objectives. Below is a list of features included in this mod. The main idea was developed in the first week of Hytale, following the ECS design as much as possible.

## Core Features

### Roles

- Custom UI always on screen that shows the assigned role for the player.
- **Innocents**: Work together to identify and eliminate the traitors.
- **Traitors**: Secretly work to eliminate all innocents and the detective.
- **Detective**: A special innocent role with unique tools to investigate and identify traitors.
- **Spectator**: Only visible among themselves, can fly around the map, cannot deal or block damage.

- TODO: There should be separate chats between living and dead players.

### Weapons and Tools

- **Special Equipment**: Configurable store with special items for traitos and detectives. Items can be bought with
  credits assigned at the beginning of a round, or by retrieving them from a dead player.

### Rounds

- Players are assigned roles at the start of each round.
- Rounds end when either the traitors or innocents achieve their objectives.
- Event system to define the round phases:
    - Phases:
        - Waiting for players
            - Players are spawned, and can walk around the world.
            - When the required player amount is reunited the START_ROUND event is triggered.
        - Playing
            - At the beginning assign player roles
                - role ratios are configurable
            - On player death:
                - If it was a traitor and there are no remaining traitors, innocents win
                - If it was an innocent and there are no remaining innocents, traitors win
                - Adjust killer karma based on rules and trigger a karma check
            - Both cases trigger FINISH_ROUND event.
            - All players that die here cannot respawn, should vanish into spectator mode
        - Aftermatch
            - Configurable time that lets players rest until next round. Useful for voting maps.

### Karma System

- Karma tracks player behavior:
    - Killing teammates reduces karma.
    - Killing opponents increases karma.
- Karma starts with N points, and each time:
    - An innocent or detective kills a traitor: karma gets incremented U points
    - A traitor kills an innocent: karma gets incremented V points
    - An innocent kills an innocent: karma gets reduced W points
    - An innocent kills a detective: karma gets reduced X points
    - A detective kills an innocent: karma gets reduced Y points
    - A traitor kills a traitor: karma gets reduced Z points
- N, U, V, W, X, Y, Z should be configurable values
- TODO: If karma gets below an A amount the player is banned for T amount of time
    - A, T configurable

### Scoreboard

- Displays player stats:
    - Role (revealed after death)
    - Kills and deaths
    - Karma
- Shows dead players only when their deaths have been confirmed.
- Shows traitor roles only when being a traitor for alive players.

### Graves System

- When a player dies, they leave behind a grave for the round that contains information about the dead player, giving the option to confirm their death.

### Translations

- Every shown text uses lang files.
- English and Spanish supported by default.

