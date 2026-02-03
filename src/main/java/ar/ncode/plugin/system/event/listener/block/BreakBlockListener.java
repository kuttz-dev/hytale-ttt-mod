package ar.ncode.plugin.system.event.listener.block;

import ar.ncode.plugin.config.DebugConfig;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BreakBlockListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {

	public BreakBlockListener() {
		super(BreakBlockEvent.class);
	}

	@Override
	public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent breakBlockEvent) {
		if (DebugConfig.INSTANCE.isCanPlaceAndDestroyBlocks()) {
			return;
		}

		// By default, block players from breaking blocks
		breakBlockEvent.setCancelled(true);
	}

	@NullableDecl
	@Override
	public Query<EntityStore> getQuery() {
		return Archetype.empty();
	}
}
