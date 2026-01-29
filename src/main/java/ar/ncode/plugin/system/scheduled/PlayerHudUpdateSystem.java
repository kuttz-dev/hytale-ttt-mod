package ar.ncode.plugin.system.scheduled;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.GameModeState;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;

public class PlayerHudUpdateSystem extends EntityTickingSystem<EntityStore> {

	private static void updatePlayerHud(PlayerGameModeInfo playerInfo) {
		if (playerInfo != null) {
			playerInfo.getHud().update();
		}
	}

	@Override
	public void tick(float dt, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
	                 @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer
	) {
		World world = store.getExternalData().getWorld();
		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

		if (gameModeState != null && RoundState.IN_GAME.equals(gameModeState.roundState)) {
			PlayerGameModeInfo playerInfo = archetypeChunk.getComponent(index, PlayerGameModeInfo.componentType);

			if (playerInfo == null || playerInfo.getHud() == null) {
				return;
			}

			playerInfo.setElapsedTimeSinceLastUpdate(playerInfo.getElapsedTimeSinceLastUpdate() + dt);

			if (playerInfo.getElapsedTimeSinceLastUpdate() >= 1) {
				updatePlayerHud(playerInfo);
				playerInfo.setElapsedTimeSinceLastUpdate(0);
			}
		}
	}

	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(Player.getComponentType(), PlayerGameModeInfo.componentType);
	}
}
