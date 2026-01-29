package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.system.event.MapEndEvent;
import ar.ncode.plugin.ui.pages.MapVotePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.*;
import static ar.ncode.plugin.model.MessageId.MAP_VOTE_NOTIFICATION;
import static ar.ncode.plugin.model.MessageId.MAP_VOTE_NOTIFICATION_NEXT_MAP;

public class MapEndEventHandler implements Consumer<MapEndEvent> {

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
					.max((entry1, entry2) -> Integer.compare(entry1.getValue(), entry2.getValue()))
					.get()
					.getKey();
		}

		return newWorldName;
	}

	@Override
	public void accept(MapEndEvent mapEndEvent) {
		World world = Universe.get().getWorld(mapEndEvent.getOldWorldUUID());
		if (world == null) {
			return;
		}
		world.execute(() -> {
			EventTitleUtil.showEventTitleToWorld(
					Message.translation(MAP_VOTE_NOTIFICATION.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);

			for (PlayerRef playerRef : world.getPlayerRefs()) {
				Ref<EntityStore> reference = playerRef.getReference();
				if (reference == null) {
					continue;
				}

				Player player = reference.getStore().getComponent(reference, Player.getComponentType());
				PlayerGameModeInfo playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);

				if (player == null || playerInfo == null) {
					continue;
				}

				playerInfo.getHud().update();

				player.getPageManager().openCustomPage(
						reference, reference.getStore(),
						new MapVotePage(playerRef, CustomPageLifetime.CantClose, worldPreviews, playerInfo)
				);
			}

			executor.schedule(
					() -> endVotesAndChangeWorld(world),
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

		executor.schedule(
				() -> ChangeWorldCommand.loadInstance(currentWorld, newWorldName),
				config.get().getTimeBeforeChangingMapInSeconds(),
				TimeUnit.SECONDS
		);
	}


}
