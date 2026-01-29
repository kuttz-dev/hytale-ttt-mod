package ar.ncode.plugin.system;

import ar.ncode.plugin.WorldAccessors;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;

public class GraveSystem {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public static void spawnGraveAtPlayerDeath(World world, GraveStoneWithNameplate graveStone,
	                                           Ref<EntityStore> reference
	) {
		try {

			TransformComponent transform = reference.getStore().getComponent(reference,
					TransformComponent.getComponentType());

			if (transform == null) {
				return;
			}

			Vector3i emptyPosition = findEmptyPlaceNearPosition(world, transform.getPosition(), 5);

			if (emptyPosition == null) {
				return;
			}

			graveStone.setGraveAndNameplatePosition(emptyPosition);
			gameModeStateForWorld.get(world.getWorldConfig().getUuid()).addGraveStone(graveStone);

			long chunkIndex = ChunkUtil.indexChunkFromBlock(emptyPosition.x, emptyPosition.z);
			WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

			if (chunk == null) {
				world.getChunkAsync(chunkIndex).thenAccept(worldChunk -> {
					if (worldChunk == null) {
						return;
					}

					addBlockAndNameplateToWorld(world, worldChunk, emptyPosition, graveStone, transform);
				});
				return;
			}

			world.execute(() -> addBlockAndNameplateToWorld(world, chunk, emptyPosition, graveStone, transform));
		} catch (Exception exception) {
			LOGGER.atSevere().log("Could not create gravestone for player, exception: {}", exception);
		}
	}

	private static void addBlockAndNameplateToWorld(World world, WorldChunk worldChunk, Vector3i emptyPosition, GraveStoneWithNameplate graveStone, TransformComponent transform) {
		boolean result = setBlockWithRotation(worldChunk,
				emptyPosition.x, emptyPosition.y, emptyPosition.z,
				config.get().getPlayerGraveId(), 0
		);

		if (result) {
			var blockRef = WorldAccessors.getBlockEntityRefAt(world, emptyPosition);
			if (blockRef == null) {
				return;
			}

			blockRef.getStore().tryRemoveComponent(blockRef, GraveStoneWithNameplate.componentType);
			blockRef.getStore().addComponent(blockRef, GraveStoneWithNameplate.componentType, graveStone);
			addNameplateToGrave(world, graveStone, transform.getRotation());
		}
	}

	private static void addNameplateToGrave(World world, GraveStoneWithNameplate graveStone,
	                                        Vector3f rotation) {
		Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
		ProjectileComponent projectileComponent = new ProjectileComponent("Projectile");

		TransformComponent transformComponent = new TransformComponent(graveStone.getNamePlatePosition(), rotation.clone());
		holder.putComponent(TransformComponent.getComponentType(), transformComponent);
		holder.putComponent(ProjectileComponent.getComponentType(), projectileComponent);
		holder.ensureComponent(UUIDComponent.getComponentType());

		if (projectileComponent.getProjectile() == null) {
			projectileComponent.initialize();
		}

		holder.addComponent(NetworkId.getComponentType(), new NetworkId(world.getEntityStore().getStore().getExternalData().takeNextNetworkId()));
		holder.addComponent(Nameplate.getComponentType(), new Nameplate(graveStone.getDeadPlayerName()));

		graveStone.setNamePlateReference(
				world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN)
		);
	}

	public static boolean setBlockWithRotation(WorldChunk chunk, int x, int y, int z, String blockName, int rotationIndex) {
		int blockIndex = BlockType.getAssetMap().getIndex(blockName);
		if (blockIndex == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Unknown key! " + blockName);
		}

		BlockType asset = BlockType.getAssetMap().getAsset(blockIndex);
		return chunk.setBlock(x, y, z, blockIndex, asset, rotationIndex, 0, 0);
	}

	private static Vector3i findEmptyPlaceNearPosition(World world, Vector3d vec, int radius) {
		Vector3i position = new Vector3i((int) vec.x, (int) vec.y, (int) vec.z);

		if (world.getBlock(position.x, position.y, position.z) == 0) {
			return new Vector3i(position.x, position.y, position.z);
		}

		for (int r = 1; r <= radius; ++r) {
			for (int dx = -r; dx <= r; ++dx) {
				for (int dy = -r; dy <= r; ++dy) {
					for (int dz = -r; dz <= r; ++dz) {
						int x = position.x + dx;
						int y = position.y + dy;
						int z = position.z + dz;
						if (world.getBlock(x, y, z) == 0) {
							return new Vector3i(x, y, z);
						}
					}
				}
			}
		}

		return null;
	}
}
