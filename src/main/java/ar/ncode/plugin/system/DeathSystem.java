package ar.ncode.plugin.system;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.component.DeadPlayerGravestoneComponent;
import ar.ncode.plugin.component.DeadPlayerInfoComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
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
			world.execute(() -> {
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

				addPlayerRemainsToWorld(world, chunk, graveStone, reference, store);
			});

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
		TransformComponent transformComponent = new TransformComponent(
				deadPlayerInfo.getPosition().toVector3d().clone(),
				deadPlayerInfo.getRotation().clone()
		);

		ModelAsset playerModelAsset = ModelAsset.getAssetMap().getAsset("Player");
		ModelAsset modelWithDeathAnimation = ModelAsset.getAssetMap().getAsset("Player_Corpse_Model");
		Model npcModel = null;

		if (playerModelAsset != null && modelWithDeathAnimation != null) {
			Model playerModel = Model.createScaledModel(playerModelAsset, 1.0F, null);
			npcModel = addAnimationsToExistingModel(playerModel, modelWithDeathAnimation);

		} else if (playerModelAsset != null) {
			npcModel = Model.createScaledModel(playerModelAsset, 1.0F);
		}

		addNpcToWorld(
				world, transformComponent, npcModel,
				((npc, newEntityRef, scopeStore) -> {
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

					npc.setDespawning(false);
					npc.setDespawnCheckRemainingSeconds(Float.MAX_VALUE);
					AnimationUtils.playAnimation(newEntityRef, AnimationSlot.Status, "Dead", false, store);
				})
		);
	}

	private static void addNpcToWorld(World world, TransformComponent transformComponent, Model npcModel, TriConsumer<NPCEntity, Ref<EntityStore>, Store<EntityStore>> postSpawn) {
		world.execute(() -> {
			var result = NPCPlugin.get()
					.spawnEntity(
							world.getEntityStore().getStore(),
							NPCPlugin.get().getIndex("Player_Dead_Corpse"),
							transformComponent.getPosition(), transformComponent.getRotation(), npcModel,
							postSpawn
					);

			if (result == null) {
				LOGGER.atSevere().log("Error adding corpse to world");
			}
		});
	}

	private static Model addAnimationsToExistingModel(Model playerModel, ModelAsset corpseModelAsset) {
		return new Model(
				playerModel.getModelAssetId(),
				playerModel.getScale(),
				playerModel.getRandomAttachmentIds(),
				playerModel.getAttachments(),
				playerModel.getBoundingBox(),
				playerModel.getModel(),
				playerModel.getTexture(),
				playerModel.getGradientSet(),
				playerModel.getGradientId(),
				playerModel.getEyeHeight(),
				playerModel.getCrouchOffset(),
				playerModel.getSittingOffset(),
				playerModel.getSleepingOffset(),
				corpseModelAsset.getAnimationSetMap(),
				playerModel.getCamera(),
				playerModel.getLight(),
				playerModel.getParticles(),
				playerModel.getTrails(),
				playerModel.getPhysicsValues(),
				playerModel.getDetailBoxes(),
				playerModel.getPhobia(),
				playerModel.getPhobiaModelAssetId()
		);
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
