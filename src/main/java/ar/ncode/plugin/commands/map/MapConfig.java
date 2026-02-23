package ar.ncode.plugin.commands.map;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.accessors.WorldAccessors;
import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static ar.ncode.plugin.commands.map.CrudMapCommand.copyFiles;
import static ar.ncode.plugin.commands.map.CrudMapCommand.reloadMaps;
import static ar.ncode.plugin.model.CustomPermissions.TTT_MAP_CONFIG_SPAWN_POINT;
import static ar.ncode.plugin.model.CustomPermissions.TTT_MAP_SAVE;

public class MapConfig extends AbstractCommandCollection {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public MapConfig() {
        super("config", "Commands related to map management");
        addSubCommand(new SetSpawnPointMapCommand());
    }

    public static class SaveMapCommand extends AbstractAsyncCommand {

        public SaveMapCommand() {
            super("save", "Saves the changes made to the map");
            requirePermission(TTT_MAP_SAVE);
        }

        protected void executeSync(@Nonnull CommandContext ctx) throws Exception {
            World currentWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
            if (currentWorld == null) {
                ctx.sendMessage(Message.raw("Error obtaining world"));
                return;
            }

            String mapName = WorldAccessors.getWorldNameForInstance(currentWorld);
            if (mapName == null) {
                ctx.sendMessage(Message.raw("Error obtaining world name"));
                return;
            }

            Path mapFolder = TroubleInTrorkTownPlugin.instance.templatesPath.resolve(mapName, "chunks");

            Path maps = Paths.get("universe/worlds");
            Path currentChunksFolder = maps.resolve(currentWorld.getName(), "chunks");

            try (Stream<Path> stream = Files.walk(currentChunksFolder)) {
                copyFiles(stream, currentChunksFolder, mapFolder);
            }

            reloadMaps();
            ctx.sendMessage(Message.raw("Changes made to map " + mapName + " have been saved."));
            LOGGER.atInfo().log("dUpdated a map. Map folder %s - Chunks folder: %s", mapFolder.toString(), currentChunksFolder.toString());
        }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
            return CompletableFuture.runAsync(() -> {
                try {
                    executeSync(ctx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static class SetSpawnPointMapCommand extends AbstractAsyncCommand {

        public SetSpawnPointMapCommand() {
            super("spawn-point", "Saves the changes made to the map");
            requirePermission(TTT_MAP_CONFIG_SPAWN_POINT);
        }

        protected void executeSync(@Nonnull CommandContext ctx) throws Exception {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("Command has to be send by player"));
                return;
            }

            World currentWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
            if (currentWorld == null) {
                ctx.sendMessage(Message.raw("Error obtaining world"));
                return;
            }

            String mapName = WorldAccessors.getWorldNameForInstance(currentWorld);
            if (mapName == null) {
                ctx.sendMessage(Message.raw("Error obtaining world name"));
                return;
            }

            Path mapFolder = TroubleInTrorkTownPlugin.instance.templatesPath.resolve(mapName, "instance.bson");
            BsonDocument doc = BsonUtil.readDocument(mapFolder).join();
            ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
            WorldConfig worldConfig = WorldConfig.CODEC.decode(doc, extraInfo);

            if (worldConfig == null) {
                ctx.sendMessage(Message.raw("Error obtaining world config"));
                return;
            }

            World world = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
            world.execute(() -> {
                Ref<EntityStore> ref = ctx.senderAsPlayerRef();
                if (ref == null) {
                    ctx.sendMessage(Message.raw("Error obtaining player"));
                    return;
                }

                var transform = ref.getStore().getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    ctx.sendMessage(Message.raw("Error obtaining player position"));
                    return;
                }

                worldConfig.setSpawnProvider(new GlobalSpawnProvider(transform.getTransform().clone()));

                var bsonDocument = WorldConfig.CODEC.encode(worldConfig, extraInfo);
                try {
                    BsonUtil.writeDocument(mapFolder, bsonDocument, true).get();
                } catch (Exception e) {
                    ctx.sendMessage(Message.raw("Error saving spawn position"));
                    return;
                }

                ctx.sendMessage(Message.raw("Spawn position saved."));
            });
        }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
            return CompletableFuture.runAsync(() -> {
                try {
                    executeSync(ctx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

}
