package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.MessageId.ROUND_INNOCENTS_WIN_MSG;
import static ar.ncode.plugin.model.MessageId.ROUND_TRAITORS_WIN_MSG;

public class FinishCurrentRoundEventHandler implements Consumer<FinishCurrentRoundEvent> {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public static boolean roundShouldEnd(GameModeState gameModeState) {
		return gameModeState.innocentsAlive == 0 || gameModeState.traitorsAlive == 0;
	}

	private static void updatePlayersKarma(GameModeState gameModeState) {
		gameModeState.karmaUpdates.forEach((playerUUID, karmaUpdate) -> {
			PlayerRef playerRef = Universe.get().getPlayer(playerUUID);

			if (playerRef == null || karmaUpdate == null) {
				return;
			}

			Ref<EntityStore> reference = playerRef.getReference();

			if (reference == null) {
				return;
			}


			PlayerGameModeInfo playerInfo = reference.getStore().getComponent(reference,
					PlayerGameModeInfo.componentType);

			if (playerInfo == null) {
				return;
			}

			playerInfo.setKarma(playerInfo.getKarma() + karmaUpdate);
		});
	}

	private static void removeDroppedItems(GameModeState gameModeState) {
		gameModeState.items.forEach(item -> {
			if (item != null && item.isValid()) {
				item.getStore().removeEntity(item, RemoveReason.REMOVE);
			}
		});

		gameModeState.items.clear();
	}

	private static void removeGraveStones(GameModeState gameModeState, World world) {
		gameModeState.graveStones.forEach(graveStone -> {
			Ref<EntityStore> namePlateReference = graveStone.getNamePlateReference();
			if (!config.get().isDebugMode()) {
				if (namePlateReference != null && namePlateReference.isValid()) {
					namePlateReference.getStore().removeEntity(namePlateReference, RemoveReason.REMOVE);
				}

				Vector3i graveStonePosition = graveStone.getGraveStonePosition();
				world.breakBlock(graveStonePosition.x, graveStonePosition.y, graveStonePosition.z, 0);
			}
		});

		gameModeState.graveStones.clear();
	}

	@Override
	public void accept(FinishCurrentRoundEvent event) {
		World world = Universe.get().getWorld(event.getWorldUUID());
		if (world == null) {
			return;
		}

		GameModeState gameModeState = gameModeStateForWorld.getOrDefault(
				event.getWorldUUID(),
				new GameModeState()
		);

		if (!roundShouldEnd(gameModeState)) {
			return;
		}

		if (gameModeState.innocentsAlive > 0) {
			EventTitleUtil.showEventTitleToWorld(
					Message.translation(ROUND_INNOCENTS_WIN_MSG.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);
		} else {
			EventTitleUtil.showEventTitleToWorld(
					Message.translation(ROUND_TRAITORS_WIN_MSG.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);
		}

		world.execute(() -> {
			removeGraveStones(gameModeState, world);
			removeDroppedItems(gameModeState);

			gameModeState.roundState = RoundState.AFTER_GAME;
			gameModeState.roundStateUpdatedAt = LocalDateTime.now();

			updatePlayersKarma(gameModeState);

			gameModeState.playedRounds++;

			if (gameModeState.hasLastRoundFinished()) {
				HytaleServer.get().getEventBus()
						.dispatchForAsync(FinishCurrentMapEvent.class)
						.dispatch(new FinishCurrentMapEvent(world.getWorldConfig().getUuid()));
				return;
			}

			executor.schedule(() -> {
						// Check if world is still alive before executing (prevents memory leak from stale references)
						if (!world.isAlive()) return;
						prepareNextRound(gameModeState, world);
					},
					config.get().getTimeAfterRoundInSeconds(),
					TimeUnit.SECONDS
			);
		});
	}

	private void prepareNextRound(GameModeState gameModeState, World world) {
		world.execute(() -> {
			for (var playerRef : world.getPlayerRefs()) {
				Ref<EntityStore> targetEntityRef = playerRef.getReference();
				if (targetEntityRef == null || !targetEntityRef.isValid()) {
					continue;
				}

				Player targetPlayer = targetEntityRef.getStore().getComponent(targetEntityRef, Player.getComponentType());
				if (targetPlayer == null) {
					continue;
				}

				PlayerGameModeInfo targetPlayerInfo = targetEntityRef.getStore().getComponent(
						targetEntityRef,
						PlayerGameModeInfo.componentType
				);

				if (targetPlayerInfo == null) {
					continue;
				}

				targetPlayerInfo.setCurrentRoundRole(null);

				if (PlayerRole.SPECTATOR.equals(targetPlayerInfo.getRole())) {
					SpectatorMode.disableSpectatorModeForPlayer(playerRef, targetEntityRef);
				}

				targetPlayer.getInventory().clear();
				targetPlayerInfo.setRole(PlayerRole.PREPARING);
				TroubleInTrorkTownPlugin.spectatorPlayers.remove(playerRef.getUuid()); // No longer spectator
				targetPlayerInfo.getHud().update();

				gameModeState.roundState = RoundState.PREPARING;
				gameModeState.roundStateUpdatedAt = LocalDateTime.now();
				HytaleServer.get().getEventBus()
						.dispatchForAsync(StartNewRoundEvent.class)
						.dispatch(new StartNewRoundEvent(world.getWorldConfig().getUuid()));
			}
		});
	}
}
