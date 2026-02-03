package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.MessageId;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.PlayerRole;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.GameModeSystem;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.MessageId.*;
import static ar.ncode.plugin.model.enums.PlayerRole.DETECTIVE;
import static ar.ncode.plugin.model.enums.PlayerRole.TRAITOR;
import static ar.ncode.plugin.model.enums.RoundState.PREPARING;

public class StartNewRoundEventHandler implements Consumer<StartNewRoundEvent> {

	public static final int STARTING_CREDITS = 1;
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public static boolean canStartNewRound(GameModeState gameModeState, World world) {
		return PREPARING.equals(gameModeState.roundState) && world.getPlayerCount() >= config.get().getRequiredPlayersToStartRound();
	}

	public static void updateEachPlayer(List<PlayerComponents> players) {
		for (var player : players) {
			Ref<EntityStore> reference = player.reference();

			EntityStatMap stats = reference.getStore().getComponent(reference, EntityStatMap.getComponentType());
			if (stats == null) continue;

			// Inventory
			player.component().getInventory().setActiveHotbarSlot((byte) 0);
			addConfiguredStartingItemsToPlayer(player.component());

			// GUI
			player.component().getPageManager().setPage(reference, reference.getStore(), Page.None);
			player.info().getHud().update();

			// Remove effects
			stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());

			NotificationStyle notificationStyle;
			if (TRAITOR.equals(player.info().getRole())) {
				notificationStyle = NotificationStyle.Danger;
				player.info().setCredits(STARTING_CREDITS);

			} else if (DETECTIVE.equals(player.info().getRole())) {
				notificationStyle = NotificationStyle.Default;
				player.info().setCredits(STARTING_CREDITS);

			} else {
				notificationStyle = NotificationStyle.Success;
			}

			NotificationUtil.sendNotification(
					player.refComponent().getPacketHandler(),
					Message.translation(PLAYER_ASSIGNED_ROLE_NOTIFICATION.get())
							.param("role", getTranslatedRole(player.info().getRole())),
					notificationStyle
			);
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

		gameModeState.updateRoundState(RoundState.PREPARING);

		if (!canStartNewRound(gameModeState, world)) return;

		GameModeSystem.INSTANCE.doBeforeRound(world, gameModeState);

		world.execute(() -> {
			EventTitleUtil.showEventTitleToWorld(
					Message.translation(ROUND_ABOUT_TO_START_MSG.get())
							.param("time", config.get().getTimeBeforeRoundInSeconds()),
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					world.getEntityStore().getStore()
			);

			// TODO: Ver si se puede quitar esto
			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), gameModeState);

			executor.schedule(() -> {
						// Check if world is still alive before executing (prevents memory leak from stale references)
						if (!world.isAlive()) return;
						if (!canStartNewRound(gameModeState, world)) return;
						GameModeSystem.INSTANCE.doAtRoundStart(world, gameModeState);
					},
					config.get().getTimeBeforeRoundInSeconds(),
					TimeUnit.SECONDS
			);
		});
	}
}
