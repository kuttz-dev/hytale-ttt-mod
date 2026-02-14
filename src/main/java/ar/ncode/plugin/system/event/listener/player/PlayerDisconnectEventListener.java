package ar.ncode.plugin.system.event.listener.player;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.GraveSystem;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.system.player.PlayerDeathSystem;
import ar.ncode.plugin.system.scheduled.DoubleTapDetector;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;
import static ar.ncode.plugin.model.TranslationKey.THERE_ARE_NOT_ENOUGH_PLAYERS;

public class PlayerDisconnectEventListener implements Consumer<PlayerDisconnectEvent> {

	private static void theGameMustContinueLogic(GameModeState gameModeState, PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> reference, World world) {
		int value = config.get().getKarmaForDisconnectingMiddleRound();
		gameModeState.karmaUpdates.merge(playerRef.getUuid(), value, Integer::sum);

		GraveStoneWithNameplate graveStone = new GraveStoneWithNameplate();

		PlayerGameModeInfo playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);
		if (playerInfo != null) {
			PlayerDeathSystem.updatePlayerCountsOnPlayerDeath(playerRef, playerInfo.getCurrentRoundRole(), gameModeState);
			graveStone.setDeadPlayerRole(playerInfo.getCurrentRoundRole());
			graveStone.setTimeOfDeath(gameModeState.getRoundRemainingTime().format(timeFormatter));
		}

		graveStone.setDeadPlayerName(playerRef.getUsername());

		GraveSystem.spawnGraveAtPlayerDeath(world, graveStone, reference);
	}

	private static void notEnoughPlayersLogic(Store<EntityStore> store, World world) {
		Message message = Message.translation(THERE_ARE_NOT_ENOUGH_PLAYERS.get());
		EventTitleUtil.showEventTitleToWorld(
				message,
				Message.raw(""),
				true, "ui/icons/EntityStats/Sword_Icon.png",
				4.0f, 1.5f, 1.5f,
				store
		);

		HytaleServer.get().getEventBus()
				.dispatchForAsync(FinishCurrentRoundEvent.class)
				.dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
	}

	@Override
	public void accept(PlayerDisconnectEvent event) {
		PlayerRef playerRef = event.getPlayerRef();
		Ref<EntityStore> reference = playerRef.getReference();
		if (reference == null || !reference.isValid()) {
			return;
		}

		// Remove component from DoubleTapDetector to prevent memory leak
		DoubleTapDetector.getInstance().removePlayer(playerRef.getUuid());

		Store<EntityStore> store = reference.getStore();
		World world = store.getExternalData().getWorld();

		if (world.getPlayerCount() == 0) {
			TroubleInTrorkTownPlugin.currentInstance = null;
		}

		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

		if (gameModeState == null) {
			return;
		}

		// Remove from spectator tracking
		gameModeState.spectators.remove(playerRef.getUuid());
		gameModeState.traitorsAlive.remove(playerRef.getUuid());
		gameModeState.innocentsAlice.remove(playerRef.getUuid());

		world.execute(() -> {
			if (!reference.isValid()) return;
			var playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);

			SpectatorMode.disableSpectatorModeForPlayer(new PlayerComponents(null, playerRef, playerInfo, reference));
			store.removeComponentIfExists(reference, LostInCombat.componentType);
			store.removeComponentIfExists(reference, ConfirmedDeath.componentType);

			boolean thereAreEnoughPlayers = world.getPlayerCount() < config.get().getRequiredPlayersToStartRound();
			if (RoundState.IN_GAME.equals(gameModeState.roundState) && thereAreEnoughPlayers) {
				notEnoughPlayersLogic(store, world);

			} else if (RoundState.IN_GAME.equals(gameModeState.roundState)) {
				theGameMustContinueLogic(gameModeState, playerRef, store, reference, world);
			}
		});
	}
}
