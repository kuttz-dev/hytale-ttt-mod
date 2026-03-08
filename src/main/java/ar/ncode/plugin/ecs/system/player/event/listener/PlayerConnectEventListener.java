package ar.ncode.plugin.ecs.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.ecs.commands.ChangeWorldCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.HashMap;
import java.util.function.Consumer;

import static ar.ncode.plugin.ecs.system.event.handler.FinishCurrentMapEventHandler.getNextMap;

public class PlayerConnectEventListener implements Consumer<PlayerConnectEvent> {

    private static void createNewWorldIfNoneExists(PlayerConnectEvent playerConnectEvent) {
        // No current instance exists, load a new map
        String nextMap = getNextMap(new HashMap<>());
        World defaultWorld = Universe.get().getWorld("default");
        if (defaultWorld == null) {
            throw new RuntimeException("Default world is required");
        }

        World newWorld = ChangeWorldCommand.createNewInstance(defaultWorld, nextMap);
        playerConnectEvent.setWorld(newWorld);
    }

    @Override
    public void accept(PlayerConnectEvent playerConnectEvent) {
        // Handle world instance transitions
        if (TroubleInTrorkTownPlugin.currentInstance == null) {
            createNewWorldIfNoneExists(playerConnectEvent);
        }

        // Instance exists, teleport player
        World targetWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
        if (targetWorld == null) {
            TroubleInTrorkTownPlugin.currentInstance = null;
            createNewWorldIfNoneExists(playerConnectEvent);
            return;
        }

        playerConnectEvent.setWorld(targetWorld);
    }

}
