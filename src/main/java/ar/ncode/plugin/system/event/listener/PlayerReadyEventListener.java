package ar.ncode.plugin.system.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.GameModeState;
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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.system.event.handler.MapEndEventHandler.getNextMap;
import static ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler.canStartNewRound;

public class PlayerReadyEventListener implements Consumer<PlayerReadyEvent> {

	@NonNullDecl
	private static PlayerRole getPlayerRoleBasedOnGameState(GameModeState gameModeState) {
		PlayerRole role;
		if (gameModeState.roundState.equals(RoundState.IN_GAME)) {
			role = PlayerRole.SPECTATOR;

		} else {
			role = PlayerRole.PREPARING;
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
				HudComponent.KillFeed
		);

		return hud;
	}

	private static boolean playerCanNotSpawn(GameModeState gameModeState) {
		return RoundState.IN_GAME.equals(gameModeState.roundState) || RoundState.AFTER_GAME.equals(gameModeState.roundState);
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
			PlayerGameModeInfo playerInfo = reference.getStore().ensureAndGetComponent(reference, PlayerGameModeInfo.componentType);

			PlayerRef playerRef = reference.getStore().getComponent(reference, PlayerRef.getComponentType());
			EffectControllerComponent effectController = reference.getStore().getComponent(reference, EffectControllerComponent.getComponentType());

			if (playerRef == null || effectController == null) {
				return;
			}

			effectController.clearEffects(reference, reference.getStore());
			SpectatorMode.disableSpectatorModeForPlayer(playerRef, reference);
			player.getInventory().clear();

			GameModeState gameModeState = gameModeStateForWorld.getOrDefault(world.getWorldConfig().getUuid(), new GameModeState());

			if (TroubleInTrorkTownPlugin.currentInstance == null && playerInfo.getWorldInstance() == null) {
				String nextMap = getNextMap(gameModeState);
				ChangeWorldCommand.loadInstance(world, nextMap);
				return;

			} else if (Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance) == null && playerInfo.getWorldInstance() == null) {
				ChangeWorldCommand.loadInstance(world, TroubleInTrorkTownPlugin.currentInstance);
				return;

			} else if (playerInfo.getWorldInstance() == null) {
				World targetWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
				InstancesPlugin.teleportPlayerToInstance(reference, reference.getStore(), targetWorld, new Transform());
				return;
			}

			PlayerRole role = getPlayerRoleBasedOnGameState(gameModeState);
			playerInfo.setRole(role);
			playerInfo.setCurrentRoundRole(role);

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
