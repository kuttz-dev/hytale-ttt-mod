package ar.ncode.plugin.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import ar.ncode.plugin.ui.hud.PlayerCurrentRoleHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.CustomPermissions.TTT_USER_GROUP;
import static ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler.canStartNewRound;
import static ar.ncode.plugin.system.player.PlayerRespawnSystem.teleportPlayerToRandomSpawnPoint;

public class PlayerReadyEventListener implements Consumer<PlayerReadyEvent> {

	private static PlayerCurrentRoleHud loadHudForPlayer(Player player, PlayerRef playerRef, PlayerGameModeInfo playerInfo) {
		PlayerCurrentRoleHud hud = new PlayerCurrentRoleHud(playerRef, playerInfo);
		HudManager hudManager = player.getHudManager();
		hudManager.setCustomHud(playerRef, hud);

		// Hide specific components (varargs only, no Set overload)
		hudManager.hideHudComponents(playerRef,
				HudComponent.Compass,
				HudComponent.ObjectivePanel,
				HudComponent.PortalPanel,
				HudComponent.BuilderToolsLegend,
				HudComponent.KillFeed,
				HudComponent.PlayerList
		);

		return hud;
	}

	private static boolean playerCanNotSpawn(GameModeState gameModeState) {
		return RoundState.IN_GAME.equals(gameModeState.roundState) || RoundState.AFTER_GAME.equals(gameModeState.roundState);
	}

	/**
	 * Schedules a world transition with a delay to prevent fade conflicts.
	 * The client needs time to fully initialize after joining before we can teleport.
	 * Also ensures only one transition happens at a time.
	 */
	private static void scheduleWorldTransition(World world, Runnable transitionAction) {
		// Skip if a transition is already in progress
		if (TroubleInTrorkTownPlugin.isWorldTransitionInProgress) {
			return;
		}

		// Mark transition as pending
		TroubleInTrorkTownPlugin.isWorldTransitionInProgress = true;

		// Delay the transition to allow client to fully initialize after initial join
		// 3 seconds should be enough for the client to complete any pending fade/loading
		HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
			try {
				transitionAction.run();
			} finally {
				// Reset flag after additional delay for the actual fade to complete
				HytaleServer.SCHEDULED_EXECUTOR.schedule(
						() -> TroubleInTrorkTownPlugin.isWorldTransitionInProgress = false,
						2,
						TimeUnit.SECONDS
				);
			}
		}), 2, TimeUnit.SECONDS);
	}

	private static void configurePlayerPermissions(PlayerRef playerRef) {
		PermissionsModule permissions = PermissionsModule.get();
		permissions.addUserToGroup(playerRef.getUuid(), TTT_USER_GROUP);
	}

	@Override
	public void accept(PlayerReadyEvent event) {
		Player playerComponent = event.getPlayer();
		Ref<EntityStore> reference = event.getPlayerRef();
		World world = playerComponent.getWorld();
		if (world == null) {
			return;
		}

		world.execute(() -> {
			PlayerRef playerRef = reference.getStore().getComponent(reference, PlayerRef.getComponentType());

			if (playerRef == null) {
				return;
			}

			configurePlayerPermissions(playerRef);

			PlayerGameModeInfo playerInfo = reference.getStore().ensureAndGetComponent(reference, PlayerGameModeInfo.componentType);
			GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

			if (gameModeState == null) {
				gameModeState = new GameModeState();
				gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);
			}


			var player = new PlayerComponents(playerComponent, playerRef, playerInfo, reference);
			player.info().setWorldInstance(world.getWorldConfig().getUuid());

			var instanceConfig = WorldAccessors.getWorldInstanceConfig(world);
			if (instanceConfig != null) {
				teleportPlayerToRandomSpawnPoint(reference, reference.getStore(), instanceConfig, world);
			}

			// TTT: Hide ALL players from compass and worldmap (always on)
			WorldMapTracker worldMapTracker = playerComponent.getWorldMapTracker();
			worldMapTracker.setPlayerMapFilter(otherPlayer -> true);  // true = hide player

			EffectControllerComponent effectController = reference.getStore().getComponent(reference, EffectControllerComponent.getComponentType());

			if (effectController == null) {
				return;
			}

			effectController.clearEffects(reference, reference.getStore());
			SpectatorMode.disableSpectatorModeForPlayer(player);
			playerComponent.getInventory().clear();

			if (gameModeState.roundState.equals(RoundState.IN_GAME)) {
				playerInfo.setSpectator(true);
			}

			var hud = loadHudForPlayer(playerComponent, playerRef, playerInfo);
			playerInfo.setHud(hud);

			if (canStartNewRound(gameModeState, world)) {
				HytaleServer.get().getEventBus()
						.dispatchForAsync(StartNewRoundEvent.class)
						.dispatch(new StartNewRoundEvent(world.getWorldConfig().getUuid()));

			} else if (playerCanNotSpawn(gameModeState)) {
				SpectatorMode.setGameModeToSpectator(player);
			}

			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);
		});
	}

}
