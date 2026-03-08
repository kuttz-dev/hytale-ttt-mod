package ar.ncode.plugin.ecs.commands.loot;

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
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
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
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.weaponsConfig;
import static ar.ncode.plugin.accessors.WorldAccessors.saveInstanceConfig;
import static ar.ncode.plugin.ecs.system.DeathSystem.findEmptyPlaceNearPosition;

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
            LOGGER.atFine().log("Adding loot for loot spawn point at position: %s with probability: %d%%", lootSpawnPoint.getSpawnPoint().getPosition(), lootSpawnPoint.getProbability());
            for (String lootTableId : lootSpawnPoint.getLootTables()) {
                LootTable lootTable = weaponsConfig.get().getLootTableById(lootTableId);
                if (lootTable == null) {
                    continue;
                }

                LOGGER.atFine().log("Processing loot table with id: %s and max items: %d%%", lootTableId, lootTable.getMaxItems());

                List<LootItem> items = Arrays.asList(lootTable.getItems());
                Collections.shuffle(items);

                int spawnedItems = 0;
                for (LootItem item : items) {
                    if (lootTable.getMaxItems() != null && spawnedItems == lootTable.getMaxItems()) {
                        LOGGER.atFine().log("Max items reached for loot table with id: %s. Stopping item spawns for this table.", lootTableId);
                        break;
                    }

                    if (!chance(item.getProbability())) {
                        continue;
                    }

                    LOGGER.atFine().log("Spawning item with id: %s and amount: %d for loot table with id: %s", item.getItemId(), item.getAmount(), lootTableId);
                    Vector3d position = lootSpawnPoint.getSpawnPoint().getPosition().clone();
                    position.x += ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
                    position.z += ThreadLocalRandom.current().nextDouble(-2.0, 2.0);

                    Vector3i emptyPosition = findEmptyPlaceNearPosition(world, position, 3);

                    if (emptyPosition == null) {
                        continue;
                    }

                    spawnItemInWorld(world, emptyPosition, lootSpawnPoint.getSpawnPoint().getRotation(), item.getItemId(), item.getAmount());
                    spawnedItems++;

                    for (IncludedLootItem included : item.getIncludes()) {
                        LOGGER.atFine().log("Spawning included item with id: %s and amount: %d included in item with id: %s for loot table with id: %s", included.getItemId(), included.getAmount(), item.getItemId(), lootTableId);
                        spawnItemInWorld(world, emptyPosition, lootSpawnPoint.getSpawnPoint().getRotation(), included.getItemId(), included.getAmount());
                    }
                }
            }
        }

        private static void spawnItemInWorld(
                World world, Vector3i position, Vector3f rotation, String itemId, int amount
        ) {
            var itemAsset = Item.getAssetMap().getAsset(itemId);
            if (itemAsset == null) {
                return;
            }

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
            if (world.getWorldConfig().getDisplayName() == null) return;
            UUID worldUUID = world.getWorldConfig().getUuid();
            InstanceConfig instanceConfig =
                    TroubleInTrorkTownPlugin.instanceConfigs.get(worldUUID).get();

            for (LootSpawnPoint lootSpawnPoint : instanceConfig.getLootSpawnPoints()) {
                if (!chance(lootSpawnPoint.getProbability())) {
                    continue;
                }

                addLoot(world, lootSpawnPoint);
            }
        }

        protected void executeSync(@NonNullDecl CommandContext ctx) {
            Ref<EntityStore> reference = ctx.senderAsPlayerRef();

            if (reference == null || !reference.isValid()) {
                ctx.sendMessage(Message.raw("You can't use this command from the console."));
                return;
            }

            var world = reference.getStore().getExternalData().getWorld();
            world.execute(() -> spawnLootForWorld(world));
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
                saveInstanceConfig(ctx, world);
            });
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
            return CompletableFuture.runAsync(() -> executeSync(commandContext));
        }
    }

}
