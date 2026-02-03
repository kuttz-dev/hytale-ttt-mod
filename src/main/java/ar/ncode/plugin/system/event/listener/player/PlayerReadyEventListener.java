package ar.ncode.plugin.system.event.listener.player;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.enums.PlayerRole;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import ar.ncode.plugin.ui.hud.PlayerCurrentRoleHud;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.CustomPermissions.TTT_USER_GROUP;
import static ar.ncode.plugin.model.CustomPermissions.USER_PERMISSIONS;
import static ar.ncode.plugin.system.event.handler.FinishCurrentMapEventHandler.getNextMap;
import static ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler.canStartNewRound;
import static ar.ncode.plugin.system.player.PlayerRespawnSystem.teleportPlayerToRandomSpawnPoint;

public class PlayerReadyEventListener implements Consumer<PlayerReadyEvent> {

	private static PlayerRole getPlayerRoleBasedOnGameState(GameModeState gameModeState) {
		PlayerRole role = null;
		if (gameModeState.roundState.equals(RoundState.IN_GAME)) {
			role = PlayerRole.SPECTATOR;
		}

		return role;
	}

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
						1500,
						TimeUnit.MILLISECONDS
				);
			}
		}), 1500, TimeUnit.MILLISECONDS);
	}

	private static void configurePlayerPermissions(PlayerRef playerRef) {
		PermissionsModule permissions = PermissionsModule.get();
		permissions.addGroupPermission(TTT_USER_GROUP, USER_PERMISSIONS);
		permissions.addUserToGroup(playerRef.getUuid(), TTT_USER_GROUP);
	}

	@Override
	public void accept(PlayerReadyEvent event) {
		Player player = event.getPlayer();
		Ref<EntityStore> reference = event.getPlayerRef();
		World world = player.getWorld();
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
			GameModeState gameModeState = gameModeStateForWorld.getOrDefault(world.getWorldConfig().getUuid(), new GameModeState());

			// Handle world instance transitions
			// We need to delay teleports to prevent "Cannot start a fade out while a fade completion callback is pending"
			// The client needs time to fully initialize after the initial world join before we can teleport them
			if (TroubleInTrorkTownPlugin.currentInstance == null) {
				// No current instance exists, load a new map with delay
				String nextMap = getNextMap(gameModeState);
				scheduleWorldTransition(world, () -> ChangeWorldCommand.loadInstance(world, nextMap));
				return;

			} else if (playerInfo.getWorldInstance() == null) {
				// Instance exists, teleport component to it with delay
				World targetWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
				if (targetWorld == null) {
					TroubleInTrorkTownPlugin.currentInstance = null;
					String nextMap = getNextMap(gameModeState);
					scheduleWorldTransition(world, () -> ChangeWorldCommand.loadInstance(world, nextMap));
					return;
				}

				scheduleWorldTransition(world, () -> {
					// Re-check if component still needs teleport (they might have disconnected)
					if (reference.isValid() && playerInfo.getWorldInstance() == null) {
						playerInfo.setWorldInstance(TroubleInTrorkTownPlugin.currentInstance);
						InstancesPlugin.teleportPlayerToInstance(reference, reference.getStore(), targetWorld, new Transform());
					}
				});
			}

			if (world.getWorldConfig().getDisplayName() != null) {
				String worldName = world.getWorldConfig().getDisplayName().replace(" ", "_").toLowerCase();
				InstanceConfig instanceConfig = TroubleInTrorkTownPlugin.instanceConfig.get(worldName).get();

				if (instanceConfig != null) {
					teleportPlayerToRandomSpawnPoint(reference, reference.getStore(), instanceConfig, world);
				}
			}

			// TTT: Hide ALL players from compass and worldmap (always on)
			WorldMapTracker worldMapTracker = player.getWorldMapTracker();
			worldMapTracker.setPlayerMapFilter(otherPlayer -> true);  // true = hide everyone

			EffectControllerComponent effectController = reference.getStore().getComponent(reference, EffectControllerComponent.getComponentType());

			if (effectController == null) {
				return;
			}

			effectController.clearEffects(reference, reference.getStore());
			SpectatorMode.disableSpectatorModeForPlayer(playerRef, reference);
			player.getInventory().clear();

			PlayerRole role = getPlayerRoleBasedOnGameState(gameModeState);
			playerInfo.setRole(role);
			playerInfo.setCurrentRoundRole(role);
			// Track spectator status for chat filtering
			if (PlayerRole.SPECTATOR.equals(role)) {
				TroubleInTrorkTownPlugin.spectatorPlayers.add(playerRef.getUuid());
			} else {
				TroubleInTrorkTownPlugin.spectatorPlayers.remove(playerRef.getUuid());
			}

			var hud = loadHudForPlayer(player, playerRef, playerInfo);
			playerInfo.setHud(hud);

			if (canStartNewRound(gameModeState, world)) {
				HytaleServer.get().getEventBus()
						.dispatchForAsync(StartNewRoundEvent.class)
						.dispatch(new StartNewRoundEvent(world.getWorldConfig().getUuid()));

			} else if (playerCanNotSpawn(gameModeState)) {
				SpectatorMode.setGameModeToSpectator(playerRef, reference);
			}

			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);
		});
	}

}
