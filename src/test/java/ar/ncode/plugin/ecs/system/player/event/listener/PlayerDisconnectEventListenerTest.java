package ar.ncode.plugin.ecs.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.config.CustomConfig;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static ar.ncode.plugin.config.CustomConfig.INNOCENT_ROLE;
import static ar.ncode.plugin.config.CustomConfig.TRAITOR_ROLE;
import static ar.ncode.plugin.ecs.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;

class PlayerDisconnectEventListenerTest {

    @BeforeAll
    static void setUp() {
        Config<CustomConfig> mockConfig = Mockito.mock(Config.class);
        Mockito.when(mockConfig.get()).thenReturn(new CustomConfig());
        TroubleInTrorkTownPlugin.config = mockConfig;
    }

    @Test
    void processInGameDisconnectEndsRoundWhenLastTraitorLeaves() {
        UUID traitorId = UUID.randomUUID();
        UUID innocentId = UUID.randomUUID();
        GameModeState gameModeState = new GameModeState();
        gameModeState.updateRoundState(ar.ncode.plugin.model.enums.RoundState.IN_GAME);
        gameModeState.traitorsAlive.add(traitorId);
        gameModeState.innocentsAlive.add(innocentId);

        PlayerRef playerRef = Mockito.mock(PlayerRef.class);
        Mockito.when(playerRef.getUuid()).thenReturn(traitorId);
        Mockito.when(playerRef.getUsername()).thenReturn("traitor");

        Ref<EntityStore> reference = Mockito.mock();
        Store<EntityStore> store = Mockito.mock();
        PlayerGameModeInfo playerInfo = Mockito.mock(PlayerGameModeInfo.class);
        Mockito.when(reference.getStore()).thenReturn(store);
        Mockito.when(store.getComponent(reference, PlayerGameModeInfo.componentType)).thenReturn(playerInfo);
        Mockito.when(playerInfo.getCurrentRoundRole()).thenReturn(TRAITOR_ROLE);

        var graveStone = PlayerDisconnectEventListener.processInGameDisconnect(gameModeState, playerRef, store, reference);

        Assertions.assertTrue(gameModeState.traitorsAlive.isEmpty());
        Assertions.assertTrue(gameModeState.innocentsAlive.contains(innocentId));
        Assertions.assertTrue(roundShouldEnd(gameModeState));
        Assertions.assertEquals("traitor", graveStone.getDeadPlayerName());
        Assertions.assertEquals(TRAITOR_ROLE, graveStone.getDeadPlayerRole());
    }

    @Test
    void processInGameDisconnectEndsRoundWhenLastInnocentLeaves() {
        UUID traitorId = UUID.randomUUID();
        UUID innocentId = UUID.randomUUID();
        GameModeState gameModeState = new GameModeState();
        gameModeState.updateRoundState(ar.ncode.plugin.model.enums.RoundState.IN_GAME);
        gameModeState.traitorsAlive.add(traitorId);
        gameModeState.innocentsAlive.add(innocentId);

        PlayerRef playerRef = Mockito.mock(PlayerRef.class);
        Mockito.when(playerRef.getUuid()).thenReturn(innocentId);
        Mockito.when(playerRef.getUsername()).thenReturn("innocent");

        Ref<EntityStore> reference = Mockito.mock();
        Store<EntityStore> store = Mockito.mock();
        PlayerGameModeInfo playerInfo = Mockito.mock(PlayerGameModeInfo.class);
        Mockito.when(reference.getStore()).thenReturn(store);
        Mockito.when(store.getComponent(reference, PlayerGameModeInfo.componentType)).thenReturn(playerInfo);
        Mockito.when(playerInfo.getCurrentRoundRole()).thenReturn(INNOCENT_ROLE);

        var graveStone = PlayerDisconnectEventListener.processInGameDisconnect(gameModeState, playerRef, store, reference);

        Assertions.assertTrue(gameModeState.innocentsAlive.isEmpty());
        Assertions.assertTrue(gameModeState.traitorsAlive.contains(traitorId));
        Assertions.assertTrue(roundShouldEnd(gameModeState));
        Assertions.assertEquals("innocent", graveStone.getDeadPlayerName());
        Assertions.assertEquals(INNOCENT_ROLE, graveStone.getDeadPlayerRole());
    }
}
