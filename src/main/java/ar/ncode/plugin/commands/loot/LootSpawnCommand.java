package ar.ncode.plugin.commands.loot;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import ar.ncode.plugin.config.loot.IncludedLootItem;
import ar.ncode.plugin.config.loot.LootItem;
import ar.ncode.plugin.config.loot.LootSpawnPoint;
import ar.ncode.plugin.config.loot.LootTable;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.weaponsConfig;
import static ar.ncode.plugin.accessors.WorldAccessors.getWorldNameForInstance;
import static ar.ncode.plugin.system.DeathSystem.findEmptyPlaceNearPosition;

public class LootSpawnCommand extends AbstractCommandCollection {

	public LootSpawnCommand() {
		super("spawn", "Spawn all the configured loot boxes in the world.");
		addSubCommand(new LootForceSpawnCommand());
		addSubCommand(new LootShowSpawnPointsCommand());
		addSubCommand(new LootAddSpawnPositionCommand());

	}

	public static class LootForceSpawnCommand extends AbstractAsyncCommand {

		public LootForceSpawnCommand() {
			super("force", "Add a loot spawn at your current position.");
		}

		private static void addLoot(World world, LootSpawnPoint lootSpawnPoint) {
			for (String lootTableId : lootSpawnPoint.getLootTables()) {
				LootTable lootTable = weaponsConfig.get().getLootTableById(lootTableId);

				if (lootTable == null) {
					continue;
				}

				for (LootItem item : lootTable.getItems()) {
					if (!chance(item.getProbability())) {
						continue;
					}

					Vector3d position = lootSpawnPoint.getSpawnPoint().getPosition().clone();
					position.x += ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
					position.z += ThreadLocalRandom.current().nextDouble(-2.0, 2.0);

					Vector3i emptyPosition = findEmptyPlaceNearPosition(world, position, 3);

					if (emptyPosition == null) {
						continue;
					}

					spawnItemInWorld(world, emptyPosition, lootSpawnPoint.getSpawnPoint().getRotation(), item.getItemId(), item.getAmount());

					for (IncludedLootItem included : item.getIncludes()) {
						spawnItemInWorld(world, emptyPosition, lootSpawnPoint.getSpawnPoint().getRotation(), included.getItemId(), included.getAmount());
					}
				}
			}
		}

		private static void spawnItemInWorld(World world, Vector3i position, Vector3f rotation, String itemId,
		                                     int amount) {
			var gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

			ItemStack itemToSpawn = new ItemStack(itemId, amount);
//			itemToSpawn.getItem().getInteractions().put(InteractionType.Pickup, "pickup_weapon_interaction");

			// Magic numbers to spread items around a bit
			Holder<EntityStore> itemEntityHolder = ItemComponent.generateItemDrop(
					world.getEntityStore().getStore(),
					itemToSpawn,
					position.toVector3d().clone(),
					rotation.clone(),
					0,
					0,
					0
			);

			if (itemEntityHolder == null) {
				return;
			}

			itemEntityHolder.removeComponent(DespawnComponent.getComponentType());

			ItemComponent itemComponent = itemEntityHolder.getComponent(ItemComponent.getComponentType());
			if (itemComponent != null) {
				itemComponent.setPickupDelay(0.5F);
			}

			Ref<EntityStore> item = world.getEntityStore().getStore().addEntity(itemEntityHolder, AddReason.SPAWN);
		}

		public static boolean chance(int probability) {
			if (probability < 0 || probability > 100) {
				throw new IllegalArgumentException("Probability must be between 0 and 100");
			}

			// nextInt(100) returns a value from 0 (inclusive) to 100 (exclusive)
			return ThreadLocalRandom.current().nextInt(100) < probability;
		}

		public static void spawnLootForWorld(World world) {
			world.execute(() -> {
				if (world.getWorldConfig().getDisplayName() == null) return;
				String worldName = getWorldNameForInstance(world);
				InstanceConfig instanceConfig =
						TroubleInTrorkTownPlugin.instanceConfig.get(worldName).get();

				for (LootSpawnPoint lootSpawnPoint : instanceConfig.getLootSpawnPoints()) {
					if (!chance(lootSpawnPoint.getProbability())) {
						continue;
					}

					addLoot(world, lootSpawnPoint);
				}
			});
		}

		protected void executeSync(@NonNullDecl CommandContext ctx) {
			Ref<EntityStore> reference = ctx.senderAsPlayerRef();

			if (reference == null || !reference.isValid()) {
				ctx.sendMessage(Message.raw("You can't use this command from the console."));
				return;
			}

			var world = reference.getStore().getExternalData().getWorld();
			spawnLootForWorld(world);
		}

		@NonNullDecl
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
			return CompletableFuture.runAsync(() -> executeSync(commandContext));
		}
	}

	public static class LootAddSpawnPositionCommand extends AbstractAsyncCommand {

		private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

		OptionalArg<Integer> probabilityArg = this.withOptionalArg("probability", "Define the probability of this lootbox" +
				" spawning", ArgTypes.INTEGER);

		public LootAddSpawnPositionCommand() {
			super("add", "Adds a loot position at the component's current location.");
		}

		protected void executeSync(@NonNullDecl CommandContext ctx) {
			Ref<EntityStore> reference = ctx.senderAsPlayerRef();

			if (reference == null || !reference.isValid()) {
				ctx.sendMessage(Message.raw("You can't use this command from the console."));
				return;
			}

			var world = reference.getStore().getExternalData().getWorld();
			world.execute(() -> {
				var transformComponent = reference.getStore().getComponent(reference, TransformComponent.getComponentType());
				if (transformComponent == null) {
					ctx.sendMessage(Message.raw("An error occurred while trying to access your component information."));
					return;
				}

				// Here you would add the logic to actually store the loot position
				LootSpawnPoint lootSpawnPoint = new LootSpawnPoint();
				lootSpawnPoint.setSpawnPoint(new SpawnPoint(
						transformComponent.getPosition().clone(),
						transformComponent.getRotation().clone()
				));

				if (probabilityArg.get(ctx) != null) {
					lootSpawnPoint.setProbability(probabilityArg.get(ctx));
				}

				var instanceConfig = WorldAccessors.getWorldInstanceConfig(world);
				LootSpawnPoint[] lootSpawnPoints = instanceConfig.getLootSpawnPoints();
				if (lootSpawnPoints == null) {
					lootSpawnPoints = new LootSpawnPoint[0];
				}

				LootSpawnPoint[] newLootSpawnPoints = new LootSpawnPoint[lootSpawnPoints.length + 1];
				System.arraycopy(lootSpawnPoints, 0, newLootSpawnPoints, 0, lootSpawnPoints.length);
				newLootSpawnPoints[lootSpawnPoints.length] = lootSpawnPoint;
				instanceConfig.setLootSpawnPoints(newLootSpawnPoints);

				WorldAccessors.getWorldInstanceConfigFile(world).ifPresent(Config::save);
				ctx.sendMessage(Message.raw("Loot position added at your current location."));
			});
		}

		@NonNullDecl
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
			return CompletableFuture.runAsync(() -> executeSync(commandContext));
		}
	}

}
