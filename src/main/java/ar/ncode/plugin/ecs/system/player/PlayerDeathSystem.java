package ar.ncode.plugin.ecs.system.player;

import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.ecs.commands.SpectatorMode;
import ar.ncode.plugin.ecs.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.ecs.component.death.LostInCombat;
import ar.ncode.plugin.ecs.system.DeathSystem;
import ar.ncode.plugin.ecs.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoleGroup;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Set;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.accessors.PlayerAccessors.getPlayerFrom;
import static ar.ncode.plugin.ecs.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;

@Getter
public class PlayerDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.BEFORE,
            DeathSystems.PlayerDeathScreen.class, OrderPriority.FURTHEST));

    public static void updatePlayerCountsOnPlayerDeath(PlayerRef playerRef, CustomRole role, GameModeState gameModeState) {
        gameModeState.spectators.add(playerRef.getUuid());

        if (role == null) {
            return;
        }

        if (RoleGroup.TRAITOR.equals(role.getRoleGroup())) {
            gameModeState.traitorsAlive.remove(playerRef.getUuid());

        } else if (RoleGroup.INNOCENT.equals(role.getRoleGroup())) {
            gameModeState.innocentsAlive.remove(playerRef.getUuid());
        }
    }

    private static int calculateKarmaForAttacker(CustomRole attackerRole, CustomRole attackedRole) {
        int value;

        if (attackerRole.getRoleGroup().equals(attackedRole.getRoleGroup())) {
            value = config.get().getKaramPointsForKillingSameRoleGroup();
        } else {
            value = config.get().getKaramPointsForKillingOppositeRoleGroup();
        }

        if (!attackedRole.isSecretRole()) {
            value = 2 * value;
        }

        return value;
    }

    private static void updateKdaAndKarma(@NonNullDecl DeathComponent deathComponent, PlayerComponents player, GameModeState gameModeState, ComponentAccessor<EntityStore> store) {
        gameModeState.deathsUpdates.put(player.refComponent().getUuid(), 1);

        if (deathComponent.getDeathInfo() == null) {
            return;
        }

        Damage.Source source = deathComponent.getDeathInfo().getSource();

        if (source instanceof Damage.EntitySource attackerRef) {
            var attacker = getPlayerFrom(attackerRef.getRef(), store).orElse(null);
            if (attacker == null) return;

            if (attacker.refComponent() != null && attacker.info() != null) {
                int value = calculateKarmaForAttacker(attacker.info().getCurrentRoundRole(), player.info().getCurrentRoundRole());
                gameModeState.karmaUpdates.merge(attacker.refComponent().getUuid(), value, Integer::sum);
                gameModeState.killUpdates.merge(attacker.refComponent().getUuid(), 1, Integer::sum);
            }
        }
    }

    private static void spawnDeadPlayerRemains(@NonNullDecl DeathComponent deathComponent, GameModeState gameModeState, PlayerComponents player, World world, ComponentAccessor<EntityStore> store) {
        DeadPlayerInfoComponent deadPlayerInfo = DeadPlayerInfoComponent.builder()
                .timeOfDeath(gameModeState.getRemainingTime().format(timeFormatter))
                .deadPlayerReference(player.reference())
                .deadPlayerRole(player.info().getCurrentRoundRole())
                .deadPlayerName(player.component().getDisplayName())
                .credits(player.info().getCredits())
                .build();

        if (deathComponent.getDeathCause() != null) {
            DamageCause damageCause = DamageCause.valueOf(deathComponent.getDeathCause().getId().toUpperCase());
            deadPlayerInfo.setCauseOfDeath(damageCause);
        }

        DeathSystem.spawnRemainsAtPlayerDeath(world, deadPlayerInfo, player.reference(), store);
    }

    private static void forceRespawnForPlayer(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl World world, PlayerComponents player) {
        world.execute(() -> {
            LOGGER.atFine().log("Scheduling respawn for player: " + player.component().getDisplayName() + " - Ref: " + reference);
            CompletableFutureUtil._catch(DeathComponent.respawn(world.getEntityStore().getStore(), reference));
        });
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), Player.getComponentType(),
                TransformComponent.getComponentType(),
                Query.not(LostInCombat.componentType)
        );
    }

    @Override
    public void onComponentAdded(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl DeathComponent deathComponent,
                                 @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Disable death screen
        deathComponent.setShowDeathMenu(false);
        deathComponent.setItemsLossMode(DeathConfig.ItemsLossMode.ALL);
        deathComponent.setItemsDurabilityLossPercentage(0.0F);

        World world = commandBuffer.getExternalData().getWorld();

        LOGGER.atInfo().log("Player died! Ref: " + reference + " - World: " + world.getName() + " - DeathCause: " + deathComponent.getDeathCause());
        // Get reference to the damaged entity
        var player = getPlayerFrom(reference, commandBuffer).orElse(null);
        if (player == null) return;

        GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
        if (gameModeState == null || !RoundState.IN_GAME.equals(gameModeState.getRoundState())) {
            LOGGER.atFine().log("Player died but round is not in IN_GAME state - respawning without processing death: " + reference);
            forceRespawnForPlayer(reference, world, player);
            return;
        }

        LOGGER.atFine().log("Processing player death for player: " + player.component().getDisplayName() + " - Ref: " + reference);
        LOGGER.atFine().log("Ensuring LostInCombat component for player: %s - Ref: ", player.component().getDisplayName(), reference);
        world.execute(() -> world.getEntityStore().getStore().ensureComponent(player.reference(), LostInCombat.componentType));
        LOGGER.atFine().log("Updating player counts for player: " + player.component().getDisplayName() + " - Ref: " + reference);
        updatePlayerCountsOnPlayerDeath(player.refComponent(), player.info().getCurrentRoundRole(), gameModeState);
        LOGGER.atFine().log("Clearing player inventory: " + player.component().getDisplayName() + " - Ref: " + reference);
        player.component().getInventory().clear();
        LOGGER.atFine().log("Updating player hud: " + player.component().getDisplayName() + " - Ref: " + reference);
        var hud = player.info().getHud();
        if (hud != null) {
            hud.update();
        }
        LOGGER.atFine().log("Updating player kda: " + player.component().getDisplayName() + " - Ref: " + reference);
        updateKdaAndKarma(deathComponent, player, gameModeState, commandBuffer);

        if (roundShouldEnd(gameModeState)) {
            LOGGER.atFine().log("Round should end after death of player: " + player.component().getDisplayName() + " - Ref: " + reference);
            HytaleServer.get().getEventBus()
                    .dispatchForAsync(FinishCurrentRoundEvent.class)
                    .dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
        } else {
            LOGGER.atFine().log("Spawning remains and setting spectator mode for player: " + player.component().getDisplayName() + " - Ref: " + reference);
            spawnDeadPlayerRemains(deathComponent, gameModeState, player, world, commandBuffer);
            LOGGER.atInfo().log("Setting spectator mode for player: " + player.component().getDisplayName() + " - Ref: " + reference);
            SpectatorMode.setGameModeToSpectator(player, commandBuffer);
        }

        forceRespawnForPlayer(reference, world, player);
    }

}
