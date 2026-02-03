package ar.ncode.plugin.system;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.config.WeaponTypeConfig;
import ar.ncode.plugin.model.enums.PlayerRole;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.weaponTypesConfig;
import static com.hypixel.hytale.common.util.ArrayUtil.contains;

@Getter
public class ItemPickUpSystem extends EntityTickingSystem<EntityStore> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.BEFORE,
			PlayerItemEntityPickupSystem.class, OrderPriority.NORMAL));

	@Override
	public void tick(
			float dt,
			int index,
			@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer
	) {
		Ref<EntityStore> itemRef = archetypeChunk.getReferenceTo(index);
		ItemComponent itemComponent = archetypeChunk.getComponent(index, ItemComponent.getComponentType());
		assert itemComponent != null;

		if (!itemRef.isValid() || itemComponent.getItemStack() == null || itemComponent.getItemStack().isEquivalentType(ItemStack.EMPTY)) {
			return;
		}

		TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
		assert transform != null;

		Vector3d itemEntityPosition = transform.getPosition();
		float pickupRadius = itemComponent.getPickupRadius(commandBuffer);
		Vector3d targetPosition = transform.getPosition();
		double distance = targetPosition.distanceTo(itemEntityPosition);
		if (distance > pickupRadius) {
			return;
		}

		SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
				store.getResource(EntityModule.get().getPlayerSpatialResourceType());
		SpatialStructure<Ref<EntityStore>> spatialStructure = playerSpatialResource.getSpatialStructure();

		Ref<EntityStore> closest = spatialStructure.closest(itemEntityPosition);
		if (closest == null || !closest.isValid()) {
			return;
		}

		var world = closest.getStore().getExternalData().getWorld();
//		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
//		if (gameModeState == null || !gameModeState.items.contains(itemRef)) {
//			return;
//		}


		world.execute(() -> {
			var playerInfo = closest.getStore().getComponent(closest, PlayerGameModeInfo.componentType);
			var player = closest.getStore().getComponent(closest, Player.getComponentType());

			if (playerInfo == null || player == null) {
				return;
			}

			var isCurrentlyPreventingPickup = commandBuffer.getComponent(closest, PreventPickup.getComponentType()) != null;

			try {
				if (PlayerRole.SPECTATOR.equals(playerInfo.getRole()) && !isCurrentlyPreventingPickup) {
					commandBuffer.ensureComponent(itemRef, PreventPickup.getComponentType());
					return;

				} else if (PlayerRole.SPECTATOR.equals(playerInfo.getRole())) {
					return;
				}

			} catch (Exception e) {
				LOGGER.atSevere().log("Error on item pickup handler", e);
			}


			Optional<WeaponTypeConfig> config = weaponTypesConfig.get().getByItemId(itemComponent.getItemStack().getItemId());
			if (config.isEmpty()) {
				// By default, all items should be blocked from picking up
				commandBuffer.ensureComponent(itemRef, PreventPickup.getComponentType());
				return;
			}

			boolean shouldPreventPickup = false;

			Map<String, Integer> map = new HashMap<>();
			map.put(config.get().getTypeId(), 1);

			ItemContainer storage = player.getInventory().getCombinedStorageFirst();
			for (short slot = 0; slot < storage.getCapacity(); slot++) {
				ItemStack itemStack = storage.getItemStack(slot);

				if (itemStack == null || ItemStack.EMPTY.isEquivalentType(itemStack)) {
					continue;
				}

				if (contains(config.get().getItemIds(), itemStack.getItemId())) {
					map.compute(
							config.get().getTypeId(),
							(k, v) -> v == null ? itemStack.getQuantity() : v + itemStack.getQuantity()
					);
				}

				if (map.get(config.get().getTypeId()) > config.get().getAllowedItemsOfSameType()) {
					shouldPreventPickup = true;
					break;
				}
			}

			try {
				if (shouldPreventPickup) {
					commandBuffer.ensureComponent(itemRef, PreventPickup.getComponentType());

				} else {
					commandBuffer.tryRemoveComponent(itemRef, PreventPickup.getComponentType());
				}

			} catch (Exception e) {
				LOGGER.atSevere().log("Error on item pickup handler", e);
			}

		});
	}

	@NullableDecl
	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(
				ItemComponent.getComponentType(),
				TransformComponent.getComponentType(),
				Query.not(Interactable.getComponentType()),
				Query.not(PickupItemComponent.getComponentType())
		);
	}
}
