# AGENTS.md

## Big Picture
- This repo is a server-side Hytale Java plugin for Trouble in Trork Town; the plugin entrypoint is [`src/main/java/ar/ncode/plugin/TroubleInTrorkTownPlugin.java`](src/main/java/ar/ncode/plugin/TroubleInTrorkTownPlugin.java).
- Bootstrap does most of the wiring in one place: it loads configs, scans map templates, registers ECS components, event listeners, commands, UI interactions, packet filters, and the ByteBuddy explosion patch.
- Runtime state is mostly global and keyed by world UUID: `gameModeStateForWorld`, `mapTemplateConfig`, `instanceConfigs`, and `currentInstance` live on `TroubleInTrorkTownPlugin`. Changes in one flow often require checking these maps too.
- Round lifecycle is event-driven: `PlayerReadyEventListener` can dispatch `StartNewRoundEvent`, `StartNewRoundEventHandler` calls `GameModeSystem.doBeforeRound/doAtRoundStart`, `WorldRoundTimeSystem` and `PlayerDeathSystem` can dispatch `FinishCurrentRoundEvent`, and `FinishCurrentMapEventHandler` handles voting + map rotation.
- Gameplay features usually span Java and asset JSON together. Example: corpse / gravestone behavior touches `PlayerDeathSystem`, `DeathSystem`, UI pages, and resources under `src/main/resources/Server/...`.

## Data And Asset Boundaries
- Global gamemode config is `CustomConfig`; weapon/category loot config is `WeaponsConfig`; per-map config is `InstanceConfig`.
- Map templates are expected under the plugin data dir `maps/<safe_name>/` with `config.json`, `instance.bson`, `preview.png`, and `chunks/`. The scaffold lives in [`src/main/resources/templates/map/`](src/main/resources/templates/map/).
- `WorldPreviewLoader` copies map templates into a generated asset pack under the plugin data dir so previews and instances become loadable assets. If map loading changes, update both template handling and asset-copy behavior.
- Checked-in runtime examples live under `run/mods/ncode_ttt/`; use them to understand actual config shape and map folder layout.

## Project Conventions
- Prefer world-thread mutations. Many command handlers are `AbstractAsyncCommand` wrappers that still call `world.execute(...)` before touching entities, inventories, or world config.
- Player-centric state is stored in ECS components, especially `PlayerGameModeInfo`; helper aggregation is wrapped in `PlayerComponents`.
- Safe map/world names are normalized with `WorldAccessors.getSafeWorldName()` (`"My Map"` -> `my_map`). Reuse that instead of inventing path logic.
- Role inventories and shop entries use project-specific string encoding from `CustomConfig.parseItemEntry`: `"ItemId:Amount"` or grouped slot bundles like `"ItemA:1|ItemB:2"`.
- Permission groups are assigned at runtime in `PlayerReadyEventListener` and `TroubleInTrorkTownPlugin.start()`, not only by command declarations. `ttt.map.save` exists but is not part of the default admin group.
- UI pages are custom Hytale pages, not web UIs. Follow patterns in `ui/pages/ShopPage.java`, `MapVotePage.java`, and `ScoreBoardPage.java`.

## Workflows
- Build/test with the wrapper: `bash ./gradlew build` or `bash ./gradlew test`.
- First wrapper run may need network access to download Gradle; local non-CI builds also require a Hytale install because `build.gradle` points at `HytaleServer.jar` under `hytaleHome`.
- CI behaves differently: when `CI` is set it resolves `com.hypixel.hytale:Server:${hytaleServerVersion}` from Hytale Maven instead of a local jar.
- `build.gradle` generates an IntelliJ `HytaleServer` run configuration; the README mentions `runServer`, but that task is not defined in the build script.
- Use `updateHytaleServerVersion` before releases if `gradle.properties` is stale; `processResources` also rewrites `src/main/resources/manifest.json`.
- Server logs and live data are under `run/` (`run/logs/`, `run/mods/ncode_ttt/`, `run/universe/`), which is useful when debugging map loading or round state issues.

## High-Value Files
- `src/main/java/ar/ncode/plugin/ecs/system/GameModeSystem.java`: round setup/cleanup, role assignment, KDA/karma updates.
- `src/main/java/ar/ncode/plugin/ecs/system/player/PlayerDeathSystem.java`: death processing, remains spawning, spectator transition, win-condition trigger.
- `src/main/java/ar/ncode/plugin/ecs/system/event/handler/FinishCurrentMapEventHandler.java`: map voting and world change timing.
- `src/main/java/ar/ncode/plugin/accessors/WorldAccessors.java`: safe-name rules, world/player lookup helpers, instance-config persistence.
- `src/main/java/ar/ncode/plugin/patches/CancelExplosionInteraction.java`: runtime patching hook; keep this in mind before changing explosion behavior.
- `src/test/java/ar/ncode/plugin/ui/pages/ScoreBoardPageTest.java`: current testing style is focused unit tests with Mockito and some reflective static setup.
