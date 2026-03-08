package ar.ncode.plugin.ecs.system.event.handler;

import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.ecs.system.GameModeSystem;
import ar.ncode.plugin.ecs.system.event.StartNewRoundEvent;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.spawning.local.LocalSpawnController;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.enums.RoleGroup.TRAITOR;
import static ar.ncode.plugin.model.enums.RoundState.PREPARING;
import static ar.ncode.plugin.model.enums.TranslationKey.PLAYER_ASSIGNED_ROLE_NOTIFICATION;
import static ar.ncode.plugin.model.enums.TranslationKey.ROUND_ABOUT_TO_START_MSG;

public class StartNewRoundEventHandler implements Consumer<StartNewRoundEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private boolean isRoundStarting = false;

    public static boolean canStartNewRound(GameModeState gameModeState, World world) {
        return PREPARING.equals(gameModeState.getRoundState()) && world.getPlayerCount() >= config.get().getRequiredPlayersToStartRound();
    }

    public static void updateEachPlayer(List<PlayerComponents> players) {
        for (var player : players) {
            CustomRole currentRoundRole = player.info().getCurrentRoundRole();
            if (currentRoundRole == null) {
                continue;
            }

            Ref<EntityStore> reference = player.reference();

            EntityStatMap stats = reference.getStore().getComponent(reference, EntityStatMap.getComponentType());
            if (stats == null) continue;

            // Inventory
            var itemGroups = config.get().getItems(currentRoundRole.getStartingItems());
            addItemsToPlayer(itemGroups, player.component().getInventory().getCombinedHotbarFirst());

            // GUI
            player.component().getPageManager().setPage(reference, reference.getStore(), Page.None);
            player.info().getHud().update();

            // Remove effects
            stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());

            // Froce respawn on player
            DeathComponent deathComponent = reference.getStore().getComponent(reference, DeathComponent.getComponentType());
            if (deathComponent != null) {
                LocalSpawnController spawnController = reference.getStore()
                        .ensureAndGetComponent(reference, LocalSpawnController.getComponentType());

                spawnController.setTimeToNextRunSeconds(0);
            }

            NotificationStyle notificationStyle;
            if (TRAITOR.equals(currentRoundRole.getRoleGroup())) {
                notificationStyle = NotificationStyle.Danger;

            } else {
                notificationStyle = NotificationStyle.Success;
            }

            player.info().setCredits(currentRoundRole.getStartingCredits());


            NotificationUtil.sendNotification(
                    player.refComponent().getPacketHandler(),
                    Message.translation(PLAYER_ASSIGNED_ROLE_NOTIFICATION.get())
                            .param("role", Message.translation(currentRoundRole.getTranslationKey())),
                    notificationStyle
            );
        }
    }

    private static void addItemsToPlayer(List<ItemStack> items, ItemContainer container) {
        for (ItemStack itemStack : items) {
            container.addItemStack(itemStack);
        }
    }

    @Override
    public void accept(StartNewRoundEvent startNewRoundEvent) {
        LOGGER.atInfo().log("Received StartNewRoundEvent for world %s", startNewRoundEvent.getWorldUUID());

        if (isRoundStarting) {
            LOGGER.atInfo().log("Round is already starting for world %s, ignoring event", startNewRoundEvent.getWorldUUID());
            return;
        }

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

        isRoundStarting = true;
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
                        isRoundStarting = false;
                    },
                    config.get().getTimeBeforeRoundInSeconds(),
                    TimeUnit.SECONDS
            );
        });
    }
}
