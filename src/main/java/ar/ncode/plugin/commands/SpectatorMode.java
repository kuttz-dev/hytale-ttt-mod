package ar.ncode.plugin.commands;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

import static ar.ncode.plugin.system.player.PlayerDeathSystem.updatePlayerCountsOnPlayerDeath;

public class SpectatorMode extends CommandBase {

	OptionalArg<PlayerRef> playerArg = this.withOptionalArg("targetPlayer", "Target component to change game mode from/to spectator", ArgTypes.PLAYER_REF);

	public SpectatorMode() {
		super("spectator", "Command to toggle spectator mode for a component");
	}

	public static boolean toggleSpectatorMode(Ref<EntityStore> reference) {
		var player = PlayerAccessors.getPlayerFrom(reference);
		if (player.isEmpty()) {
			return false;
		}

		Intangible intangible = reference.getStore().getComponent(reference, Intangible.getComponentType());
		if (intangible == null) {
			setGameModeToSpectator(player.get());
			return true;

		} else {
			disableSpectatorModeForPlayer(player.get());
			return false;
		}
	}

	public static void disableSpectatorModeForPlayer(PlayerComponents player) {
		var reference = player.reference();
		if (!reference.isValid()) return;

		if (player.info() != null) {
			player.info().setSpectator(false);
		}

		var gameState = WorldAccessors.gameModeStateForPlayerWorld(reference);
		if (gameState != null) {
			gameState.spectators.remove(player.refComponent().getUuid());
		}

		// Remove spectator mode
		Store<EntityStore> store = reference.getStore();
		store.removeComponentIfExists(reference, Intangible.getComponentType());
		store.removeComponentIfExists(reference, Invulnerable.getComponentType());
		showPlayerToAll(player.refComponent(), player.refComponent().getUuid());

		MovementManager movementManager = store.getComponent(reference, MovementManager.getComponentType());
		MovementStatesComponent statesComponent = store.getComponent(reference, MovementStatesComponent.getComponentType());

		if (movementManager == null) {
			return;
		}

		MovementSettings movementSettings = movementManager.getSettings();
		if (movementSettings == null) {
			return;
		}

		movementSettings.canFly = false;
		movementManager.update(player.refComponent().getPacketHandler());

		if (statesComponent == null) {
			return;
		}

		MovementStates currentStates = statesComponent.getMovementStates();
		if (currentStates != null && currentStates.flying) {
			// MovementStates has a public boolean field 'flying'
			currentStates.flying = false;

			// The client manages the physics state, so we tell it to stop flying.
			// SavedMovementStates(boolean flying) constructor exists in source.
			// SetMovementStates(SavedMovementStates) constructor exists in source.
			player.refComponent().getPacketHandler().writeNoCache(new SetMovementStates(new SavedMovementStates(false)));
		}
	}

	public static void setGameModeToSpectator(PlayerComponents player) {
		// Get effect from asset store
//                EntityEffect effect = EntityEffect.getAssetMap().getAsset("Spectator");
//                if (effect == null) {
//                    ctx.sendMessage(Message.raw("Spectator effect not found in asset store."));
//                    return;
//                }
//
//                boolean applied = controller.addEffect(reference, effect, reference.getStore());
//                if (!applied) {
//                    ctx.sendMessage(Message.raw("Failed to apply spectator effect to target component."));
//                    return;
//                }

		var reference = player.reference();
		player.info().setSpectator(true);
		var gameState = WorldAccessors.gameModeStateForPlayerWorld(reference);
		updatePlayerCountsOnPlayerDeath(player.refComponent(), player.info().getCurrentRoundRole(), gameState);

		MovementManager movementManager = reference.getStore().getComponent(reference, MovementManager.getComponentType());
		MovementSettings movementSettings = movementManager.getSettings();
		if (movementSettings == null) {
			return;
		}
		movementSettings.canFly = true;
		movementManager.update(player.refComponent().getPacketHandler());

		reference.getStore().ensureComponent(reference, Intangible.getComponentType());
		reference.getStore().ensureComponent(reference, Invulnerable.getComponentType());
		hidePlayerFromAllNonSpectators(player.refComponent(), player.refComponent().getUuid());
	}

	static void hidePlayerFromAllNonSpectators(PlayerRef playerRef, UUID playerUuid) {
		Universe.get().getWorlds().forEach((worldName, world) -> world.execute(() -> {
			for (PlayerRef targetRef : world.getPlayerRefs()) {
				if (targetRef.equals(playerRef)) {
					continue;
				}

				Ref<EntityStore> targetEntityRef = targetRef.getReference();
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

				if (targetPlayerInfo == null || !targetPlayerInfo.isSpectator()) {
					targetRef.getHiddenPlayersManager().hidePlayer(playerUuid);
				}
			}
		}));
	}

	static void showPlayerToAll(PlayerRef playerRef, UUID playerUuid) {
		Universe.get().getWorlds().forEach((worldName, world) -> world.execute(() -> {
			for (PlayerRef targetRef : world.getPlayerRefs()) {
				if (!targetRef.equals(playerRef)) {
					Ref<EntityStore> targetEntityRef = targetRef.getReference();
					if (targetEntityRef != null && targetEntityRef.isValid()) {
						Store<EntityStore> targetStore = targetEntityRef.getStore();
						Player targetPlayer = targetStore.getComponent(targetEntityRef, Player.getComponentType());
						if (targetPlayer != null) {
							targetRef.getHiddenPlayersManager().showPlayer(playerUuid);
						}
					}
				}
			}

		}));
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext ctx) {
		PlayerRef targetPlayerRef = playerArg.get(ctx);
		Ref<EntityStore> reference;
		if (targetPlayerRef == null) {
			reference = ctx.senderAsPlayerRef();
			if (reference == null || !reference.isValid()) {
				ctx.sendMessage(Message.raw("You must specify a target component when using this command from the console."));
				return;
			}

			reference.getStore().getExternalData().getWorld()
					.execute(() -> toggleSpectatorMode(reference));
			return;

		} else {
			reference = targetPlayerRef.getReference();
		}

		if (reference == null || !reference.isValid() || !targetPlayerRef.isValid()) {
			ctx.sendMessage(Message.raw("Can not apply command to invalid target."));
			return;
		}

		boolean result = toggleSpectatorMode(reference);
		if (result) {
			ctx.sendMessage(Message.raw("Spectator mode applied for target."));
		} else {
			ctx.sendMessage(Message.raw("Spectator mode removed for target."));
		}
	}
}
