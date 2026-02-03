package ar.ncode.plugin.system;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.commands.loot.LootSpawnCommand;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.DebugConfig;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.PlayerRole;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.spawning.local.LocalSpawnController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.accessors.WorldAccessors.getPlayersAt;
import static ar.ncode.plugin.accessors.WorldAccessors.getWorldNameForInstance;
import static ar.ncode.plugin.model.MessageId.*;
import static ar.ncode.plugin.model.enums.PlayerRole.*;
import static ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler.updateEachPlayer;
import static ar.ncode.plugin.system.player.PlayerRespawnSystem.teleportPlayerToRandomSpawnPoint;

public class GameModeSystem {

	public final static GameModeSystem INSTANCE = new GameModeSystem();

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

			int karma = playerInfo.getKarma() + karmaUpdate;
			if (karma > 1000) {
				karma = 1000;
			}
			playerInfo.setKarma(karma);
		});
	}

	private static void removeDroppedItems(World world) {
		// Get the ECS Store for entities
		Store<EntityStore> store = world.getEntityStore().getStore();

		// Get the specific component type that identifies a dropped item
		Query<EntityStore> query = Query.and(
				ItemComponent.getComponentType(),
				TransformComponent.getComponentType(),
				Query.not(Interactable.getComponentType()),
				Query.not(PickupItemComponent.getComponentType())
		);

		store.forEachChunk(query, (archetypeChunk, commandBuffer) -> {
			// Iterate through all entities in the current chunk
			for (int i = 0; i < archetypeChunk.size(); i++) {
				// Get the reference to the specific entity in this chunk
				Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(i);

				if (entityRef.isValid()) {
					// Queue the entity for removal.
					// CommandBuffer.removeEntity(Ref, RemoveReason) is verified in the source.
					commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
				}
			}
		});
	}

	private static void removeGraveStones(GameModeState gameModeState, World world) {
		gameModeState.graveStones.forEach(graveStone -> {
			Ref<EntityStore> namePlateReference = graveStone.getNamePlateReference();
			if (!DebugConfig.INSTANCE.isPersistentGraveStones()) {
				if (namePlateReference != null && namePlateReference.isValid()) {
					namePlateReference.getStore().removeEntity(namePlateReference, RemoveReason.REMOVE);
				}

				Vector3i graveStonePosition = graveStone.getGraveStonePosition();
				world.breakBlock(graveStonePosition.x, graveStonePosition.y, graveStonePosition.z, 0);
			}
		});

		gameModeState.graveStones.clear();
	}

	private static void showRoundResultInEventTitle(GameModeState gameModeState, World world) {
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
	}

	private static void setPlayersRoles(GameModeState state, List<PlayerComponents> players, int playerCount) {
		// Calcular cantidad de traidores y detectives (mínimo 1 de cada)
		int configuredTraitors = playerCount / config.get().getTraitorsRatio();
		int numTraitors = Math.max(config.get().getMinAmountOfTraitors(), configuredTraitors);

		int configuredDetectives = playerCount / config.get().getDetectivesRatio();
		int numDetectives = Math.max(config.get().getMinAmountOfDetectives(), configuredDetectives);
		numDetectives = Math.min(numDetectives, config.get().getMaxDetectives());

		int assignedTraitors = 0;
		int assignedDetectives = 0;

		for (var player : players) {
			Ref<EntityStore> reference = player.reference();
			DeathComponent deathComponent = reference.getStore().getComponent(reference, DeathComponent.getComponentType());

			if (deathComponent != null) {
				LocalSpawnController spawnController = reference.getStore()
						.ensureAndGetComponent(reference, LocalSpawnController.getComponentType());

				spawnController.setTimeToNextRunSeconds(0);
			}

			if (assignedTraitors < numTraitors) {
				player.info().setRole(TRAITOR);
				player.info().setCurrentRoundRole(TRAITOR);
				assignedTraitors++;
				TroubleInTrorkTownPlugin.traitorPlayers.add(player.refComponent().getUuid()); // Track for chat filtering

			} else if (assignedDetectives < numDetectives) {
				player.info().setRole(DETECTIVE);
				player.info().setCurrentRoundRole(DETECTIVE);
				assignedDetectives++;

			} else {
				player.info().setRole(INNOCENT);
				player.info().setCurrentRoundRole(INNOCENT);
			}
		}

		state.innocentsAlive = playerCount + assignedDetectives - assignedTraitors;
		state.traitorsAlive = assignedTraitors;
	}

	public static void updatePlayerRole(PlayerComponents player, PlayerRole role, UUID uuid, GameModeState gameModeState) {
		if (PlayerRole.SPECTATOR.equals(role)) {
			gameModeState.spectatorPlayers.add(uuid);

		} else if (PlayerRole.TRAITOR.equals(role)) {
			gameModeState.traitorPlayers.add(uuid);
		}

		player.info().setRole(role);
	}

	public void doAfterRound(World world, GameModeState state) {
		world.execute(() -> {
			// Show result
			showRoundResultInEventTitle(state, world);

			// Clean world
			removeGraveStones(state, world);
			removeDroppedItems(world);

			// Update round state
			state.updateRoundState(RoundState.AFTER_GAME);
			state.playedRounds++;

			// Update players
			updatePlayersKarma(state);

			var players = getPlayersAt(world);
			for (var player : players) {
				player.component().getInventory().clear();
				player.info().getHud().update();
			}
		});
	}

	public void doBeforeRound(World world, GameModeState state) {
		world.execute(() -> {
			LootSpawnCommand.LootForceSpawnCommand.spawnLootForWorld(world);

			String worldName = getWorldNameForInstance(world);
			if (worldName == null) return;

			InstanceConfig instanceConfig = TroubleInTrorkTownPlugin.instanceConfig.get(worldName).get();
			if (instanceConfig == null) return;

			var players = getPlayersAt(world);
			for (var player : players) {
				Ref<EntityStore> reference = player.reference();
				teleportPlayerToRandomSpawnPoint(reference, reference.getStore(), instanceConfig, world);
				reference.getStore().tryRemoveComponent(reference, ConfirmedDeath.componentType);
				reference.getStore().tryRemoveComponent(reference, LostInCombat.componentType);

				if (PlayerRole.SPECTATOR.equals(player.info().getRole())) {
					SpectatorMode.disableSpectatorModeForPlayer(player.refComponent(), reference);
				}

				player.info().setCurrentRoundRole(null);
				player.info().setRole(null);
				player.info().getHud().update();
			}
		});
	}

	public void doAtRoundStart(World world, GameModeState state) {
		world.execute(() -> {
			// Clear spectators and traitors
			TroubleInTrorkTownPlugin.spectatorPlayers.clear();
			TroubleInTrorkTownPlugin.traitorPlayers.clear();

			var players = getPlayersAt(world);
			Collections.shuffle(players); // Shuffle for random role assignment

			setPlayersRoles(state, players, world.getPlayerCount());
			updateEachPlayer(players);

			state.updateRoundState(RoundState.IN_GAME);

			EventTitleUtil.showEventTitleToWorld(
					Message.translation(ROUND_START_MSG.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);

			// TODO: Revisar si se puede quitar
			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), state);
		});
	}

}
