package ar.ncode.plugin.ecs.system.event.listener;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.ecs.commands.ChangeWorldCommand;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RemoveWorldListener implements Consumer<RemoveWorldEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void accept(RemoveWorldEvent event) {
        try {
            World world = event.getWorld();
            String instanceName = world.getName();
            RemoveWorldEvent.RemovalReason reason = event.getRemovalReason();
            String defaultWorldName = HytaleServer.get().getConfig().getDefaults().getWorld();

            if (!reason.equals(RemoveWorldEvent.RemovalReason.EXCEPTIONAL)) {
                return;
            }

            LOGGER.atInfo().log("World " + instanceName + " has been removed! Reason: " + reason);

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                LOGGER.atInfo().log("World " + instanceName + " reloading!!!!");

                if (defaultWorldName.equals(instanceName)) {
                    Universe.get().loadWorld(defaultWorldName)
                            .thenAccept((newWorld) -> {
                                LOGGER.atInfo().log("Default World has been loaded!");
                            });

                } else {
                    String mapName = WorldAccessors.getWorldNameForInstance(world);

                    World defaultWorld = Universe.get().getWorld(defaultWorldName);
                    if (defaultWorld == null) {
                        LOGGER.atSevere().log("Default world not found - cannot load new world instance!");
                        return;
                    }

                    ChangeWorldCommand.loadNewWorld(defaultWorld, mapName);
                    ChangeWorldCommand.cleanUpOldWorld(world.getWorldConfig().getUuid());
                    LOGGER.atInfo().log("World " + instanceName + " has been loaded!");
                }
            }, 5L, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error handling world remove event.", e.getMessage());
        }
    }
}
