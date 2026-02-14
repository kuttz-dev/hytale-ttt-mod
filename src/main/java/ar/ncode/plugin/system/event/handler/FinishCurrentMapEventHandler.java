package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.ui.pages.MapVotePage;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.*;
import static ar.ncode.plugin.accessors.WorldAccessors.getPlayersAt;
import static ar.ncode.plugin.model.TranslationKey.MAP_VOTE_NOTIFICATION;
import static ar.ncode.plugin.model.TranslationKey.MAP_VOTE_NOTIFICATION_NEXT_MAP;

public class FinishCurrentMapEventHandler implements Consumer<FinishCurrentMapEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public static String getNextMap(GameModeState gameState) {
		String newWorldName = null;
		if (gameState.mapVotes.isEmpty()) {
			// Choose a random map
			int randomIndex = (int) (Math.random() * worldPreviews.size());
			newWorldName = worldPreviews.get(randomIndex).getWorldName();

		} else {
			// Choose the map with the most votes
			newWorldName = gameState.mapVotes.entrySet().stream()
					.max(Comparator.comparingInt(Map.Entry::getValue))
					.get()
					.getKey();
		}

		return newWorldName;
	}

	@Override
	public void accept(FinishCurrentMapEvent finishCurrentMapEvent) {
		World world = Universe.get().getWorld(finishCurrentMapEvent.getOldWorldUUID());
		if (world == null) return;

		world.execute(() -> {
			EventTitleUtil.showEventTitleToWorld(
					Message.translation(MAP_VOTE_NOTIFICATION.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);

			var players = getPlayersAt(world);

			for (var player : players) {
				player.info().getHud().update();

				HytaleServer.SCHEDULED_EXECUTOR.schedule(() ->
						world.execute(() ->
								player.component().getPageManager().openCustomPage(
										player.reference(),
										player.reference().getStore(),
										new MapVotePage(
												player.refComponent(), CustomPageLifetime.CantClose,
												worldPreviews, player.info()
										)
								)
						), 2, TimeUnit.SECONDS
				);
			}

			executor.schedule(() -> {
						// Check if world is still alive before executing (prevents memory leak from stale references)
						if (!world.isAlive()) return;
						endVotesAndChangeWorld(world);
					},
					config.get().getTimeToVoteMapInSeconds(),
					TimeUnit.SECONDS
			);
		});
	}

	private void endVotesAndChangeWorld(World currentWorld) {
		UUID oldWorldId = currentWorld.getWorldConfig().getUuid();
		var gameState = gameModeStateForWorld.get(oldWorldId);
		String newWorldName = getNextMap(gameState);

		// Remove old GameModeState to prevent memory leak when loading new instance
		gameModeStateForWorld.remove(oldWorldId);

		EventTitleUtil.showEventTitleToWorld(
				Message.translation(MAP_VOTE_NOTIFICATION_NEXT_MAP.get())
						.param("map_name", newWorldName)
						.param("time", String.valueOf(config.get().getTimeBeforeChangingMapInSeconds())),
				Message.raw(""),
				true, "ui/icons/EntityStats/Sword_Icon.png",
				4.0f, 1.5f, 1.5f,
				currentWorld.getEntityStore().getStore()
		);

		executor.schedule(() -> {
					// Check if world is still alive before executing (prevents memory leak from stale references)
					if (!currentWorld.isAlive()) return;
					ChangeWorldCommand.loadInstance(currentWorld, newWorldName);
				},
				config.get().getTimeBeforeChangingMapInSeconds(),
				TimeUnit.SECONDS
		);
	}


}
