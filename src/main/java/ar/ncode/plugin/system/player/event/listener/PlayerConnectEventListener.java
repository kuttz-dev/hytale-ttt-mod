package ar.ncode.plugin.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.model.GameModeState;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.system.event.handler.FinishCurrentMapEventHandler.getNextMap;

public class PlayerConnectEventListener implements Consumer<PlayerConnectEvent> {

	@Override
	public void accept(PlayerConnectEvent playerConnectEvent) {
		// Handle world instance transitions
		if (TroubleInTrorkTownPlugin.currentInstance == null) {
			createNewWorldInNoneExists(playerConnectEvent);
		}

		// Instance exists, teleport player
		World targetWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
		if (targetWorld == null) {
			TroubleInTrorkTownPlugin.currentInstance = null;
			createNewWorldInNoneExists(playerConnectEvent);
			return;
		}

		playerConnectEvent.setWorld(targetWorld);
	}

	private static void createNewWorldInNoneExists(PlayerConnectEvent playerConnectEvent) {
		// No current instance exists, load a new map
		GameModeState gameModeState = new GameModeState();
		String nextMap = getNextMap(gameModeState);
		World defaultWorld = Universe.get().getWorld("default");
		if (defaultWorld == null) {
			throw new RuntimeException("Default world is required");
		}

		World newWorld = ChangeWorldCommand.createNewInstance(defaultWorld, nextMap);

		TroubleInTrorkTownPlugin.currentInstance = newWorld.getWorldConfig().getUuid();
		gameModeStateForWorld.put(newWorld.getWorldConfig().getUuid(), gameModeState);

		playerConnectEvent.setWorld(newWorld);
	}

}
