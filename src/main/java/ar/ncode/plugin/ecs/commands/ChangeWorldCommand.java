package ar.ncode.plugin.ecs.commands;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import ar.ncode.plugin.ecs.system.scheduled.DoubleTapDetector;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.patches.server.core.universe.world.spawn.CustomSpawnProvider;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.FitToHeightMapSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ChangeWorldCommand extends CommandBase {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    RequiredArg<String> targetWorld = this.withRequiredArg("targetWorld", "Target world name", ArgTypes.STRING);

    public ChangeWorldCommand() {
        super("change-world", "Debug command to change the current world.");
    }

    /**
     * Loads a new world instance and teleports all players from the current world to it.
     * Note: This method does NOT check the transition flag - callers should use
     * scheduleWorldTransition() from PlayerReadyEventListener to ensure proper timing.
     *
     * @param currentWorld The world to teleport players from
     * @param newWorldName The name of the world template to spawn
     */
    public static void loadInstance(World currentWorld, String newWorldName) {
        currentWorld.execute(() -> {
            // Store old world info before spawning new instance
            String oldWorldName = currentWorld.getName();
            UUID oldWorldUuid = currentWorld.getWorldConfig().getUuid();

            loadNewWorld(currentWorld, newWorldName);
            cleanUpOldWorld(oldWorldUuid);

            // Schedule direct world removal after teleports complete (2 seconds delay)
            // Using Universe.removeWorld() directly instead of safeRemoveInstance() because
            // safeRemoveInstance() silently fails if the world has no InstanceWorldConfig
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                try {
                    // Don't remove the default world!
                    String defaultWorldName = HytaleServer.get().getConfig().getDefaults().getWorld();
                    if (oldWorldName.equalsIgnoreCase(defaultWorldName)) {
                        LOGGER.atInfo().log("Skipping removal of default world: " + oldWorldName);
                        return;
                    }

                    World oldWorld = Universe.get().getWorld(oldWorldName);
                    if (oldWorld != null && oldWorld.getPlayerCount() == 0) {
                        LOGGER.atInfo().log("Removing old world instance: " + oldWorldName);
                        Universe.get().removeWorld(oldWorldName);
                    } else if (oldWorld != null) {
                        LOGGER.atWarning().log("Cannot remove world %s - still has %d players",
                                oldWorldName, oldWorld.getPlayerCount());
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log("Failed to remove old world: " + oldWorldName);
                }
            }, 3, TimeUnit.SECONDS);
        });
    }

    public static void loadNewWorld(World currentWorld, String mapTemplateName) {
        World targetWorld = createNewInstance(currentWorld, mapTemplateName);
        currentWorld.execute(() -> {
            teleportPlayersToNewWorld(currentWorld, targetWorld);
        });
    }

    private static void teleportPlayersToNewWorld(World currentWorld, World targetWorld) {
        for (PlayerRef playerRef : currentWorld.getPlayerRefs()) {
            Ref<EntityStore> ref = playerRef.getReference();

            if (ref == null || !ref.isValid()) {
                continue;
            }

            PlayerGameModeInfo playerInfo = ref.getStore().getComponent(ref, PlayerGameModeInfo.componentType);

            if (playerInfo != null) {
                playerInfo.setWorldInstance(targetWorld.getWorldConfig().getUuid());
            }

            InstancesPlugin.teleportPlayerToInstance(
                    ref,
                    ref.getStore(),
                    targetWorld,
                    null
            );
        }
    }

    public static void cleanUpOldWorld(UUID oldWorldUuid) {
        // Clear all component states to prevent memory leak when changing instances
        DoubleTapDetector.getInstance().clearAllPlayers();

        // Clean up old GameModeState
        TroubleInTrorkTownPlugin.gameModeStateForWorld.remove(oldWorldUuid);

        // Clean up old GameModeState
        TroubleInTrorkTownPlugin.instanceConfigs.remove(oldWorldUuid);
    }

    public static World createNewInstance(World currentWorld, String mapTemplateName) {
        try {
            var newWorld = InstancesPlugin.get()
                    .spawnInstance(mapTemplateName, currentWorld, new Transform())
                    .get();

            TroubleInTrorkTownPlugin.currentInstance = newWorld.getWorldConfig().getUuid();
            Config<InstanceConfig> instanceConfig = TroubleInTrorkTownPlugin.mapTemplateConfig.get(mapTemplateName).getInstanceConfig();
            TroubleInTrorkTownPlugin.instanceConfigs.put(
                    TroubleInTrorkTownPlugin.currentInstance,
                    instanceConfig
            );
            TroubleInTrorkTownPlugin.gameModeStateForWorld.put(newWorld.getWorldConfig().getUuid(), new GameModeState());

            if (instanceConfig != null) {
                newWorld.getWorldConfig()
                        .setSpawnProvider(new FitToHeightMapSpawnProvider(new CustomSpawnProvider(instanceConfig.get())));
            } else {
                LOGGER.atSevere().log("No instance config found for template: " + mapTemplateName);
            }

            return newWorld;

        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to spawn instance: " + mapTemplateName);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        World originWorld = commandContext.senderAs(Player.class).getWorld();
        String[] targetWorldName = commandContext.getInput(targetWorld);

        if (targetWorldName.length != 1) {
            commandContext.sendMessage(Message.raw("You must specify a target world name."));
            return;
        }

        if (originWorld == null) {
            commandContext.sendMessage(Message.raw("You are not in a world."));
            return;
        }

        loadInstance(originWorld, targetWorldName[0]);
    }
}
