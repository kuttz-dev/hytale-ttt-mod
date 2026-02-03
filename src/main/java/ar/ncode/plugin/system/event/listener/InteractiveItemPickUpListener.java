package ar.ncode.plugin.system.event.listener;

import ar.ncode.plugin.config.DebugConfig;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.logging.Level;

public class InteractiveItemPickUpListener extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public InteractiveItemPickUpListener() {
		super(InteractivelyPickupItemEvent.class);
	}

	@Override
	public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
	                   @NonNullDecl InteractivelyPickupItemEvent event) {
		if (DebugConfig.INSTANCE.isCanPlaceAndDestroyBlocks()) {
			return;
		}

		// By default, block players from breaking blocks
		event.setCancelled(true);

		try {
			event.setItemStack(ItemStack.EMPTY);
		} catch (Exception e) {
			LOGGER.at(Level.WARNING).log("Could not set empty item stack: %s", e.getMessage());
		}
	}

	@NullableDecl
	@Override
	public Query<EntityStore> getQuery() {
		return Archetype.empty();
	}
}
