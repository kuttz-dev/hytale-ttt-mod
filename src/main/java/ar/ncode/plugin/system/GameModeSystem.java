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
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.*;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.accessors.WorldAccessors.getPlayersAt;
import static ar.ncode.plugin.accessors.WorldAccessors.getWorldNameForInstance;
import static ar.ncode.plugin.model.TranslationKey.*;
import static ar.ncode.plugin.model.enums.RoleGroup.INNOCENT;
import static ar.ncode.plugin.model.enums.RoleGroup.TRAITOR;
import static ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler.updateEachPlayer;
import static ar.ncode.plugin.system.player.PlayerRespawnSystem.teleportPlayerToRandomSpawnPoint;

public class GameModeSystem {

	public static final GameModeSystem INSTANCE = new GameModeSystem();

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
		if (!gameModeState.innocentsAlice.isEmpty()) {
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
		Collections.shuffle(players); // Shuffle for random role assignment
		var roles = config.get().getRoles();

		// Track which players already got a special role
		Set<UUID> assigned = new HashSet<>();

		for (var role : roles) {
			if (INNOCENT.equals(role.getRoleGroup())) {
				continue;
			}

			int expectedAssignedPlayers = playerCount / role.getRatio();
			expectedAssignedPlayers = Math.max(role.getMinimumAssignedPlayersWithRole(), expectedAssignedPlayers);

			int assignedPlayers = 0;

			for (var player : players) {
				UUID uuid = player.refComponent().getUuid();
				if (assigned.contains(uuid)) {
					continue;
				}

				if (assignedPlayers >= expectedAssignedPlayers) {
					break;
				}
				player.info().setCurrentRoundRole(role);
				assignedPlayers++;
				assigned.add(uuid);

				if (TRAITOR.equals(role.getRoleGroup())) {
					state.traitorsAlive.add(player.refComponent().getUuid());

				} else {
					state.innocentsAlice.add(player.refComponent().getUuid());
				}
			}
		}
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

				if (player.info().isSpectator()) {
					SpectatorMode.disableSpectatorModeForPlayer(player);
				}

				player.info().setCurrentRoundRole(null);
				player.info().getHud().update();
			}
		});
	}

	public void doAtRoundStart(World world, GameModeState state) {
		world.execute(() -> {
			// Clear spectators and traitors
			state.spectators.clear();
			state.traitorsAlive.clear();

			var players = getPlayersAt(world);
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
