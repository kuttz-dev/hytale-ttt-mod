package ar.ncode.plugin.ecs.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.ecs.commands.ChangeWorldCommand;
import ar.ncode.plugin.ecs.commands.SpectatorMode;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import ar.ncode.plugin.ecs.component.death.ConfirmedDeath;
import ar.ncode.plugin.ecs.component.death.LostInCombat;
import ar.ncode.plugin.ecs.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.ecs.system.player.PlayerDeathSystem;
import ar.ncode.plugin.ecs.system.scheduled.DoubleTapDetector;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.ecs.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;
import static ar.ncode.plugin.ecs.system.player.PlayerDeathSystem.spawnDeadPlayerRemains;
import static ar.ncode.plugin.model.enums.TranslationKey.THERE_ARE_NOT_ENOUGH_PLAYERS;

public class PlayerDisconnectEventListener implements Consumer<PlayerDisconnectEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    static void processInGameDisconnect(GameModeState gameModeState, PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> reference, World world) {
        int value = config.get().getKarmaForDisconnectingMiddleRound();
        gameModeState.karmaUpdates.merge(playerRef.getUuid(), value, Integer::sum);

        PlayerGameModeInfo playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);
        if (playerInfo != null) {
            PlayerDeathSystem.updatePlayerCountsOnPlayerDeath(playerRef, playerInfo.getCurrentRoundRole(), gameModeState);
        }

        var playerComponent = store.getComponent(reference, Player.getComponentType());
        var player = new PlayerComponents(playerComponent, playerRef, playerInfo, reference);
        spawnDeadPlayerRemains(gameModeState, player, world, store, DamageCause.DISCONNECTED);

    }

    private static void finishRound(World world) {
        HytaleServer.get().getEventBus()
                .dispatchForAsync(FinishCurrentRoundEvent.class)
                .dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
    }

    private static void notEnoughPlayersLogic(Store<EntityStore> store, World world) {
        Message message = Message.translation(THERE_ARE_NOT_ENOUGH_PLAYERS.get());
        EventTitleUtil.showEventTitleToWorld(
                message,
                Message.raw(""),
                true, "ui/icons/EntityStats/Sword_Icon.png",
                4.0f, 1.5f, 1.5f,
                store
        );

        finishRound(world);
    }

    @Override
    public void accept(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        Ref<EntityStore> reference = playerRef.getReference();
        if (reference == null || !reference.isValid()) {
            return;
        }

        // Remove component from DoubleTapDetector to prevent memory leak
        DoubleTapDetector.getInstance().removePlayer(playerRef.getUuid());

        Store<EntityStore> store = reference.getStore();
        World world = store.getExternalData().getWorld();

        if (world.getPlayerCount() == 0) {
            TroubleInTrorkTownPlugin.currentInstance = null;
            ChangeWorldCommand.cleanUpOldWorld(world.getWorldConfig().getUuid());
        }

        GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

        if (gameModeState == null) {
            return;
        }

        // Remove from spectator tracking
        gameModeState.spectators.remove(playerRef.getUuid());
        gameModeState.traitorsAlive.remove(playerRef.getUuid());
        gameModeState.innocentsAlive.remove(playerRef.getUuid());

        world.execute(() -> {
            if (!reference.isValid()) return;
            var playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);

            SpectatorMode.disableSpectatorModeForPlayer(new PlayerComponents(null, playerRef, playerInfo, reference), reference.getStore());
            store.removeComponentIfExists(reference, LostInCombat.componentType);
            store.removeComponentIfExists(reference, ConfirmedDeath.componentType);

            // If the player disconnects while dead, make sure we don't persist a "not alive" state
            // into their next session (vanilla PlayerAddedSystem will warn loudly about it).
            store.removeComponentIfExists(reference, DeferredCorpseRemoval.getComponentType());
            store.removeComponentIfExists(reference, DeathComponent.getComponentType());

            if (RoundState.IN_GAME.equals(gameModeState.getRoundState())) {
                LOGGER.atInfo().log("Player %s disconnected during the round. Processing as in-game disconnect.", playerRef.getUsername());
                processInGameDisconnect(gameModeState, playerRef, store, reference, world);
            }
        });

        if (RoundState.IN_GAME.equals(gameModeState.getRoundState()) && roundShouldEnd(gameModeState)) {
            finishRound(world);
        }
    }
}
