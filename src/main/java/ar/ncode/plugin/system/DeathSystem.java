package ar.ncode.plugin.system;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.component.DeadPlayerGravestoneComponent;
import ar.ncode.plugin.component.DeadPlayerInfoComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;

public class DeathSystem {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public static void spawnRemainsAtPlayerDeath(World world, DeadPlayerInfoComponent graveStone,
	                                             Ref<EntityStore> reference, ComponentAccessor<EntityStore> store
	) {
		if (!config.get().playersLeaveRemainsWhenDie()) {
			return;
		}

		try {
			TransformComponent transform = store.getComponent(reference, TransformComponent.getComponentType());
			if (transform == null) {
				return;
			}

			TransformComponent newTransform = transform.clone();
			Vector3i emptyPosition = findEmptyPlaceNearPosition(world, newTransform.getPosition(), 5);

			if (emptyPosition == null) {
				return;
			}

			graveStone.setGraveAndNameplatePosition(emptyPosition);
			graveStone.setRotation(newTransform.getRotation());

			long chunkIndex = ChunkUtil.indexChunkFromBlock(emptyPosition.x, emptyPosition.z);
			WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

			if (chunk == null) {
				world.getChunkAsync(chunkIndex).thenAccept(worldChunk -> {
					if (worldChunk == null) {
						return;
					}

					addPlayerRemainsToWorld(world, worldChunk, graveStone, reference, store);
				});
				return;
			}

			world.execute(() -> addPlayerRemainsToWorld(world, chunk, graveStone, reference, store));
		} catch (Exception exception) {
			LOGGER.atSevere().log("Could not create gravestone for component, exception: {}", exception);
		}
	}

	private static void addPlayerRemainsToWorld(World world, WorldChunk worldChunk, DeadPlayerInfoComponent deadPlayerInfo, Ref<EntityStore> reference, ComponentAccessor<EntityStore> store) {
		if (config.get().playersLeaveGravestonesWhenDie()) {
			addGraveStoneWithNamePlateToWorld(world, worldChunk, deadPlayerInfo);

		} else {
			addCorpseToWorld(world, deadPlayerInfo, reference, store);
		}
	}

	private static void addCorpseToWorld(World world, DeadPlayerInfoComponent deadPlayerInfo, Ref<EntityStore> reference, ComponentAccessor<EntityStore> store) {
		TransformComponent transformComponent = new TransformComponent(deadPlayerInfo.getPosition().toVector3d(), deadPlayerInfo.getRotation());
		Model newModel = Model.createScaledModel(ModelAsset.getAssetMap().getAsset("Player"), 1.0F);

		Pair<Ref<EntityStore>, NPCEntity> pair = NPCPlugin.get()
				.spawnEntity(
						world.getEntityStore().getStore(), NPCPlugin.get().getIndex("DeadCorpse"),
						transformComponent.getPosition(), transformComponent.getRotation(), newModel, (TriConsumer) null
				);

		if (pair == null) {
			return;
		}

		Ref<EntityStore> newEntityRef = pair.first();
		gameModeStateForWorld.get(world.getWorldConfig().getUuid()).corpses.add(newEntityRef);
		store.addComponent(newEntityRef, DeadPlayerInfoComponent.componentType, deadPlayerInfo);

		if (reference.isValid()) {

			PlayerSkinComponent playerSkinComponent = store.getComponent(reference, PlayerSkinComponent.getComponentType());

			if (playerSkinComponent != null) {
				PlayerSkinComponent skinComp = new PlayerSkinComponent(playerSkinComponent.getPlayerSkin().clone());
				store.addComponent(newEntityRef, PlayerSkinComponent.getComponentType(), skinComp);
				skinComp.setNetworkOutdated();
			}
		}
	}

	private static void addGraveStoneWithNamePlateToWorld(World world, WorldChunk worldChunk, DeadPlayerInfoComponent deadPlayerInfo) {
		boolean result = setBlockWithRotation(worldChunk,
				deadPlayerInfo.getPosition().x, deadPlayerInfo.getPosition().y, deadPlayerInfo.getPosition().z,
				config.get().getPlayerGraveId(), 0
		);

		if (result) {
			gameModeStateForWorld.get(world.getWorldConfig().getUuid()).addGraveStone(deadPlayerInfo);

			var blockRef = WorldAccessors.getBlockEntityRefAt(world, deadPlayerInfo.getPosition());
			if (blockRef == null) {
				return;
			}

			blockRef.getStore().tryRemoveComponent(blockRef, DeadPlayerGravestoneComponent.componentType);
			blockRef.getStore().addComponent(blockRef, DeadPlayerGravestoneComponent.componentType, new DeadPlayerGravestoneComponent(deadPlayerInfo));

			if (config.get().gravestonesHaveNameplates()) {
				addNameplateToGrave(world, deadPlayerInfo);
			}
		}
	}

	private static void addNameplateToGrave(World world, DeadPlayerInfoComponent graveStone) {
		Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
		ProjectileComponent projectileComponent = new ProjectileComponent("Projectile");

		TransformComponent transformComponent = new TransformComponent(graveStone.getNamePlatePosition(), graveStone.getRotation());
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

	public static Vector3i findEmptyPlaceNearPosition(World world, Vector3d vec, int radius) {
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
