package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.commands.loot.LootSpawnCommand;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.MessageId;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.spawning.local.LocalSpawnController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.component.enums.PlayerRole.*;
import static ar.ncode.plugin.component.enums.RoundState.IN_GAME;
import static ar.ncode.plugin.component.enums.RoundState.PREPARING;
import static ar.ncode.plugin.model.MessageId.*;

public class StartNewRoundEventHandler implements Consumer<StartNewRoundEvent> {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public static boolean canStartNewRound(GameModeState gameModeState, World world) {
		return PREPARING.equals(gameModeState.roundState) && world.getPlayerCount() >= config.get().getRequiredPlayersToStartRound();
	}

	private static void startNewRound(GameModeState gameModeState, World world) {
		world.execute(() -> {
			int playerCount = world.getPlayerCount();

			// Calcular cantidad de traidores y detectives (mínimo 1 de cada)
			int configuredTraitors = playerCount / config.get().getTraitorsRatio();
			int numTraitors = Math.max(config.get().getMinAmountOfTraitors(), configuredTraitors);


			int configuredDetectives = playerCount / config.get().getDetectivesRatio();
			int numDetectives = Math.max(config.get().getMinAmountOfDetectives(), configuredDetectives);
			numDetectives = Math.min(numDetectives, config.get().getMaxDetectives());

			int assignedTraitors = 0;
			int assignedDetectives = 0;

			List<PlayerRef> playerRefs = new ArrayList<>(world.getPlayerRefs());
			Collections.shuffle(playerRefs); // Shuffle for random role assignment

			for (PlayerRef playerRef : playerRefs) {
				Ref<EntityStore> reference = playerRef.getReference();
				if (reference == null) {
					continue;
				}

				PlayerGameModeInfo playerInfo = reference.getStore()
						.getComponent(reference, PlayerGameModeInfo.componentType);


				if (playerInfo == null) {
					return;
				}

				DeathComponent deathComponent = reference.getStore()
						.getComponent(reference, DeathComponent.getComponentType());

				if (deathComponent != null) {
					LocalSpawnController spawnController = reference.getStore()
							.ensureAndGetComponent(reference, LocalSpawnController.getComponentType());

					spawnController.setTimeToNextRunSeconds(0);
				}


				// Clear spectator status - player is now alive
				TroubleInTrorkTownPlugin.spectatorPlayers.remove(playerRef.getUuid());

				if (assignedTraitors < numTraitors) {
					playerInfo.setRole(TRAITOR);
					playerInfo.setCurrentRoundRole(TRAITOR);
					assignedTraitors++;

				} else if (assignedDetectives < numDetectives) {
					playerInfo.setRole(DETECTIVE);
					playerInfo.setCurrentRoundRole(DETECTIVE);
					assignedDetectives++;

				} else {
					playerInfo.setRole(INNOCENT);
					playerInfo.setCurrentRoundRole(INNOCENT);
				}
			}

			gameModeState.innocentsAlive = playerCount + assignedDetectives - assignedTraitors;
			gameModeState.traitorsAlive = assignedTraitors;

			updateEachPlayer(playerRefs);
			gameModeState.roundState = IN_GAME;
			gameModeState.roundStateUpdatedAt = LocalDateTime.now();

			EventTitleUtil.showEventTitleToWorld(
					Message.translation(ROUND_START_MSG.get()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);
			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);
		});
	}

	private static void updateEachPlayer(List<PlayerRef> playerRefs) {
		for (PlayerRef playerRef : playerRefs) {
			Ref<EntityStore> reference = playerRef.getReference();

			if (reference == null) {
				continue;
			}

			Player player = reference.getStore().getComponent(reference, Player.getComponentType());
			PlayerGameModeInfo playerInfo = reference.getStore()
					.ensureAndGetComponent(reference, PlayerGameModeInfo.componentType);
			EntityStatMap stats = reference.getStore().getComponent(reference, EntityStatMap.getComponentType());

			if (player == null || player.getInventory() == null || stats == null) {
				continue;
			}

			stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
			player.getInventory().setActiveHotbarSlot((byte) 0);
			addConfiguredStartingItemsToPlayer(player);

			NotificationStyle notificationStyle;

			if (TRAITOR.equals(playerInfo.getRole())) {
				notificationStyle = NotificationStyle.Danger;
				playerInfo.setCredits(1);

			} else if (DETECTIVE.equals(playerInfo.getRole())) {
				notificationStyle = NotificationStyle.Default;
				playerInfo.setCredits(1);

			} else {
				notificationStyle = NotificationStyle.Success;
			}


			NotificationUtil.sendNotification(
					playerRef.getPacketHandler(),
					Message.translation(PLAYER_ASSIGNED_ROLE_NOTIFICATION.get())
							.param("role", getTranslatedRole(playerInfo.getRole())),
					notificationStyle
			);


			if (playerInfo.getHud() != null) {
				playerInfo.getHud().update();
			}
			player.getPageManager().setPage(reference, reference.getStore(), Page.None);
		}
	}

	private static void addConfiguredStartingItemsToPlayer(Player player) {
		var itemGroups = config.get().getItems(config.get().getStartingItemsInHotbar());
		addItemsToPlayer(itemGroups, player.getInventory().getHotbar());

		itemGroups = config.get().getItems(config.get().getStartingItemsInInventory());
		addItemsToPlayer(itemGroups, player.getInventory().getStorage());
	}

	private static void addItemsToPlayer(List<ItemStack> items, ItemContainer container) {
		for (ItemStack itemStack : items) {
			container.addItemStack(itemStack);
		}
	}

	private static Message getTranslatedRole(PlayerRole role) {
		MessageId messageId = switch (role) {
			case INNOCENT -> MessageId.HUD_CURRENT_ROLE_INNOCENT;
			case TRAITOR -> HUD_CURRENT_ROLE_TRAITOR;
			case DETECTIVE -> MessageId.HUD_CURRENT_ROLE_DETECTIVE;
			case SPECTATOR -> MessageId.HUD_CURRENT_ROLE_SPECTATOR;
			default -> MessageId.HUD_CURRENT_ROLE_PREPARING;
		};

		return Message.translation(messageId.get());
	}

	@Override
	public void accept(StartNewRoundEvent startNewRoundEvent) {
		World world = Universe.get().getWorld(startNewRoundEvent.getWorldUUID());
		if (world == null) {
			return;
		}

		GameModeState gameModeState = gameModeStateForWorld.getOrDefault(
				startNewRoundEvent.getWorldUUID(),
				new GameModeState()
		);

		world.execute(() -> {
			if (canStartNewRound(gameModeState, world)) {
				Message newRoundWillBeginMessagee = Message.translation(ROUND_ABOUT_TO_START_MSG.get())
						.param("time", config.get().getTimeBeforeRoundInSeconds());

				EventTitleUtil.showEventTitleToWorld(
						newRoundWillBeginMessagee,
						Message.raw(""),
						true, "ui/icons/EntityStats/Sword_Icon.png",
						4.0f, 1.5f, 1.5f,
						world.getEntityStore().getStore()
				);

				gameModeState.roundState = RoundState.STARTING;
				gameModeState.roundStateUpdatedAt = LocalDateTime.now();

				gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);
				LootSpawnCommand.LootForceSpawnCommand.spawnLootForWorld(world);

				executor.schedule(() -> {
							// Check if world is still alive before executing (prevents memory leak from stale references)
							if (!world.isAlive()) return;
							startNewRound(gameModeState, world);
						},
						config.get().getTimeBeforeRoundInSeconds(),
						TimeUnit.SECONDS
				);
			}
		});
	}
}
