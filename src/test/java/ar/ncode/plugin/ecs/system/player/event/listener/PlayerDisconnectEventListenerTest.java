package ar.ncode.plugin.ecs.system.player.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.config.CustomConfig;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.UUID;

import static ar.ncode.plugin.config.CustomConfig.INNOCENT_ROLE;
import static ar.ncode.plugin.config.CustomConfig.TRAITOR_ROLE;
import static ar.ncode.plugin.ecs.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;

class PlayerDisconnectEventListenerTest {

    @BeforeAll
    static void setUp() throws Exception {
        Config<CustomConfig> mockConfig = Mockito.mock(Config.class);
        CustomConfig customConfig = Mockito.spy(new CustomConfig());
        Mockito.doReturn(false).when(customConfig).playersLeaveRemainsWhenDie();
        Mockito.when(mockConfig.get()).thenReturn(customConfig);
        TroubleInTrorkTownPlugin.config = mockConfig;

        EntityModule entityModule = Mockito.mock(EntityModule.class);
        Mockito.when(entityModule.getPlayerComponentType()).thenReturn(new ComponentType<>());
        setStaticField(EntityModule.class, "instance", entityModule);
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    void processInGameDisconnectEndsRoundWhenLastTraitorLeaves() {
        UUID traitorId = UUID.randomUUID();
        UUID innocentId = UUID.randomUUID();
        GameModeState gameModeState = new GameModeState();
        gameModeState.updateRoundState(ar.ncode.plugin.model.enums.RoundState.IN_GAME);
        gameModeState.traitorsAlive.add(traitorId);
        gameModeState.innocentsAlive.add(innocentId);

        Ref<EntityStore> reference = Mockito.mock();
        Store<EntityStore> store = Mockito.mock();
        World world = Mockito.mock(World.class);
        Player player = Mockito.mock(Player.class);
        PlayerRef playerRef = Mockito.mock(PlayerRef.class);
        PlayerGameModeInfo playerInfo = Mockito.mock(PlayerGameModeInfo.class);

        Mockito.when(playerRef.getUuid()).thenReturn(traitorId);
        Mockito.when(playerRef.getUsername()).thenReturn("traitor");
        Mockito.when(reference.getStore()).thenReturn(store);
        Mockito.when(store.getComponent(reference, Player.getComponentType())).thenReturn(player);
        Mockito.when(store.getComponent(reference, PlayerGameModeInfo.componentType)).thenReturn(playerInfo);
        Mockito.when(player.getDisplayName()).thenReturn("traitor");
        Mockito.when(playerInfo.getCurrentRoundRole()).thenReturn(TRAITOR_ROLE);
        Mockito.when(playerInfo.getCredits()).thenReturn(1);

        PlayerDisconnectEventListener.processInGameDisconnect(gameModeState, playerRef, store, reference, world);

        Assertions.assertTrue(gameModeState.traitorsAlive.isEmpty());
        Assertions.assertTrue(gameModeState.innocentsAlive.contains(innocentId));
        Assertions.assertTrue(roundShouldEnd(gameModeState));
        Assertions.assertEquals(-100, gameModeState.karmaUpdates.get(traitorId));
    }

    @Test
    void processInGameDisconnectEndsRoundWhenLastInnocentLeaves() {
        UUID traitorId = UUID.randomUUID();
        UUID innocentId = UUID.randomUUID();
        GameModeState gameModeState = new GameModeState();
        gameModeState.updateRoundState(ar.ncode.plugin.model.enums.RoundState.IN_GAME);
        gameModeState.traitorsAlive.add(traitorId);
        gameModeState.innocentsAlive.add(innocentId);

        Ref<EntityStore> reference = Mockito.mock();
        Store<EntityStore> store = Mockito.mock();
        World world = Mockito.mock(World.class);
        Player player = Mockito.mock(Player.class);
        PlayerRef playerRef = Mockito.mock(PlayerRef.class);
        PlayerGameModeInfo playerInfo = Mockito.mock(PlayerGameModeInfo.class);

        Mockito.when(playerRef.getUuid()).thenReturn(innocentId);
        Mockito.when(playerRef.getUsername()).thenReturn("innocent");
        Mockito.when(reference.getStore()).thenReturn(store);
        Mockito.when(store.getComponent(reference, Player.getComponentType())).thenReturn(player);
        Mockito.when(store.getComponent(reference, PlayerGameModeInfo.componentType)).thenReturn(playerInfo);
        Mockito.when(player.getDisplayName()).thenReturn("innocent");
        Mockito.when(playerInfo.getCurrentRoundRole()).thenReturn(INNOCENT_ROLE);
        Mockito.when(playerInfo.getCredits()).thenReturn(0);

        PlayerDisconnectEventListener.processInGameDisconnect(gameModeState, playerRef, store, reference, world);

        Assertions.assertTrue(gameModeState.innocentsAlive.isEmpty());
        Assertions.assertTrue(gameModeState.traitorsAlive.contains(traitorId));
        Assertions.assertTrue(roundShouldEnd(gameModeState));
        Assertions.assertEquals(-100, gameModeState.karmaUpdates.get(innocentId));
    }
}
